package com.android.purebilibili.core.ui.transition

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.navigation.isVideoCardReturnTargetRoute
import kotlin.math.roundToInt

private const val VIDEO_CARD_TRANSITION_MAX_BLUR_RADIUS_PX = 36f
private const val VIDEO_CARD_TRANSITION_BLUR_QUANTUM_PX = 2f
private const val VIDEO_CARD_TRANSITION_MAX_SCRIM_ALPHA = 0.22f
private const val VIDEO_CARD_TRANSITION_RETURN_SCRIM_ALPHA = 0.10f
private const val VIDEO_CARD_TRANSITION_MAX_CONTENT_SCALE_REDUCTION = 0.045f

// 开场背景虚化时长与共享元素 morph(标准 460ms)大致同步，
// 略短以便卡片落位前背景已完成虚化，避免 160ms 内 blur 一闪就到位的突兀感。
internal const val VIDEO_CARD_TRANSITION_BACKGROUND_FORWARD_DURATION_MS = 300
internal const val VIDEO_CARD_TRANSITION_BACKGROUND_RETURN_DURATION_MS = 460
internal const val VIDEO_CARD_TRANSITION_BACKGROUND_CANCEL_DURATION_MS = 160

internal enum class VideoCardTransitionBackgroundPhase {
    IDLE,
    OPENING,
    HELD,
    RETURNING
}

internal data class VideoCardTransitionBackgroundFrame(
    val blurRadiusPx: Float,
    val scrimAlpha: Float,
    val contentScale: Float
)

internal data class VideoCardTransitionBackgroundState(
    val progressProvider: () -> Float = { 0f },
    val phaseProvider: () -> VideoCardTransitionBackgroundPhase = {
        VideoCardTransitionBackgroundPhase.IDLE
    },
    val motionTierProvider: () -> MotionTier = { MotionTier.Normal }
)

internal val LocalVideoCardTransitionBackgroundState = compositionLocalOf {
    VideoCardTransitionBackgroundState()
}

internal fun resolveVideoCardTransitionBackgroundFrame(
    progress: Float,
    phase: VideoCardTransitionBackgroundPhase,
    motionTier: MotionTier = MotionTier.Normal,
    sdkInt: Int = Build.VERSION.SDK_INT
): VideoCardTransitionBackgroundFrame {
    val clamped = progress.coerceIn(0f, 1f)
    val blurStrength = resolveVideoCardTransitionBlurStrength(clamped)
    // 低端/省电/无障碍减弱动画(Reduced)时跳过整帧 GPU 实时模糊，
    // 仅保留 scrim + 轻微缩放作为回退，避免全屏 RenderEffect 的开销。
    val rawBlurRadiusPx = if (
        phase != VideoCardTransitionBackgroundPhase.IDLE &&
        motionTier != MotionTier.Reduced &&
        sdkInt >= Build.VERSION_CODES.S
    ) {
        VIDEO_CARD_TRANSITION_MAX_BLUR_RADIUS_PX * blurStrength
    } else {
        0f
    }

    return VideoCardTransitionBackgroundFrame(
        blurRadiusPx = quantizeVideoCardTransitionBlurRadius(rawBlurRadiusPx),
        scrimAlpha = when (phase) {
            VideoCardTransitionBackgroundPhase.OPENING ->
                VIDEO_CARD_TRANSITION_MAX_SCRIM_ALPHA * clamped
            VideoCardTransitionBackgroundPhase.RETURNING ->
                VIDEO_CARD_TRANSITION_RETURN_SCRIM_ALPHA * blurStrength
            VideoCardTransitionBackgroundPhase.IDLE,
            VideoCardTransitionBackgroundPhase.HELD -> 0f
        },
        contentScale = when (phase) {
            VideoCardTransitionBackgroundPhase.OPENING ->
                1f - VIDEO_CARD_TRANSITION_MAX_CONTENT_SCALE_REDUCTION * clamped
            VideoCardTransitionBackgroundPhase.IDLE,
            VideoCardTransitionBackgroundPhase.HELD,
            VideoCardTransitionBackgroundPhase.RETURNING -> 1f
        }
    )
}

internal fun shouldApplyVideoCardTransitionBackgroundToRoute(
    entryRoute: String?,
    sourceRoute: String?,
    activeMainHostRoute: String?
): Boolean {
    val normalizedEntryRoute = normalizeVideoCardTransitionRoute(entryRoute) ?: return false
    val normalizedSourceRoute = normalizeVideoCardTransitionRoute(sourceRoute) ?: return false
    if (!isVideoCardReturnTargetRoute(normalizedSourceRoute)) return false
    if (normalizedEntryRoute == normalizedSourceRoute) return true
    return normalizedEntryRoute == "main_host" &&
        normalizeVideoCardTransitionRoute(activeMainHostRoute) == normalizedSourceRoute
}

/**
 * 每帧内 graphicsLayer 与 drawWithContent 会先后读取同一 frame，
 * 用一个基于 (progress, phase) 的一次性缓存避免同帧重复计算纯函数。
 */
private class VideoCardTransitionBackgroundFrameCache {
    private var lastProgress = Float.NaN
    private var lastPhase: VideoCardTransitionBackgroundPhase? = null
    private var lastMotionTier: MotionTier? = null
    private var cached = VideoCardTransitionBackgroundFrame(
        blurRadiusPx = 0f,
        scrimAlpha = 0f,
        contentScale = 1f
    )

    fun resolve(
        progress: Float,
        phase: VideoCardTransitionBackgroundPhase,
        motionTier: MotionTier
    ): VideoCardTransitionBackgroundFrame {
        if (progress != lastProgress || phase != lastPhase || motionTier != lastMotionTier) {
            lastProgress = progress
            lastPhase = phase
            lastMotionTier = motionTier
            cached = resolveVideoCardTransitionBackgroundFrame(progress, phase, motionTier)
        }
        return cached
    }
}

internal fun Modifier.videoCardTransitionBackgroundEffect(
    progressProvider: () -> Float,
    phaseProvider: () -> VideoCardTransitionBackgroundPhase,
    motionTierProvider: () -> MotionTier = { MotionTier.Normal }
): Modifier {
    val frameCache = VideoCardTransitionBackgroundFrameCache()
    return graphicsLayer {
        val frame = frameCache.resolve(progressProvider(), phaseProvider(), motionTierProvider())
        scaleX = frame.contentScale
        scaleY = frame.contentScale
        renderEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && frame.blurRadiusPx > 0.01f) {
            RenderEffect
                .createBlurEffect(
                    frame.blurRadiusPx,
                    frame.blurRadiusPx,
                    Shader.TileMode.CLAMP
                )
                .asComposeRenderEffect()
        } else {
            null
        }
    }.drawWithContent {
        drawContent()
        val frame = frameCache.resolve(progressProvider(), phaseProvider(), motionTierProvider())
        if (frame.scrimAlpha > 0.001f) {
            drawRect(Color.Black.copy(alpha = frame.scrimAlpha))
        }
    }
}

private fun resolveVideoCardTransitionBlurStrength(progress: Float): Float {
    val clamped = progress.coerceIn(0f, 1f)
    // 模糊要比位移/缩放更早进入可感知区，否则 160ms 内肉眼很难看出背景虚化。
    return 1f - (1f - clamped) * (1f - clamped)
}

private fun quantizeVideoCardTransitionBlurRadius(radiusPx: Float): Float {
    if (radiusPx <= 0f) return 0f
    return ((radiusPx / VIDEO_CARD_TRANSITION_BLUR_QUANTUM_PX).roundToInt() *
        VIDEO_CARD_TRANSITION_BLUR_QUANTUM_PX)
        .coerceIn(0f, VIDEO_CARD_TRANSITION_MAX_BLUR_RADIUS_PX)
}

private fun normalizeVideoCardTransitionRoute(route: String?): String? {
    val normalized = route?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return if (normalized.startsWith("home?category=")) {
        "home"
    } else {
        normalized.substringBefore("?")
    }
}

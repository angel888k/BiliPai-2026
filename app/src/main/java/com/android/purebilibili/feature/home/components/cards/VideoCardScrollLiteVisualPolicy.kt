package com.android.purebilibili.feature.home.components.cards

import com.android.purebilibili.core.ui.transition.VideoCardTransitionBackgroundPhase

internal data class VideoCardScrollLiteVisualPolicy(
    val coverShadowElevationDp: Float,
    val showCoverGradientMask: Boolean,
    val showHistoryProgressBar: Boolean,
    val showCompactStatsOnCover: Boolean,
    val showSecondaryStatsRow: Boolean
)

internal fun resolveVideoCardScrollLiteVisualPolicy(
    scrollLiteModeEnabled: Boolean,
    compactStatsOnCover: Boolean
): VideoCardScrollLiteVisualPolicy {
    if (scrollLiteModeEnabled) {
        return VideoCardScrollLiteVisualPolicy(
            coverShadowElevationDp = 0f,
            showCoverGradientMask = false,
            showHistoryProgressBar = false,
            showCompactStatsOnCover = compactStatsOnCover,
            showSecondaryStatsRow = !compactStatsOnCover
        )
    }

    return VideoCardScrollLiteVisualPolicy(
        coverShadowElevationDp = 0f,
        // 统计信息移到封面外时也不需要暗渐变；保持静止和滚动状态一致，避免整批封面明暗闪烁。
        showCoverGradientMask = false,
        showHistoryProgressBar = true,
        showCompactStatsOnCover = compactStatsOnCover,
        showSecondaryStatsRow = !compactStatsOnCover
    )
}

internal fun shouldEnableVideoCardCoverCrossfade(
    isScrollInProgress: Boolean,
    isReturningFromDetail: Boolean,
    useCoverSharedBounds: Boolean,
    isSharedReturnTarget: Boolean
): Boolean {
    if (isScrollInProgress) return false
    // 返回目标封面由 sharedBounds 承接播放器画面，Coil 淡入会在落位后再次改变亮度导致闪烁。
    return !(isReturningFromDetail && useCoverSharedBounds && isSharedReturnTarget)
}

/**
 * 首页卡片 → 详情页 CARD_SHELL morph 期间，源卡片封面应让位给 overlay，
 * 避免「列表封面 + shared overlay + 冻结景深层」三重叠。
 *
 * 隐藏时机：
 * - OPENING：尽早藏封面，便于景深层首帧 record 不含清晰封面（减冻结重影）
 * - shared transition 进行中：overlay 独占像素
 * - 预测返回手势进行中：跟手 morph 时列表封面不抢戏
 *
 * morph 结束后（isTransitionActive=false 且非 OPENING）立即显示封面；
 * 返回目标已关 Coil crossfade，避免落位二次淡入闪烁。
 */
internal fun shouldHideHomeCardCoverDuringShellMorph(
    useCardContainerSharedBounds: Boolean,
    isSharedMorphSourceCard: Boolean,
    isReturningFromDetail: Boolean,
    isSharedTransitionActive: Boolean,
    transitionBackgroundPhase: VideoCardTransitionBackgroundPhase,
    isVideoCardReturnGestureInProgress: Boolean,
): Boolean {
    if (!useCardContainerSharedBounds || !isSharedMorphSourceCard) {
        return false
    }
    // OPENING 优先：不依赖 isTransitionActive 首帧时序，保证冻结 record 时封面已透明。
    if (transitionBackgroundPhase == VideoCardTransitionBackgroundPhase.OPENING) {
        return true
    }
    if (isSharedTransitionActive) {
        return true
    }
    if (isVideoCardReturnGestureInProgress) {
        return true
    }
    // RETURNING 且仍带 returning session 时，若 shared 已结束则应显示封面承接落位。
    // isReturningFromDetail 单独不再强制显示（旧逻辑在返回全程露封面导致重影）。
    @Suppress("UNUSED_PARAMETER")
    val unusedReturning = isReturningFromDetail
    return false
}

internal data class StoryVideoCardScrollLiteVisualPolicy(
    val coverShadowElevationDp: Float,
    val showSecondaryStatsRow: Boolean
)

internal fun resolveStoryVideoCardScrollLiteVisualPolicy(
    scrollLiteModeEnabled: Boolean
): StoryVideoCardScrollLiteVisualPolicy {
    return if (scrollLiteModeEnabled) {
        StoryVideoCardScrollLiteVisualPolicy(
            coverShadowElevationDp = 0f,
            showSecondaryStatsRow = true
        )
    } else {
        StoryVideoCardScrollLiteVisualPolicy(
            coverShadowElevationDp = 0f,
            showSecondaryStatsRow = true
        )
    }
}

package com.android.purebilibili.feature.home.components.cards

import androidx.compose.ui.graphics.Color
import com.android.purebilibili.core.ui.transition.VideoCardTransitionBackgroundPhase
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoCardScrollLiteVisualPolicyTest {

    @Test
    fun `normal mode removes cover gradient behind compact stats`() {
        val policy = resolveVideoCardScrollLiteVisualPolicy(
            scrollLiteModeEnabled = false,
            compactStatsOnCover = true
        )

        assertEquals(0f, policy.coverShadowElevationDp, 0.0001f)
        assertFalse(policy.showCoverGradientMask)
        assertTrue(policy.showHistoryProgressBar)
        assertTrue(policy.showCompactStatsOnCover)
        assertFalse(policy.showSecondaryStatsRow)
    }

    @Test
    fun `normal mode removes cover gradient when stats move below cover`() {
        val policy = resolveVideoCardScrollLiteVisualPolicy(
            scrollLiteModeEnabled = false,
            compactStatsOnCover = false
        )

        assertEquals(0f, policy.coverShadowElevationDp, 0.0001f)
        assertFalse(policy.showCoverGradientMask)
        assertTrue(policy.showHistoryProgressBar)
        assertFalse(policy.showCompactStatsOnCover)
        assertTrue(policy.showSecondaryStatsRow)
    }

    @Test
    fun `scroll lite mode keeps stats without cover shadow`() {
        val policy = resolveVideoCardScrollLiteVisualPolicy(
            scrollLiteModeEnabled = true,
            compactStatsOnCover = true
        )

        assertEquals(0f, policy.coverShadowElevationDp, 0.0001f)
        assertFalse(policy.showCoverGradientMask)
        assertFalse(policy.showHistoryProgressBar)
        assertTrue(policy.showCompactStatsOnCover)
        assertFalse(policy.showSecondaryStatsRow)
    }

    @Test
    fun `scroll lite mode keeps secondary row when cover stats are disabled`() {
        val policy = resolveVideoCardScrollLiteVisualPolicy(
            scrollLiteModeEnabled = true,
            compactStatsOnCover = false
        )

        assertEquals(0f, policy.coverShadowElevationDp, 0.0001f)
        assertFalse(policy.showCompactStatsOnCover)
        assertTrue(policy.showSecondaryStatsRow)
    }

    @Test
    fun `normal mode keeps story card secondary stats row`() {
        val policy = resolveStoryVideoCardScrollLiteVisualPolicy(
            scrollLiteModeEnabled = false
        )

        assertEquals(0f, policy.coverShadowElevationDp, 0.0001f)
        assertTrue(policy.showSecondaryStatsRow)
    }

    @Test
    fun `scroll lite mode removes story card shadows but keeps stats`() {
        val policy = resolveStoryVideoCardScrollLiteVisualPolicy(
            scrollLiteModeEnabled = true
        )

        assertEquals(0f, policy.coverShadowElevationDp, 0.0001f)
        assertTrue(policy.showSecondaryStatsRow)
    }

    @Test
    fun `home video card variants do not attach shadow modifiers`() {
        listOf(
            "VideoCard.kt",
            "StoryVideoCard.kt",
            "GlassVideoCard.kt",
            "CinematicVideoCard.kt"
        ).forEach { fileName ->
            val source = File("src/main/java/com/android/purebilibili/feature/home/components/cards/$fileName")
                .readText()

            assertFalse("$fileName should not draw video cover shadows", source.contains(".shadow("))
        }
    }

    @Test
    fun `elegant video card clips static cover container to cover shape`() {
        val source = File("src/main/java/com/android/purebilibili/feature/home/components/cards/VideoCard.kt")
            .readText()

        assertTrue(
            "首页视频封面本体必须裁剪 coverShape，不能只依赖 sharedBounds 的 overlay 裁剪。",
            source.contains(
                ".aspectRatio(coverAspectRatio)\n" +
                    "                .clip(coverShape)"
            )
        )
    }

    @Test
    fun `cover stats do not claim separate shared bounds when shell owns morph`() {
        // CARD_SHELL 容器已接管共享元素；封面上的播放量/弹幕不再挂独立 sharedBounds，
        // 避免与 shell morph / 冻结景深叠层抢 key。
        val source = File("src/main/java/com/android/purebilibili/feature/home/components/cards/VideoCard.kt")
            .readText()
        val coverStatsBlock = source
            .substringAfter("if (scrollLitePolicy.showCompactStatsOnCover) {")
            .substringBefore("//  时长标签")

        assertTrue(coverStatsBlock.contains("BoxWithConstraints("))
        assertFalse(coverStatsBlock.contains("videoViewsSharedElementKey"))
        assertFalse(coverStatsBlock.contains("sharedBounds("))
        assertTrue(source.contains("videoCardShellSharedBoundsOrEmpty("))
    }

    @Test
    fun `return target cover disables crossfade during shared transition`() {
        assertFalse(
            shouldEnableVideoCardCoverCrossfade(
                isScrollInProgress = false,
                isReturningFromDetail = true,
                useCoverSharedBounds = true,
                isSharedReturnTarget = true
            )
        )
    }

    @Test
    fun `non return target cover keeps crossfade`() {
        assertTrue(
            shouldEnableVideoCardCoverCrossfade(
                isScrollInProgress = false,
                isReturningFromDetail = true,
                useCoverSharedBounds = true,
                isSharedReturnTarget = false
            )
        )
        assertTrue(
            shouldEnableVideoCardCoverCrossfade(
                isScrollInProgress = false,
                isReturningFromDetail = false,
                useCoverSharedBounds = true,
                isSharedReturnTarget = true
            )
        )
    }

    @Test
    fun `scrolling disables cover crossfade`() {
        assertFalse(
            shouldEnableVideoCardCoverCrossfade(
                isScrollInProgress = true,
                isReturningFromDetail = false,
                useCoverSharedBounds = false,
                isSharedReturnTarget = false
            )
        )
    }

    @Test
    fun `home video metadata uses on surface colors for readable up and publish text`() {
        val onSurface = Color(0xFF1D1B20)
        val colors = resolveHomeVideoCardMetadataColors(onSurface)

        assertEquals(onSurface, colors.upNameColor)
        assertEquals(onSurface.copy(alpha = 0.82f), colors.upMetaColor)
        assertEquals(onSurface.copy(alpha = 0.68f), colors.upBadgeTextColor)
        assertEquals(onSurface.copy(alpha = 0.10f), colors.upBadgeBackgroundColor)
        assertEquals(onSurface.copy(alpha = 0.72f), colors.publishTimeColor)
    }

    @Test
    fun `home video card reserves followed badge height to align publish rows`() {
        val cardSource = File("src/main/java/com/android/purebilibili/feature/home/components/cards/VideoCard.kt")
            .readText()
        val upBadgeSource = File("src/main/java/com/android/purebilibili/core/ui/components/UpBadgeName.kt")
            .readText()

        assertTrue(cardSource.contains("trailingSlotMinHeight = 20.dp"))
        assertTrue(upBadgeSource.contains(".heightIn(min = trailingSlotMinHeight)"))
    }

    @Test
    fun homeCardCover_hiddenForOpeningFreezeAndActiveShellMorph() {
        // OPENING：不依赖 isTransitionActive，保证冻结层 record 时封面已透明
        assertTrue(
            shouldHideHomeCardCoverDuringShellMorph(
                useCardContainerSharedBounds = true,
                isSharedMorphSourceCard = true,
                isReturningFromDetail = false,
                isSharedTransitionActive = false,
                transitionBackgroundPhase = VideoCardTransitionBackgroundPhase.OPENING,
                isVideoCardReturnGestureInProgress = false,
            )
        )
        // shared morph 进行中（含返回）：overlay 独占
        assertTrue(
            shouldHideHomeCardCoverDuringShellMorph(
                useCardContainerSharedBounds = true,
                isSharedMorphSourceCard = true,
                isReturningFromDetail = true,
                isSharedTransitionActive = true,
                transitionBackgroundPhase = VideoCardTransitionBackgroundPhase.RETURNING,
                isVideoCardReturnGestureInProgress = false,
            )
        )
        // 预测返回手势
        assertTrue(
            shouldHideHomeCardCoverDuringShellMorph(
                useCardContainerSharedBounds = true,
                isSharedMorphSourceCard = true,
                isReturningFromDetail = false,
                isSharedTransitionActive = false,
                transitionBackgroundPhase = VideoCardTransitionBackgroundPhase.HELD,
                isVideoCardReturnGestureInProgress = true,
            )
        )
        // morph 结束：显示封面承接落位
        assertFalse(
            shouldHideHomeCardCoverDuringShellMorph(
                useCardContainerSharedBounds = true,
                isSharedMorphSourceCard = true,
                isReturningFromDetail = true,
                isSharedTransitionActive = false,
                transitionBackgroundPhase = VideoCardTransitionBackgroundPhase.IDLE,
                isVideoCardReturnGestureInProgress = false,
            )
        )
        // 非源卡 / 未启用 shell：不隐藏
        assertFalse(
            shouldHideHomeCardCoverDuringShellMorph(
                useCardContainerSharedBounds = true,
                isSharedMorphSourceCard = false,
                isReturningFromDetail = false,
                isSharedTransitionActive = true,
                transitionBackgroundPhase = VideoCardTransitionBackgroundPhase.OPENING,
                isVideoCardReturnGestureInProgress = false,
            )
        )
    }
}

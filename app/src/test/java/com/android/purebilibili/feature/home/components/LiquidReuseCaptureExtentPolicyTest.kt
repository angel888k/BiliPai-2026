package com.android.purebilibili.feature.home.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LiquidReuseCaptureExtentPolicyTest {

    @Test
    fun captureExtentAddsBleedOnBothSides() {
        assertEquals(
            100f + LIQUID_REUSE_LOCAL_SAMPLING_BLEED_DP * 2f,
            resolveLiquidReuseCaptureExtentDp(controlSizeDp = 100f),
            absoluteTolerance = 0.001f
        )
    }

    @Test
    fun captureExtentCoversFullWidthCapsuleDragScale() {
        // 88/56 scale needs ~28.5% half-width margin on each side of a 100dp pill → ~14.25dp.
        // Bleed is 40dp so a full-width capsule still stays inside export after drag scale.
        val pillWidth = 100f
        val halfGrowth = pillWidth * ((88f / 56f) - 1f) / 2f
        assertTrue(LIQUID_REUSE_LOCAL_SAMPLING_BLEED_DP > halfGrowth)
        assertTrue(
            resolveLiquidReuseCaptureExtentDp(pillWidth) >=
                pillWidth + halfGrowth * 2f
        )
    }

    @Test
    fun zeroControlSizeStillYieldsNonNegativeExtent() {
        assertEquals(
            LIQUID_REUSE_LOCAL_SAMPLING_BLEED_DP * 2f,
            resolveLiquidReuseCaptureExtentDp(controlSizeDp = 0f),
            absoluteTolerance = 0.001f
        )
    }
}

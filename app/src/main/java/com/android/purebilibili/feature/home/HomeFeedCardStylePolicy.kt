package com.android.purebilibili.feature.home

import com.android.purebilibili.core.store.HomeFeedCardStyle

internal data class HomeFeedCardLayout(
    val coverAspectRatio: Float,
    val outerPaddingDp: Int,
    val itemSpacingDp: Int,
    val storyCardHorizontalPaddingDp: Int
)

internal fun resolveHomeFeedCardLayout(style: HomeFeedCardStyle): HomeFeedCardLayout {
    return when (style) {
        HomeFeedCardStyle.CURRENT -> HomeFeedCardLayout(
            coverAspectRatio = 16f / 10f,
            outerPaddingDp = 8,
            itemSpacingDp = 8,
            storyCardHorizontalPaddingDp = 16
        )

        HomeFeedCardStyle.OFFICIAL -> HomeFeedCardLayout(
            coverAspectRatio = 16f / 9f,
            outerPaddingDp = 4,
            itemSpacingDp = 6,
            storyCardHorizontalPaddingDp = 0
        )
    }
}

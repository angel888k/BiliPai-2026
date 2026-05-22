package com.android.purebilibili.feature.video.share

import android.content.Intent

internal const val WECHAT_PACKAGE_NAME = "com.tencent.mm"
internal const val QQ_PACKAGE_NAME = "com.tencent.mobileqq"

internal data class VideoSharePayload(
    val title: String,
    val bvid: String,
    val url: String,
    val text: String
)

internal enum class VideoShareTarget(val packageName: String?) {
    WECHAT(WECHAT_PACKAGE_NAME),
    QQ(QQ_PACKAGE_NAME),
    COPY_LINK(null),
    MORE(null)
}

internal fun buildVideoSharePayload(
    title: String,
    bvid: String
): VideoSharePayload {
    val cleanTitle = title.trim()
    val cleanBvid = bvid.trim()
    val fallbackTitle = cleanTitle.ifBlank { cleanBvid }
    val url = "https://www.bilibili.com/video/$cleanBvid"
    return VideoSharePayload(
        title = fallbackTitle,
        bvid = cleanBvid,
        url = url,
        text = "【$fallbackTitle】\n$url"
    )
}

internal fun buildVideoShareIntent(payload: VideoSharePayload): Intent {
    return Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, payload.title)
        putExtra(Intent.EXTRA_TEXT, payload.text)
    }
}

internal fun buildTargetedShareIntent(
    payload: VideoSharePayload,
    packageName: String
): Intent {
    return buildVideoShareIntent(payload).apply {
        setPackage(packageName)
    }
}

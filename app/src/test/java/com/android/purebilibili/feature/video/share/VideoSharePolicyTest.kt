package com.android.purebilibili.feature.video.share

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VideoSharePolicyTest {

    @Test
    fun buildVideoSharePayload_outputsTitleUrlAndText() {
        val payload = buildVideoSharePayload(
            title = " Uzi回应送老婆贵价项链 ",
            bvid = " BV1aRG46aEnz "
        )

        assertEquals("Uzi回应送老婆贵价项链", payload.title)
        assertEquals("BV1aRG46aEnz", payload.bvid)
        assertEquals("https://www.bilibili.com/video/BV1aRG46aEnz", payload.url)
        assertEquals(
            "【Uzi回应送老婆贵价项链】\nhttps://www.bilibili.com/video/BV1aRG46aEnz",
            payload.text
        )
    }

    @Test
    fun videoShareTarget_mapsWechatAndQqPackages() {
        assertEquals("com.tencent.mm", VideoShareTarget.WECHAT.packageName)
        assertEquals("com.tencent.mobileqq", VideoShareTarget.QQ.packageName)
        assertNull(VideoShareTarget.COPY_LINK.packageName)
        assertNull(VideoShareTarget.MORE.packageName)
    }

    @Test
    fun buildVideoShareIntent_usesOrdinaryPlainTextActionSend() {
        val source = loadVideoSharePolicySource()

        assertTrue(
            source.contains("Intent(Intent.ACTION_SEND)"),
            "More share should use ordinary ACTION_SEND"
        )
        assertTrue(
            source.contains("""type = "text/plain""""),
            "Video share intents should use text/plain"
        )
        assertTrue(
            source.contains("putExtra(Intent.EXTRA_SUBJECT, payload.title)"),
            "Video share intents should include title as subject"
        )
        assertTrue(
            source.contains("putExtra(Intent.EXTRA_TEXT, payload.text)"),
            "Video share intents should include unified share text"
        )
    }

    @Test
    fun buildTargetedShareIntent_setsTargetPackage() {
        val source = loadVideoSharePolicySource()

        assertTrue(
            source.contains("setPackage(packageName)"),
            "Targeted WeChat/QQ sharing should constrain the ACTION_SEND intent to the target package"
        )
    }

    private fun loadVideoSharePolicySource(): String {
        val candidates = listOf(
            File("src/main/java/com/android/purebilibili/feature/video/share/VideoSharePolicy.kt"),
            File("app/src/main/java/com/android/purebilibili/feature/video/share/VideoSharePolicy.kt")
        )
        val sourceFile = candidates.firstOrNull { it.exists() }
            ?: error("Cannot locate VideoSharePolicy.kt from ${File(".").absolutePath}")
        return sourceFile.readText()
    }
}

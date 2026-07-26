package com.android.purebilibili.feature.web

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WebViewSessionStructureTest {

    private val screenSource = File(
        "src/main/java/com/android/purebilibili/feature/web/WebViewScreen.kt",
    ).readText()
    private val stateSource = File(
        "src/main/java/com/android/purebilibili/feature/web/WebViewSessionState.kt",
    ).readText()

    @Test
    fun screen_routesTopAndSystemBackThroughTheSameSessionDispatcher() {
        assertTrue(screenSource.contains("IconButton(onClick = { sessionState.dispatchBack(onBack) })"))
        assertTrue(screenSource.contains("isBackEnabled = sessionState.canGoBack"))
        assertTrue(screenSource.contains("sessionState.dispatchBack(onBack)"))
        assertTrue(screenSource.contains("reportPredictiveProgress = false"))
        assertTrue(screenSource.contains("sessionState.cancelBackGesture()"))
    }

    @Test
    fun screen_tracksHistoryAndDoesNotCompareAgainstTheCurrentPageUrl() {
        assertTrue(screenSource.contains("override fun doUpdateVisitedHistory("))
        assertTrue(screenSource.contains("override fun onPageFinished("))
        assertTrue(screenSource.contains("sessionState.requestRoute(webView, url)"))
        assertFalse(screenSource.contains("webView.url != url"))
    }

    @Test
    fun sessionSavesRestoresAndReleasesWebViewInOrder() {
        assertTrue(stateSource.contains("webView.restoreState(stateToRestore)"))
        assertTrue(stateSource.contains("webView.saveState(state)"))

        val save = stateSource.indexOf("captureWebViewState(webView)")
        val stop = stateSource.indexOf("webView.stopLoading()")
        val pause = stateSource.indexOf("webView.onPause()")
        val disconnectChrome = stateSource.indexOf("webView.webChromeClient = null")
        val disconnectClient = stateSource.indexOf("webView.webViewClient = RELEASED_WEB_VIEW_CLIENT")
        val removeViews = stateSource.indexOf("webView.removeAllViews()")
        val clearHistory = stateSource.indexOf("webView.clearHistory()")
        val destroy = stateSource.indexOf("webView.destroy()")

        assertTrue(save in 0..<stop)
        assertTrue(stop < pause)
        assertTrue(pause < disconnectChrome)
        assertTrue(disconnectChrome < disconnectClient)
        assertTrue(disconnectClient < removeViews)
        assertTrue(removeViews < clearHistory)
        assertTrue(clearHistory < destroy)
        assertFalse(stateSource.contains("clearCache("))
        assertFalse(stateSource.contains("CookieManager"))
        assertFalse(stateSource.contains("pauseTimers("))
    }
}

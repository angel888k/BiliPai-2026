package com.android.purebilibili.feature.web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WebViewSessionPolicyTest {

    @Test
    fun requestRoute_loadsEachRouteInputOnlyOnce() {
        val policy = WebViewSessionPolicy()

        assertEquals(WebViewRouteRequest.LOAD, policy.requestRoute("https://example.com/entry"))
        assertEquals(
            WebViewRouteRequest.KEEP_CURRENT_PAGE,
            policy.requestRoute("https://example.com/entry"),
        )
        assertEquals(WebViewRouteRequest.LOAD, policy.requestRoute("https://example.com/other"))
    }

    @Test
    fun historyChanges_doNotChangeLastRequestedRoute() {
        val policy = WebViewSessionPolicy()
        policy.requestRoute("https://example.com/entry")

        policy.updateHistory(canGoBack = true)
        policy.updateHistory(canGoBack = false)

        assertEquals("https://example.com/entry", policy.lastRequestedRouteUrl)
        assertEquals(
            WebViewRouteRequest.KEEP_CURRENT_PAGE,
            policy.requestRoute("https://example.com/entry"),
        )
    }

    @Test
    fun invalidatedRestore_allowsTheRouteToLoadAgain() {
        val policy = WebViewSessionPolicy(initialRequestedRouteUrl = "https://example.com/entry")

        policy.invalidateRouteRequest()

        assertEquals(WebViewRouteRequest.LOAD, policy.requestRoute("https://example.com/entry"))
    }

    @Test
    fun resolveBack_prefersWebHistoryUntilItIsExhausted() {
        val policy = WebViewSessionPolicy()
        policy.updateHistory(canGoBack = true)

        assertEquals(WebViewBackAction.GO_BACK_IN_PAGE, policy.resolveBack())
        assertTrue(policy.canGoBack)

        assertEquals(WebViewBackAction.EXIT_SCREEN, policy.resolveBack(actualCanGoBack = false))
        assertFalse(policy.canGoBack)
    }

    @Test
    fun cancelBackGesture_doesNotMutateHistoryOrRouteIdentity() {
        val policy = WebViewSessionPolicy()
        policy.requestRoute("https://example.com/entry")
        policy.updateHistory(canGoBack = true)

        policy.cancelBackGesture()

        assertTrue(policy.canGoBack)
        assertEquals("https://example.com/entry", policy.lastRequestedRouteUrl)
    }
}

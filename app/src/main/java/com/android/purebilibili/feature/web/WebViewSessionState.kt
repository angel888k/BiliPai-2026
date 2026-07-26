package com.android.purebilibili.feature.web

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue

internal enum class WebViewRouteRequest {
    LOAD,
    KEEP_CURRENT_PAGE,
}

internal enum class WebViewBackAction {
    GO_BACK_IN_PAGE,
    EXIT_SCREEN,
}

/**
 * Pure policy for keeping route requests separate from the WebView's current history URL.
 *
 * A page reached through an in-page link must not make Compose reload the route URL during an
 * unrelated recomposition. Only a changed route input starts a new top-level load.
 */
internal class WebViewSessionPolicy(
    initialRequestedRouteUrl: String? = null,
    initialCanGoBack: Boolean = false,
) {
    var lastRequestedRouteUrl: String? = initialRequestedRouteUrl
        private set

    var canGoBack: Boolean = initialCanGoBack
        private set

    fun requestRoute(routeUrl: String): WebViewRouteRequest {
        if (lastRequestedRouteUrl == routeUrl) {
            return WebViewRouteRequest.KEEP_CURRENT_PAGE
        }
        lastRequestedRouteUrl = routeUrl
        return WebViewRouteRequest.LOAD
    }

    fun updateHistory(canGoBack: Boolean) {
        this.canGoBack = canGoBack
    }

    fun invalidateRouteRequest() {
        lastRequestedRouteUrl = null
    }

    fun resolveBack(actualCanGoBack: Boolean = canGoBack): WebViewBackAction {
        canGoBack = actualCanGoBack
        return if (actualCanGoBack) {
            WebViewBackAction.GO_BACK_IN_PAGE
        } else {
            WebViewBackAction.EXIT_SCREEN
        }
    }

    fun cancelBackGesture() = Unit
}

/**
 * Owns one in-app browser session without retaining a released WebView.
 *
 * The attached WebView is intentionally transient. Only its saved-state bundle, route request
 * identity and history capability survive recreation.
 */
@Stable
internal class WebViewSessionState private constructor(
    private val policy: WebViewSessionPolicy,
    private var restoredWebViewState: Bundle?,
) {
    constructor() : this(
        policy = WebViewSessionPolicy(),
        restoredWebViewState = null,
    )

    var canGoBack by mutableStateOf(policy.canGoBack)
        private set

    private var attachedWebView: WebView? = null

    fun attach(webView: WebView) {
        attachedWebView = webView
        val stateToRestore = restoredWebViewState
        restoredWebViewState = null
        val restored = stateToRestore != null && webView.restoreState(stateToRestore) != null
        if (!restored) {
            policy.invalidateRouteRequest()
        }
        updateHistory(webView)
    }

    fun requestRoute(webView: WebView, routeUrl: String) {
        if (policy.requestRoute(routeUrl) == WebViewRouteRequest.LOAD) {
            webView.loadUrl(routeUrl)
        }
    }

    fun updateHistory(webView: WebView?) {
        val historyAvailable = webView?.canGoBack() == true
        policy.updateHistory(historyAvailable)
        canGoBack = historyAvailable
    }

    fun dispatchBack(onHistoryExhausted: () -> Unit) {
        val webView = attachedWebView
        when (policy.resolveBack(actualCanGoBack = webView?.canGoBack() == true)) {
            WebViewBackAction.GO_BACK_IN_PAGE -> webView?.goBack()
            WebViewBackAction.EXIT_SCREEN -> {
                canGoBack = false
                onHistoryExhausted()
            }
        }
    }

    fun cancelBackGesture() {
        policy.cancelBackGesture()
    }

    fun release(webView: WebView) {
        captureWebViewState(webView)
        webView.stopLoading()
        webView.onPause()
        webView.webChromeClient = null
        webView.webViewClient = RELEASED_WEB_VIEW_CLIENT
        webView.removeAllViews()
        webView.clearHistory()
        if (attachedWebView === webView) {
            attachedWebView = null
        }
        webView.destroy()
    }

    private fun saveForCompose(): Bundle {
        attachedWebView?.let(::captureWebViewState)
        return Bundle().apply {
            putString(KEY_LAST_REQUESTED_ROUTE_URL, policy.lastRequestedRouteUrl)
            putBoolean(KEY_CAN_GO_BACK, policy.canGoBack)
            restoredWebViewState?.let { putBundle(KEY_WEB_VIEW_STATE, Bundle(it)) }
        }
    }

    private fun captureWebViewState(webView: WebView) {
        val state = Bundle()
        if (webView.saveState(state) != null) {
            restoredWebViewState = state
        }
    }

    companion object {
        private const val KEY_LAST_REQUESTED_ROUTE_URL = "last_requested_route_url"
        private const val KEY_CAN_GO_BACK = "can_go_back"
        private const val KEY_WEB_VIEW_STATE = "web_view_state"
        private val RELEASED_WEB_VIEW_CLIENT = WebViewClient()

        val Saver: Saver<WebViewSessionState, Bundle> = Saver(
            save = { state -> state.saveForCompose() },
            restore = { bundle ->
                WebViewSessionState(
                    policy = WebViewSessionPolicy(
                        initialRequestedRouteUrl = bundle.getString(KEY_LAST_REQUESTED_ROUTE_URL),
                        initialCanGoBack = bundle.getBoolean(KEY_CAN_GO_BACK),
                    ),
                    restoredWebViewState = bundle.getBundle(KEY_WEB_VIEW_STATE),
                )
            },
        )
    }
}

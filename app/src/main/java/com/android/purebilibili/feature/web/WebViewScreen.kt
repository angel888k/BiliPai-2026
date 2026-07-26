package com.android.purebilibili.feature.web

import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.android.purebilibili.core.ui.AdaptiveScaffold
import com.android.purebilibili.core.ui.AdaptiveTopAppBar
import com.android.purebilibili.core.ui.rememberAppBackIcon
import com.android.purebilibili.core.util.BilibiliNavigationTarget
import com.android.purebilibili.core.util.BilibiliNavigationTargetParser
import kotlinx.coroutines.launch

/**
 * WebViewScreen - 应用内浏览器
 * 
 * 支持拦截 Bilibili 链接并跳转到原生界面：
 * - 视频: bilibili.com/video/BV... 或 av...
 * - UP主空间: space.bilibili.com/{mid}
 * - 直播: live.bilibili.com/{roomId}
 * - 动态: t.bilibili.com/{dynamicId} 或 opus/{dynamicId}
 * - 番剧: bilibili.com/bangumi/play/ss{id} 或 ep{id}
 * - 音乐: music.bilibili.com/h5/music-detail?music_id=...
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    url: String,
    title: String? = null,
    onBack: () -> Unit,
    // [新增] 链接拦截回调
    onVideoClick: ((bvid: String) -> Unit)? = null,
    onSpaceClick: ((mid: Long) -> Unit)? = null,
    onLiveClick: ((roomId: Long) -> Unit)? = null,
    onDynamicClick: ((dynamicId: String) -> Unit)? = null,
    onBangumiClick: ((seasonId: Long, epId: Long) -> Unit)? = null,
    onMusicClick: ((musicId: String) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val sessionState = rememberSaveable(saver = WebViewSessionState.Saver) {
        WebViewSessionState()
    }
    val webHistoryBackState = rememberNavigationEventState(NavigationEventInfo.None)
    val latestOnVideoClick = rememberUpdatedState(onVideoClick)
    val latestOnSpaceClick = rememberUpdatedState(onSpaceClick)
    val latestOnLiveClick = rememberUpdatedState(onLiveClick)
    val latestOnDynamicClick = rememberUpdatedState(onDynamicClick)
    val latestOnBangumiClick = rememberUpdatedState(onBangumiClick)
    val latestOnMusicClick = rememberUpdatedState(onMusicClick)

    NavigationBackHandler(
        state = webHistoryBackState,
        isBackEnabled = sessionState.canGoBack,
        reportPredictiveProgress = false,
        onBackCancelled = { commitTransition ->
            sessionState.cancelBackGesture()
            commitTransition()
        },
        onBackCompleted = { commitTransition ->
            sessionState.dispatchBack(onBack)
            commitTransition()
        },
    )

    AdaptiveScaffold(
        topBar = {
            AdaptiveTopAppBar(
                title = title ?: "浏览器",
                navigationIcon = {
                    IconButton(onClick = { sessionState.dispatchBack(onBack) }) {
                        Icon(rememberAppBackIcon(), contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        
                        // [核心] 自定义 WebViewClient 拦截 Bilibili 链接
                        webViewClient = object : WebViewClient() {
                            override fun doUpdateVisitedHistory(
                                view: WebView?,
                                url: String?,
                                isReload: Boolean,
                            ) {
                                super.doUpdateVisitedHistory(view, url, isReload)
                                sessionState.updateHistory(view)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                sessionState.updateHistory(view)
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val requestUrl = request?.url?.toString() ?: return false
                                return handleBilibiliUrl(
                                    webView = view,
                                    urlString = requestUrl,
                                    hasUserGesture = request.hasGesture()
                                )
                            }
                            
                            // 兼容旧版 API
                            @Deprecated("Deprecated in Java")
                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                return url?.let {
                                    handleBilibiliUrl(
                                        webView = view,
                                        urlString = it,
                                        hasUserGesture = false
                                    )
                                } ?: false
                            }
                            
                            /**
                             * 处理 Bilibili URL 拦截
                             * @param webView WebView 实例，用于加载转换后的 URL
                             * @return true 表示已拦截处理，false 表示继续加载网页
                             */
                            private fun handleBilibiliUrl(
                                webView: WebView?,
                                urlString: String,
                                hasUserGesture: Boolean
                            ): Boolean {
                                android.util.Log.d("WebViewScreen", "🔗 Intercepting URL: $urlString")
                                try {
                                    val uri = android.net.Uri.parse(urlString)
                                    val scheme = uri.scheme ?: ""
                                    val host = uri.host ?: ""
                                    
                                    android.util.Log.d("WebViewScreen", "🔍 Scheme: $scheme, Host: $host")

                                    fun dispatchTarget(target: BilibiliNavigationTarget): Boolean {
                                        return when (target) {
                                            is BilibiliNavigationTarget.Video -> {
                                                latestOnVideoClick.value?.invoke(target.videoId)
                                                latestOnVideoClick.value != null
                                            }

                                            is BilibiliNavigationTarget.Space -> {
                                                latestOnSpaceClick.value?.invoke(target.mid)
                                                latestOnSpaceClick.value != null
                                            }

                                            is BilibiliNavigationTarget.Live -> {
                                                latestOnLiveClick.value?.invoke(target.roomId)
                                                latestOnLiveClick.value != null
                                            }

                                            is BilibiliNavigationTarget.BangumiSeason -> {
                                                latestOnBangumiClick.value?.invoke(target.seasonId, 0)
                                                latestOnBangumiClick.value != null
                                            }

                                            is BilibiliNavigationTarget.BangumiEpisode -> {
                                                latestOnBangumiClick.value?.invoke(0, target.epId)
                                                latestOnBangumiClick.value != null
                                            }

                                            is BilibiliNavigationTarget.Music -> {
                                                latestOnMusicClick.value?.invoke(target.musicId)
                                                latestOnMusicClick.value != null
                                            }

                                            is BilibiliNavigationTarget.Dynamic -> {
                                                latestOnDynamicClick.value?.invoke(target.dynamicId)
                                                latestOnDynamicClick.value != null
                                            }

                                            is BilibiliNavigationTarget.Search -> false
                                            is BilibiliNavigationTarget.Article -> false
                                        }
                                    }

                                    when (val action = resolveWebViewNavigationAction(urlString, hasUserGesture)) {
                                        is WebViewNavigationAction.Block -> {
                                            android.util.Log.d("WebViewScreen", "⛔ Blocked navigation: $urlString")
                                            return true
                                        }

                                        is WebViewNavigationAction.LoadInWebView -> {
                                            android.util.Log.d("WebViewScreen", "🔄 Deep link -> ${action.url}")
                                            webView?.loadUrl(action.url)
                                            return true
                                        }

                                        is WebViewNavigationAction.DispatchTarget -> {
                                            if (dispatchTarget(action.target)) {
                                                android.util.Log.d("WebViewScreen", "✅ Routed target: ${action.target}")
                                                return true
                                            }
                                        }

                                        WebViewNavigationAction.AllowWebLoad -> Unit
                                    }

                                    if (scheme == "bilibili" || scheme == "bili") {
                                        android.util.Log.w("WebViewScreen", "⚠️ Blocked unknown deep link: $urlString")
                                        return true
                                    }

                                    if (host.contains("b23.tv")) {
                                        scope.launch {
                                            val resolvedTarget = BilibiliNavigationTargetParser.resolve(urlString)
                                            if (resolvedTarget != null && dispatchTarget(resolvedTarget)) {
                                                android.util.Log.d("WebViewScreen", "✅ Routed resolved short link: $resolvedTarget")
                                            } else {
                                                webView?.post { webView.loadUrl(urlString) }
                                            }
                                        }
                                        return true
                                    }
                                    
                                } catch (e: Exception) {
                                    android.util.Log.e("WebViewScreen", "URL parsing error: ${e.message}")
                                }
                                
                                return false // 不拦截，继续加载
                            }
                        }

                        sessionState.attach(this)
                        sessionState.requestRoute(this, url)
                    }
                },
                update = { webView ->
                    sessionState.requestRoute(webView, url)
                },
                onRelease = sessionState::release,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

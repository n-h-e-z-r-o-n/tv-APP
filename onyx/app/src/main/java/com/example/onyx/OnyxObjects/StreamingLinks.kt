package com.example.onyx.OnyxObjects

import android.util.Log
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.OptIn
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.example.onyx.Database.AppDatabase
import com.example.onyx.R
import com.example.onyx.Video_payer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebViewAssetLoader
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import android.os.SystemClock
import android.view.ViewGroup
import android.widget.FrameLayout


object StreamingLinks {


    suspend fun extractStreamFromServer ( context: Context,  Weburl: String, container: ViewGroup): StreamData? =

        withContext(Dispatchers.Main) {

            var isWebviewDestroyed = false

            val result = CompletableDeferred<StreamData?>()

            val webView = WebView(context)

            /*
            val root = (context as Activity).findViewById<ViewGroup>(android.R.id.content)
            val params = FrameLayout.LayoutParams(1, 1)
            webView.visibility = View.INVISIBLE
            root.addView(webView, params)


            val container = FrameLayout(context)
            container.addView(webView)
            (context as Activity).setContentView(container)


            */

            fun webviewCleanUp() {
                if (isWebviewDestroyed) return
                isWebviewDestroyed = true

                try {

                    webView.post {
                        webView.onPause()
                        webView.stopLoading()
                        webView.webChromeClient = null
                        webView.loadUrl("about:blank")
                        webView.clearHistory()
                        webView.clearFormData()
                        webView.clearCache(false)
                        webView.destroy()
                        container.removeView(webView)
                    }

                } catch (e: Exception) {
                    Log.e("Stream-Result", "WebView cleanup failed", e)
                }
            }

            val params = FrameLayout.LayoutParams(100, 100)
            webView.layoutParams = params
            //webView.visibility = View.INVISIBLE
            container.addView(webView)


            // Setup WebView
            webView.webChromeClient = WebChromeClient()
            webView.webViewClient = object : WebViewClient() {


                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)

                    if (isWebviewDestroyed) return
                    if (view !== webView) return

                    webView.postDelayed({
                        webView.evaluateJavascript(
                            """
                            (function() {
                                // ✅ Target only known ad patterns
                                const safeSelectors = [
                                    'iframe[src*="doubleclick"]',
                                    'iframe[src*="adservice"]',
                                    'iframe[src*="/ads"]',
                                    'div[id^="ad_"]',
                                    'div[id*="_ad_"]',
                                    '.adsbox',
                                    '#ads',
                                    '.ad-banner',
                                    '.advertisement',
                                    '.sponsor',
                                    'div[class="ad-container"]'
                                ];
                                
                                safeSelectors.forEach(sel => {
                                    document.querySelectorAll(sel).forEach(el => {
                                        el.remove();
                                    });
                                });
                            
                                // ✅ Remove only large fixed-position elements covering the center
                                const centerX = window.innerWidth / 2;
                                const centerY = window.innerHeight / 2;
                            
                                document.querySelectorAll('*').forEach(el => {
                                    const style = window.getComputedStyle(el);
                                    if (style.position === 'fixed') {
                                        const rect = el.getBoundingClientRect();
                                        const coversCenter = rect.left <= centerX && rect.right >= centerX &&
                                                             rect.top <= centerY && rect.bottom >= centerY;
                            
                                        if (coversCenter && (rect.height > 80 || rect.width > 80)) {
                                            el.remove();
                                        }
                                    }
                                });
                            
                                // ✅ Watch for newly injected ads (MutationObserver)
                                if (!window.__adObserverAdded) {
                                    const observer = new MutationObserver(mutations => {
                                        mutations.forEach(m => {
                                            m.addedNodes.forEach(node => {
                                                if (node.nodeType === 1) {
                                                    const el = node;
                                                    if (el.matches('.adsbox, .ad-banner, iframe[src*="doubleclick"], iframe[src*="adservice"]')) {
                                                        el.remove();
                                                    }
                                                }
                                            });
                                        });
                                    });
                            
                                    observer.observe(document.body, { childList: true, subtree: true });
                                    window.__adObserverAdded = true;
                                }
                                
                                // ✅ Add center click simulation
                                
                                
                            })(); // ← Only ONE closing here for the main function
                            """.trimIndent(),
                            null
                        )
                    }, 6000L)   // ⏱️ 2-second delay */


                    webView.postDelayed({
                        webView.performCenterClick()
                    }, 6000)

                }

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val url = request?.url.toString()



                    //Log.d("Stream-Result", "STREAM_REQUEST ALL : $url")

                    val videoExtensions = listOf(
                        ".mp4",
                        ".m3u8",
                        ".webm",
                        ".mov",
                        ".mkv",
                        ".avi",
                        ".flv",
                        ".wmv",
                        ".ts",
                        ".m4v",
                        ".3gp",
                        ".ogv",
                        ".mpeg",
                        ".mpg",
                        ".f4v"
                    )

                    val streamingIndicators = listOf(
                        "video=", "stream=", "media=", "playback", "videoplayback",
                        "master.m3u8", "playlist.m3u8"
                    )

                    val isVideo = videoExtensions.any { url.contains(it) }

                    if (isVideo) {

                        val headers = request?.requestHeaders
                        val referer = headers?.get("Referer")
                        val ua = headers?.get("User-Agent")

                        Log.d("Stream-Result", "STREAM_REQUEST FOUND : $url")


                        //result.complete(url)
                        result.complete(
                            StreamData(
                                url = url,
                                referer = referer,
                                userAgent = ua
                            )
                        )

                    }


                    if (url.contains("doubleclick.net") ||
                        url.contains("googlesyndication.com") ||
                        url.contains("adservice.google.com") ||
                        url.contains("popads.net") ||
                        url.contains("adexchangeclear.com") ||
                        url.contains("propellerads") ||
                        url.contains("adsterra")
                    ) {
                        return WebResourceResponse("text/plain", "utf-8", null) // block
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val webUrlPre = Weburl.split('.')[0]
                    Log.d("Stream-Result", "STREAM_REQUEST weburlPrefix : $webUrlPre")

                    val url = request?.url.toString()

                    return if (url.startsWith(webUrlPre)) {
                        false
                    } else {
                        Log.d("Stream-Result", "STREAM_REQUEST BLOCKED : $url")
                        true
                    }


                }

            }
            val userAgent ="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36"

            webView.settings.userAgentString = userAgent
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.settings.mediaPlaybackRequiresUserGesture = false
            webView.settings.setSupportMultipleWindows(false)
            //webView.settings.userAgentString = WebSettings.getDefaultUserAgent(context)
            webView.settings.mediaPlaybackRequiresUserGesture = false //This allows videos to play automatically once loaded

            //This prevents most scripts from opening new tabs or windows automatically.
            webView.settings.javaScriptCanOpenWindowsAutomatically = false
            webView.settings.setSupportMultipleWindows(false)

            webView.settings.loadsImagesAutomatically = false //stops automatic image rendering
            webView.settings.blockNetworkImage = true //prevents images from even being downloaded


            webView.loadUrl(Weburl)

            // Wait until stream found
            //result.await()



            try {
                // ⬇️ Prevent infinite suspension
                val streamUrl = withTimeoutOrNull(35000) {
                    result.await()
                }
                return@withContext streamUrl
            } finally {

                webviewCleanUp()
            }


        }



    fun WebView.performCenterClick(
        repeat: Int = 50,
        interval: Long = 5000
    ) {

        var count = 0

        fun click() {
            if (count >= repeat) return
            val x = width / 2f
            val y = height / 2f
            val downTime = SystemClock.uptimeMillis()

            val downEvent = MotionEvent.obtain(
                downTime,
                downTime,
                MotionEvent.ACTION_DOWN,
                x,
                y,
                0
            )

            val upEvent = MotionEvent.obtain(
                downTime,
                downTime + 50,
                MotionEvent.ACTION_UP,
                x,
                y,
                0
            )

            dispatchTouchEvent(downEvent)
            dispatchTouchEvent(upEvent)

            downEvent.recycle()
            upEvent.recycle()

            count++
            postDelayed({ click() }, interval)
        }
        click()
    }

    suspend fun extractAllStreams(
        context: Context,
        container: ViewGroup,
        imdb: String,
        type: String,
        seasonNo: String = "",
        episodeNo: String = "",
    ): JSONObject = withContext(Dispatchers.IO) {

        val servers = getServerUrls(imdb, type, seasonNo, episodeNo)
        val result = JSONObject()

        Log.e("Stream-Result", " extractAll  servers: $servers")

        for ((name, url) in servers) {

            try {
                val stream =  extractStreamFromServer(context, url, container)

                if (stream != null) {
                    Log.e("Stream-Result", "Server : $name  , VideoUrl : $stream")
                    result.put(name, stream)
                }

            } catch (e: Exception) {
                Log.e("Stream-Result", " extractAll  Error: $e")
            }
        }

        return@withContext result
    }

    suspend fun extractAllStreamsParallel(
        context: Context,
        container: ViewGroup,
        imdb: String,
        type: String,
        seasonNo: String = "",
        episodeNo: String = "",
    ): JSONObject = coroutineScope {

        val servers = getServerUrls(imdb, type, seasonNo, episodeNo)
        val result = JSONObject()

        Log.e("Stream-Result", "extractAll servers: $servers")

        val jobs = servers.map { (name, url) ->

            async(Dispatchers.IO) {

                try {

                    val stream = extractStreamFromServer(context, url, container)

                    if (stream != null) {
                        Log.e("Stream-Result", "Server : $name  , VideoUrl : $stream")
                        name to stream
                    } else {
                        null
                    }

                } catch (e: Exception) {

                    Log.e("Stream-Result", "extractAll Error: $e")
                    null
                }
            }
        }

        val streams = jobs.awaitAll()

        streams.filterNotNull().forEach { (name, url) ->
            result.put(name, url)
        }

        result
    }






     fun getServerUrls(showId: String, type: String, seasonNo: String , episodeNo: String): Map<String, String> {

        val servers = mutableMapOf<String, String>()

        if (type == "movie") {
            servers["vidsrc"] = "https://vidsrc.to/embed/movie/$showId"
            servers["primewire"] = "https://primewire.si/embed/movie?tmdb=$showId"
            servers["vidking"] = "https://www.vidking.net/embed/movie/$showId"
            servers["player"] = "https://player.embed-api.stream/?id=$showId&type=movie"


        } else {
            servers["vidsrc"] =  "https://vidsrc.to/embed/tv/$showId/$seasonNo/$episodeNo"
            servers["primewire"] = "https://www.primewire.si/embed/tv?tmdb=$showId&season=$seasonNo&episode=$episodeNo"
            servers["vidking"] = "https://www.vidking.net/embed/tv/$showId/$seasonNo/$episodeNo"
            servers["player"] = "https://player.embed-api.stream/?id=$showId&s=$seasonNo&e=$episodeNo"

        }

        return servers
    }


}

data class StreamData(
    val url: String,
    val referer: String?,
    val userAgent: String?
)
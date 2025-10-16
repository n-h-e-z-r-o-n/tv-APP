package com.example.onyx

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.webkit.*
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Play : AppCompatActivity() {

    @Volatile
    private var isVideoLaunching = false

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        GlobalUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_play)

        val imdbCode = intent.getStringExtra("imdb_code")
        val type = intent.getStringExtra("type")
        val seasonNo = intent.getStringExtra("seasonNo")
        val episodeNo = intent.getStringExtra("episodeNo")
        val server = intent.getStringExtra("server") ?: "VidSrc.to"



        // Increment watch statistics using GlobalUtils
        if(type == "movie"){
            GlobalUtils.incrementMoviesWatched(this)
        }else{
            GlobalUtils.incrementSeriesWatched(this)
        }

        val webView = findViewById<WebView>(R.id.webView)

        // Setup WebView
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {


            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)



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
                        
                            console.log('✅ Safe ad cleanup executed');
                        })();
                    """.trimIndent(),
                        null
                    )
                }, 6000L) // ⏱️ 2-second delay */



                // Wait for page to render, then simulate a few center clicks
                webView.postDelayed({
                    simulateRepeatedCenterClicks(webView, repeatCount = 10, intervalMs = 1200L)
                }, 3000)
            }

            @OptIn(UnstableApi::class)
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url.toString()

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


                // Check if URL is a video by extension or indicator
                val isVideo = videoExtensions.any { url.endsWith(it) } ||
                        streamingIndicators.any { url.contains(it) }

                if (isVideo) {
                    runOnUiThread {
                        // Clear WebView data and cookies before launching video player
                        clearWebViewData()
                        Video_payer.playVideoExternally(this@Play, url)
                        finish()
                    }
                }


                if (url.contains("doubleclick.net") ||
                    url.contains("googlesyndication.com") ||
                    url.contains("adservice.google.com") ||
                    url.contains("popads.net") ||
                    url.contains("adexchangeclear.com") ||
                    url.contains("propellerads") ||
                    url.contains("adsterra")) {
                    return WebResourceResponse("text/plain", "utf-8", null) // block
                }


                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url.toString()
                return if (url.startsWith("https://vidsrc.to/") ||
                          url.startsWith("https://player.embed-api.stream/") ||
                          url.startsWith("https://www.2embed.skin/") ||
                          url.startsWith("https://www.2embed.cc/") ||
                          url.startsWith("https://embed.su/") ||
                          url.startsWith("https://www.primewire.tf/") ||
                          url.startsWith("https://www.vidking.net/")
                    ) {
                    false
                } else {
                    //Toast.makeText(this@Play, "Blocked $url", Toast.LENGTH_SHORT).show()
                    true
                }
            }


        }

        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.setSupportMultipleWindows(false)
        settings.userAgentString = WebSettings.getDefaultUserAgent(this)
        webView.settings.mediaPlaybackRequiresUserGesture = false //This allows videos to play automatically once loaded

        //This prevents most scripts from opening new tabs or windows automatically.
        webView.settings.javaScriptCanOpenWindowsAutomatically = false
        webView.settings.setSupportMultipleWindows(false)

        // Get complete URL based on server selection and content type
        val url = getServerUrl(server, type, imdbCode, seasonNo, episodeNo)
        
        // Load the URL
        webView.loadUrl(url)
        Log.d("DEBUG_WEBVIEW", "imdbCode: $imdbCode - type: $type -seasonNo:  $seasonNo - episodeNo: $episodeNo - server: $server ")
        Log.d("DEBUG_WEBVIEW", "url: $url ")





    }


    @OptIn(UnstableApi::class)
    private fun playVideoExternally(videoUrl: String) {
        synchronized(this) {
            if (isVideoLaunching) {
                Log.d("DEBUG_TAG_PlayActivity", "Video launch ignored: already launching.")
                return
            }
            isVideoLaunching = true
        }

        Log.d("DEBUG_TAG_PlayActivity", "Launching external video: $videoUrl")

        try {
            // Clear WebView data and cookies before launching video player
            clearWebViewData()
            Video_payer.playVideoExternally(this, videoUrl)
            finish()
        } catch (e: Exception) {
            Log.e("DEBUG_TAG_PlayActivity", "Failed to launch external video", e)
        } finally {
            synchronized(this) {
                isVideoLaunching = false
            }
        }
    }
    
    private fun clearWebViewData() {
        try {
            // Clear WebView cache, cookies, and data
            val webView = findViewById<WebView>(R.id.webView)
            webView?.let { wv ->
                // Stop loading
                wv.stopLoading()
                
                // Clear cache
                wv.clearCache(true)
                
                // Clear history
                wv.clearHistory()
                
                // Clear form data
                wv.clearFormData()
                
                // Clear cookies
                val cookieManager = CookieManager.getInstance()
                cookieManager.removeAllCookies(null)
                cookieManager.flush()
                
                // Clear WebView storage
                wv.clearMatches()
                
                // Load blank page
                wv.loadUrl("about:blank")
                
                Log.d("DEBUG_TAG_PlayActivity", "WebView data cleared successfully")
            }
        } catch (e: Exception) {
            Log.e("DEBUG_TAG_PlayActivity", "Failed to clear WebView data", e)
        }
    }


    private fun simulateRepeatedCenterClicks(
        webView: WebView,
        repeatCount: Int,
        intervalMs: Long = 500L
    ) {
        lifecycleScope.launch {
            var centerX = webView.width / 2
            var centerY = webView.height / 2




            repeat(repeatCount) { index ->
                val downTime = System.currentTimeMillis()
                val eventTime = downTime + 100

                val downEvent = MotionEvent.obtain(
                    downTime, eventTime,
                    MotionEvent.ACTION_DOWN, centerX.toFloat(), centerY.toFloat(), 0
                )
                val upEvent = MotionEvent.obtain(
                    downTime, eventTime + 50,
                    MotionEvent.ACTION_UP, centerX.toFloat(), centerY.toFloat(), 0
                )

                webView.dispatchTouchEvent(downEvent)
                webView.dispatchTouchEvent(upEvent)

                downEvent.recycle()
                upEvent.recycle()


                delay(intervalMs)
            }
        }
    }

    private fun getServerUrl(server: String, urlType: String?, showId: String?, seasonNo: String?, episodeNo: String?): String {
        val serverIndex = getServerIndex(server)
        
        return if (urlType == "movie") {
            when (serverIndex) {
                1 -> "https://vidsrc.to/embed/movie/$showId"
                2 -> "https://player.embed-api.stream/?id=$showId&type=movie"
                3 -> "https://www.2embed.skin/embed/$showId"
                4 -> "https://embed.su/embed/movie/$showId"
                5 -> "https://www.primewire.tf/embed/movie?tmdb=$showId"
                6 -> "https://www.vidking.net/embed/movie/$showId"
                else -> "https://vidsrc.to/embed/movie/$showId"
            }
        } else {
            when (serverIndex) {
                1 -> "https://vidsrc.to/embed/tv/$showId/$seasonNo/$episodeNo"
                2 -> "https://player.embed-api.stream/?id=$showId&s=$seasonNo&e=$episodeNo"
                3 -> "https://www.2embed.cc/embedtv/$showId&s=$seasonNo&e=$episodeNo"
                4 -> "https://embed.su/embed/tv/$showId/$seasonNo/$episodeNo"
                5 -> "https://www.primewire.tf/embed/tv?tmdb=$showId&season=$seasonNo&episode=$episodeNo"
                6 -> "https://www.vidking.net/embed/tv/$showId/$seasonNo/$episodeNo"
                else -> "https://vidsrc.to/embed/tv/$showId/$seasonNo/$episodeNo"
            }
        }
    }
    
    private fun getServerIndex(server: String): Int {
        return when (server) {
            "VidSrc.to" -> 1
            "Embed API Stream" -> 2
            "2Embed" -> 3
            "Embed.su" -> 4
            "PrimeWire" -> 5
            "vidking" -> 6
            else -> 1 // Default to VidSrc.to
        }
    }
}

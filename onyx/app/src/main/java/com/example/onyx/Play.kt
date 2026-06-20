package com.example.onyx

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.example.onyx.OnyxObjects.GlobalUtils
import com.example.onyx.OnyxObjects.StreamingLinks.performCenterClick
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Play : AppCompatActivity() {

    @Volatile
    private var isVideoLaunching = false
    private var showId: String = ""
    private var showType: String = ""
    private var showTitle: String = ""
    private var showPoster: String = ""
    private var showBackdrop: String = ""
    private var showSNo: String = ""
    private var showENo: String = ""



    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        GlobalUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_play)

        /*
        intent.putExtra("imdb_code", showId)
        intent.putExtra("type", showType)
        intent.putExtra("title", showTitle)
        intent.putExtra("poster", showPoster)
        intent.putExtra("backdrop", showBackdrop)
        intent.putExtra("seasonNo", '0')
        intent.putExtra("EpisodeNo", '0')
        */


        showId = intent.getStringExtra("imdb_code")?: ""
        showType = intent.getStringExtra("type")?: ""
        showTitle =  intent.getStringExtra("title")?: ""
        showPoster =  intent.getStringExtra("poster")?: ""
        showBackdrop =  intent.getStringExtra("showBackdrop")?: ""
        showSNo =  intent.getStringExtra("seasonNo")?: ""
        showENo =  intent.getStringExtra("episodeNo")?: ""

        Log.d("DEBUG_WEBVIEW", "imdbCode: $showId - type: $showType   -title: $showTitle  -seasonNo:  $showSNo - episodeNo: $showENo ")
        Log.d("DEBUG_WEBVIEW", "imdbCode: $showId - type: $showType   -title: $showTitle  -showPoster:  $showPoster - showBackdrop: $showBackdrop ")




        // Increment watch statistics using GlobalUtils
        if(showType == "movie"){
            GlobalUtils.incrementMoviesWatched(this)
        }else{
            GlobalUtils.incrementSeriesWatched(this)
        }

        //-----------------------------------------------------------------------------------------

        val webView = findViewById<WebView>(R.id.webView)


        // Set WebView background color to avoid white flash before content loads
        val typedValue = TypedValue()
        theme.resolveAttribute(R.attr.BG_color, typedValue, true)
        webView.setBackgroundColor(typedValue.data)

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


                /*
                // Wait for page to render, then simulate a few center clicks
                webView.postDelayed({
                    simulateRepeatedCenterClicks(webView, repeatCount = 10, intervalMs = 1200L)
                }, 3000)

                 */

                webView.postDelayed({
                    webView.performCenterClick()
                }, 6000)
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
                    val headers = request?.requestHeaders
                    val referer = headers?.get("Referer")
                    val ua = headers?.get("User-Agent")

                    runOnUiThread {
                        // Clear WebView data and cookies before launching video player
                        clearWebViewData()
                        //Video_payer.playVideoExternally(this@Play, url)
                        Video_payer.playVideoExternally(this@Play, url, referer, ua, showId, showType, showTitle, showPoster, showBackdrop, showSNo, showENo)

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
                          url.startsWith("https://2embed") ||
                          url.startsWith("https://embedmaster") ||
                          url.startsWith("https://embdmstrplayer") ||
                          url.startsWith("https://embed.su/") ||
                          url.startsWith("https://primewire.si/") ||
                          url.startsWith("https://vidking.net/")
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
        val url = GlobalUtils.getServerUrl(this, showType, showId.trim(), showSNo.trim(), showENo.trim())

        // Load the URL
        webView.loadUrl(url)
        Log.d("DEBUG_WEBVIEW", "imdbCode: $showType - type: $showType -seasonNo:  $showSNo - episodeNo: $showENo ")
        Log.d("DEBUG_WEBVIEW", "url: $url ")

        setupBackPressedCallback()

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

                wv.destroy()


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




    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    clearWebViewData()
                    finish()
                }
            }
        )
    }

    override fun onStop() {
        super.onStop()

    }

    override fun onDestroy() {

        isVideoLaunching = false

        // Cancel any running coroutines
        lifecycleScope.coroutineContext.cancelChildren()



        // Finish activity
        finish()

        super.onDestroy()
    }




}

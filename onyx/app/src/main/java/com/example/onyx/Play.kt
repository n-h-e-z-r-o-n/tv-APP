package com.example.onyx

import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.webkit.*
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Play : AppCompatActivity() {

    private var isVideoLaunching = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_play)

        val imdbCode = intent.getStringExtra("imdb_code")
        val type = intent.getStringExtra("type")
        val seasonNo = intent.getStringExtra("seasonNo")
        val episodeNo = intent.getStringExtra("episodeNo")
        val server = intent.getStringExtra("server") ?: "VidSrc.to"

        val webView = findViewById<WebView>(R.id.webView)

        // Setup WebView
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Wait for page to render, then simulate a few center clicks
                webView.postDelayed({
                    simulateRepeatedCenterClicks(webView, repeatCount = 3, intervalMs = 1200L)
                }, 1500)
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url.toString()

                if (url.endsWith(".mp4") || url.endsWith(".m3u8")
                    || url.endsWith(".webm") || url.endsWith(".mov")
                ) {
                    runOnUiThread {
                        playVideoExternally(url)
                    }
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
                          url.startsWith("https://www.primewire.tf/")) {
                    false // allow server navigation
                } else {
                    Toast.makeText(this@Play, "Blocked $url", Toast.LENGTH_SHORT).show()
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

        // Get complete URL based on server selection and content type
        val url = getServerUrl(server, type, imdbCode, seasonNo, episodeNo)
        
        // Load the URL
        webView.loadUrl(url)
        Log.e("Play Data 5M", "Loading from server '$server': $url")
    }

    @OptIn(UnstableApi::class)
    private fun playVideoExternally(videoUrl: String) {
        if (isVideoLaunching) {
            Log.d("DEBUG_TAG_PlayActivity", "Video launch ignored: already launching.")
            return
        }
        isVideoLaunching = true
        Log.d("DEBUG_TAG_PlayActivity", "Launching external video: $videoUrl")

        try {
        PlayerManager.playVideoExternally(this, videoUrl)

            // Finish safely after short delay
            lifecycleScope.launch {
                delay(500)
        finish()
    }
        } catch (e: Exception) {
            isVideoLaunching = false
            Toast.makeText(this, "Failed to launch player", Toast.LENGTH_SHORT).show()
            Log.e("DEBUG_TAG_PlayActivity", "Error launching video", e)
        }
    }

    private fun simulateRepeatedCenterClicks(
        webView: WebView,
        repeatCount: Int,
        intervalMs: Long = 500L
    ) {
        lifecycleScope.launch {
            val centerX = webView.width / 2
            val centerY = webView.height / 2

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

                Log.d("DEBUG_TAG_Click", "Simulated click #${index + 1} at ($centerX, $centerY)")

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
                else -> "https://vidsrc.to/embed/movie/$showId"
            }
        } else {
            when (serverIndex) {
                1 -> "https://vidsrc.to/embed/tv/$showId/$seasonNo/$episodeNo"
                2 -> "https://player.embed-api.stream/?id=$showId&s=$seasonNo&e=$episodeNo"
                3 -> "https://www.2embed.cc/embedtv/$showId&s=$seasonNo&e=$episodeNo"
                4 -> "https://embed.su/embed/tv/$showId/$seasonNo/$episodeNo"
                5 -> "https://www.primewire.tf/embed/tv?tmdb=$showId&season=$seasonNo&episode=$episodeNo"
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
            else -> 1 // Default to VidSrc.to
        }
    }
}

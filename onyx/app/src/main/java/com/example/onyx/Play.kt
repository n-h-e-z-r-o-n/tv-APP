package com.example.onyx

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.*
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Play : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_play)

        val imdbCode = intent.getStringExtra("imdb_code")
        val type = intent.getStringExtra("type")
        val seasonNo = intent.getStringExtra("seasonNo")
        val episodeNo = intent.getStringExtra("episodeNo")

        Log.e("Play Data 1", imdbCode.toString())
        Log.e("Play Data 2", type.toString())
        Log.e("Play Data 3", seasonNo.toString())
        Log.e("Play Data 4", episodeNo.toString())




        val webView = findViewById<WebView>(R.id.webView)

        // Setup WebView
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Wait for page to load completely, then simulate click
                Log.d("DEBUG_TAG_Load", "Page finished loading: $url")

                // Delay the click simulation to ensure everything is rendered
                webView.postDelayed({
                    //simulateCenterClick(webView)
                    simulateRepeatedCenterClicks(webView, repeatCount = 500, intervalMs = 1000L)
                }, 10) // 2 second delay to ensure page is fully rendered
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url.toString()

                // Detect video URLs
                if (url.endsWith(".mp4") || url.endsWith(".m3u8") ||
                    url.endsWith(".webm") || url.endsWith(".mov")) {

                    runOnUiThread {
                        playVideoExternally(url)
                    }
                    // Block inside WebView (prevent duplicate load)
                    //return WebResourceResponse("text/plain", "utf-8", null)
                }

                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url.toString()
                return if (url.startsWith("https://vidsrc.to/")) {
                    false // allow vidsrc navigation
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
        settings.setSupportMultipleWindows(false) // block popups

        // Load movie embed URL
        if(type=="movie") {
            webView.loadUrl("https://vidsrc.to/embed/${type}/${imdbCode}")
            Log.e("Play Data 5M", "https://vidsrc.to/embed/${type}/${imdbCode}")
        } else{
            webView.loadUrl("https://vidsrc.to/embed/tv/${imdbCode}/${seasonNo}/${episodeNo}")
            Log.e("Play Data 5M","https://vidsrc.to/embed/tv/${imdbCode}/${seasonNo}/${episodeNo}")
        }
    }



    private fun playVideoExternally(videoUrl: String) {
        Log.d("DEBUG_TAG_PlayActivity", "Video URL detected: $videoUrl")

        // Launch custom video player activity
        val intent = Intent(this, Video_payer::class.java).apply {
            putExtra("video_url", videoUrl)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)

        // Optional: Finish current activity to return to TV interface
        finish()
    }

    private fun simulateRepeatedCenterClicks(
        webView: WebView,
        repeatCount: Int,
        intervalMs: Long = 500L // delay between clicks
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val centerX = webView.width / 2
            val centerY = webView.height / 2

            repeat(repeatCount) { index ->
                val downTime = System.currentTimeMillis()
                val eventTime = downTime + 100

                val downEvent = MotionEvent.obtain(
                    downTime,
                    eventTime,
                    MotionEvent.ACTION_DOWN,
                    centerX.toFloat(),
                    centerY.toFloat(),
                    0
                )
                val upEvent = MotionEvent.obtain(
                    downTime,
                    eventTime + 50,
                    MotionEvent.ACTION_UP,
                    centerX.toFloat(),
                    centerY.toFloat(),
                    0
                )

                webView.dispatchTouchEvent(downEvent)
                webView.dispatchTouchEvent(upEvent)

                downEvent.recycle()
                upEvent.recycle()

                Log.d("DEBUG_TAG_Click", "Simulated click #${index + 1} at ($centerX, $centerY)")

                delay(intervalMs) // wait before next click
            }
        }
    }


}

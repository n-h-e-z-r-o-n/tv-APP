package com.example.onyx

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.*
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.util.Log

class Play : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_play)

        val imdbCode = intent.getStringExtra("imdb_code")
        val type = intent.getStringExtra("type")

        val webView = findViewById<WebView>(R.id.webView)

        // Setup WebView
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {

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
        webView.loadUrl("https://vidsrc.to/embed/${type}/${imdbCode}")
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

}

package com.example.onyx

import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.onyx.OnyxObjects.StreamingLinks
import kotlinx.coroutines.launch

class testpage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_testpage)

        lifecycleScope.launch {

            //val webView = findViewById<WebView>(R.id.webViewTest            )
            val container = findViewById<LinearLayout>(R.id.container)

            //https://vidsrc.to/embed/movie/533444
            val stream = StreamingLinks.extractStreamFromServer(
                this@testpage,
                "https://vidsrc.to/embed/movie/533444",
                container
            )
            Log.e("Stream-Result", "Server : $stream")

            //val result = StreamingLinks.extractAllStreams(this@testpage, container, "533444", "movie", "", "")
            val result = StreamingLinks.extractAllStreamsParallel(this@testpage, container, "533444", "movie", "", "")
            Log.d("Stream-Result", " extractAll : $result")

        }
    }
}
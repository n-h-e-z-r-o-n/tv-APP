package com.example.onyx

import android.content.Intent
import android.os.Bundle

import android.widget.ImageButton
import androidx.activity.ComponentActivity
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.squareup.picasso.Picasso



class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        SSLHelper.trustAllCertificates() // <-- add this line


        val client = UnsafeOkHttpClient.getUnsafeOkHttpClient()
        val picasso = Picasso.Builder(this)
            .downloader(com.squareup.picasso.OkHttp3Downloader(client))
            .build()
        Picasso.setSingletonInstance(picasso)

        setupSidebar()

    }


    private fun setupSidebar() {
        val btnHome = findViewById<ImageButton>(R.id.btnHome)
        val btnMovies = findViewById<ImageButton>(R.id.btnMovies)
        val btnTvShows = findViewById<ImageButton>(R.id.btnTvShow)
        val btnSearch = findViewById<ImageButton>(R.id.btnSearch)
        val btnProfile = findViewById<ImageButton>(R.id.btnProfile)

        btnHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        btnMovies.setOnClickListener {
            startActivity(Intent(this, Movie_Page::class.java))
        }

        btnTvShows.setOnClickListener {
            startActivity(Intent(this, Tv_Page::class.java))
        }

        btnSearch.setOnClickListener {
            startActivity(Intent(this, Search_Page::class.java))
        }

    }
}

data class MovieItem(
    val title: String,
    val imageUrl: String,
    val imdbCode: String,
    val type: String
)

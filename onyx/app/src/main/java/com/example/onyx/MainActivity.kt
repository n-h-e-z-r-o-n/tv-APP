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

        NavAction.setupSidebar(this)

    }


}



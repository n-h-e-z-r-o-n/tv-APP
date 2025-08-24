package com.example.onyx

import android.app.Application
import com.squareup.picasso.Picasso
import com.squareup.picasso.OkHttp3Downloader

class OnyxApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Configure SSL only once when the app starts
        SSLHelper.trustAllCertificates()
        
        // Configure Picasso with unsafe HTTP client
        val client = UnsafeOkHttpClient.getUnsafeOkHttpClient()
        val picasso = Picasso.Builder(this)
            .downloader(OkHttp3Downloader(client))
            .build()
        Picasso.setSingletonInstance(picasso)
    }
}

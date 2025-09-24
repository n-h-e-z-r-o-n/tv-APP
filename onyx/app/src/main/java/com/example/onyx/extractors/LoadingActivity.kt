package com.example.onyx.extractors

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

import com.bumptech.glide.Glide
import com.example.onyx.R

class LoadingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        val imageView = findViewById<ImageView>(R.id.loadingGif)

        // Load the GIF from assets or drawable
        Glide.with(this)
            .asGif()
            .load(R.raw.pattern) // put your gif in res/drawable
            .into(imageView)
    }


}
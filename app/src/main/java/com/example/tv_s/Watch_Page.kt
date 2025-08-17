package com.example.tv_s

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity


class Watch_Page : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_watch_page)

        // Get extras from Intent
        val imdbCode = intent.getStringExtra("imdb_code")
        val type = intent.getStringExtra("type")

        // Show them (for testing)
        //findViewById<TextView>(R.id.watchPage).text =  "IMDB Code: $imdbCode\nType: $type"
        val recyclerView = findViewById<TextView>(R.id.watchPage)
        recyclerView.text =  "IMDB Code: $imdbCode\nType: $type"
    }
}
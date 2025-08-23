package com.example.onyx

import android.app.Activity
import android.content.Intent
import android.widget.ImageButton

object NavAction {
    fun setupSidebar(activity: Activity) {
        val btnHome = activity.findViewById<ImageButton>(R.id.btnHome)
        val btnMovies = activity.findViewById<ImageButton>(R.id.btnMovies)
        val btnTvShows = activity.findViewById<ImageButton>(R.id.btnTvShow)
        val btnSearch = activity.findViewById<ImageButton>(R.id.btnSearch)
        val btnProfile = activity.findViewById<ImageButton>(R.id.btnProfile)

        btnHome?.setOnClickListener {
            if (activity !is Home_Page) {
                activity.startActivity(Intent(activity, MainActivity::class.java))
            }
        }

        btnMovies?.setOnClickListener {
            if (activity !is Movie_Page) {
                activity.startActivity(Intent(activity, Movie_Page::class.java))
            }
        }

        btnTvShows?.setOnClickListener {
            if (activity !is Tv_Page) {
                activity.startActivity(Intent(activity, Tv_Page::class.java))
            }
        }

        btnSearch?.setOnClickListener {
            if (activity !is Search_Page) {
                activity.startActivity(Intent(activity, Search_Page::class.java))
            }
        }

    }
}

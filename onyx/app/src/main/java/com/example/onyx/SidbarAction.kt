package com.example.onyx

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

object NavAction {
    fun setupSidebar(activity: Activity) {
        val btnHome = activity.findViewById<ImageButton>(R.id.btnHome)
        val btnMovies = activity.findViewById<ImageButton>(R.id.btnMovies)
        val btnTvShows = activity.findViewById<ImageButton>(R.id.btnTvShow)
        val btnSearch = activity.findViewById<ImageButton>(R.id.btnSearch)
        val btnProfile = activity.findViewById<ImageButton>(R.id.btnProfile)

        val sidebar = activity.findViewById<LinearLayout>(R.id.sideBar)

        sidebar?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                sidebar.visibility = View.VISIBLE
            } else {
                sidebar.visibility = View.GONE
            }
        }


        btnHome?.setOnClickListener {
            if (activity !is Home_Page) {
                activity.startActivity(Intent(activity, Home_Page::class.java))
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


        // Highlight based on current activity
        val buttons = listOf(btnHome, btnMovies, btnTvShows, btnSearch, btnProfile)
        when (activity) {
            is Home_Page -> highlightActive(btnHome, buttons)
            is Movie_Page -> highlightActive(btnMovies, buttons)
            is Tv_Page -> highlightActive(btnTvShows, buttons)
            is Search_Page -> highlightActive(btnSearch, buttons)
            //is Profile_Page -> highlightActive(btnProfile, buttons, activity)
        }

    }


    private fun highlightActive(
        activeBtn: ImageButton?,
        allButtons: List<ImageButton?>
    ) {
        allButtons.forEach { it?.isSelected = false }
        activeBtn?.isSelected = true
    }



}

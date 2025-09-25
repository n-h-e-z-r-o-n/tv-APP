package com.example.onyx

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView


object NavAction {
    fun setupSidebar(activity: Activity) {
        val btnHome = activity.findViewById<ImageButton>(R.id.btnHome)
        val btnMovies = activity.findViewById<ImageButton>(R.id.btnMovies)
        val btnTvShows = activity.findViewById<ImageButton>(R.id.btnTvShow)
        val btnSearch = activity.findViewById<ImageButton>(R.id.btnSearch)
        val btnFav = activity.findViewById<ImageButton>(R.id.btnFav)
        val btnProfile = activity.findViewById<ImageButton>(R.id.btnProfile)


        val sidebar = activity.findViewById<LinearLayout>(R.id.sideBar)
        val labelHome = activity.findViewById<TextView>(R.id.labelHome)
        val labelMovies = activity.findViewById<TextView>(R.id.labelMovies)
        val labelTvShow = activity.findViewById<TextView>(R.id.labelTvShow)
        val labelSearch = activity.findViewById<TextView>(R.id.labelSearch)
        val labelProfile = activity.findViewById<TextView>(R.id.labelProfile)
        val labelFav = activity.findViewById<TextView>(R.id.labelFav)

        val buttons = listOf(btnHome, btnMovies, btnTvShows, btnSearch, btnFav, btnProfile)
        val labels = listOf(labelHome, labelMovies, labelTvShow, labelSearch, labelProfile, labelFav)



        btnHome?.setOnClickListener {
            if (activity !is Home_Page) {
                val intent = Intent(activity, Home_Page::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                activity.startActivity(intent)
            }
        }

        btnMovies?.setOnClickListener {
            if (activity !is Movie_Page) {
                val intent = Intent(activity, Movie_Page::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                activity.startActivity(intent)
            }
        }

        btnTvShows?.setOnClickListener {
            if (activity !is Tv_Page) {
                val intent = Intent(activity, Tv_Page::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                activity.startActivity(intent)
            }
        }

        btnSearch?.setOnClickListener {
            if (activity !is Search_Page) {
                val intent = Intent(activity, Search_Page::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                activity.startActivity(intent)
            }
        }

        btnFav?.setOnClickListener {
            if (activity !is Favorite_Page) {
                val intent = Intent(activity, Favorite_Page::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                activity.startActivity(intent)
            }
        }




        // Highlight based on current activity
        val activeButton: ImageButton? = when (activity) {
            is Home_Page -> btnHome
            is Movie_Page -> btnMovies
            is Tv_Page -> btnTvShows
            is Search_Page -> btnSearch
            is Favorite_Page -> btnFav
            else -> btnHome
        }

        highlightActive(activeButton, buttons)
        activeButton?.post { activeButton.requestFocus() }          // Request focus on the active button for TV D-pad usability


        // ✅ Add focus scaling effect to each button
        buttons.forEach { btn ->
            btn?.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    v.animate().scaleX(1.2f).scaleY(1.2f).setDuration(150).start()
                } else {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                }

                // Expand/collapse sidebar and toggle labels
                sidebar?.let { bar ->
                    val anyFocused = buttons.any { it?.hasFocus() == true }
                    if (anyFocused) {
                        expandSidebar(activity, bar, true)
                        setLabelsVisible(labels, true)
                    } else {
                        // Post to ensure focus state has settled
                        bar.post {
                            val stillAnyFocused = buttons.any { it?.hasFocus() == true }
                            if (!stillAnyFocused) {
                                setLabelsVisible(labels, false)
                                expandSidebar(activity, bar, false)
                            }
                        }
                    }
                }
            }
        }


    }


    private fun highlightActive(
        activeBtn: ImageButton?,
        allButtons: List<ImageButton?>
    ) {
        allButtons.forEach { it?.isSelected = false }
        activeBtn?.isSelected = true
    }

    fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    private fun expandSidebar(context: Context, sidebar: LinearLayout, expand: Boolean) {
        val collapsed = 40.dpToPx(context)
        val expanded = 610.dpToPx(context)
        val start = sidebar.layoutParams.width
        val end = if (expand) expanded else collapsed
        if (start == end) return

        val animator = ValueAnimator.ofInt(start, end)
        animator.duration = 180
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { valueAnimator ->
            val value = valueAnimator.animatedValue as Int
            val params: ViewGroup.LayoutParams = sidebar.layoutParams
            params.width = value
            sidebar.layoutParams = params
        }
        animator.start()
    }

    private fun setLabelsVisible(labels: List<TextView?>, visible: Boolean) {
        labels.forEach { label ->
            label?.visibility = if (visible) View.VISIBLE else View.GONE
        }
    }



}

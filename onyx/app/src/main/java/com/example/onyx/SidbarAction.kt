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

import android.graphics.Typeface
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import kotlinx.coroutines.withContext
import java.io.File
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


object NavAction {

    fun setupSidebar(activity: Activity) {
        val btnHome = activity.findViewById<ImageButton>(R.id.btnHome)
        val btnMovies = activity.findViewById<ImageButton>(R.id.btnMovies)
        val btnTvShows = activity.findViewById<ImageButton>(R.id.btnTvShow)
        val btnSearch = activity.findViewById<ImageButton>(R.id.btnSearch)
        val btnFav = activity.findViewById<ImageButton>(R.id.btnFav)
        val btnNotification = activity.findViewById<ImageButton>(R.id.btnNotification)
        val btnProfile = activity.findViewById<ImageButton>(R.id.btnProfile)


        val sidebar = activity.findViewById<LinearLayout>(R.id.sideBar)
        val labelHome = activity.findViewById<TextView>(R.id.labelHome)
        val labelMovies = activity.findViewById<TextView>(R.id.labelMovies)
        val labelTvShow = activity.findViewById<TextView>(R.id.labelTvShow)
        val labelSearch = activity.findViewById<TextView>(R.id.labelSearch)
        val labelFav = activity.findViewById<TextView>(R.id.labelFav)
        val labelNotification = activity.findViewById<TextView>(R.id.labelNotification)
        val labelProfile = activity.findViewById<TextView>(R.id.labelProfile)

        val buttons = listOf(btnHome, btnMovies, btnTvShows, btnSearch, btnFav, btnNotification, btnProfile)
        val labels = listOf(labelHome, labelMovies, labelTvShow, labelSearch,labelFav, labelNotification, labelProfile )

        val navigationMap = mapOf(
            btnHome to Home_Page::class.java,
            btnMovies to Movie_Page::class.java,
            btnTvShows to Tv_Page::class.java,
            btnSearch to Search_Page::class.java,
            btnFav to Favorite_Page::class.java,
            btnNotification to Notification_Page::class.java,
            btnProfile to Profile_Page::class.java
        )





        // Highlight based on current activity
        val activeButton: ImageButton? = when (activity) {
            is Home_Page -> btnHome
            is Movie_Page -> btnMovies
            is Tv_Page -> btnTvShows
            is Search_Page -> btnSearch
            is Favorite_Page -> btnFav
            is Notification_Page -> btnNotification
            is Profile_Page -> btnProfile
            else -> btnHome
        }

        highlightActive(activeButton, buttons)
        activeButton?.post { activeButton.requestFocus() }          // Request focus on the active button for TV D-pad usability

        navigationMap.forEach { (button, targetClass) ->
            button?.setOnClickListener {
                if (activity::class.java != targetClass) {
                    val intent = Intent(activity, targetClass)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    activity.startActivity(intent)
                }
            }
        }

        // ✅ Add focus scaling effect to each button
        // ✅ Focus animations for buttons + labels
        buttons.forEachIndexed { index, btn ->
            val label = labels[index]
            btn?.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    // Button scaling
                    v.animate().scaleX(1.2f).scaleY(1.2f).setDuration(150).start()

                    // Label visible, bold, and scaled
                    label?.let {
                        //it.animate().scaleX(1.2f).scaleY(1.2f).setDuration(150).start()
                        it.setTypeface(null, Typeface.BOLD)
                    }
                } else {
                    // Reset scaling
                    v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()

                    label?.let {
                        //it.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                        it.setTypeface(null, Typeface.NORMAL)
                    }
                }

                // Sidebar expand/collapse
                sidebar?.let { bar ->
                    val anyFocused = buttons.any { it?.hasFocus() == true }
                    if (anyFocused) {
                        expandSidebar(activity, bar, true)
                        setLabelsVisible(labels, true)
                    } else {
                        bar.post {
                            if (!buttons.any { it?.hasFocus() == true }) {
                                setLabelsVisible(labels, false)
                                expandSidebar(activity, bar, false)
                            }
                        }
                    }
                }
            }
        }


        checkNotifications(activity)

    }

    private fun checkNotifications(activity: Activity) {
        val badge = activity.findViewById<View>(R.id.notificationBadge) // CardView badge

        CoroutineScope(Dispatchers.IO).launch {
            while (true) { // 🔁 infinite loop to periodically check
                val notificationsJson = NotificationHelper.getNotifications(activity)
                val uniqueNotifications = notificationsJson.distinctBy { it.imdbCode }

                Log.e("NotificationHelper", "AutoCheck ${uniqueNotifications}")


                val file = File(activity.cacheDir, "notifications.json")
                val gson = Gson()
                val jsonString = gson.toJson(uniqueNotifications)
                Log.e("NotificationHelper", "file AutoCheck ${jsonString}")
                file.writeText(jsonString)

                withContext(Dispatchers.Main) {
                    badge?.visibility = if (uniqueNotifications.isNotEmpty()) View.VISIBLE else View.GONE
                }

                delay(18000_000) // ⏳ wait 1 minute before checking again
            }
        }
    }

    fun loadNotifications(context: Activity): List<NotificationItem> {
        val file = File(context.cacheDir, "notifications.json")
        if (!file.exists()) return emptyList()

        val jsonString = file.readText()
        val gson = Gson()
        val type = object : TypeToken<List<NotificationItem>>() {}.type
        return gson.fromJson(jsonString, type)
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

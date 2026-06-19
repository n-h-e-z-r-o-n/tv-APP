package com.example.onyx.OnyxObjects

import android.app.Activity
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.onyx.*
import com.example.onyx.Database.SessionManger
import java.lang.ref.WeakReference

object NavAction {
    private var previouslyFocusedView: WeakReference<View>? = null
    private var isSidebarOpen = false

    fun setupSidebar(activity: Activity) {
        val sidebar = activity.findViewById<FrameLayout>(R.id.sideBar)
        val mainBox = activity.findViewById<CardView>(R.id.mainBox)

        if (sidebar == null || mainBox == null) return

        val btnShows = activity.findViewById<ImageButton>(R.id.sidebarBtnShows)
        val btnAnime = activity.findViewById<ImageButton>(R.id.sidebarBtnAnime)
        val btnSearch = activity.findViewById<ImageButton>(R.id.sidebarSearchBtn)
        val btnWatching = activity.findViewById<ImageButton>(R.id.sidebarWatchListBtn)
        val btnNotification = activity.findViewById<ImageButton>(R.id.sidebarNotificationBtn)
        val btnProfile = activity.findViewById<CardView>(R.id.sidebarBtnProfile)
        val profileImg = activity.findViewById<ImageView>(R.id.sidebarBtnProfileImg)

        val labelMvTv = activity.findViewById<TextView>(R.id.sidebarLabelShows)
        val labelAnime = activity.findViewById<TextView>(R.id.sidebarLabelAnime)
        val labelSearch = activity.findViewById<TextView>(R.id.sidebarLabelSearch)
        val labelWatched = activity.findViewById<TextView>(R.id.sidebarLabelWatchList)
        val labelNotification = activity.findViewById<TextView>(R.id.sidebarLabelNotification)
        val labelProfile = activity.findViewById<TextView>(R.id.sidebarLabelProfile)

        val buttons = listOf(btnShows, btnAnime, btnSearch, btnWatching, btnNotification, btnProfile)
        val labels = listOf(labelMvTv, labelAnime, labelSearch, labelWatched, labelNotification, labelProfile)

        val validButtons = buttons.filterNotNull()
        val validLabels = labels.filterNotNull()

        val activeButton: View? = null // Assuming this is set dynamically later, but defaults to null

        validButtons.forEach { it.isSelected = it == activeButton }

        fun hasAnyButtonFocus(): Boolean = validButtons.any { it.hasFocus() }

        // --- FOCUS MANAGEMENT LOGIC START ---
        validButtons.forEachIndexed { index, view ->
            view.setOnFocusChangeListener { _, hasFocus ->
                // Visual feedback for focus
                view.animate()
                    .scaleX(if (hasFocus) 1.2f else 1f)
                    .scaleY(if (hasFocus) 1.2f else 1f)
                    .setDuration(0)
                    .start()

                if (index < validLabels.size) {
                    validLabels[index].setTypeface(
                        null,
                        if (hasFocus) Typeface.BOLD else Typeface.NORMAL
                    )
                }

                if (hasFocus) {
                    if (!isSidebarOpen) {
                        showSidebar(activity, sidebar, mainBox, activeButton, btnShows)
                    }
                } else {
                    view.postDelayed({
                        if (!hasAnyButtonFocus() && isSidebarOpen) {
                            // User navigated away via DPAD. Do NOT restore previous focus.
                            hideSidebar(activity, sidebar, mainBox, restoreFocus = false)
                        }
                    }, 50)
                }
            }
        }
        // --- FOCUS MANAGEMENT LOGIC END ---

        // Initial setup
        activeButton?.post {
            if (sidebar.visibility == View.VISIBLE) {
                activeButton.requestFocus()
            }
        }

        // Back button handling
        if (activity is ComponentActivity) {
            activity.onBackPressedDispatcher.addCallback(
                activity,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        if (isSidebarOpen) {
                            validButtons.forEach { it.clearFocus() }
                            // Explicit close via back press. Restore previous focus.
                            hideSidebar(activity, sidebar, mainBox, restoreFocus = true)
                        } else {
                            showSidebar(activity, sidebar, mainBox, activeButton, btnShows)
                        }
                    }
                }
            )
        }

        // Handle taps outside
        mainBox.setOnClickListener {
            if (isSidebarOpen) {
                validButtons.forEach { it.clearFocus() }
                // Explicit close via click. Restore previous focus.
                hideSidebar(activity, sidebar, mainBox, restoreFocus = true)
            }
        }

        if (profileImg != null) loadProfileImage(activity, profileImg)
    }

    private fun showSidebar(activity: Activity, sidebar: FrameLayout, mainBox: CardView, activeButton: View?, btnShows: View?) {
        if (isSidebarOpen) return

        previouslyFocusedView = WeakReference(activity.currentFocus)

        sidebar.visibility = View.VISIBLE
        isSidebarOpen = true

        try { activity.findViewById<LinearLayout>(R.id.NavBar).visibility = View.GONE } catch(e: Exception) {}

        val density = activity.resources.displayMetrics.density
        val params = mainBox.layoutParams as ViewGroup.MarginLayoutParams

        if (mainBox.isLaidOut) {
            //mainBox.radius = 20 * density
            val margin = (0 * density).toInt()
            params.setMargins(margin, 0, 0, 0)
            mainBox.layoutParams = params
        }

        // Fix 1: Guarantee a target view to request focus, bypassing the null issue
        val targetButton = activeButton ?: btnShows
        targetButton?.postDelayed({
            if (isSidebarOpen) {
                targetButton.requestFocus()
            }
        }, 50)
    }

    // Fix 2: Added `restoreFocus` flag to prevent fighting with DPAD navigation
    private fun hideSidebar(activity: Activity, sidebar: FrameLayout, mainBox: CardView, restoreFocus: Boolean) {
        if (!isSidebarOpen) return

        sidebar.visibility = View.GONE
        isSidebarOpen = false

        try { activity.findViewById<LinearLayout>(R.id.NavBar).visibility = View.VISIBLE } catch(e: Exception) {}

        val params = mainBox.layoutParams as ViewGroup.MarginLayoutParams

        if (mainBox.isLaidOut) {
            mainBox.radius = 0f
            params.setMargins(0, 0, 0, 0)
            mainBox.layoutParams = params
        }

        if (restoreFocus) {
            previouslyFocusedView?.get()?.let { view ->
                if (view.isAttachedToWindow) {
                    view.requestFocus()
                }
            }
        }

        previouslyFocusedView = null
    }

    private fun loadProfileImage(activity: Activity, imageView: ImageView) {
        try {
            val avatar = SessionManger(activity).getUserAvatar()
            val avatarPath = "file:///android_asset/$avatar"
            Glide.with(activity)
                .load(avatarPath)
                .transform(CircleCrop())
                .dontAnimate()
                .into(imageView)
        } catch (e: Exception) {
        }
    }
}
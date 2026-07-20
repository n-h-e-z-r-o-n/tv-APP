package com.example.onyx

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.onyx.OnyxObjects.GlobalUtils
import com.example.onyx.OnyxObjects.NavAction
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.onyx.OnyxObjects.LoadingAnimation
import kotlinx.coroutines.cancelChildren

class HomeActivity : AppCompatActivity() {

    var showsFragment: ShowsFragment? = null
    var animeFragment: AnimeFragment? = null
    private var profileFragment: ProfileFragment? = null
    private var watchingFragment: WatchingFragment? = null

    private var notificationFragment: notificationFragment? = null

    private var searchFragment: SearchFragment? = null

    private var activeFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GlobalUtils.applyTheme(this)
        setContentView(R.layout.activity_home)


        ////////////////////////////////////////////////////////////////////////////////////////////
        val loadingImageView = findViewById<ImageView>(R.id.AnimationBG)

        val typedValue = TypedValue()
        theme.resolveAttribute(R.attr.themeImage, typedValue, true)

        Glide.with(this)
            .asGif()
            .load( typedValue.resourceId)
            .into(loadingImageView)
        ///////////////////////////////////////////////////////////////////////////////////////////

        // Set up the sidebar (using existing logic, but we need to override the click behavior)
        NavAction.setupSidebar(this)

        setupSidebarForFragments()

        // Load default fragment
        if (savedInstanceState == null) {
            val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
            val lastFragment = prefs.getString("last_fragment", "shows") ?: "shows"
            
            val btnShows = findViewById<ImageButton>(R.id.sidebarBtnShows)
            val btnAnime = findViewById<ImageButton>(R.id.sidebarBtnAnime)
            val btnProfile = findViewById<CardView>(R.id.sidebarBtnProfile)
            val btnWatching = findViewById<ImageButton>(R.id.sidebarWatchListBtn)
            val searchBtn = findViewById<ImageView>(R.id.sidebarSearchBtn)
            val notificationBtn = findViewById<ImageButton>(R.id.sidebarNotificationBtn)
            
            val buttons = listOf<View>(btnShows, btnAnime, btnProfile, btnWatching, searchBtn, notificationBtn)
            val selectedBtn = when(lastFragment) {
                "shows" -> btnShows
                "anime" -> btnAnime
                "profile" -> btnProfile
                "watching" -> btnWatching
                "favorites" -> findViewById<View>(R.id.sidebarLabelFavorites)
                "search" -> searchBtn
                "notifications" -> notificationBtn
                else -> btnShows
            }
            
            updateSelection(buttons, selectedBtn)
            switchFragment(lastFragment)
        }
    }

    private fun setupSidebarForFragments() {
        val btnShows = findViewById<ImageButton>(R.id.sidebarBtnShows)
        val btnAnime = findViewById<ImageButton>(R.id.sidebarBtnAnime)
        val btnProfile = findViewById<CardView>(R.id.sidebarBtnProfile)
        val btnWatching = findViewById<ImageButton>(R.id.sidebarWatchListBtn)
        val searchBtn = findViewById<ImageView>(R.id.sidebarSearchBtn)
        val notificationBtn = findViewById<ImageButton>(R.id.sidebarNotificationBtn)
        val btnFavorites = findViewById<ImageButton>(R.id.sidebarFavoritesBtn)

        val buttons = listOf(btnShows, btnAnime, btnProfile, btnWatching, btnFavorites)

        btnShows.setOnClickListener {
            updateSelection(buttons, btnShows)
            switchFragment("shows")
        }

        btnAnime.setOnClickListener {
            updateSelection(buttons, btnAnime)
            switchFragment("anime")
        }

        searchBtn.setOnClickListener {
            updateSelection(buttons, searchBtn)
            switchFragment("search")
        }

        notificationBtn.setOnClickListener {
            updateSelection(buttons, notificationBtn)
            switchFragment("notifications")
        }

        btnProfile.setOnClickListener {
            updateSelection(buttons, btnProfile)
            switchFragment("profile")
        }

        btnWatching.setOnClickListener {
            updateSelection(buttons, btnWatching)
            switchFragment("watching")
        }

        btnFavorites?.setOnClickListener {
            updateSelection(buttons, btnFavorites)
            switchFragment("favorites")
        }
    }

    private fun updateSelection(buttons: List<View>, selected: View) {
        buttons.forEach { it.isSelected = (it == selected) }
    }

    private fun switchFragment(tag: String) {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        prefs.edit().putString("last_fragment", tag).apply()

        val fm = supportFragmentManager
        val transaction = fm.beginTransaction()

        // Define your VIP list of core sidebar tabs
        val coreNavigationTags = listOf("shows", "anime", "profile", "watching", "favorites", "notifications", "search")

        // 1. Find ALL currently visible fragments
        fm.fragments.filter { it.isVisible }.forEach { fragment ->

            if (coreNavigationTags.contains(fragment.tag)) {
                // It IS a main sidebar tab -> Hide and FREEZE it
                transaction.hide(fragment)
                transaction.setMaxLifecycle(fragment, Lifecycle.State.STARTED)
            } else {
                // It is a sub-screen (like WatchAnimeFragment) -> DESTROY it completely
                transaction.remove(fragment)
            }
        }

        var targetFragment = fm.findFragmentByTag(tag)
        if (targetFragment == null) {
            // If the fragment doesn't exist, create it
            targetFragment = when (tag) {
                "shows" -> ShowsFragment()
                "anime" -> AnimeFragment()
                "profile" -> ProfileFragment()
                "watching" -> WatchingFragment()
                "favorites" -> FavoritesFragment()
                "notifications" -> notificationFragment()
                "search" -> SearchFragment()
                else -> ShowsFragment()
            }
            transaction.add(R.id.fragmentContainer, targetFragment, tag)

            // Assign our references for clarity, though findFragmentByTag works too
            when (tag) {
                "shows" -> showsFragment = targetFragment as ShowsFragment
                "anime" -> animeFragment = targetFragment as AnimeFragment
                "profile" -> profileFragment = targetFragment as ProfileFragment
                "watching" -> watchingFragment = targetFragment as WatchingFragment
                "notifications" -> notificationFragment = targetFragment as notificationFragment
                "search" -> searchFragment = targetFragment as SearchFragment
            }
        } else {
            // 2. Show and WAKE UP the returning fragment
            transaction.show(targetFragment)
            transaction.setMaxLifecycle(targetFragment, Lifecycle.State.RESUMED)
        }

        activeFragment = targetFragment
        transaction.commit()
    }

    fun navigateToFragment(fragment: Fragment, args: Bundle? = null) {
        if (args != null) {
            fragment.arguments = args
        }
        val fm = supportFragmentManager
        val transaction = fm.beginTransaction()

        // Hide all currently visible fragments (keeps them alive in the background)
        fm.fragments.filter { it.isVisible }.forEach {
            transaction.hide(it)
            transaction.setMaxLifecycle(it, Lifecycle.State.STARTED)
        }

        // Prevent duplicates: Check if an instance of this fragment class already exists and remove it
        val fragmentClass = fragment::class.java
        fm.fragments.filter { it::class.java == fragmentClass }.forEach {
            transaction.remove(it)
        }

        // Add the new fragment
        transaction.add(R.id.fragmentContainer, fragment, fragmentClass.simpleName)
        transaction.commit()
    }

    fun navigateToExistingAndDestroyCurrent(existingFragment: Fragment, fragmentToDestroy: Fragment) {
        val fm = supportFragmentManager
        val transaction = fm.beginTransaction()

        // 1. Explicitly destroy the target fragment for maximum safety
        transaction.remove(fragmentToDestroy)

        // 2. Show the returning fragment
        if (existingFragment.isAdded) {
            transaction.show(existingFragment)

            // Wake up the returning fragment's lifecycle
            transaction.setMaxLifecycle(existingFragment, Lifecycle.State.RESUMED)
        } else {
            // Fallback: Add it if it somehow hasn't been added yet
            transaction.add(R.id.fragmentContainer, existingFragment)
        }

        transaction.commit()
    }

    fun returnToCoreNavigation(fragmentToDestroy: Fragment) {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val lastTag = prefs.getString("last_fragment", "shows") ?: "shows"
        
        // Remove the sub-screen
        supportFragmentManager.beginTransaction().remove(fragmentToDestroy).commit()
        
        // Simulate a click on the sidebar to restore styling and show the correct fragment
        when (lastTag) {
            "shows" -> findViewById<View>(R.id.sidebarBtnShows)?.performClick()
            "anime" -> findViewById<View>(R.id.sidebarBtnAnime)?.performClick()
            "profile" -> findViewById<View>(R.id.sidebarBtnProfile)?.performClick()
            "watching" -> findViewById<View>(R.id.sidebarWatchListBtn)?.performClick()
            "favorites" -> findViewById<View>(R.id.sidebarLabelFavorites)?.performClick()
            "notifications" -> findViewById<View>(R.id.sidebarNotificationBtn)?.performClick()
            "search" -> findViewById<View>(R.id.sidebarSearchBtn)?.performClick()
            else -> findViewById<View>(R.id.sidebarBtnShows)?.performClick()
        }
    }
}
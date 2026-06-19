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
import androidx.lifecycle.Lifecycle
import com.bumptech.glide.Glide
import com.example.onyx.OnyxObjects.LoadingAnimation

class HomeActivity : AppCompatActivity() {

    private var showsFragment: ShowsFragment? = null
    private var animeFragment: AnimeFragment? = null
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
            switchFragment("shows")
            findViewById<ImageButton>(R.id.sidebarBtnShows).isSelected = true
        }
    }

    private fun setupSidebarForFragments() {
        val btnShows = findViewById<ImageButton>(R.id.sidebarBtnShows)
        val btnAnime = findViewById<ImageButton>(R.id.sidebarBtnAnime)
        val btnProfile = findViewById<CardView>(R.id.sidebarBtnProfile)
        val btnWatching = findViewById<ImageButton>(R.id.sidebarWatchListBtn)
        val searchBtn = findViewById<ImageView>(R.id.sidebarSearchBtn)
        val notificationBtn = findViewById<ImageButton>(R.id.sidebarNotificationBtn)

        val buttons = listOf(btnShows, btnAnime, btnProfile, btnWatching)

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


    }
    
    private fun updateSelection(buttons: List<View>, selected: View) {
        buttons.forEach { it.isSelected = (it == selected) }
    }

    private fun switchFragment(tag: String) {
        val fm = supportFragmentManager
        val transaction = fm.beginTransaction()

        if (activeFragment != null) {
            // 1. Hide and FREEZE the currently active fragment
            transaction.hide(activeFragment!!)
            transaction.setMaxLifecycle(activeFragment!!, Lifecycle.State.STARTED)
        }

        var targetFragment = fm.findFragmentByTag(tag)
        if (targetFragment == null) {
            // If the fragment doesn't exist, create it
            targetFragment = when (tag) {
                "shows" -> ShowsFragment()
                "anime" -> AnimeFragment()
                "profile" -> ProfileFragment()
                "watching" -> WatchingFragment()
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
        
        // Hide all visible fragments to prevent focus bleeding
        fm.fragments.filter { it.isVisible }.forEach { 
            transaction.hide(it) 
        }


        val currentFragment = fm.findFragmentById(R.id.fragmentContainer)
        if (currentFragment != null) {
            transaction.hide(currentFragment)
        }

        transaction.add(R.id.fragmentContainer, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            GlobalUtils.hideSystemUI(this)
        }
    }

    override fun onResume() {
        super.onResume()
        GlobalUtils.hideSystemUI(this)
    }
}

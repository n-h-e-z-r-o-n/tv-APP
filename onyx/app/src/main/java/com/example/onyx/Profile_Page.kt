package com.example.onyx

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Profile_Page : AppCompatActivity() {
    
    private lateinit var autoPlaySwitch: Switch
    private lateinit var notificationsSwitch: Switch
    private lateinit var moviesWatchedText: TextView
    private lateinit var seriesWatchedText: TextView
    private lateinit var qualityValueText: TextView
    private lateinit var appVersionText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile_page)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        NavAction.setupSidebar(this)
        
        // Initialize views
        initializeViews()
        
        // Load saved settings
        loadSettings()
        
        // Setup click listeners
        setupClickListeners()
        
        // Load statistics
        loadStatistics()
        
        // Setup focus handling for TV remote
        setupFocusHandling()
    }
    
    private fun initializeViews() {
        autoPlaySwitch = findViewById(R.id.autoPlaySwitch)
        notificationsSwitch = findViewById(R.id.notificationsSwitch)
        moviesWatchedText = findViewById(R.id.moviesWatched)
        seriesWatchedText = findViewById(R.id.seriesWatched)
        qualityValueText = findViewById(R.id.qualityValue)
        appVersionText = findViewById(R.id.appVersion)
        

    }
    
    private fun loadSettings() {
        // Load auto-play setting using GlobalUtils
        autoPlaySwitch.isChecked = GlobalUtils.isAutoPlayEnabled(this)
        
        // Load notifications setting using GlobalUtils
        notificationsSwitch.isChecked = GlobalUtils.areNotificationsEnabled(this)
        
        // Load video quality setting using GlobalUtils
        qualityValueText.text = GlobalUtils.getVideoQuality(this)
    }
    
    private fun setupClickListeners() {
        // Auto-play switch
        autoPlaySwitch.setOnCheckedChangeListener { _, isChecked ->
            GlobalUtils.setAutoPlay(this, isChecked)
            Toast.makeText(this, "Auto-play ${if (isChecked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
        }
        
        // Notifications switch
        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            GlobalUtils.setNotifications(this, isChecked)
            Toast.makeText(this, "Notifications ${if (isChecked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
        }
        
        // Quality setting click
        val qualitySetting = findViewById<LinearLayout>(R.id.qualitySetting)
        qualitySetting.setOnClickListener {
            showQualityDialog()
        }
        
        // Clear cache click
        val clearCache = findViewById<LinearLayout>(R.id.clearCache)
        clearCache.setOnClickListener {
            if (GlobalUtils.clearAppCache(this)) {
                Toast.makeText(this, "Cache cleared successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to clear cache", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Version info click
        val versionInfo = findViewById<LinearLayout>(R.id.versionInfo)
        versionInfo.setOnClickListener {
            Toast.makeText(this, "Onyx TV App v${appVersionText.text}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun loadStatistics() {
        // Load watched movies count using GlobalUtils
        moviesWatchedText.text = GlobalUtils.getMoviesWatched(this).toString()
        
        // Load watched series count using GlobalUtils
        seriesWatchedText.text = GlobalUtils.getSeriesWatched(this).toString()
    }
    
    private fun showQualityDialog() {
        val qualities = arrayOf("720p", "1080p", "4K")
        val currentQuality = qualityValueText.text.toString()
        val currentIndex = qualities.indexOf(currentQuality)
        
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Select Video Quality")
            .setSingleChoiceItems(qualities, currentIndex) { dialog, which ->
                val selectedQuality = qualities[which]
                qualityValueText.text = selectedQuality
                GlobalUtils.setVideoQuality(this, selectedQuality)
                Toast.makeText(this, "Video quality set to $selectedQuality", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    
    private fun setupFocusHandling() {
        // Setup focus handling for TV remote navigation
        val focusableViews = listOf(
            findViewById<LinearLayout>(R.id.autoPlaySetting),
            findViewById<LinearLayout>(R.id.notificationsSetting),
            findViewById<LinearLayout>(R.id.qualitySetting),
            findViewById<LinearLayout>(R.id.versionInfo),
            findViewById<LinearLayout>(R.id.clearCache)
        )
        
        focusableViews.forEach { view ->
            view.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    v.background = getDrawable(R.drawable.setting_item_background)
                    v.scaleX = 1.0f
                    v.scaleY = 1.05f
                } else {
                    v.background = getDrawable(R.drawable.setting_item_background)
                    v.scaleX = 1.0f
                    v.scaleY = 1.0f
                }
            }
        }
        
        // Set initial focus
        findViewById<LinearLayout>(R.id.autoPlaySetting).requestFocus()
    }
    

}
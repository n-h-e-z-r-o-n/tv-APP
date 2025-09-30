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
    private lateinit var themeValueText: TextView
    private lateinit var appVersionText: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        GlobalUtils.applyTheme(this)
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
        themeValueText = findViewById(R.id.themeValue)
        appVersionText = findViewById(R.id.appVersion)
        
        // Set app version using GlobalUtils
        appVersionText.text = GlobalUtils.getAppVersion(this)
    }
    
    private fun loadSettings() {
        // Load auto-play setting using GlobalUtils
        autoPlaySwitch.isChecked = GlobalUtils.isAutoPlayEnabled(this)
        
        // Load notifications setting using GlobalUtils
        notificationsSwitch.isChecked = GlobalUtils.areNotificationsEnabled(this)
        
        // Load video quality setting using GlobalUtils
        qualityValueText.text = GlobalUtils.getVideoQuality(this)
        
        // Load theme setting using GlobalUtils
        val currentTheme = GlobalUtils.getAppTheme(this)
        themeValueText.text = currentTheme.replaceFirstChar { it.uppercase() }
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
        
        // Theme setting click
        val themeSetting = findViewById<LinearLayout>(R.id.themeSetting)
        themeSetting.setOnClickListener {
            showThemeDialog()
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
        
        // Check for updates click
        val checkUpdates = findViewById<LinearLayout>(R.id.checkUpdates)
        checkUpdates.setOnClickListener {
            checkForUpdates()
        }
        
        // Restart app click
        val restartApp = findViewById<LinearLayout>(R.id.restartApp)
        restartApp.setOnClickListener {
            showRestartDialog()
        }
        
        // Terms and Conditions click
        val termsAndConditions = findViewById<LinearLayout>(R.id.termsAndConditions)
        termsAndConditions.setOnClickListener {
            startActivity(android.content.Intent(this, TermsAndConditionsActivity::class.java))
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
    
    private fun showThemeDialog() {
        val themes = GlobalUtils.getAvailableThemes()
        val currentTheme = GlobalUtils.getAppTheme(this)
        val currentIndex = themes.indexOf(currentTheme)
        
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Select App Theme")
            .setSingleChoiceItems(themes.map { it.replaceFirstChar { char -> char.uppercase() } }.toTypedArray(), currentIndex) { dialog, which ->
                val selectedTheme = themes[which]
                GlobalUtils.setAppTheme(this, selectedTheme)
                themeValueText.text = selectedTheme.replaceFirstChar { it.uppercase() }
                dialog.dismiss()
                
                // Show restart suggestion dialog
                showThemeChangeDialog(selectedTheme)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun checkForUpdates() {
        // Simulate checking for updates
        Toast.makeText(this, "Checking for updates...", Toast.LENGTH_SHORT).show()
        
        // In a real app, you would check with your update server here
        // For now, we'll just show a message
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            Toast.makeText(this, "You are using the latest version!", Toast.LENGTH_LONG).show()
        }, 2000)
    }
    
    private fun showThemeChangeDialog(selectedTheme: String) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Theme Changed")
            .setMessage("Theme changed to ${selectedTheme.replaceFirstChar { it.uppercase() }}. Would you like to restart the app now to see the full effect?")
            .setPositiveButton("Restart Now") { _, _ ->
                Toast.makeText(this, "Restarting app...", Toast.LENGTH_SHORT).show()
                GlobalUtils.restartApp(this)
            }
            .setNegativeButton("Later") { _, _ ->
                Toast.makeText(this, "Theme will be applied after restart", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
    
    private fun showRestartDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Restart App")
            .setMessage("Are you sure you want to restart the application? This will close all current activities and restart the app.")
            .setPositiveButton("Restart") { _, _ ->
                Toast.makeText(this, "Restarting app...", Toast.LENGTH_SHORT).show()
                GlobalUtils.restartApp(this)
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
            findViewById<LinearLayout>(R.id.themeSetting),
            findViewById<LinearLayout>(R.id.versionInfo),
            findViewById<LinearLayout>(R.id.clearCache),
            findViewById<LinearLayout>(R.id.checkUpdates),
            findViewById<LinearLayout>(R.id.restartApp),
            findViewById<LinearLayout>(R.id.termsAndConditions)
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
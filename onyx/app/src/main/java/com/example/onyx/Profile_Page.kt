package com.example.onyx

import android.Manifest
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class Profile_Page : AppCompatActivity() {
    
    private lateinit var autoPlaySwitch: Switch
    private lateinit var notificationsSwitch: Switch
    private lateinit var moviesWatchedText: TextView
    private lateinit var seriesWatchedText: TextView
    private lateinit var qualityValueText: TextView
    private lateinit var themeValueText: TextView
    private lateinit var appVersionText: TextView
    
    // APK Update related properties
    private var progressDialog: ProgressDialog? = null
    private val installPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, proceed with installation
            Toast.makeText(this, "Install permission granted", Toast.LENGTH_SHORT).show()
        } else {
            // Permission denied, show settings dialog
            showInstallPermissionDialog()
        }
    }
    
    // GitHub raw URL for the APK file - replace with your actual URL
    private val apkDownloadUrl = "https://github.com/n-h-e-z-r-o-n/tv-APP/raw/refs/heads/main/onyx/app/release/app-release.apk"
    
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

        getRemainingDays()
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
        
        val builder = androidx.appcompat.app.AlertDialog.Builder(this, R.style.CustomDialogTheme)
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
        
        val builder = androidx.appcompat.app.AlertDialog.Builder(this, R.style.CustomDialogTheme)
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
        Toast.makeText(this, "Checking for updates...", Toast.LENGTH_SHORT).show()
        
        // Check if install permission is granted (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                // Request install permission
                installPermissionLauncher.launch(Manifest.permission.REQUEST_INSTALL_PACKAGES)
                return
            }
        }
        
        // Check storage permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1001)
            return
        }
        
        // Start download
        downloadAndInstallApk()
    }
    
    private fun downloadAndInstallApk() {
        progressDialog = ProgressDialog(this).apply {
            setTitle("Downloading Update")
            setMessage("Please wait while we download the latest version...")
            setCancelable(false)
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            show()
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(apkDownloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()
                
                val fileLength = connection.contentLength
                val input: InputStream = connection.inputStream
                
                // Create downloads directory if it doesn't exist
                val downloadsDir = File(getExternalFilesDir(null), "OnyxUpdates")
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }

                val apkFile = File(downloadsDir, "onyx-update.apk")
                val output = FileOutputStream(apkFile)
                
                val data = ByteArray(1024)
                var total: Long = 0
                var count: Int
                
                while (input.read(data).also { count = it } != -1) {
                    total += count.toLong()
                    output.write(data, 0, count)
                    
                    // Update progress
                    withContext(Dispatchers.Main) {
                        progressDialog?.max = fileLength
                        progressDialog?.progress = total.toInt()
                    }
                }
                
                output.flush()
                output.close()
                input.close()
                
                withContext(Dispatchers.Main) {
                    progressDialog?.dismiss()
                    installApk(apkFile)
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog?.dismiss()
                    Toast.makeText(this@Profile_Page, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun installApk(apkFile: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(this, "${packageName}.fileprovider", apkFile)
            } else {
                Uri.fromFile(apkFile)
            }
            
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(intent)
            Toast.makeText(this, "Installation started", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Toast.makeText(this, "Installation failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun showInstallPermissionDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this, R.style.CustomDialogTheme)
        builder.setTitle("Install Permission Required")
            .setMessage("This app needs permission to install APK files. Please enable 'Install unknown apps' permission in settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showThemeChangeDialog(selectedTheme: String) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this, R.style.CustomDialogTheme)
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
        val builder = androidx.appcompat.app.AlertDialog.Builder(this, R.style.CustomDialogTheme)
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
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            1001 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Storage permission granted, proceed with download
                    downloadAndInstallApk()
                } else {
                    Toast.makeText(this, "Storage permission is required to download updates", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        progressDialog?.dismiss()
    }


    private fun getRemainingDays() {
        val subscriptionWidget =  findViewById<TextView>(R.id.SubscriptionLeft)
        val prefs = getSharedPreferences("SubscriptionPrefs", Context.MODE_PRIVATE)
        val lastPaymentTime = prefs.getLong("lastPaymentTime", 0L)

        if (lastPaymentTime == 0L) {
            // No record found (user never paid)
            subscriptionWidget.text = "(user never paid)"
        }

        // Subscription duration: 30 days (in milliseconds)
        val subscriptionDuration = 30L * 24 * 60 * 60 * 1000

        val currentTime = System.currentTimeMillis()
        val expiryTime = lastPaymentTime + subscriptionDuration

        // If already expired
        if (currentTime >= expiryTime) {
            subscriptionWidget.text = "expired"
        }

        // Calculate remaining time in days
        val remainingMillis = expiryTime - currentTime
        val remainingDays = (remainingMillis / (24 * 60 * 60 * 1000)).toInt()


        subscriptionWidget.text = remainingDays.toString()
    }


}
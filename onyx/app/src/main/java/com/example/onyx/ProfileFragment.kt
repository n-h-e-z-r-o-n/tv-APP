package com.example.onyx

import android.Manifest
import android.view.LayoutInflater
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

import com.example.onyx.Database.AppDatabase
import com.example.onyx.Database.SessionManger
import com.example.onyx.OnyxObjects.GlobalUtils
import com.example.onyx.OnyxObjects.NavAction


import androidx.fragment.app.Fragment
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var db: AppDatabase
    private lateinit var  sm: SessionManger

    private lateinit var moviesWatchedText: TextView
    private lateinit var seriesWatchedText: TextView
    private lateinit var qualityValueText: TextView
    private lateinit var themeValueText: TextView
    private lateinit var appVersionText: TextView
    
    // APK Update related properties
    // APK Update related properties
    private var updateDialog: androidx.appcompat.app.AlertDialog? = null
    // installPermissionLauncher removed as it's not applicable for REQUEST_INSTALL_PACKAGES (requires Intent)
    
    private val versionJsonUrl = BuildConfig.APPV_J //"https://github.com/n-h-e-z-r-o-n/tv-APP/raw/refs/heads/main/App/version.json"
    
    // Data class for update info
    data class UpdateInfo(
        @com.google.gson.annotations.SerializedName("versionCode")
        val versionCode: Int,
        @com.google.gson.annotations.SerializedName("versionName")
        val versionName: String,
        @com.google.gson.annotations.SerializedName("changelog")
        val changelog: String,
        @com.google.gson.annotations.SerializedName("downloadUrl")
        val downloadUrl: String
    )
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        GlobalUtils.applyTheme(requireActivity())
        super.onViewCreated(view, savedInstanceState)
        
        
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        db = AppDatabase(requireActivity())         // Initialize database
        sm = SessionManger(requireActivity())         // Initialize session manager

        


        ////////////////////////////////////////////////////////////////////////////////////////////
        val loadingImageView = requireView().findViewById<ImageView>(R.id.backgroundImgContainer)

        val typedValue = TypedValue()
        requireActivity().theme.resolveAttribute(R.attr.themeImage, typedValue, true)

        Glide.with(requireActivity())
            .asGif()
            .load( typedValue.resourceId)
            .into(loadingImageView)

        ////////////////////////////////////////////////////////////////////////////////////////////
        val profileImage = requireView().findViewById<ImageView>(R.id.ProfileImg)
        val assetPath = "file:///android_asset/${sm.getUserAvatar()}"
        Glide.with(requireActivity())
            .load(assetPath)
            .placeholder(R.drawable.ic_person)
            .into(profileImage)

        val logoutBtn = requireView().findViewById<LinearLayout>(R.id.logoutBtn)
        logoutBtn.setOnClickListener {
            sm.clearSession()
            val intent = Intent(requireActivity(), Login_Page::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            // requireActivity().finish()
        }
        ////////////////////////////////////////////////////////////////////////////////////////////

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

    override fun onResume() {
        super.onResume()
        val rootView = requireActivity().window.decorView.rootView
        if (rootView.findFocus() == null) {
            requireView().findViewById<LinearLayout>(R.id.themeSetting).requestFocus()
        }
    }
    
    private fun initializeViews() {
        moviesWatchedText = requireView().findViewById(R.id.moviesWatched)
        seriesWatchedText = requireView().findViewById(R.id.seriesWatched)
        themeValueText = requireView().findViewById(R.id.themeValue)
        appVersionText = requireView().findViewById(R.id.appVersion)
        
        // Set app version using GlobalUtils
        appVersionText.text = GlobalUtils.getAppVersion(requireActivity())
    }
    
    private fun loadSettings() {
        // Load requireActivity().theme setting using GlobalUtils
        val currentTheme = GlobalUtils.getAppTheme(requireActivity())
        themeValueText.text = currentTheme.replaceFirstChar { it.uppercase() }
    }
    
    private fun setupClickListeners() {
        // Theme setting click
        val themeSetting = requireView().findViewById<LinearLayout>(R.id.themeSetting)
        themeSetting.setOnClickListener {
            showThemeDialog()
        }
        themeSetting.requestFocus()
        
        // Clear cache click
        val clearCache = requireView().findViewById<LinearLayout>(R.id.clearCache)
        clearCache.setOnClickListener {
            if (GlobalUtils.clearAppCache(requireActivity())) {
                Toast.makeText(requireActivity(), "Cache cleared successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireActivity(), "Failed to clear cache", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Version info click
        val versionInfo = requireView().findViewById<LinearLayout>(R.id.versionInfo)
        versionInfo.setOnClickListener {
            Toast.makeText(requireActivity(), "Onyx TV App v${appVersionText.text}", Toast.LENGTH_LONG).show()
        }
        
        // Check for updates click
        val checkUpdates = requireView().findViewById<LinearLayout>(R.id.checkUpdates)
        checkUpdates.setOnClickListener {
            checkForUpdates()
        }
        
        // Restart app click
        val restartApp = requireView().findViewById<LinearLayout>(R.id.restartApp)
        restartApp.setOnClickListener {
            showRestartDialog()
        }
        
        // Terms and Conditions click
        val termsAndConditions = requireView().findViewById<LinearLayout>(R.id.termsAndConditions)
        termsAndConditions.setOnClickListener {
            startActivity(android.content.Intent(requireActivity(), TermsAndConditionsActivity::class.java))
        }
    }
    
    private fun loadStatistics() {
        // Load watched movies count using GlobalUtils
        moviesWatchedText.text = GlobalUtils.getMoviesWatched(requireActivity()).toString()
        
        // Load watched series count using GlobalUtils
        seriesWatchedText.text = GlobalUtils.getSeriesWatched(requireActivity()).toString()
    }


    private fun showThemeDialog() {
        val themes = GlobalUtils.getAvailableThemes()
        val currentTheme = GlobalUtils.getAppTheme(requireActivity())
        val currentIndex = themes.indexOf(currentTheme)

        val themeNames = themes
            .map { it.replaceFirstChar { c -> c.uppercase() } }
            .toTypedArray()

        val builder = androidx.appcompat.app.AlertDialog.Builder(requireActivity(), R.style.CustomDialogTheme)
            .setTitle("Select App Theme")
            .setSingleChoiceItems(themeNames, currentIndex) { dialog, which ->
                val selectedTheme = themes[which]
                GlobalUtils.setAppTheme(requireActivity(), selectedTheme)
                themeValueText.text = selectedTheme.replaceFirstChar { it.uppercase() }
                dialog.dismiss()

                val intent = Intent(requireActivity(), this::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)

                // Optional: finish current activity manually to be safe
                //// requireActivity().finish()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

        val dialog = builder.create()

        dialog.setOnShowListener {
            val alertDialog = it as AlertDialog
            val listView = alertDialog.listView ?: return@setOnShowListener

            // Resolve FG color
            val fgValue = TypedValue()
            requireActivity().theme.resolveAttribute(R.attr.FG_color, fgValue, true)
            val fgColor = fgValue.data

            // Resolve Accent color (focus / selection)
            val accentValue = TypedValue()
            requireActivity().theme.resolveAttribute(R.attr.AccentColor, accentValue, true)
            val accentColor = accentValue.data

            // Set focus/selection color
            listView.selector = ColorDrawable(accentColor)
            listView.choiceMode = ListView.CHOICE_MODE_SINGLE

            // Ensure text color stays correct
            for (i in 0 until listView.childCount) {
                val child = listView.getChildAt(i)
                if (child is TextView) {
                    child.setTextColor(fgColor)
                }
            }
            // Style negative button
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(fgColor)
        }

        dialog.show()
    }


    private fun checkForUpdates() {
        Toast.makeText(requireActivity(), "Checking for updates...", Toast.LENGTH_SHORT).show()

        // Check if install permission is granted (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!requireActivity().packageManager.canRequestPackageInstalls()) {
                // Request install permission
                showInstallPermissionDialog()
                return
            }
        }

        lifecycleScope.launch(Dispatchers.IO){
            try {
                val url = URL(versionJsonUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val reader = java.io.InputStreamReader(inputStream)
                    val updateInfo = com.google.gson.Gson().fromJson(reader, UpdateInfo::class.java)
                    
                    withContext(Dispatchers.Main) {
                    if (!isAdded || view == null) return@withContext
                        // Get the ACTUAL installed version code from PackageManager
                        // This is more reliable than BuildConfig.VERSION_CODE which is compiled at build time
                        val installedVersionCode = try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                requireActivity().packageManager.getPackageInfo(requireActivity().packageName, PackageManager.PackageInfoFlags.of(0)).longVersionCode.toInt()
                            } else {
                                @Suppress("DEPRECATION")
                                requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0).versionCode
                            }
                        } catch (e: Exception) {
                            // Fallback to BuildConfig if PackageManager fails
                            BuildConfig.VERSION_CODE
                        }
                        
                        // Debug logging to help diagnose issues
                        android.util.Log.d("UpdateCheck", "Installed versionCode: $installedVersionCode, Remote versionCode: ${updateInfo.versionCode}")
                        
                        if (updateInfo.versionCode > installedVersionCode) {
                            showUpdateConfirmation(updateInfo)
                        } else {
                            Toast.makeText(requireActivity(), "App is up to date", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                    if (!isAdded || view == null) return@withContext
                         Toast.makeText(requireActivity(), "Failed to check for updates: Server Error", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded || view == null) return@withContext
                    e.printStackTrace()
                    Toast.makeText(requireActivity(), "Failed to check for updates: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showUpdateConfirmation(updateInfo: UpdateInfo) {
        androidx.appcompat.app.AlertDialog.Builder(requireActivity(), R.style.CustomDialogTheme)
            .setTitle("Update Available: v${updateInfo.versionName}")
            .setMessage("Changelog:\n${updateInfo.changelog}\n\nWould you like to update now?")
            .setPositiveButton("Update Now") { _, _ ->
                downloadAndInstallApk(updateInfo.downloadUrl)
            }
            .setNegativeButton("Later", null)
            .show()
    }
    
    private fun downloadAndInstallApk(downloadUrlString: String) {
        // Setup custom progress dialog
        val dialogView = LayoutInflater.from(requireActivity()).inflate(R.layout.dialog_update_progress, null)
        val progressBar = dialogView.findViewById<android.widget.ProgressBar>(R.id.updateProgressBar)
        val progressText = dialogView.findViewById<TextView>(R.id.updateProgressText)
        val sizeText = dialogView.findViewById<TextView>(R.id.updateSizeText)
        
        updateDialog = androidx.appcompat.app.AlertDialog.Builder(requireActivity(), R.style.CustomDialogTheme)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        
        updateDialog?.show()
        
        // Make the dialog background transparent to show the CardView corners
        updateDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(downloadUrlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()
                
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("Server returned HTTP ${connection.responseCode} ${connection.responseMessage}")
                }
                
                val fileLength = connection.contentLength
                val input: InputStream = connection.inputStream
                
                // Create downloads directory if it doesn't exist - using app-specific directory which needs no permissions
                val downloadsDir = File(requireActivity().getExternalFilesDir(null), "OnyxUpdates")
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }

                val apkFile = File(downloadsDir, "onyx-update.apk")
                // Delete old update if exists
                if (apkFile.exists()) {
                    apkFile.delete()
                }
                
                val output = FileOutputStream(apkFile)
                
                val data = ByteArray(4096) // Increased buffer size
                var total: Long = 0
                var count: Int
                
                while (input.read(data).also { count = it } != -1) {
                    total += count.toLong()
                    output.write(data, 0, count)
                    
                    // Update progress
                    if (fileLength > 0) {
                        // Calculate percentage
                        val progress = (total * 100 / fileLength).toInt()
                        
                        withContext(Dispatchers.Main) {
                    if (!isAdded || view == null) return@withContext
                            progressBar.progress = progress
                            progressText.text = "$progress%"
                            
                            // Format bytes to MB
                            val totalMb = String.format("%.1f", total / (1024f * 1024f))
                            val maxMb = String.format("%.1f", fileLength / (1024f * 1024f))
                            sizeText.text = "$totalMb MB / $maxMb MB"
                        }
                    }
                }
                
                output.flush()
                output.close()
                input.close()
                
                withContext(Dispatchers.Main) {
                    if (!isAdded || view == null) return@withContext
                    updateDialog?.dismiss()
                    installApk(apkFile)
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded || view == null) return@withContext
                    updateDialog?.dismiss()
                    e.printStackTrace()
                    Toast.makeText(requireActivity(), "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun installApk(apkFile: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(requireActivity(), "${requireActivity().packageName}.fileprovider", apkFile)
            } else {
                Uri.fromFile(apkFile)
            }
            
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(intent)
            Toast.makeText(requireActivity(), "Installation started", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Toast.makeText(requireActivity(), "Installation failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun showInstallPermissionDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireActivity(), R.style.CustomDialogTheme)
        builder.setTitle("Install Permission Required")
            .setMessage("This app needs permission to install APK files. Please enable 'Install unknown apps' permission in settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${requireActivity().packageName}")
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showThemeChangeDialog(selectedTheme: String) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireActivity(), R.style.CustomDialogTheme)
        builder.setTitle("Theme Changed")
            .setMessage("Theme changed to ${selectedTheme.replaceFirstChar { it.uppercase() }}. Would you like to restart the app now to see the full effect?")
            .setPositiveButton("Restart Now") { _, _ ->
                Toast.makeText(requireActivity(), "Restarting app...", Toast.LENGTH_SHORT).show()
                GlobalUtils.restartApp(requireActivity())
            }
            .setNegativeButton("Later") { _, _ ->
                Toast.makeText(requireActivity(), "Theme will be applied after restart", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
    
    private fun showRestartDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireActivity(), R.style.CustomDialogTheme)
        builder.setTitle("Restart App")
            .setMessage("Are you sure you want to restart the application? This will close all current activities and restart the app.")
            .setPositiveButton("Restart") { _, _ ->
                Toast.makeText(requireActivity(), "Restarting app...", Toast.LENGTH_SHORT).show()
                GlobalUtils.restartApp(requireActivity())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    
    private fun setupFocusHandling() {
        // Setup focus handling for TV remote navigation
        val focusableViews = listOf(
            requireView().findViewById<LinearLayout>(R.id.themeSetting),
            requireView().findViewById<LinearLayout>(R.id.versionInfo),
            requireView().findViewById<LinearLayout>(R.id.clearCache),
            requireView().findViewById<LinearLayout>(R.id.checkUpdates),
            requireView().findViewById<LinearLayout>(R.id.restartApp),
            requireView().findViewById<LinearLayout>(R.id.termsAndConditions)
        )
        
        focusableViews.forEach { view ->
            view.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    v.background = requireActivity().getDrawable(R.drawable.setting_item_background)
                    v.scaleX = 1.0f
                    v.scaleY = 1.05f
                } else {
                    v.background = requireActivity().getDrawable(R.drawable.setting_item_background)
                    v.scaleX = 1.0f
                    v.scaleY = 1.0f
                }
            }
        }

    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            1001 -> {
                // Storage permission request result - No longer needed/used
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        updateDialog?.dismiss()
    }


    private fun getRemainingDays() {
        val subscriptionWidget = requireView().findViewById<TextView>(R.id.SubscriptionLeft)

        // Launch coroutine on main thread (since we need to update UI)
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                // Switch to IO thread for database operation
                val remainingDays = withContext(Dispatchers.IO) {
                    db.getSubscriptionDaysLeft()
                }

                // Back on main thread to update UI
                subscriptionWidget.text = when {
                    remainingDays <= 0 -> "expired"
                    else -> remainingDays.toString()
                }
            } catch (e: Exception) {
                subscriptionWidget.text = "N/A"
                Log.e("Profile_Page", "Error getting subscription days", e)
            }
        }
    }




}
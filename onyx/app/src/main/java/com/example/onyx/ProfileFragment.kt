package com.example.onyx

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.onyx.Database.AppDatabase
import com.example.onyx.Database.SessionManger
import com.example.onyx.OnyxObjects.GlobalUtils
import com.example.onyx.databinding.FragmentProfileBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: AppDatabase
    private lateinit var sm: SessionManger

    private var updateDialog: AlertDialog? = null
    private var themeDialog: AlertDialog? = null
    private var restartDialog: AlertDialog? = null
    private var logoutDialog: AlertDialog? = null

    private val versionJsonUrl = BuildConfig.APPV_J
    private var lastFocusedViewId: Int = R.id.themeSetting
    private var currentUserId: Int = -1

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

    private data class ProfileUiState(
        val username: String,
        val memberLabel: String,
        val moviesWatched: Int,
        val episodesWatched: Int,
        val animeFavorites: Int,
        val subscriptionDaysLeft: Long,
        val subscriptionType: String,
        val subscriptionActive: Boolean
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        db = AppDatabase(requireActivity())
        sm = SessionManger(requireActivity())
        currentUserId = sm.getUserId()

        initializeStaticUi()
        loadProfileImage()
        loadSettings()
        setupClickListeners()
        setupFocusHandling()
        loadProfileData()
    }

    override fun onResume() {
        super.onResume()
        binding.root.post {
            val target = binding.root.findViewById<View>(lastFocusedViewId) ?: binding.themeSetting
            if (target.isShown && target.isFocusable) {
                target.requestFocus()
            } else {
                binding.themeSetting.requestFocus()
            }
        }
    }

    private fun initializeStaticUi() {
        binding.appVersion.text = GlobalUtils.getAppVersion(requireActivity())
    }

    private fun loadProfileImage() {
        val assetPath = "file:///android_asset/${sm.getUserAvatar()}"
        Glide.with(this)
            .load(assetPath)
            .placeholder(R.drawable.ic_person)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(binding.profileImage)
    }

    private fun loadSettings() {
        val currentTheme = GlobalUtils.getAppTheme(requireActivity())
        binding.themeValue.text = GlobalUtils.getThemeDisplayName(currentTheme)

        val isDynamicColor = sm.isDynamicColorEnabled()
        binding.dynamicColorValue.text = if (isDynamicColor) "On" else "Off"
    }

    private fun setupClickListeners() {
        binding.logoutBtn.setOnClickListener {
            showLogoutDialog()
        }

        binding.themeSetting.setOnClickListener {
            showThemeDialog()
        }

        binding.dynamicColorSetting.setOnClickListener {
            val newState = !sm.isDynamicColorEnabled()
            sm.setDynamicColorEnabled(newState)
            binding.dynamicColorValue.text = if (newState) "On" else "Off"
            Toast.makeText(
                requireActivity(),
                "Dynamic Color ${if (newState) "Enabled" else "Disabled"}",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.clearCache.setOnClickListener {
            val cleared = GlobalUtils.clearAppCache(requireActivity())
            Toast.makeText(
                requireActivity(),
                if (cleared) "Cache cleared successfully" else "Failed to clear cache",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.versionInfo.setOnClickListener {
            Toast.makeText(
                requireActivity(),
                "Installed version ${binding.appVersion.text}",
                Toast.LENGTH_LONG
            ).show()
        }

        binding.checkUpdates.setOnClickListener {
            checkForUpdates()
        }

        binding.restartApp.setOnClickListener {
            showRestartDialog()
        }

        binding.termsAndConditions.setOnClickListener {
            startActivity(Intent(requireActivity(), TermsAndConditionsActivity::class.java))
        }
    }

    private fun setupFocusHandling() {
        val focusableViews = listOf(
            binding.logoutBtn,
            binding.themeSetting,
            binding.dynamicColorSetting,
            binding.versionInfo,
            binding.clearCache,
            binding.checkUpdates,
            binding.restartApp,
            binding.termsAndConditions
        )

        focusableViews.forEach { itemView ->
            itemView.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    lastFocusedViewId = view.id
                }
                view.animate()
                    .scaleX(if (hasFocus) 1.03f else 1f)
                    .scaleY(if (hasFocus) 1.03f else 1f)
                    .setDuration(140)
                    .start()
            }
        }
    }

    private fun loadProfileData() {
        lifecycleScope.launch(Dispatchers.Main) {
            val uiState = withContext(Dispatchers.IO) {
                db.resetExpiredSubscription()

                val username = db.getUsernameById(currentUserId)
                    ?.takeIf { it.isNotBlank() }
                    ?: "Profile $currentUserId"
                val subscriptionType = db.getSubscriptionType()
                val subscriptionActive = db.isSubscriptionActive()
                val subscriptionDaysLeft = db.getSubscriptionDaysLeft()

                ProfileUiState(
                    username = username,
                    memberLabel = buildMemberLabel(currentUserId, subscriptionType, subscriptionActive),
                    moviesWatched = GlobalUtils.getMoviesWatched(requireActivity()),
                    episodesWatched = GlobalUtils.getSeriesWatched(requireActivity()),
                    animeFavorites = db.getFavoriteAnimeCount(currentUserId),
                    subscriptionDaysLeft = subscriptionDaysLeft,
                    subscriptionType = subscriptionType,
                    subscriptionActive = subscriptionActive
                )
            }

            if (!isAdded || _binding == null) return@launch
            renderProfileData(uiState)
        }
    }

    private fun renderProfileData(uiState: ProfileUiState) {
        binding.profileName.text = uiState.username
        binding.profileMeta.text = uiState.memberLabel
        binding.moviesWatched.text = uiState.moviesWatched.toString()
        binding.seriesWatched.text = uiState.episodesWatched.toString()
        binding.animeWatched.text = uiState.animeFavorites.toString()

        if (uiState.subscriptionActive) {
            binding.subscriptionLeft.text = uiState.subscriptionDaysLeft.toString()
            binding.subscriptionLeft.setTextColor(
                ContextCompat.getColor(requireContext(), android.R.color.holo_green_light)
            )
            binding.subscriptionStatus.text = "${formatSubscriptionType(uiState.subscriptionType)} active"
        } else {
            binding.subscriptionLeft.text = "0"
            binding.subscriptionLeft.setTextColor(
                ContextCompat.getColor(requireContext(), android.R.color.holo_red_light)
            )
            binding.subscriptionStatus.text = "Inactive"
        }
    }

    private fun buildMemberLabel(userId: Int, subscriptionType: String, subscriptionActive: Boolean): String {
        val planLabel = if (subscriptionActive) {
            formatSubscriptionType(subscriptionType)
        } else {
            "Free plan"
        }
        return "Member #$userId • $planLabel"
    }

    private fun formatSubscriptionType(subscriptionType: String): String {
        return when (subscriptionType.uppercase()) {
            "MONTHLY" -> "Monthly plan"
            "3MONTH" -> "3-month plan"
            "YEARLY" -> "Yearly plan"
            else -> "Free plan"
        }
    }

    private fun showLogoutDialog() {
        logoutDialog = AlertDialog.Builder(requireActivity(), R.style.CustomDialogTheme)
            .setTitle("Logout")
            .setMessage("Sign out of this profile and return to profile selection?")
            .setPositiveButton("Logout") { _, _ ->
                sm.clearSession()
                val intent = Intent(requireActivity(), Login_Page::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .create()

        logoutDialog?.show()
    }

    private fun showThemeDialog() {
        val themes = GlobalUtils.getAvailableThemes()
        val currentTheme = GlobalUtils.getAppTheme(requireActivity())
        val currentIndex = themes.indexOf(currentTheme)
        val themeNames = themes.map { GlobalUtils.getThemeDisplayName(it) }.toTypedArray()

        themeDialog = AlertDialog.Builder(requireActivity(), R.style.CustomDialogTheme)
            .setTitle("Select App Theme")
            .setSingleChoiceItems(themeNames, currentIndex) { dialog, which ->
                val selectedTheme = themes[which]
                val previousTheme = GlobalUtils.getAppTheme(requireActivity())

                if (selectedTheme == previousTheme) {
                    dialog.dismiss()
                    return@setSingleChoiceItems
                }

                GlobalUtils.setAppTheme(requireActivity(), selectedTheme)
                binding.themeValue.text = GlobalUtils.getThemeDisplayName(selectedTheme)
                dialog.dismiss()
                requireActivity().recreate()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        themeDialog?.setOnShowListener {
            val alertDialog = it as AlertDialog
            val listView = alertDialog.listView ?: return@setOnShowListener

            val fgValue = TypedValue()
            requireActivity().theme.resolveAttribute(R.attr.FG_color, fgValue, true)
            val fgColor = fgValue.data

            val accentValue = TypedValue()
            requireActivity().theme.resolveAttribute(R.attr.AccentColor, accentValue, true)
            val accentColor = accentValue.data

            listView.selector = ColorDrawable(accentColor)
            listView.choiceMode = ListView.CHOICE_MODE_SINGLE

            for (index in 0 until listView.childCount) {
                val child = listView.getChildAt(index)
                if (child is TextView) {
                    child.setTextColor(fgColor)
                }
            }

            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(fgColor)
        }

        themeDialog?.show()
    }

    private fun checkForUpdates() {
        Toast.makeText(requireActivity(), "Checking for updates...", Toast.LENGTH_SHORT).show()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !requireActivity().packageManager.canRequestPackageInstalls()
        ) {
            showInstallPermissionDialog()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val connection = (URL(versionJsonUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                    connect()
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = connection.inputStream.reader()
                    val updateInfo = com.google.gson.Gson().fromJson(reader, UpdateInfo::class.java)

                    withContext(Dispatchers.Main) {
                        if (!isAdded || _binding == null) return@withContext
                        val installedVersionCode = getInstalledVersionCode()
                        Log.d(
                            "UpdateCheck",
                            "Installed versionCode: $installedVersionCode, Remote versionCode: ${updateInfo.versionCode}"
                        )

                        if (updateInfo.versionCode > installedVersionCode) {
                            showUpdateConfirmation(updateInfo)
                        } else {
                            Toast.makeText(requireActivity(), "App is up to date", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        if (!isAdded || _binding == null) return@withContext
                        Toast.makeText(
                            requireActivity(),
                            "Failed to check for updates: Server Error",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (error: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded || _binding == null) return@withContext
                    error.printStackTrace()
                    Toast.makeText(
                        requireActivity(),
                        "Failed to check for updates: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun getInstalledVersionCode(): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requireActivity()
                    .packageManager
                    .getPackageInfo(
                        requireActivity().packageName,
                        PackageManager.PackageInfoFlags.of(0)
                    )
                    .longVersionCode
                    .toInt()
            } else {
                @Suppress("DEPRECATION")
                requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0).versionCode
            }
        } catch (error: Exception) {
            BuildConfig.VERSION_CODE
        }
    }

    private fun showUpdateConfirmation(updateInfo: UpdateInfo) {
        AlertDialog.Builder(requireActivity(), R.style.CustomDialogTheme)
            .setTitle("Update Available: v${updateInfo.versionName}")
            .setMessage("Changelog:\n${updateInfo.changelog}\n\nWould you like to update now?")
            .setPositiveButton("Update Now") { _, _ ->
                downloadAndInstallApk(updateInfo.downloadUrl)
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun downloadAndInstallApk(downloadUrlString: String) {
        val dialogView = LayoutInflater.from(requireActivity()).inflate(R.layout.dialog_update_progress, null)
        val progressBar = dialogView.findViewById<android.widget.ProgressBar>(R.id.updateProgressBar)
        val progressText = dialogView.findViewById<TextView>(R.id.updateProgressText)
        val sizeText = dialogView.findViewById<TextView>(R.id.updateSizeText)

        updateDialog = AlertDialog.Builder(requireActivity(), R.style.CustomDialogTheme)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        updateDialog?.show()
        updateDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val connection = (URL(downloadUrlString).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                    connect()
                }

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("Server returned HTTP ${connection.responseCode} ${connection.responseMessage}")
                }

                val fileLength = connection.contentLength
                val input: InputStream = connection.inputStream
                val downloadsDir = File(requireActivity().getExternalFilesDir(null), "OnyxUpdates")
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }

                val apkFile = File(downloadsDir, "onyx-update.apk")
                if (apkFile.exists()) {
                    apkFile.delete()
                }

                val output = FileOutputStream(apkFile)
                val data = ByteArray(4096)
                var total = 0L
                var count: Int
                var lastProgress = 0

                while (input.read(data).also { count = it } != -1) {
                    total += count.toLong()
                    output.write(data, 0, count)

                    if (fileLength > 0) {
                        val progress = (total * 100 / fileLength).toInt()
                        if (progress > lastProgress) {
                            lastProgress = progress
                            withContext(Dispatchers.Main) {
                                if (!isAdded || _binding == null) return@withContext
                                progressBar.progress = progress
                                progressText.text = "$progress%"

                                val totalMb = String.format("%.1f", total / (1024f * 1024f))
                                val maxMb = String.format("%.1f", fileLength / (1024f * 1024f))
                                sizeText.text = "$totalMb MB / $maxMb MB"
                            }
                        }
                    }
                }

                output.flush()
                output.close()
                input.close()

                withContext(Dispatchers.Main) {
                    if (!isAdded || _binding == null) return@withContext
                    updateDialog?.dismiss()
                    installApk(apkFile)
                }
            } catch (error: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded || _binding == null) return@withContext
                    updateDialog?.dismiss()
                    error.printStackTrace()
                    Toast.makeText(requireActivity(), "Download failed: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun installApk(apkFile: File) {
        try {
            val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    requireActivity(),
                    "${requireActivity().packageName}.fileprovider",
                    apkFile
                )
            } else {
                Uri.fromFile(apkFile)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            startActivity(intent)
            Toast.makeText(requireActivity(), "Installation started", Toast.LENGTH_SHORT).show()
        } catch (error: Exception) {
            Toast.makeText(requireActivity(), "Installation failed: ${error.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showInstallPermissionDialog() {
        AlertDialog.Builder(requireActivity(), R.style.CustomDialogTheme)
            .setTitle("Install Permission Required")
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

    private fun showRestartDialog() {
        restartDialog = AlertDialog.Builder(requireActivity(), R.style.CustomDialogTheme)
            .setTitle("Restart App")
            .setMessage("Are you sure you want to restart the application? This will close all current activities and restart the app.")
            .setPositiveButton("Restart") { _, _ ->
                Toast.makeText(requireActivity(), "Restarting app...", Toast.LENGTH_SHORT).show()
                GlobalUtils.restartApp(requireActivity())
            }
            .setNegativeButton("Cancel", null)
            .create()

        restartDialog?.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        updateDialog?.dismiss()
        themeDialog?.dismiss()
        restartDialog?.dismiss()
        logoutDialog?.dismiss()
        _binding = null
    }
}

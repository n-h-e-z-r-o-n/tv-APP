package com.example.onyx

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.onyx.Database.SessionManger
import com.example.onyx.OnyxObjects.GlobalUtils
import com.example.onyx.OnyxObjects.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class MainActivity : AppCompatActivity() {

    private var keepSplashVisible = true
    private var hasNavigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { keepSplashVisible }

        GlobalUtils.applyTheme(this)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        setupBackPressedCallback()

        val logo: TextView = findViewById(R.id.mediaTxt)
        GlobalUtils.animateGradientTextString(logo, "#042C40", "#FFFFFF")
        GlobalUtils.hideSystemUI(this)

        startStartupFlow()
    }

    private fun startStartupFlow() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                GlobalUtils.autoRestoreDatabaseIfNeeded(applicationContext)
            }

            val hasSession = SessionManger(this@MainActivity).getUserId() != -1

            keepSplashVisible = false
            warmStartupData(hasSession)
            delay(5000)
            navigateToNextScreen(hasSession)

        }
    }

    private fun navigateToNextScreen(hasSession: Boolean) {
        if (hasNavigated || isFinishing || isDestroyed) return
        hasNavigated = true

        val destination = if (hasSession) {
            HomeActivity::class.java
        } else {
            PayWall::class.java
        }

        startActivity(Intent(this, destination))
        finish()
    }

    private fun warmStartupData(hasSession: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {

            if (GlobalUtils.getSavedCountryCode(applicationContext).isBlank()) {
                runCatching { GlobalUtils.ipCheck(applicationContext) }
            }

            if (hasSession && shouldRunDailyNotificationCheck()) {
                runCatching { NotificationHelper.getTvNotifications(applicationContext) }
                runCatching { NotificationHelper.getAnimeNotifications(applicationContext) }
                markDailyNotificationCheckRan()
            }
        }
    }

    private fun shouldRunDailyNotificationCheck(): Boolean {
        val prefs = getSharedPreferences(STARTUP_PREFS, MODE_PRIVATE)
        val lastRunDate = prefs.getString(KEY_LAST_NOTIFICATION_CHECK_DATE, "") ?: ""
        val today = LocalDate.now().toString()
        return lastRunDate != today
    }

    private fun markDailyNotificationCheckRan() {
        val today = LocalDate.now().toString()
        getSharedPreferences(STARTUP_PREFS, MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_NOTIFICATION_CHECK_DATE, today)
            .apply()
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

    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
            }
        })
    }

    companion object {
        private const val STARTUP_PREFS = "MAIN_ACTIVITY_STARTUP"
        private const val KEY_LAST_NOTIFICATION_CHECK_DATE = "LAST_NOTIFICATION_CHECK_DATE"
    }
}

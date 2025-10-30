package com.example.onyx

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import android.widget.ImageView
import com.bumptech.glide.Glide


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        GlobalUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)



        val loadingImageView = findViewById<ImageView>(R.id.LoadingAnimation)
        Glide.with(this)
            .asGif()
            .load(R.raw.loadsplash)
            .into(loadingImageView)

        Handler(Looper.getMainLooper()).postDelayed({
            if (isSubscriptionActive()) {
                startActivity(Intent(this, Home_Page::class.java))
            } else {
                startActivity(Intent(this, PayWall::class.java))
            }
            finish()
        }, 10500)

    }

    private fun isSubscriptionActive(): Boolean {
        val prefs = getSharedPreferences("SubscriptionPrefs", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()

        val expiryTime = prefs.getLong("expiryTime", 0L)
        if (expiryTime > 0L) {
            val active = now < expiryTime
            val daysLeft = ((expiryTime - now) / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
            Log.d("SUBSCRIPTION", "Active: $active | Days left: $daysLeft (from expiryTime)")
            return active
        }

        // Fallback for legacy data: use lastPaymentTime + 30 days
        val lastPaymentTime = prefs.getLong("lastPaymentTime", 0L)
        if (lastPaymentTime == 0L) return false
        val subscriptionDurationMs = 30L * 24 * 60 * 60 * 1000
        val active = now - lastPaymentTime < subscriptionDurationMs
        val daysLeft = ((subscriptionDurationMs - (now - lastPaymentTime)) / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
        Log.d("SUBSCRIPTION", "Active: $active | Days left: $daysLeft (legacy)")
        return active
    }

}









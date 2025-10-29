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

    private val SUBSCRIPTION_DURATION_MS = 30L * 24 * 60 * 60 * 1000 // 30 days in ms

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
                startActivity(Intent(this, PayWall::class.java))
                finish()
            }else{
                startActivity(Intent(this, PayWall::class.java))

                //startActivity(Intent(this, PayWall::class.java))
                finish()
            }

        }, 500)

    }

    private fun isSubscriptionActive(): Boolean {
        val prefs = getSharedPreferences("SubscriptionPrefs", Context.MODE_PRIVATE)
        val lastPaymentTime = prefs.getLong("lastPaymentTime", 0L)
        val now = System.currentTimeMillis()

        if (lastPaymentTime == 0L) return false

        val timePassed = now - lastPaymentTime
        val active = timePassed < SUBSCRIPTION_DURATION_MS

        Log.d("SUBSCRIPTION", "Active: $active | Days left: ${(SUBSCRIPTION_DURATION_MS - timePassed) / (1000 * 60 * 60 * 24)}")
        return active
    }

}









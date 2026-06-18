package com.example.onyx

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Shader
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.OvershootInterpolator
import android.webkit.WebView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.onyx.Database.SessionManger
import com.example.onyx.OnyxObjects.GlobalUtils
import com.example.onyx.OnyxObjects.NotificationHelper
import com.example.onyx.OnyxObjects.StreamingLinks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext



class MainActivity : AppCompatActivity() {

    private lateinit var  sm: SessionManger

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        GlobalUtils.applyTheme(this)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        setupBackPressedCallback()

        // Hide navigation bar and status bar (Immersive mode)
        //GlobalUtils.hideSystemUI(this)


        ////////////////////////////////////////////////////////////////////////////////////////////


        ////////////////////////////////////////////////////////////////////////////////////////////





        /**/
        lifecycleScope.launch {

            // 1️⃣ Wait until restore finishes (runs on IO thread)
            withContext(Dispatchers.IO) {

                NotificationHelper.getTvNotifications(this@MainActivity)
                NotificationHelper.getAnimeNotifications(this@MainActivity)

                GlobalUtils.autoRestoreDatabaseIfNeeded(this@MainActivity)
            }

            delay(1000)

            if (!GlobalUtils.isTv(this@MainActivity)) {

                //sm = SessionManger(this@MainActivity)
                //sm.saveUserId(1453)
                //sm.saveAvatar("profile_avatars/1.png")
                //startActivity(Intent(this@MainActivity, Instraction::class.java))

                val r = GlobalUtils.ipCheck(this@MainActivity)
                startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                finish()

            } else {
                val r = GlobalUtils.ipCheck(this@MainActivity)
                startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                finish()
            }
        }
        //startActivity(Intent(this@MainActivity, Watch_Page::class.java))
    }


    // This ensures the UI stays hidden when the activity regains focus
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            GlobalUtils.hideSystemUI(this)
        }
    }



    // This handles when the immersive mode is interrupted
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
}
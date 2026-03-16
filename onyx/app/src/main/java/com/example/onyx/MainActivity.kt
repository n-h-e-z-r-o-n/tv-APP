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
        setContentView(R.layout.activity_main)

        supportActionBar?.hide()
        setupBackPressedCallback()

        // Hide navigation bar and status bar (Immersive mode)
        //GlobalUtils.hideSystemUI(this)


        ////////////////////////////////////////////////////////////////////////////////////////////
        val logo = findViewById<TextView>(R.id.onyxTitle)
        logo.apply {
            alpha = 0f
            scaleX = 0.7f
            scaleY = 0.7f
            translationY = 80f
            letterSpacing = 0f


            setLayerType(View.LAYER_TYPE_SOFTWARE, paint)
            paint.setShadowLayer(
                4f,   // radius
                5f,   // dx
                6f,   // dy
                Color.parseColor("#FFFFFF")
            )
        }

        logo.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1.05f)
            .scaleY(1.05f)
            .setDuration(3000)
            .setInterpolator(OvershootInterpolator(2f))
            .withEndAction {
                // settle back to normal size
                logo.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(300)
                    .start()
            }
            .start()
        // Wait for layout before measuring width
        logo.post {
            val paint = logo.paint
            val width = paint.measureText(logo.text.toString())

            val textShader = LinearGradient(
                0f, 0f, width, 0f,
                intArrayOf(
                    Color.parseColor("#000d1a"), // Original Color
                    Color.parseColor("#4A90E2"), // Highlight Color
                    Color.parseColor("#000d1a")  // Original Color
                ), null, Shader.TileMode.CLAMP
            )

            logo.paint.shader = textShader

            // Animate the gradient position
            val matrix = Matrix()
            ValueAnimator.ofFloat(0f, 2f * width).apply {
                duration = 2000
                repeatCount = ValueAnimator.INFINITE
                addUpdateListener {
                    val dx = it.animatedValue as Float
                    matrix.setTranslate(dx - width, 0f)
                    textShader.setLocalMatrix(matrix)
                    logo.paint.shader = textShader
                    logo.invalidate()
                }
                start()
            }
        }

        ////////////////////////////////////////////////////////////////////////////////////////////





        /* */
        lifecycleScope.launch {

            // 1️⃣ Wait until restore finishes (runs on IO thread)
            withContext(Dispatchers.IO) {

                NotificationHelper.getTvNotifications(this@MainActivity)
                NotificationHelper.getAnimeNotifications(this@MainActivity)

                GlobalUtils.autoRestoreDatabaseIfNeeded(this@MainActivity)
            }

            delay(9000)

            if (!GlobalUtils.isTv(this@MainActivity)) {

                //sm = SessionManger(this@MainActivity)
                //sm.saveUserId(1453)
                //sm.saveAvatar("profile_avatars/1.png")
                //startActivity(Intent(this@MainActivity, Instraction::class.java))

                val r = GlobalUtils.ipCheck(this@MainActivity)
                startActivity(Intent(this@MainActivity, Login_Page::class.java))

            } else {
                val r = GlobalUtils.ipCheck(this@MainActivity)
                startActivity(Intent(this@MainActivity, Login_Page::class.java))
            }
        }



        //startActivity(Intent(this@MainActivity, Watch_Anime_Page::class.java))


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
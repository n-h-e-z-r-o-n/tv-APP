package com.example.onyx

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
            startActivity(Intent(this, PayWall::class.java))
            finish()
        }, 7500)

    }

}









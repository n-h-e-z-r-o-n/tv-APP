package com.example.onyx

import android.content.Intent
import android.os.Bundle
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



        loadingAnimation.setup(this@MainActivity)
        NavAction.setupSidebar(this@MainActivity)

        this.startActivity(Intent(this, Home_Page::class.java))
        //finish()

    }


}




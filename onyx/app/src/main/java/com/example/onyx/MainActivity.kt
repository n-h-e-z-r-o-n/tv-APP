package com.example.onyx

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import android.widget.ImageView
import com.bumptech.glide.Glide


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        loadingAnimation.setup(this@MainActivity)
        NavAction.setupSidebar(this@MainActivity)

        this.startActivity(Intent(this, Home_Page::class.java))
        //finish()

    }


}




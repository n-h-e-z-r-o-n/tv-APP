package com.example.onyx

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        NavAction.setupSidebar(this@MainActivity)

        this.startActivity(Intent(this, Home_Page::class.java))

    }


}



package com.example.onyx

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class TermsAndConditionsActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        GlobalUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_terms_and_conditions)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        NavAction.setupSidebar(this)
        
        // Setup focus handling for TV remote
        setupFocusHandling()
    }
    
    private fun setupFocusHandling() {
        // Setup focus handling for TV remote navigation
        // Since this is a scrollable content page, we'll set focus on the main content
        val mainContent = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main)
        mainContent.isFocusable = true
        mainContent.isFocusableInTouchMode = true
        mainContent.requestFocus()
        
        // Handle back button press
        mainContent.setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK || 
                keyCode == android.view.KeyEvent.KEYCODE_ESCAPE) {
                finish()
                true
            } else {
                false
            }
        }
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}



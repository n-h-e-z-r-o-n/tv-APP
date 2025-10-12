package com.example.onyx

import android.content.Context
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView

class SmoothScroller(context: Context) : LinearSmoothScroller(context) {
    
    override fun calculateSpeedPerPixel(displayMetrics: android.util.DisplayMetrics): Float {
        // Control the speed of scrolling (lower = slower, higher = faster)
        // 25f means it takes 25ms to scroll 1 pixel
        return 25f / displayMetrics.densityDpi
    }
    
    override fun calculateTimeForScrolling(dx: Int): Int {

        return super.calculateTimeForScrolling(dx).coerceAtMost(1000) // Max 1 second
    }
}

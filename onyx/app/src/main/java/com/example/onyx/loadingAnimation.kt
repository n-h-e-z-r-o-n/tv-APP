package com.example.onyx

import android.app.Activity
import android.widget.ImageView
import android.view.View
import com.bumptech.glide.Glide

object loadingAnimation {

    fun setup(activity: Activity) {
        val loadingImageView = activity.findViewById<ImageView>(R.id.loadingGif)

        Glide.with(activity)
            .asGif()
            .load(R.raw.dotloading)
            .into(loadingImageView)
    }

    /** Show the full-screen loading overlay */
    fun show(activity: Activity) {
        val container = activity.findViewById<View>(R.id.loadingContainer)
        container?.visibility = View.VISIBLE
    }

    /** Hide the overlay */
    fun hide(activity: Activity) {
        val container = activity.findViewById<View>(R.id.loadingContainer)
        container?.visibility = View.GONE
    }

}
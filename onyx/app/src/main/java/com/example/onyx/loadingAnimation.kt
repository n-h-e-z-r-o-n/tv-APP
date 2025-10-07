package com.example.onyx

import android.app.Activity
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide

object LoadingAnimation {

    private var currentAnimationRes: Int = R.raw.dotloading // default

    fun setup(activity: Activity, animationRes: Int = R.raw.dotloading) {
        currentAnimationRes = animationRes
        val loadingImageView = activity.findViewById<ImageView>(R.id.loadingGif)

        Glide.with(activity)
            .asGif()
            .load(animationRes)
            .into(loadingImageView)
    }

    fun show(activity: Activity) {
        val container = activity.findViewById<View>(R.id.loadingContainer)
        container?.visibility = View.VISIBLE
    }

    fun hide(activity: Activity) {
        val container = activity.findViewById<View>(R.id.loadingContainer)
        container?.visibility = View.GONE
    }
}

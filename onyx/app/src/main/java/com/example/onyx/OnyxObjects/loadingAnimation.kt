package com.example.onyx.OnyxObjects

import android.content.Context
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.example.onyx.R

object LoadingAnimation {

    private var currentAnimationRes: Int = R.raw.line_loading // default

    // Pass the root View (from Activity or Fragment) to find the views
    fun setup(context: Context, rootView: View, animationRes: Int = R.raw.line_loading) {
        currentAnimationRes = animationRes
        val loadingImageView = rootView.findViewById<ImageView>(R.id.loadingGif)

        // Safe call in case the layout doesn't contain the loadingGif ID
        loadingImageView?.let {
            Glide.with(context)
                .asGif()
                .load(animationRes)
                .into(it)
        }
    }

    fun show(rootView: View) {
        val container = rootView.findViewById<View>(R.id.loadingContainer)
        container?.visibility = View.VISIBLE
    }

    fun hide(rootView: View) {
        val container = rootView.findViewById<View>(R.id.loadingContainer)
        container?.visibility = View.GONE
    }
}
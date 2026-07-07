package com.example.onyx.OnyxClasses


import android.content.Context

import android.os.Handler

import android.os.Looper

import android.util.TypedValue

import android.view.KeyEvent

import android.view.View

import android.view.ViewGroup

import android.view.animation.AccelerateDecelerateInterpolator

import android.view.animation.Interpolator

import android.widget.FrameLayout

import androidx.cardview.widget.CardView


class CardStack {


    private var dp50: Float = 0f

    private var dp90: Float = 0f

    private var dp120: Float = 0f

    private var dp140: Float = 0f

    private var dp150: Float = 0f

    private var dp250: Float = 0f


    private lateinit var elevations: FloatArray

    private lateinit var scales : FloatArray

    private lateinit var translations  : FloatArray


    private var animationD:Long = 200


    private var isAnimating = false

    private val interpolator: Interpolator = AccelerateDecelerateInterpolator()

    private val MAX_VISIBLE_CARDS = 4


    fun setupCardStackFromContainer(

        container: FrameLayout,

        autoSwipeDelay: Long = 10000L

    ) {


        // Ensure container has CardView children

        val cards = (0 until container.childCount)

            .mapNotNull { container.getChildAt(it) as? CardView }


        if (cards.isEmpty()) return


        // ---------------- Auto Swipe ----------------

        val autoSwipeHandler = Handler(Looper.getMainLooper())

        var autoSwipeRunnable: Runnable? = null

        var autoSwipeRunning = false


        fun stopAutoSwipe() {

            autoSwipeRunning = false

            autoSwipeRunnable?.let { autoSwipeHandler.removeCallbacks(it) }

            autoSwipeRunnable = null

        }


        fun startAutoSwipe() {

            if (autoSwipeRunning) return

            autoSwipeRunning = true


            autoSwipeRunnable = object : Runnable {

                override fun run() {

                    if (!container.hasFocus()) {

                        swapRight(container, keepFocus = false)

                        autoSwipeHandler.postDelayed(this, autoSwipeDelay)

                    } else stopAutoSwipe()

                }

            }


            autoSwipeHandler.postDelayed(autoSwipeRunnable!!, autoSwipeDelay)

        }



        val context = container.context

        dp50 = dp(context, 50f)

        dp90 = dp(context, 90f)

        dp120 = dp(context, 120f)

        dp140 = dp(context, 140f)

        dp150 = dp(context, 150f)

        dp250 = dp(context, 250f)


        translations = floatArrayOf(

            0f, dp50, dp90, dp120, dp140, dp150

        )

        scales = floatArrayOf(1.0f, 0.95f, 0.9f, 0.85f, 0.8f, 0.7f)

        elevations = floatArrayOf(6f, 5f, 4f, 3f, 2f, 1f)


        var autoSwipeResumeRunnable: Runnable? = null


        // ---------------- Setup Card Listeners ----------------


        cards.forEach { card ->

            card.isFocusable = true

            card.isFocusableInTouchMode = true



            card.setOnFocusChangeListener { v, hasFocus ->

                if (hasFocus) {

                    stopAutoSwipe() // stop any ongoing auto-swipe

                    autoSwipeResumeRunnable?.let { container.removeCallbacks(it) } // cancel pending resumes

                    autoSwipeResumeRunnable = null


                    if (v.scaleX != 1f || v.translationX != 0f) {
                        v.animate().cancel()
                        v.bringToFront()
                        v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .translationX(0f)
                            .setDuration(animationD)
                            .setInterpolator(AccelerateDecelerateInterpolator())
                            .start()
                    } else {
                        v.bringToFront()
                    }
                    v.elevation = 7f

                } else {


                    // Schedule auto-swipe restart after 300ms

                    autoSwipeResumeRunnable?.let { container.removeCallbacks(it) }

                    autoSwipeResumeRunnable = Runnable {

                        if (!container.hasFocus()) startAutoSwipe()

                    }

                    container.postDelayed(autoSwipeResumeRunnable!!, 300)

                }

            }


            card.setOnKeyListener { _, keyCode, event ->

                if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false

                when (keyCode) {

                    KeyEvent.KEYCODE_DPAD_LEFT -> { swapLeft(container); true }

                    KeyEvent.KEYCODE_DPAD_RIGHT -> { swapRight(container); true }

                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> false

                    else -> false

                }

            }

        }


        // ---------------- Cleanup on View Detach -------------------------------------------------

        container.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {

            override fun onViewAttachedToWindow(v: View) {}


            override fun onViewDetachedFromWindow(v: View) {

                stopAutoSwipe()
                autoSwipeResumeRunnable?.let { container.removeCallbacks(it) }
                autoSwipeResumeRunnable = null

                container.removeOnAttachStateChangeListener(this)


                // Clear all listeners to prevent memory leaks

                cards.forEach { card ->

                    card.setOnFocusChangeListener(null)

                    card.setOnKeyListener(null)

                }

            }

        })


        layoutStack(container)

        container.postDelayed({ if (!container.hasFocus()) startAutoSwipe() }, 2000)

    }


    private fun layoutStack(container: FrameLayout) {

        val count = container.childCount

        if (count == 0) return


        for (i in 0 until count) {

            val card = container.getChildAt(i)


            val posFromTop = count - 1 - i

            card.isFocusable = (posFromTop == 0)
            card.isFocusableInTouchMode = (posFromTop == 0)

            if (posFromTop >= MAX_VISIBLE_CARDS) {

                // Hide cards below the visible stack

                card.visibility = View.INVISIBLE

                card.translationX = 0f

                card.scaleX = 0.7f

                card.scaleY = 0.7f

                card.elevation = 0f

            } else {

                val index = posFromTop

                val targetTranslation = translations.getOrElse(index) { translations.last() }

                val targetScale = scales.getOrElse(index) { scales.last() }

                val targetElevation = elevations.getOrElse(index) { elevations.last() }


                card.visibility = View.VISIBLE


                // Only animate if changed

                val needsTranslation = card.translationX != targetTranslation

                val needsScale = card.scaleX != targetScale || card.scaleY != targetScale

                val needsElevation = card.elevation != targetElevation


                card.animate().cancel()

                if (needsTranslation || needsScale) {

                    card.animate()

                        .translationX(targetTranslation)

                        .scaleX(targetScale)

                        .scaleY(targetScale)

                        .setDuration(animationD)

                        .setInterpolator(interpolator)

                        .start()

                }


                if (needsElevation) {

                    card.elevation = targetElevation

                }

            }

        }

    }



    private fun swapRight(container: FrameLayout, keepFocus: Boolean = true) {


        if (isAnimating) return

        isAnimating = true


        if (container.childCount == 0) return

        val top = container.getChildAt(container.childCount - 1)


        top.animate()

            .translationXBy(-dp250)

            .scaleX(0.85f)

            .scaleY(0.85f)

            .rotation(-5f)

            .setDuration(animationD)

            .setInterpolator(AccelerateDecelerateInterpolator())

            .withLayer()

            .withEndAction {

                top.rotation = 0f


                // Optimized view reordering

                val parent = top.parent as? ViewGroup


                parent?.removeView(top)

                parent?.addView(top, 0)



                layoutStack(container) // Re-layout stack positions


                if (keepFocus) {

                    container.getChildAt(container.childCount - 1)?.requestFocus()

                }

                isAnimating = false
            }

            .start()

    }



    private fun swapLeft(container: FrameLayout, keepFocus: Boolean = true) {


        if (isAnimating) return

        isAnimating = true


        if (container.childCount == 0) return

        val bottom = container.getChildAt(0)


        bottom.animate()

            .translationXBy(-dp250)

            .scaleX(0.85f)

            .scaleY(0.85f)

            .rotation(-5f)

            .setDuration(animationD)

            .setInterpolator(AccelerateDecelerateInterpolator())

            .withLayer()

            .withEndAction {

                bottom.rotation = 0f


                // Optimized view reordering

                val parent = bottom.parent as? ViewGroup

                parent?.removeView(bottom)

                parent?.addView(bottom)


                layoutStack(container)


                if (keepFocus) {

                    container.getChildAt(container.childCount - 1)?.requestFocus()

                }

                isAnimating = false
            }

            .start()

    }


    private fun dp(context: Context, value: Float): Float {

        return TypedValue.applyDimension(

            TypedValue.COMPLEX_UNIT_DIP,

            value,

            context.resources.displayMetrics

        )

    }

}
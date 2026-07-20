package com.example.onyx.OnyxClasses

import android.graphics.Rect
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import kotlin.math.abs
import kotlin.math.roundToInt

class FocusOverlay<T> (
        private val overlay: MaterialCardView,
        private val recyclerView: RecyclerView,
        private val adapter: RecyclerView.Adapter<*>,
        private val projectItemAction: (T) -> Unit
    ) {

        var embeddedFocusedView: View? = null
        private var initialHeroPopulated = false
        private var selectRetryCount = 0
        init {

            recyclerView.layoutManager =  object : LinearLayoutManager(
                recyclerView.context,
                LinearLayoutManager.HORIZONTAL,
                false
            ) {
                override fun requestChildRectangleOnScreen(
                    parent: RecyclerView,
                    child: View,
                    rect: Rect,
                    immediate: Boolean,
                    focusedChildVisible: Boolean
                ): Boolean {
                    return false
                }
            }

            recyclerView.addOnChildAttachStateChangeListener(object : RecyclerView.OnChildAttachStateChangeListener {
                override fun onChildViewAttachedToWindow(view: View) {
                    view.foreground = null
                }
                override fun onChildViewDetachedFromWindow(view: View) {
                }
            })


            val focusDataObserver = object : RecyclerView.AdapterDataObserver() {
                override fun onChanged() = enforceFirstItemFocusIfNeeded(recyclerView)
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    if (positionStart == 0) {
                        //enforceFirstItemFocusIfNeeded(movieRecyclerView)
                        selectFirstItemSilently()
                    }
                }
            }

            try {
                adapter.registerAdapterDataObserver(focusDataObserver)
            } catch (e: Exception) {
                // Ignore if already registered
            }


            try {
                val focusAction: (View, T) -> Unit = { view, item ->
                    overlay.strokeWidth = 3

                    embeddedFocusedView?.let { previous ->
                        if (previous !== view) {
                            val oldLp = previous.layoutParams as RecyclerView.LayoutParams
                            oldLp.marginStart = 0
                            oldLp.marginEnd = 0
                            oldLp.topMargin = 0
                            oldLp.bottomMargin = 0
                            previous.layoutParams = oldLp
                            previous.requestLayout()
                            previous.animate()
                                .alpha(1f)
                                .setDuration(300L)
                                .start()
                        }
                    }

                    val lp = view.layoutParams as RecyclerView.LayoutParams
                    val focusedMargin = (overlay.width * 0.3f).toInt()
                    lp.marginStart = focusedMargin
                    lp.marginEnd = focusedMargin
                    view.layoutParams = lp
                    view.requestLayout()

                    embeddedFocusedView = view
                    view.animate()
                        .alpha(0.03f)
                        .setDuration(300L)
                        .start()

                    view.post {
                        if (embeddedFocusedView == view) {
                            centerChildUnderFixedFocus(recyclerView, overlay, view)


                        }
                        projectItemAction(item)
                    }
                }
                val focusLostAction: () -> Unit = {
                    overlay.strokeWidth = 0
                }
                if (adapter is FocusableAdapter<*>) {
                    @Suppress("UNCHECKED_CAST")
                    val focusableAdapter = adapter as FocusableAdapter<T>
                    focusableAdapter.onItemFocused = focusAction
                    focusableAdapter.onItemFocusLost = focusLostAction
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        fun selectFirstItemSilently() {

            if (initialHeroPopulated || adapter.itemCount == 0) return

            recyclerView.post {
                if (!recyclerView.isAttachedToWindow) return@post

                val firstView = recyclerView.layoutManager?.findViewByPosition(0)

                if (firstView != null) {
                    // Prevent this from running again
                    initialHeroPopulated = true
                    selectRetryCount = 0

                    recyclerView.scrollToPosition(0)


                    // 1. Get the data dynamically
                    var firstItem: T? = null
                    if (adapter is FocusableAdapter<*>) {
                        @Suppress("UNCHECKED_CAST")
                        val focusableAdapter = adapter as FocusableAdapter<T>
                        firstItem = focusableAdapter.getItem(0)
                    }

                    if (firstItem == null) return@post

                    // 2. Protect against overlay not being measured yet
                    val actualOverlayWidth = if (overlay.width == 0) {
                        (110f * overlay.resources.displayMetrics.density).roundToInt()
                    } else {
                        overlay.width
                    }

                    // 3. Apply the visual state manually
                    val lp = firstView.layoutParams as RecyclerView.LayoutParams
                    val focusedMargin = (actualOverlayWidth * 0.3f).toInt()
                    lp.marginStart = focusedMargin
                    lp.marginEnd = focusedMargin
                    firstView.layoutParams = lp

                    firstView.alpha = 0.03f

                    // 4. Center it instantly (smooth = false so it doesn't visibly slide in on launch)
                    centerChildUnderFixedFocus(recyclerView, overlay, firstView, smooth = false)

                    // 5. Project the data to the background hero
                    projectItemAction(firstItem)

                    // 6. Track this view so when the user actually starts scrolling, it resets properly!
                    embeddedFocusedView = firstView

                } else {
                    // If the view isn't physically laid out on the screen yet, check again in 50ms.
                    if (selectRetryCount < 20) {
                        selectRetryCount++
                        recyclerView.postDelayed({ selectFirstItemSilently() }, 50)
                    }
                }
            }
        }


        fun centerChildUnderFixedFocus(
            recyclerView: RecyclerView,
            overlay: View,
            child: View,
            smooth: Boolean = true
        ) {
            if (recyclerView.width == 0) return

            // Calculate the overlay anchor X directly within the function
            val viewportCenterX = if (overlay.width == 0) {
                val fallbackWidth = (110f * overlay.resources.displayMetrics.density).roundToInt()
                val lp = overlay.layoutParams as? FrameLayout.LayoutParams
                val startMargin = lp?.marginStart ?: 0
                (startMargin + fallbackWidth / 2).coerceIn(0, recyclerView.width)
            } else {
                val recyclerLocation = IntArray(2)
                val overlayLocation = IntArray(2)
                recyclerView.getLocationInWindow(recyclerLocation)
                overlay.getLocationInWindow(overlayLocation)

                val overlayCenterX = overlayLocation[0] - recyclerLocation[0] + (overlay.width / 2)
                overlayCenterX.coerceIn(0, recyclerView.width)
            }

            val childCenterX = child.left + (child.width / 2)
            val distanceToCenter = childCenterX - viewportCenterX
            if (distanceToCenter == 0) return

            if (smooth) {
                val duration = (110 + abs(distanceToCenter) * 0.20f)
                    .roundToInt()
                    .coerceIn(110, 260)
                recyclerView.smoothScrollBy(
                    distanceToCenter,
                    0,
                    AccelerateDecelerateInterpolator(),
                    duration
                )
            } else {
                recyclerView.scrollBy(distanceToCenter, 0)
            }
        }


        fun enforceFirstItemFocusIfNeeded(recyclerView: RecyclerView) {
            val adapter = recyclerView.adapter ?: return
            if (adapter.itemCount <= 0) return

            val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
            layoutManager.scrollToPositionWithOffset(0, 0)

            recyclerView.post {
                val firstItem = recyclerView.findViewHolderForAdapterPosition(0)?.itemView
                firstItem?.requestFocus()
            }
        }

}


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

        companion object {
            private const val FOCUSED_ITEM_ALPHA = 0.12f
            private const val UNFOCUSED_ITEM_ALPHA = 1f
            private const val ITEM_ALPHA_DURATION_MS = 180L
            private const val HERO_UPDATE_DEBOUNCE_MS = 180L
            private const val MIN_SCROLL_DELTA_PX = 4
            private const val MAX_SCROLL_DURATION_MS = 180
            private const val SIBLING_TRANSLATION_DURATION_MS = 180L
        }

        var embeddedFocusedView: View? = null
        private var initialHeroPopulated = false
        private var selectRetryCount = 0
        private var overlayAnchorX: Int? = null
        private var pendingProjection: Runnable? = null

        init {
            overlay.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                updateOverlayAnchor()
            }
            recyclerView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                updateOverlayAnchor()
                if (embeddedFocusedView?.isAttachedToWindow == true) {
                    updateVisibleChildTranslations(animate = false)
                }
            }
            overlay.post { updateOverlayAnchor() }

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
                    if (view !== embeddedFocusedView) {
                        view.animate().cancel()
                        view.alpha = UNFOCUSED_ITEM_ALPHA
                    }
                    if (view !== embeddedFocusedView) {
                        view.translationX = 0f
                    }
                    if (embeddedFocusedView?.isAttachedToWindow == true) {
                        recyclerView.post { updateVisibleChildTranslations(animate = false) }
                    }
                }
                override fun onChildViewDetachedFromWindow(view: View) {
                    view.animate().cancel()
                    view.alpha = UNFOCUSED_ITEM_ALPHA
                    view.translationX = 0f
                    if (embeddedFocusedView === view) {
                        embeddedFocusedView = null
                    }
                }
            })
            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (embeddedFocusedView?.isAttachedToWindow == true) {
                        updateVisibleChildTranslations(animate = false)
                    }
                }
            })


            val focusDataObserver = object : RecyclerView.AdapterDataObserver() {
                override fun onChanged() {
                    if (!initialHeroPopulated && !recyclerView.hasFocus()) {
                        enforceFirstItemFocusIfNeeded(recyclerView)
                    }
                }
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    if (!initialHeroPopulated && positionStart == 0) {
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
                            resetItemVisualState(previous, animate = true)
                        }
                    }

                    embeddedFocusedView = view
                    applyFocusedVisualState(view, animate = true)
                    updateVisibleChildTranslations(animate = true)

                    view.post {
                        if (embeddedFocusedView === view && view.isAttachedToWindow) {
                            centerChildUnderFixedFocus(recyclerView, overlay, view)
                        }
                    }
                    scheduleProjection(view, item)
                }
                val focusLostAction: () -> Unit = {
                    recyclerView.post {
                        if (!recyclerView.hasFocus() && embeddedFocusedView?.hasFocus() != true) {
                            overlay.strokeWidth = 0
                            resetVisibleChildTranslations(animate = true)
                        }
                    }
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

                    applyFocusedVisualState(firstView, animate = false)

                    centerChildUnderFixedFocus(recyclerView, overlay, firstView, smooth = false)

                    projectItemAction(firstItem)

                    embeddedFocusedView = firstView
                    updateVisibleChildTranslations(animate = false)

                } else {
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
            if (recyclerView.width == 0 || !child.isAttachedToWindow) return

            val viewportCenterX = overlayAnchorX ?: calculateOverlayAnchorX()

            val childCenterX = child.left + (child.width / 2)
            val distanceToCenter = childCenterX - viewportCenterX
            if (abs(distanceToCenter) <= MIN_SCROLL_DELTA_PX) return

            if (smooth) {
                recyclerView.stopScroll()
                val duration = (140 + abs(distanceToCenter) * 0.05f)
                    .roundToInt()
                    .coerceAtMost(MAX_SCROLL_DURATION_MS)
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

        private fun applyFocusedVisualState(view: View, animate: Boolean) {
            view.animate().cancel()
            if (animate) {
                view.animate()
                    .alpha(FOCUSED_ITEM_ALPHA)
                    .setDuration(ITEM_ALPHA_DURATION_MS)
                    .start()
            } else {
                view.alpha = FOCUSED_ITEM_ALPHA
            }
        }

        private fun resetItemVisualState(view: View, animate: Boolean) {
            view.animate().cancel()
            if (animate) {
                view.animate()
                    .alpha(UNFOCUSED_ITEM_ALPHA)
                    .setDuration(ITEM_ALPHA_DURATION_MS)
                    .start()
            } else {
                view.alpha = UNFOCUSED_ITEM_ALPHA
            }
        }

        private fun scheduleProjection(view: View, item: T) {
            pendingProjection?.let(recyclerView::removeCallbacks)
            pendingProjection = Runnable {
                if (embeddedFocusedView === view && view.isAttachedToWindow) {
                    projectItemAction(item)
                }
                pendingProjection = null
            }
            recyclerView.postDelayed(pendingProjection, HERO_UPDATE_DEBOUNCE_MS)
        }

        private fun updateOverlayAnchor() {
            if (recyclerView.width == 0) return
            overlayAnchorX = calculateOverlayAnchorX()
        }

        private fun updateVisibleChildTranslations(animate: Boolean) {
            val focusedView = embeddedFocusedView?.takeIf { it.isAttachedToWindow } ?: run {
                resetVisibleChildTranslations(animate)
                return
            }
            val overlayBounds = calculateOverlayBounds() ?: return
            val gapPx = calculateSiblingGapPx()

            val visibleChildren = buildList {
                for (index in 0 until recyclerView.childCount) {
                    add(recyclerView.getChildAt(index))
                }
            }.sortedBy { it.left }

            val focusedIndex = visibleChildren.indexOf(focusedView)
            if (focusedIndex == -1) {
                resetVisibleChildTranslations(animate)
                return
            }

            applyChildTranslation(focusedView, 0f, animate)

            var previousOriginalRight = focusedView.right
            var previousFinalRight = overlayBounds.second + gapPx
            for (index in focusedIndex + 1 until visibleChildren.size) {
                val child = visibleChildren[index]
                val originalGap = (child.left - previousOriginalRight).coerceAtLeast(0)
                val targetLeft = maxOf(child.left, previousFinalRight + originalGap)
                val translation = (targetLeft - child.left).toFloat()
                applyChildTranslation(child, translation, animate)
                previousOriginalRight = child.right
                previousFinalRight = child.right + translation.toInt()
            }

            var nextOriginalLeft = focusedView.left
            var nextFinalLeft = overlayBounds.first - gapPx
            for (index in focusedIndex - 1 downTo 0) {
                val child = visibleChildren[index]
                val originalGap = (nextOriginalLeft - child.right).coerceAtLeast(0)
                val targetRight = minOf(child.right, nextFinalLeft - originalGap)
                val translation = (targetRight - child.right).toFloat()
                applyChildTranslation(child, translation, animate)
                nextOriginalLeft = child.left
                nextFinalLeft = child.left + translation.toInt()
            }
        }

        private fun resetVisibleChildTranslations(animate: Boolean) {
            for (index in 0 until recyclerView.childCount) {
                applyChildTranslation(recyclerView.getChildAt(index), 0f, animate)
            }
        }

        private fun applyChildTranslation(view: View, translationX: Float, animate: Boolean) {
            if (kotlin.math.abs(view.translationX - translationX) < 0.5f) return
            if (animate) {
                view.animate()
                    .translationX(translationX)
                    .setDuration(SIBLING_TRANSLATION_DURATION_MS)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            } else {
                view.translationX = translationX
            }
        }

        private fun calculateOverlayBounds(): Pair<Int, Int>? {
            if (recyclerView.width == 0) return null

            val layoutWidth = overlay.layoutParams?.width ?: 0
            val overlayWidth = when {
                overlay.width > 0 -> overlay.width
                layoutWidth > 0 -> layoutWidth
                else -> (110f * overlay.resources.displayMetrics.density).roundToInt()
            }

            if (overlay.width > 0) {
                val recyclerLocation = IntArray(2)
                val overlayLocation = IntArray(2)
                recyclerView.getLocationInWindow(recyclerLocation)
                overlay.getLocationInWindow(overlayLocation)

                val left = (overlayLocation[0] - recyclerLocation[0]).coerceIn(0, recyclerView.width)
                val right = (left + overlayWidth).coerceIn(0, recyclerView.width)
                return left to right
            }

            val centerX = overlayAnchorX ?: calculateOverlayAnchorX()
            val halfWidth = overlayWidth / 2
            return (centerX - halfWidth).coerceIn(0, recyclerView.width) to
                (centerX + halfWidth).coerceIn(0, recyclerView.width)
        }

        private fun calculateSiblingGapPx(): Int {
            return (12f * recyclerView.resources.displayMetrics.density).roundToInt()
        }

        private fun calculateOverlayAnchorX(): Int {
            if (overlay.width == 0) {
                val layoutWidth = overlay.layoutParams?.width ?: 0
                val fallbackWidth = when {
                    layoutWidth > 0 -> layoutWidth
                    else -> (110f * overlay.resources.displayMetrics.density).roundToInt()
                }
                val lp = overlay.layoutParams as? FrameLayout.LayoutParams
                val startMargin = lp?.marginStart ?: 0
                return (startMargin + fallbackWidth / 2).coerceIn(0, recyclerView.width)
            }

            val recyclerLocation = IntArray(2)
            val overlayLocation = IntArray(2)
            recyclerView.getLocationInWindow(recyclerLocation)
            overlay.getLocationInWindow(overlayLocation)

            val overlayCenterX = overlayLocation[0] - recyclerLocation[0] + (overlay.width / 2)
            return overlayCenterX.coerceIn(0, recyclerView.width)
        }

}


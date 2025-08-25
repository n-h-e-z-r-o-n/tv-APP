package com.example.onyx
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.min


class StackLayoutManager(
    private val maxVisible: Int = 6,
    private val scaleGap: Float = 0.05f,
    private val transGap: Int = 100
) : RecyclerView.LayoutManager() {

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.WRAP_CONTENT,
            RecyclerView.LayoutParams.WRAP_CONTENT,

        )
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        detachAndScrapAttachedViews(recycler)
        if (itemCount == 0) return

        val visibleCount = min(itemCount, maxVisible)

        for (i in visibleCount - 1 downTo 0) {
            val view = recycler.getViewForPosition(i)
            addView(view)
            measureChildWithMargins(view, 0, 0)

            val width = getDecoratedMeasuredWidth(view)
            val height = getDecoratedMeasuredHeight(view)

            val left = (width / 8)
            val top = (height / 8)

            layoutDecoratedWithMargins(
                view,
                left,
                top,
                left + width,
                top + height
            )

            val scale = 1f - i * scaleGap
            view.scaleX = scale
            view.scaleY = scale

            view.translationX = (i * transGap).toFloat()
            view.translationY = (i * 20).toFloat()

            view.elevation = (maxVisible - i).toFloat()
        }
    }

    override fun canScrollHorizontally(): Boolean = false
    override fun canScrollVertically(): Boolean = false
}

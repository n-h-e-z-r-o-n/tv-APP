package com.example.onyx.OnyxClasses

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.text.format.DateUtils
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.cardview.widget.CardView
import androidx.compose.ui.graphics.ColorMatrixColorFilter
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.onyx.Actor_Page
import com.example.onyx.Anime_Video_Player
import com.example.onyx.CategoryFragment
import com.example.onyx.Database.AppDatabase
import com.example.onyx.FetchData.TMDBapi
import com.example.onyx.OnyxObjects.GlobalUtils
import com.example.onyx.Play
import com.example.onyx.R
import com.example.onyx.WatchAnimeFragment
import com.example.onyx.Watch_Page
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class GridAdapter(
    private val items: MutableList<MovieItemOne>,
    private val layoutResId: Int,
) : RecyclerView.Adapter<GridAdapter.ViewHolder>(), FocusableAdapter<MovieItemOne> {

    companion object {
        private const val VIEW_TYPE_MOVIE = 0
        private const val VIEW_TYPE_ADD_BUTTON = 1
        private var lastKeyTime = 0L
        private val KEY_DEBOUNCE_DELAY = 300L // ms
    }

    var onAddMoreClicked: (() -> Unit)? = null
    override var onItemFocused: ((View, MovieItemOne) -> Unit)? = null
    override var onItemFocusLost: (() -> Unit)? = null
    override fun getItem(position: Int): MovieItemOne? = items.getOrNull(position)
    var isLoadingMore = false
        set(value) {
            field = value
            notifyItemChanged(items.size) // refresh the "Add More" item only
        }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val Movie_image: ImageView? = view.findViewById(R.id.itemImage)
        val showYear: TextView? = view.findViewById(R.id.itemText)
        val showTitle: TextView? = view.findViewById(R.id.showTitle)
        val showRating: TextView? = view.findViewById(R.id.showRating)
        val showRS: TextView? = view.findViewById(R.id.showRS)
        val showType: TextView? = view.findViewById(R.id.showType)
        val Logo_image: ImageView? = view.findViewById(R.id.itemLogo)
    }

    override fun getItemViewType(position: Int): Int {
        // The last item is the "Add More" button
        return if (position == items.size) VIEW_TYPE_ADD_BUTTON else VIEW_TYPE_MOVIE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutId = if (viewType == VIEW_TYPE_ADD_BUTTON) {
            R.layout.item_add_more
        } else {
            layoutResId
        }

        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        // ==========================================
        // 1. HANDLE "ADD MORE" BUTTON
        // ==========================================
        if (getItemViewType(position) == VIEW_TYPE_ADD_BUTTON) {
            val content = holder.itemView.findViewById<View>(R.id.addMoreContent)
            val loading = holder.itemView.findViewById<View>(R.id.addMoreLoading)

            if (isLoadingMore) {
                content.visibility = View.GONE
                loading.visibility = View.VISIBLE
                holder.itemView.isClickable = false
                holder.itemView.isFocusable = false
            } else {
                content.visibility = View.VISIBLE
                loading.visibility = View.GONE
                holder.itemView.isClickable = true
                holder.itemView.isFocusable = true
            }

            // Fallback click listener
            holder.itemView.setOnClickListener {
                if (!isLoadingMore) {
                    isLoadingMore = true
                    val recycler = holder.itemView.parent as RecyclerView
                    val prevPosition = holder.bindingAdapterPosition - 1

                    if (prevPosition >= 0) {
                        recycler.post {
                            recycler.findViewHolderForAdapterPosition(prevPosition)
                                ?.itemView
                                ?.requestFocus()
                        }
                    }
                    onAddMoreClicked?.invoke()
                }
            }

            // Note: Auto-focus click removed because the 6-item prefetch handles this safely now.
            return
        }

        // ==========================================
        // 2. HANDLE STANDARD MOVIE ITEM
        // ==========================================
        val currentItem = items[position]
        val title = currentItem.title
        val imageUrl = currentItem.posterUlr
        val imdbCode = currentItem.imdbCode
        val type = currentItem.type
        val year = currentItem.year
        val rating = currentItem.rating
        val runtime = currentItem.runtime

        holder.showYear?.text = if (year.length >= 4) year.substring(0, 4) else "N/A"
        holder.showTitle?.text = title
        holder.showRating?.text = rating
        holder.showRS?.text = runtime
        holder.showType?.text = type

        // Note: Doing API calls directly inside onBindViewHolder can cause scroll lag.
        // Ensure TMDBapi caches results or handles async heavily!
        try {
            val fetch = TMDBapi(holder.itemView.context)
            if (holder.Logo_image != null && holder.showTitle != null) {
                fetch.fetchLogos(type, imdbCode, holder.Logo_image, holder.showTitle)
            }
        } catch (e: Exception){}

        holder.Movie_image?.let {
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                .centerInside()
                .into(it)
        }

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, Watch_Page::class.java).apply {
                putExtra("imdb_code", imdbCode)
                putExtra("type", type)
            }
            context.startActivity(intent)
        }

        // ==========================================
        // 3. TV FOCUS PREFETCH LOGIC
        // ==========================================
        holder.itemView.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                onItemFocused?.invoke(v, currentItem) // show popup

                // âœ… Trigger load when focused on the last 6 items
                val prefetchThreshold = 6
                val currentPos = holder.bindingAdapterPosition

                if (!isLoadingMore && currentPos != RecyclerView.NO_POSITION && currentPos >= items.size - prefetchThreshold) {
                    isLoadingMore = true
                    v.post {
                        onAddMoreClicked?.invoke()
                    }
                }
            } else {
                onItemFocusLost?.invoke()   // hide popup
            }
        }

        holder.itemView.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
            val now = System.currentTimeMillis()
            if (now - lastKeyTime < KEY_DEBOUNCE_DELAY) return@setOnKeyListener true
            lastKeyTime = now

            val currentPos = holder.bindingAdapterPosition
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (currentPos == 0) return@setOnKeyListener true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (currentPos == items.size) return@setOnKeyListener true
                }
            }
            false
        }
    }

    override fun getItemCount(): Int {
        return items.size + 1
    }

    fun addItem(item: MovieItemOne) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun addItems(newItems: List<MovieItemOne>) {
        val startPos = items.size
        items.addAll(newItems)
        notifyItemRangeInserted(startPos, newItems.size)
    }

    fun clearItems() {
        val size = items.size
        items.clear()
        notifyItemRangeRemoved(0, size) // Much smoother than notifyDataSetChanged()
    }
}


data class MovieItemOne(
    val title: String = "",
    val backdropUrl: String= "",
    val posterUlr: String= "",
    val imdbCode: String= "",
    val type: String = "",
    val year: String = "",
    val rating: String = "",
    val runtime: String = ""
)
////////////////////////////////////////////////////////////////////////////////////////////////////

class FilterAdapter(
    private val items: MutableList<filterItemOne>,
    private val layoutResId: Int
) : RecyclerView.Adapter<FilterAdapter.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_MOVIE = 0
        private const val VIEW_TYPE_ADD_BUTTON = 1
        private var lastKeyTime = 0L
        private val KEY_DEBOUNCE_DELAY = 370L // ms
    }

    var onAddMoreClicked: (() -> Unit)? = null
    var onItemFocused: ((View, filterItemOne) -> Unit)? = null
    var onItemFocusLost: (() -> Unit)? = null
    var isLoadingMore = false
        set(value) {
            field = value
            // Only refresh the "Add More" button to show/hide the loading spinner
            notifyItemChanged(items.size)
        }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val Movie_image: ImageView? = view.findViewById(R.id.itemImage)
        val showYear: TextView? = view.findViewById(R.id.itemText)
        val showTitle: TextView? = view.findViewById(R.id.showTitle)
        val showRating: TextView? = view.findViewById(R.id.showRating)
        val showRS: TextView? = view.findViewById(R.id.showRS)
        val showType: TextView? = view.findViewById(R.id.showType)

        init {
            itemView.setOnKeyListener { _, keyCode, event ->
                if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false

                // 1. D-Pad Debounce Logic
                val now = System.currentTimeMillis()
                if (now - lastKeyTime < KEY_DEBOUNCE_DELAY) return@setOnKeyListener true
                lastKeyTime = now

                // 2. Boundary Trap Logic
                val currentPos = bindingAdapterPosition

                // Safety check for invalid positions
                if (currentPos == RecyclerView.NO_POSITION) return@setOnKeyListener false

                // Total items is the adapter's item count (movies + add button)
                val totalItems = bindingAdapter?.itemCount ?: (items.size + 1)

                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        // Swallow left click if on the very first item
                        if (currentPos == 0) return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        // Swallow right click if on the very last item (the Add More button)
                        if (currentPos == totalItems - 1) return@setOnKeyListener true
                    }
                }

                // Otherwise, let the RecyclerView handle the scroll normally
                false
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        // The last item is the "Add More" button
        return if (position == items.size) VIEW_TYPE_ADD_BUTTON else VIEW_TYPE_MOVIE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutId = if (viewType == VIEW_TYPE_ADD_BUTTON) {
            R.layout.item_add_more
        } else {
            layoutResId
        }

        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        // ==========================================
        // 1. HANDLE "ADD MORE" BUTTON
        // ==========================================
        if (getItemViewType(position) == VIEW_TYPE_ADD_BUTTON) {
            val content = holder.itemView.findViewById<View>(R.id.addMoreContent)
            val loading = holder.itemView.findViewById<View>(R.id.addMoreLoading)

            // Toggle spinner vs button based on state
            if (isLoadingMore) {
                content.visibility = View.GONE
                loading.visibility = View.VISIBLE
                holder.itemView.isClickable = false
                holder.itemView.isFocusable = false
            } else {
                content.visibility = View.VISIBLE
                loading.visibility = View.GONE
                holder.itemView.isClickable = true
                holder.itemView.isFocusable = true
            }

            // Fallback click listener (if user manually clicks it before prefetch triggers)
            holder.itemView.setOnClickListener {
                if (!isLoadingMore) {
                    isLoadingMore = true

                    val recycler = holder.itemView.parent as RecyclerView
                    val prevPosition = holder.bindingAdapterPosition - 1

                    if (prevPosition >= 0) {
                        recycler.post {
                            recycler.findViewHolderForAdapterPosition(prevPosition)
                                ?.itemView
                                ?.requestFocus()
                        }
                    }
                    onAddMoreClicked?.invoke()
                }
            }
            return
        }

        // ==========================================
        // 2. HANDLE STANDARD MOVIE ITEM
        // ==========================================
        val currentItem = items[position]

        holder.showYear?.text = currentItem.year
        holder.showTitle?.text = currentItem.title
        holder.showRating?.text = currentItem.rating
        holder.showRS?.text = currentItem.runtime
        holder.showType?.text = currentItem.type

        // Glide Image Loading (wait until ImageView is measured)
        holder.itemView.post {
            val ctx = holder.itemView.context
            if (ctx is android.app.Activity && (ctx.isDestroyed || ctx.isFinishing)) return@post
            val currentHeight = holder.itemView.height
            val currentWidth = holder.itemView.width
            val sizeH = (currentHeight * 2f).toInt()
            val sizeW = (currentWidth * 2f).toInt()

            holder.Movie_image?.let {
                Glide.with(holder.itemView.context)
                    .load(currentItem.posterUlr)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                    .override(sizeW, sizeH)
                    //.override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .thumbnail(
                        Glide.with(holder.itemView.context)
                            .load(currentItem.posterUlr)
                            .sizeMultiplier(0.5f)
                    )
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(it)
            }
        }

        // Click to open Watch_Page
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, Watch_Page::class.java).apply {
                putExtra("imdb_code", currentItem.imdbCode)
                putExtra("type", currentItem.type)
            }
            context.startActivity(intent)
        }

        // TV Focus logic & Background Prefetching
        holder.itemView.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                // Trigger standard UI updates
                onItemFocused?.invoke(v, currentItem)

                // âœ… TV PREFETCH LOGIC: Load more if we are in the last 6 items
                val prefetchThreshold = 8
                val currentPos = holder.bindingAdapterPosition

                if (!isLoadingMore && currentPos != RecyclerView.NO_POSITION && currentPos >= items.size - prefetchThreshold) {
                    isLoadingMore = true

                    // Post to avoid interrupting the TV focus outline animation
                    v.post {
                        onAddMoreClicked?.invoke()
                    }
                }
            } else {
                onItemFocusLost?.invoke()
            }
        }


    }

    override fun getItemCount(): Int {
        // Total items = movies + the add button
        return items.size + 1
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    fun addItem(item: filterItemOne) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun addItems(newItems: List<filterItemOne>) {
        val startPos = items.size
        items.addAll(newItems)
        notifyItemRangeInserted(startPos, newItems.size)
    }

    fun clearItems() {
        val size = items.size
        items.clear()
        notifyItemRangeRemoved(0, size)
    }
}


data class filterItemOne(
    val title: String = "",
    val backdropUrl: String= "",
    val posterUlr: String= "",
    val imdbCode: String= "",
    val type: String = "",
    val year: String = "",
    val rating: String = "",
    val runtime: String = "",
    val overview: String = "",
    val isAdult: Boolean = false,
    val genres: String = ""
)
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////

class OtherAdapter(
    private val  items: MutableList<MovieItem>,   // âœ… mutable now,
    private val layoutResId: Int   // ðŸ‘ˆ pass in the layout resource
) :  RecyclerView.Adapter<OtherAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {


        val CardViewSquare: CardView = view.findViewById(R.id.CardViewSquare)
        val Movie_image: ImageView = view.findViewById(R.id.itemImage)
        val itemText: TextView = view.findViewById(R.id.itemText)

        init {
            itemView.setOnFocusChangeListener { v, hasFocus ->
                // Scale animation
                v.animate()
                    .scaleX(if (hasFocus) 1.02f else 1f)
                    .scaleY(if (hasFocus) 1.02f else 1f)
                    .setDuration(150)
                    .start()
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutResId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val currentItem = items[position]

        val title = currentItem.title
        val imageUrl = currentItem.imageUrl
        val imdbCode = currentItem.imdbCode
        val type = currentItem.type


        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
            .centerCrop()
            .into(holder.Movie_image)


        holder.CardViewSquare.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, Watch_Page::class.java).apply {
                putExtra("imdb_code", imdbCode)
                putExtra("type", type)
            }
            context.startActivity(intent)
            Log.e("OtherAdapter", "clicked ${intent.toString()}")
        }

    }

    override fun getItemCount() = items.size

    fun addItem(item: MovieItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)

    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////


class CategoryAdapter(
    private val  items: MutableList<categoryItem>,   // âœ… mutable now,
    private val layoutResId: Int   // ðŸ‘ˆ pass in the layout resource

) :  RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    companion object {
        private var lastKeyTime = 0L
        private val KEY_DEBOUNCE_DELAY = 150L // ms
    }

    var onItemFocused: ((View, categoryItem) -> Unit)? = null
    var onItemFocusLost: (() -> Unit)? = null

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val CardViewSquare: CardView = view.findViewById(R.id.categoryView)
        val category_image: ImageView = view.findViewById(R.id.categoryImage)


        init {
            itemView.setOnFocusChangeListener { v, hasFocus ->
                // Scale animation
                v.animate()
                    .scaleX(if (hasFocus) 1.02f else 1f)
                    .scaleY(if (hasFocus) 1.02f else 1f)
                    .setDuration(150)
                    .start()
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutResId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val currentItem = items[position]
        val imageUrl = currentItem.cImg
        val imdbCode = currentItem.cCode
        val companyName = currentItem.cName

        // âœ… wait until ImageView is measured
        holder.category_image.post {
            val ctx = holder.itemView.context
            if (ctx is android.app.Activity && (ctx.isDestroyed || ctx.isFinishing)) return@post
            val currentHeight = holder.category_image.height
            val size = (currentHeight  * 1.5f).toInt()

            Glide.with(holder.itemView.context)
                .load(imageUrl)
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                .override(Target.SIZE_ORIGINAL, size)
                .thumbnail(
                    Glide.with(holder.itemView.context)
                        .load(imageUrl)
                        .sizeMultiplier(0.1f)
                )
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.category_image)

        }



        //GlobalUtils.enableFullViewOnDescendantFocus(currentItem.parentView, holder.CardViewSquare)


        holder.CardViewSquare.setOnClickListener {
            val context = holder.itemView.context


            val args = android.os.Bundle().apply {
                putString("company_id", imdbCode)
                putString("company_name", companyName)
            }
            (context as com.example.onyx.HomeActivity).navigateToFragment(CategoryFragment(), args)
        }

        holder.CardViewSquare.setOnFocusChangeListener { v, hasFocus ->
            
            v.animate()
                .scaleX(if (hasFocus) 1.05f else 1f)
                .scaleY(if (hasFocus) 1.05f else 1f)
                .setDuration(150)
                .start()

            if (hasFocus) {
                onItemFocused?.invoke(v, currentItem)
            }
            else {
                onItemFocusLost?.invoke()   // hide popup
            }
        }


        holder.CardViewSquare.setOnKeyListener { v, keyCode, event ->

            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
            val now = System.currentTimeMillis()
            if (now - lastKeyTime < KEY_DEBOUNCE_DELAY) return@setOnKeyListener true
            lastKeyTime = now

            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    //if (position == 0) return@setOnKeyListener true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (position == items.size-1) return@setOnKeyListener true
                }
            }

            false
        }
    }

    override fun getItemCount() = items.size

    fun addItem(item: categoryItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)

    }
}

data class categoryItem(
    val cCode: String = "",
    val cImg: String= "",
    val cName: String = "",
    val parentView: ViewGroup

)

////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////


class GridAdapter2(
    private val  items: MutableList<MovieItem>,   // âœ… mutable now,
    private val layoutResId: Int   // ðŸ‘ˆ pass in the layout resource
) :  RecyclerView.Adapter<GridAdapter2.ViewHolder>() {

    var onAddMoreClicked: (() -> Unit)? = null
    var onItemFocused: ((View, MovieItemOne) -> Unit)? = null
    var onItemFocusLost: (() -> Unit)? = null

    var isLoadingMore = false

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val Movie_image: ImageView = view.findViewById(R.id.itemImage)
        val showYear: TextView = view.findViewById(R.id.itemText)

        val showTitle: TextView = view.findViewById(R.id.showTitle)
        val showRating: TextView = view.findViewById(R.id.showRating)
        val showRS: TextView = view.findViewById(R.id.showRS)
        val showType: TextView = view.findViewById(R.id.showType)


        init {
            itemView.setOnFocusChangeListener { v, hasFocus ->
                // Scale animation
                v.animate()
                    .scaleX(if (hasFocus) 1.02f else 1f)
                    .scaleY(if (hasFocus) 1.02f else 1f)
                    .setDuration(150)
                    .start()

                try {
                    // Overlay fade
                    val overlay: View = itemView.findViewById(R.id.focusOverlay)

                    if (hasFocus) {
                        overlay.apply {
                            alpha = 0f
                            visibility = View.VISIBLE
                            animate().alpha(1f).setDuration(150).start()
                        }
                    } else {
                        overlay.animate()
                            .alpha(0f)
                            .setDuration(150)
                            .withEndAction { overlay.visibility = View.GONE }
                            .start()
                    }
                } catch (e : Exception){}
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutResId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val currentItem = items[position]

        val title = currentItem.title
        val imageUrl = currentItem.imageUrl
        val imdbCode = currentItem.imdbCode
        val type = currentItem.type
        val year = currentItem.year
        val rating = currentItem.rating
        val runtime = currentItem.runtime





        holder.showYear.text = year
        holder.showTitle.text = title
        holder.showRating.text = rating
        holder.showRS.text = runtime
        holder.showType.text = type



        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
            .centerInside()
            .into(holder.Movie_image)


        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, Watch_Page::class.java)
            intent.putExtra("imdb_code", currentItem.imdbCode)
            intent.putExtra("type", currentItem.type)
            context.startActivity(intent)
        }

    }

    override fun getItemCount() = items.size

    // ðŸ‘‡ helper to add items one by one
    fun addItem(item: MovieItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)

    }

    fun clearItems() {
        items.clear()
        notifyDataSetChanged()
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////



class RecommendAdapter(
    private val  items: MutableList<MovieItem>,   // âœ… mutable now,
    private val layoutResId: Int   // ðŸ‘ˆ pass in the layout resource
) :  RecyclerView.Adapter<RecommendAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {


        val CardViewSquare: CardView = view.findViewById(R.id.CardViewSquare)
        val Movie_image: ImageView = view.findViewById(R.id.itemImage)
        val itemText: TextView = view.findViewById(R.id.itemText)



        init {
            itemView.setOnFocusChangeListener { v, hasFocus ->
                // Scale animation
                v.animate()
                    .scaleX(if (hasFocus) 1.02f else 1f)
                    .scaleY(if (hasFocus) 1.02f else 1f)
                    .setDuration(150)
                    .start()
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutResId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val currentItem = items[position]

        val title = currentItem.title
        val imageUrl = currentItem.imageUrl
        val imdbCode = currentItem.imdbCode
        val type = currentItem.type

        holder.itemText.text = title


        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
            .centerCrop()
            .into(holder.Movie_image)


        holder.CardViewSquare.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, Watch_Page::class.java).apply {
                putExtra("imdb_code", imdbCode)
                putExtra("type", type)
            }
            context.startActivity(intent)
            Log.e("OtherAdapter", "clicked ${intent.toString()}")
        }

    }

    override fun getItemCount() = items.size

    fun addItem(item: MovieItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)

    }
}
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////



class CastAdapter(
    private val  items: MutableList<CastItem>,   // âœ… mutable now,
    private val layoutResId: Int   // ðŸ‘ˆ pass in the layout resource
) :  RecyclerView.Adapter<CastAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val CardViewcontiner: CardView = view.findViewById(R.id.CardViewcontiner)
        val Movie_image: ImageView = view.findViewById(R.id.itemImage)
        val Actor_Name: TextView = view.findViewById(R.id.itemText)



        init {

            itemView.setOnFocusChangeListener { v, hasFocus ->
                // Scale animation
                v.animate()
                    .scaleX(if (hasFocus) 1.02f else 1f)
                    .scaleY(if (hasFocus) 1.02f else 1f)
                    .setDuration(150)
                    .start()

            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutResId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val currentItem = items[position]

        val title = currentItem.title
        val imageUrl = currentItem.imageUrl
        val imdbCode = currentItem.imdbCode
        val type = currentItem.type



        holder.Actor_Name.text = title

        val density = holder.itemView.getContext().getResources().getDisplayMetrics().density
        val sizeInPx = (100 * density * 1.5f).toInt() // 100dp converted to pixels

        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
            //.override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
            .override(sizeInPx, sizeInPx)
            .thumbnail(
                Glide.with(holder.itemView.context)
                    .load(imageUrl)
                    .sizeMultiplier(0.1f)
            )
            .circleCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(holder.Movie_image)

        holder.CardViewcontiner.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, Actor_Page::class.java)
            intent.putExtra("imdb_code", imdbCode)
            intent.putExtra("type", type)
            context.startActivity(intent)
        }

        holder.CardViewcontiner.setOnKeyListener { v, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false

            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (position == 0) {
                        // First item - stop focus from moving out to the left
                        return@setOnKeyListener true
                    }
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (position == items.size - 1) {
                        // Last item - stop focus from moving out to the right
                        return@setOnKeyListener true
                    }
                }
            }

            false
        }


    }

    override fun getItemCount() = items.size

    // ðŸ‘‡ helper to add items one by one
    fun addItem(item: CastItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)

    }

}


data class CastItem(
    val title: String = "",
    val imageUrl: String= "",
    val imdbCode: String= "",
    val type: String = "",

)

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class ProfileAdapter (
    private val  items: MutableList<profileItem>,   // âœ… mutable now,
    private val layoutResId: Int   // ðŸ‘ˆ pass in the layout resource
) :  RecyclerView.Adapter<ProfileAdapter .ViewHolder>() {

    var onProfileSelected: ((profileItem) -> Unit)? = null

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val CardViewcontiner: CardView = view.findViewById(R.id.profileCardContiner)
        val profileImageWidget: ImageView = view.findViewById(R.id.itemUserAvatar)
        val usernameWidget: TextView = view.findViewById(R.id.itemUsername)



        init {

            itemView.setOnFocusChangeListener { v, hasFocus ->
                // Scale animation
                v.animate()
                    .scaleX(if (hasFocus) 1.02f else 1f)
                    .scaleY(if (hasFocus) 1.02f else 1f)
                    .setDuration(150)
                    .start()

            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutResId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val currentItem = items[position]

        val username = currentItem.username
        val userid = currentItem.userid
        val avatarImg = currentItem.avatar


        holder.usernameWidget.text = username

        // Handle "Create Profile" button appearance
        if (userid == "CREATE") {
            holder.profileImageWidget.setImageResource(R.drawable.ic_account)
            holder.profileImageWidget.scaleType = ImageView.ScaleType.CENTER_INSIDE
            holder.profileImageWidget.setBackgroundColor(Color.parseColor("#00000000"))


        } else {
            // Handle avatar loading - if empty, use placeholder
            if (avatarImg.isNotEmpty()) {

                val assetPath = "file:///android_asset/$avatarImg"
                Glide.with(holder.itemView.context)
                    .load(assetPath)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.profileImageWidget)
            } else {
                holder.profileImageWidget.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }

        holder.CardViewcontiner.setOnClickListener {
            onProfileSelected?.invoke(currentItem) ?: run {

            }
        }

        holder.CardViewcontiner.setOnKeyListener { v, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false

            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (position == 0) {
                        // First item - stop focus from moving out to the left
                        return@setOnKeyListener true
                    }
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (position == items.size - 1) {
                        // Last item - stop focus from moving out to the right
                        return@setOnKeyListener true
                    }
                }
            }

            false
        }


    }

    override fun getItemCount() = items.size

    fun addItem(item: profileItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)

    }
    
    fun clearItems() {
        items.clear()
        notifyDataSetChanged()
    }

}


data class profileItem(
    val username: String = "",
    val avatar: String= "",
    val userid: String= "",
    )

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// AvatarAdapter - for selecting profile avatars from assets
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class AvatarAdapter(
    private val avatarPaths: List<String>,
    private val layoutResId: Int
) : RecyclerView.Adapter<AvatarAdapter.ViewHolder>() {

    var onAvatarSelected: ((String) -> Unit)? = null
    private var selectedPosition: Int = -1

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatarImage: ImageView = view.findViewById(R.id.avatarImage)
        val avatarCardView: CardView = view.findViewById(R.id.avatarCardView)

        init {
            itemView.setOnFocusChangeListener { v, hasFocus ->
                // Scale animation on focus
                v.animate()
                    .scaleX(if (hasFocus) 1.05f else 1f)
                    .scaleY(if (hasFocus) 1.05f else 1f)
                    .setDuration(150)
                    .start()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutResId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val avatarPath = avatarPaths[position]
        val context = holder.itemView.context

        // Load image from assets using Glide
        try {
            val assetPath = "file:///android_asset/$avatarPath"
            Glide.with(context)
                .load(assetPath)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.avatarImage)
        } catch (e: Exception) {
            Log.e("AvatarAdapter", "Error loading avatar: ${e.message}", e)
            holder.avatarImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        // Highlight selected avatar
        if (position == selectedPosition) {
            holder.avatarCardView.setCardBackgroundColor(
                Color.parseColor("#4CAF50")
            )
        } else {
            holder.avatarCardView.setCardBackgroundColor(
                Color.TRANSPARENT
            )
        }

        // Handle click
        holder.avatarCardView.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition
            
            // Notify changes for selection highlight
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
            
            // Invoke callback
            onAvatarSelected?.invoke(avatarPath)
        }

        // Handle focus for TV/remote control
        holder.itemView.isFocusable = true
        holder.itemView.isFocusableInTouchMode = false
    }

    override fun getItemCount() = avatarPaths.size
}


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////

class FavAdapter(
    private val items: MutableList<FavItem>,
    private val layoutResId: Int,
    private val backdropView: ImageView? = null,
    private val favTitleView: TextView? = null,
    private val favGenreView: TextView? = null,
    private val favTypeView: TextView? = null,
    private val favRatingView: TextView? = null,
    private val favYearView: TextView? = null,
    private val favOverviewView: TextView? = null,
    private val RemoveFaveItemBtn: LinearLayout? = null
) : RecyclerView.Adapter<FavAdapter.ViewHolder>() {

    // âœ… ADDED: Companion object to hold the debounce variables
    companion object {
        private var lastKeyTime = 0L
        private const val KEY_DEBOUNCE_DELAY = 300L // ms - Tweak this number to feel faster or slower
        private const val FAVORITE_MIN_WIDTH_DP = 100
        private const val FAVORITE_RESERVED_MAX_WIDTH_DP = 420
    }

    private fun requestFlexboxRelayout(itemView: View) {
        itemView.post {
            itemView.requestLayout()
            (itemView.parent as? RecyclerView)?.let { recyclerView ->
                recyclerView.post {
                    recyclerView.invalidateItemDecorations()
                    recyclerView.requestLayout()
                }
            }
        }
    }

    private fun dpToPx(view: View, dp: Int): Int {
        return (dp * view.resources.displayMetrics.density).toInt()
    }

    private fun reserveFavoriteWidth(itemView: View, imageView: ImageView) {
        imageView.maxWidth = dpToPx(itemView, FAVORITE_RESERVED_MAX_WIDTH_DP)
        itemView.minimumWidth = dpToPx(itemView, FAVORITE_RESERVED_MAX_WIDTH_DP)
    }

    private fun releaseFavoriteWidth(itemView: View, imageView: ImageView) {
        imageView.maxWidth = dpToPx(itemView, FAVORITE_RESERVED_MAX_WIDTH_DP)
        itemView.minimumWidth = dpToPx(itemView, FAVORITE_MIN_WIDTH_DP)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val Movie_image: ImageView = view.findViewById(R.id.itemImage)
        val itemText: TextView? = view.findViewById(R.id.itemText)

        init {
            itemView.setOnFocusChangeListener { _, hasFocus ->

                // Scale animation
                itemView.animate()
                    .scaleX(if (hasFocus) 1.02f else 1f)
                    .scaleY(if (hasFocus) 1.02f else 1f)
                    .setDuration(150)
                    .start()

                if (hasFocus) {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        val item = items[pos]

                        backdropView?.let {
                            Glide.with(it.context)
                                .load(item.backdropUrl)
                                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                                .into(it)

                        }


                        favTitleView?.text = item.title
                        favGenreView?.text = item.genres
                        favTypeView?.text = item.showType
                        favRatingView?.text = "${item.voteAverage}"
                        favYearView?.text = item.releaseDate
                        favOverviewView?.text = item.overview

                        RemoveFaveItemBtn?.setOnClickListener {
                            val currentPos = bindingAdapterPosition
                            if (currentPos != RecyclerView.NO_POSITION) {
                                // TODO: removeFavorite(it.context, item.imdbCode, item.showType)

                                items.removeAt(currentPos)
                                notifyItemRemoved(currentPos)

                                favTitleView?.text = ""
                                favOverviewView?.text = ""
                                backdropView?.setImageDrawable(null)
                            }
                        }
                    }
                }
            }

            itemView.setOnKeyListener { _, keyCode, event ->
                if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false

                // âœ… ADDED: D-Pad Debounce Logic
                val now = System.currentTimeMillis()
                if (now - lastKeyTime < KEY_DEBOUNCE_DELAY) return@setOnKeyListener true
                lastKeyTime = now

                val currentPos = bindingAdapterPosition

                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        // If we are at the very first item, swallow the left click
                        if (currentPos == 0) {
                            return@setOnKeyListener true
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        // If we are at the very last item, swallow the right click
                        if (currentPos == items.size - 1) {
                            return@setOnKeyListener true
                        }
                    }
                }

                // Otherwise, let the RecyclerView handle the scroll normally
                false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutResId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = items[position]
        val posterUrl = currentItem.posterUrl
        val backdropUrl = currentItem.backdropUrl
        val imdbCode = currentItem.imdbCode
        val type = currentItem.showType

        holder.itemText?.text = currentItem.title
        reserveFavoriteWidth(holder.itemView, holder.Movie_image)

        Glide.with(holder.itemView.context)
            .load(backdropUrl)
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    releaseFavoriteWidth(holder.itemView, holder.Movie_image)
                    requestFlexboxRelayout(holder.itemView)
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    releaseFavoriteWidth(holder.itemView, holder.Movie_image)
                    requestFlexboxRelayout(holder.itemView)
                    return false
                }
            })
            .centerInside()
            .into(holder.Movie_image)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            if (type == "anime") {
                val args = android.os.Bundle().apply {
                    putString("anime_code", imdbCode)
                    putString("anime_poster", posterUrl)
                }
                (context as com.example.onyx.HomeActivity).navigateToFragment(com.example.onyx.WatchAnimeFragment(), args)
            } else {
                val intent = Intent(context, Watch_Page::class.java).apply {
                    putExtra("imdb_code", imdbCode)
                    putExtra("type", type)
                }
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount() = items.size

    // ==========================================
    // HELPER METHODS
    // ==========================================

    fun addItem(item: FavItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun updateItems_(newItems: List<FavItem>) {
        val oldSize = items.size
        items.clear()
        notifyItemRangeRemoved(0, oldSize)

        items.addAll(newItems)
        notifyItemRangeInserted(0, newItems.size)
    }

    suspend fun updateItems(newItems: List<FavItem>) {
        val oldSize = items.size
        items.clear()
        notifyItemRangeRemoved(0, oldSize)

        // Load 6 items at a time (tweak this based on how many fit on your screen)
        val chunkSize = 6

        for (i in newItems.indices step chunkSize) {
            val end = Math.min(i + chunkSize, newItems.size)
            val chunk = newItems.subList(i, end)

            val currentStart = items.size
            items.addAll(chunk)
            notifyItemRangeInserted(currentStart, chunk.size)

            // âœ… The Magic: Yield back to the Main Thread for 16ms (1 frame at 60fps)
            // This gives the TV time to draw the items before processing the next batch.
            delay(16)
        }
    }

    fun clearItems() {
        val size = items.size
        items.clear()
        notifyItemRangeRemoved(0, size)
    }
}


data class FavItem(
    val title: String,
    val posterUrl: String,
    val backdropUrl: String,
    val releaseDate: String,
    val runtime: String,
    val overview: String,
    val voteAverage: String,
    val genres: String,
    val production: String,
    val parentalGuide: String,
    val imdbCode: String,
    val showType : String,
)
//////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////

class EqualSxpaceItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val spanCount = (parent.layoutManager as? GridLayoutManager)?.spanCount ?: 1

        // Apply equal spacing on all sides
        outRect.left = space / 2
        outRect.right = space / 2
        outRect.top = space / 2
        outRect.bottom = space / 2

        // Optional: extra space for first/last rows & columns so edges are even
        if (position < spanCount) {
            outRect.top = space // first row
        }
        if (position % spanCount == 0) {
            outRect.left = space // first column
        }
    }
}


class EqualSpaceItemDecoration_o(private val space: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val spanCount = (parent.layoutManager as? GridLayoutManager)?.spanCount ?: 1

        // Apply equal spacing on all sides
        outRect.left = space / 2
        outRect.right = space / 2
        outRect.top = space / 2
        outRect.bottom = space / 2

        // Optional: extra space for first/last rows & columns so edges are even
        if (position < spanCount) {
            outRect.top = space // first row
        }
        if (position % spanCount == 0) {
            outRect.left = space // first column
        }
    }
}

class EqualSpaceItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val layoutManager = parent.layoutManager

        // 1. Apply base equal spacing on all sides (creates 'space' between items)
        outRect.left = space / 2
        outRect.right = space / 2
        outRect.top = space / 2
        outRect.bottom = space / 2

        // 2. Only apply Grid edge logic if it is actually a Grid
        if (layoutManager is GridLayoutManager) {
            val spanCount = layoutManager.spanCount

            if (position < spanCount) {
                outRect.top = space // first row
            }
            if (position % spanCount == 0) {
                outRect.left = space // first column
            }
        }
        // 3. Optional: Edge logic for Linear Layouts (Horizontal or Vertical)
        else if (layoutManager is LinearLayoutManager) {
            // If you want the outer edges of the list to be equal to the space between items,
            // uncomment the lines below based on your orientation:

            /*
            if (layoutManager.orientation == LinearLayoutManager.VERTICAL) {
                // if (position == 0) outRect.top = space // Extra space at very top
                // outRect.left = space  // Full space on left
                // outRect.right = space // Full space on right
            } else { // HORIZONTAL
                // if (position == 0) outRect.left = space // Extra space at very left
                // outRect.top = space    // Full space on top
                // outRect.bottom = space // Full space on bottom
            }
            */
        }
    }
}


//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


class EpisodesAdapter(
    private val episodes: MutableList<EpisodeItem>,
    private val db: AppDatabase,
    private val userId: Int,
) : RecyclerView.Adapter<EpisodesAdapter.EpisodeViewHolder>() {

    private val adapterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    inner class EpisodeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val epNoView: TextView = view.findViewById(R.id.episode_Number)
        val titleView: TextView = view.findViewById(R.id.episode_title)
        val durationView: TextView = view.findViewById(R.id.episode_duration)
        val ratingView: TextView = view.findViewById(R.id.episode_Rating)
        val descView: TextView = view.findViewById(R.id.episode_description)
        val epsImg: ImageView = view.findViewById(R.id.Ep_IMG)

        var cWatchSeek_bar: SeekBar = view.findViewById(R.id.cWatchSeek_bar)

        val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        var job: Job? = null  // Track the coroutine for this ViewHolder

        var lastClickTime = 0L


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_episode, parent, false)
        return EpisodeViewHolder(view)

    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        val ep = episodes[position]

        holder.job?.cancel()  // cancel previous coroutine if any
        holder.job = holder.scope.launch {
            val itemId = "${ep.seriesId}_S${ep.seasonNumber}_E${ep.episodesNumber}"


            val (lastPos, durationPos) = withContext(Dispatchers.IO) {
                val last = db.getResumePosition(userId, itemId, "tv").toLong()
                val duration = db.getDurationPosition(userId, itemId, "tv").toLong()
                last to duration
            }

            val safeDuration = if (durationPos == 0L) 1L else durationPos
            val progress = ((lastPos.toDouble() / safeDuration.toDouble()) * 1000).toInt()
            holder.cWatchSeek_bar.max = 1000
            holder.cWatchSeek_bar.progress = progress.coerceIn(0, 1000)
        }


        holder.epNoView.text = "S${ep.seasonNumber}-E${ep.episodesNumber}"
        holder.titleView.text = ep.episodesName
        holder.durationView.text = "â± ${ep.episodesRuntime} min"
        holder.ratingView.text = "â˜… ${ep.episodesRating}"
        holder.descView.text = ep.episodesDescription

        GlobalUtils.enableFullViewOnDescendantFocus(ep.parentView, holder.itemView)


        val url = "https://image.tmdb.org/t/p/w1280${ep.episodesImage}"
        val currentHeight = holder.itemView.height
        val currentWidth = holder.itemView.width
        val sizeH = (currentHeight  * 2f).toInt()
        val sizeW = (currentWidth  * 2f).toInt()
        Glide.with(holder.itemView.context)
            .load(url)
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
            //.override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
            .override(sizeW, sizeH)
            .into(holder.epsImg)

        //var lastClickTime = 0L
        holder.itemView.setOnClickListener {view ->
            val now = System.currentTimeMillis()
            if (now - holder.lastClickTime < 1000) return@setOnClickListener
            holder.lastClickTime = now

            val context = holder.itemView.context
            val intent = Intent(context, Play::class.java).apply {
                Log.e("DEBUG_Each EpisodeWatch", "Eps ${ep.episodesNumber} Season ${ep.seasonNumber}")
                putExtra("imdb_code", ep.seriesId)
                putExtra("type", "tv")
                putExtra("seasonNo", ep.seasonNumber)
                putExtra("episodeNo", ep.episodesNumber)
                putExtra("poster", ep.showPoster)
                putExtra("backdrop", ep.showBackdrop)
                putExtra("title", ep.showTitle)
            }
            //context.startActivity(intent)
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("EpisodesAdapter", "Play activity not found", e)
                Toast.makeText(context, "Cannot open player", Toast.LENGTH_SHORT).show()
            }
        }



        // âœ… Attach the KeyListener here
        holder.itemView.setOnKeyListener { v, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false

            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (position == 0) {
                        // First item - stop focus from moving out to the left
                        return@setOnKeyListener true
                    }
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (position == episodes.size - 1) {
                        // Last item - stop focus from moving out to the right
                        return@setOnKeyListener true
                    }
                }
            }

            false
        }
    }

    override fun getItemCount(): Int = episodes.size

    fun updateData(newList: List<EpisodeItem>) {


        episodes.clear()
        episodes.addAll(newList)
        notifyDataSetChanged()
    }

    fun clear() {
        adapterScope.cancel()
    }
}



data class EpisodeItem(
    val showTitle: String = "",
    val showPoster: String = "",
    val showBackdrop: String = "",
    val episodesName: String = "",
    val episodesImage: String= "",
    val episodesNumber: String= "",
    val episodesRating: String = "",
    val episodesRuntime: String = "",
    val episodesDescription: String = "",
    val seriesId: String = "",
    val seasonNumber: String = "",
    val parentView: ViewGroup

)

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


data class MovieItem(
    val title: String = "",
    val imageUrl: String= "",
    val imdbCode: String= "",
    val type: String = "",
    val year: String = "",
    val rating: String = "",
    val runtime: String = ""
)


//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////





class NotificationAdapter(
    private val items: MutableList<NotificationItem>,
    private val layoutResId: Int,
    private val onNotificationClicked: (NotificationItem) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        //val image: ImageView = view.findViewById(R.id.notification_title)
        val showTitle: TextView = view.findViewById(R.id.notification_title)
        val message: TextView = view.findViewById(R.id.notification_message)

        val backdrop: ImageView = view.findViewById(R.id.notification_backdrop)
        val imageContainer: ImageView = view.findViewById(R.id.notification_icon)
        val timeContainer: TextView = view.findViewById(R.id.timestamp_text)


        init {

            itemView.setOnFocusChangeListener { v, hasFocus ->
                // Scale animation
                v.animate()
                    .scaleX(if (hasFocus) 1.02f else 1f)
                    .scaleY(if (hasFocus) 1.02f else 1f)
                    .setDuration(150)
                    .start()
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutResId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {


        val currentItem = items[position]

        val title = currentItem.title
        val imageUrl = currentItem.imageUrl
        val imdbCode = currentItem.imdbCode
        val type = currentItem.type
        val info = currentItem.info
        val updateSeason  = currentItem.newSeason
        val updateEpisode  = currentItem.newEpisode
        val lastPos = currentItem.time?.toLongOrNull() ?: 0L

        holder.timeContainer.text = formatTimeFromString(currentItem.time)


        holder.showTitle.text =  title
        holder.message.text =  info



        // 2. Load the image with Glide
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
            .into(holder.backdrop)
            //.diskCacheStrategy(DiskCacheStrategy.ALL)


        holder.itemView.setOnClickListener {
            val adapterPos = holder.bindingAdapterPosition
            if (adapterPos == RecyclerView.NO_POSITION) return@setOnClickListener
            
            val clickedItem = items[adapterPos]
            items.removeAt(adapterPos)
            notifyItemRemoved(adapterPos)

            onNotificationClicked(clickedItem)
        }

    }

    override fun getItemCount() = items.size

    // ðŸ‘‡ helper to add items one by one
    fun addItem(item: NotificationItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }
    
    // ðŸ‘‡ helper to refresh all items
    fun clearItems() {
        items.clear()
        notifyDataSetChanged()
    }

    fun updateItems(newItems: List<NotificationItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
    
    // ðŸ‘‡ helper to remove specific item by imdbCode
    fun removeItem(imdbCode: String) {
        val index = items.indexOfFirst { it.imdbCode == imdbCode }
        if (index != -1) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    private fun formatTimeFromString(timeString: String?): String {

        val timeMillis = timeString
            ?.trim()
            ?.toLongOrNull()
            ?: return "Just now"

        return DateUtils.getRelativeTimeSpanString(
            timeMillis,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }
}


data class NotificationItem(
    val notificationId: String,
    val imdbCode: String,
    val title: String,
    val imageUrl: String?,
    val info: String,
    val type: String = "tv",
    val newSeason: String,
    val newEpisode: String,
    val time: String
)

////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////

class cWatchingAdapter(
    private val items: MutableList<HashMap<String, String>>,
    private val layoutResId: Int
) : RecyclerView.Adapter<cWatchingAdapter.ViewHolder>() {


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val rootCard: CardView = view.findViewById(R.id.rootCard)
        val poster: ImageView = view.findViewById(R.id.itemImage)
        val title: TextView = view.findViewById(R.id.watchItemTitle)
        val episode: TextView = view.findViewById(R.id.watchItemEpisode)
        val lastPosition: TextView = view.findViewById(R.id.watchItemLastPosition)
        val duration: TextView = view.findViewById(R.id.watchItemDuration)
        val seekBar: SeekBar = view.findViewById(R.id.cWatchSeek_bar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutResId, parent, false)
        return ViewHolder(view)
    }

    @OptIn(UnstableApi::class)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = items[position]

        val itemId = item["item_id"] ?: ""
        val type = item["type"] ?: ""
        val title = item["title"] ?: ""
        val posterUrl = item["poster"] ?: ""
        val backdropUrl = item["backdrop"] ?: ""
        val episodeNumber = item["episode_number"] ?: ""
        val seasonNumber = item["season_number"] ?: ""
        val lastPos = item["last_position"]?.toLongOrNull() ?: 0L
        val duration = item["duration"]?.toLongOrNull() ?: 1L

        // ---------- UI ----------
        holder.title.text = title


        holder.lastPosition.text = formatTimeMillis(lastPos)
        holder.duration.text = formatTimeMillis(duration)


        val progress = ((lastPos.toDouble() / duration.toDouble()) * 1000).toInt()
        holder.seekBar.progress = progress.coerceIn(0, 1000)

        Glide.with(holder.itemView.context)
            .load(posterUrl)
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
            .centerCrop()
            .into(holder.poster)

        // ---------- CLICK â†’ RESUME ----------
        val context = holder.itemView.context
        if(type=="anime"){
            holder.episode.text = "E$episodeNumber"

            holder.rootCard.setOnClickListener {
                val context = holder.itemView.context

                Anime_Video_Player.Companion.playVideoExternally(context, itemId, episodeNumber, seasonNumber)
            }

        }else if(type=="movie"){
            holder.episode.text = ""

            holder.rootCard.setOnClickListener {

                val intent = Intent(context, Watch_Page::class.java)
                intent.putExtra("imdb_code", itemId)
                intent.putExtra("type", type)
                intent.putExtra("title", title)
                intent.putExtra("poster", posterUrl)
                intent.putExtra("backdrop", backdropUrl)
                intent.putExtra("seasonNo", seasonNumber)
                intent.putExtra("EpisodeNo", episodeNumber)
                intent.putExtra("continue_play", true)

                context.startActivity(intent)
            }

        }else if(type=="tv"){
            holder.episode.text = "S$seasonNumber-E$episodeNumber"
            val seriesId = itemId.substringBefore("_")

            holder.rootCard.setOnClickListener {

                val intent = Intent(context, Watch_Page::class.java)
                intent.putExtra("imdb_code", seriesId)
                intent.putExtra("type", type)
                intent.putExtra("title", title)
                intent.putExtra("poster", posterUrl)
                intent.putExtra("backdrop", backdropUrl)
                intent.putExtra("seasonNo", seasonNumber)
                intent.putExtra("EpisodeNo", episodeNumber)
                intent.putExtra("continue_play", true)

                context.startActivity(intent)
            }
        }

    }

    override fun getItemCount(): Int = items.size

    fun updateItems_ (newItems: List<HashMap<String, String>>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    suspend fun updateItems(newItems: List<HashMap<String, String>>)  {
        val oldSize = items.size
        items.clear()
        notifyItemRangeRemoved(0, oldSize)

        // Load 6 items at a time (tweak this based on how many fit on your screen)
        val chunkSize = 6

        for (i in newItems.indices step chunkSize) {
            val end = Math.min(i + chunkSize, newItems.size)
            val chunk = newItems.subList(i, end)

            val currentStart = items.size
            items.addAll(chunk)
            notifyItemRangeInserted(currentStart, chunk.size)

            // âœ… The Magic: Yield back to the Main Thread for 16ms (1 frame at 60fps)
            // This gives the TV time to draw the items before processing the next batch.
            delay(16)
        }
    }

    fun clearItems() {
        items.clear()
        notifyDataSetChanged()
    }

    // ---------- UTIL ----------
    private fun formatTimeMillis(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0)
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        else
            String.format("%02d:%02d", minutes, seconds)
    }
}



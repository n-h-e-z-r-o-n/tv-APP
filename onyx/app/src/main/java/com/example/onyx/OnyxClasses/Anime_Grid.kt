package com.example.onyx.OnyxClasses

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.onyx.R
import com.example.onyx.WatchAnimeFragment
import com.example.onyx.Watch_Page
import com.bumptech.glide.request.target.Target
import com.example.onyx.HomeActivity
import com.google.android.material.card.MaterialCardView


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
class AnimeTrendingAdapter(
    initialItems: List<TrendingAnimeItem>,
    private val layoutResId: Int
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_NORMAL = 0
        private const val VIEW_TYPE_ADD_MORE = 1

        private const val CLICK_DEBOUNCE_MS = 350L

        private const val FOOTER_ID = Long.MIN_VALUE

        private const val PAYLOAD_LOADING = "payload_loading"
    }


    private val items = initialItems.toMutableList()


    private val stableIds = mutableMapOf<String, Long>()
    private var nextStableId = 1L


    var onAddMoreClicked: (() -> Unit)? = null

    var onItemFocused: ((position: Int) -> Unit)? = null


    var onItemClicked: ((TrendingAnimeItem) -> Unit)? = null

    private var lastClickTime = 0L

    var isLoadingNext = false
        set(value) {
            if (field == value) return

            field = value

            notifyItemChanged(items.size, PAYLOAD_LOADING)
        }

    init {
        setHasStableIds(true)
    }
    private inner class AnimeViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view) {

        val cardContainer: MaterialCardView? =
            view.findViewById(R.id.CardViewcontiner)

        val movieImage: ImageView? =
            view.findViewById(R.id.itemImage)

        val rank: TextView? =
            view.findViewById(R.id.rank)

        val title: TextView? =
            view.findViewById(R.id.title)

        val cardSub: TextView? =
            view.findViewById(R.id.cardSub)

        val cardDub: TextView? =
            view.findViewById(R.id.cardDub)
    }

    private inner class AddMoreViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view) {

        val content: View? =
            view.findViewById(R.id.addMoreContent)

        val loading: View? =
            view.findViewById(R.id.addMoreLoading)
    }


    override fun getItemCount(): Int {
        /*
         * +1 = Add More footer
         */
        return items.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == items.size) {
            VIEW_TYPE_ADD_MORE
        } else {
            VIEW_TYPE_NORMAL
        }
    }

    override fun getItemId(position: Int): Long {

        if (position == items.size) {
            return FOOTER_ID
        }

        val item = items.getOrNull(position)
            ?: return RecyclerView.NO_ID

        return stableIds.getOrPut(item.id) {
            nextStableId++
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {

            VIEW_TYPE_ADD_MORE -> {
                val view = inflater.inflate(
                    R.layout.item_add_more,
                    parent,
                    false
                )

                AddMoreViewHolder(view)
            }

            else -> {
                val view = inflater.inflate(
                    layoutResId,
                    parent,
                    false
                )

                AnimeViewHolder(view)
            }
        }
    }


    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {

        when (holder) {

            is AnimeViewHolder -> {
                bindAnime(holder)
            }

            is AddMoreViewHolder -> {
                bindAddMore(holder)
            }
        }
    }

    /*
     * Payload version means changing isLoadingNext doesn't unnecessarily
     * rebind other footer state.
     */
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {

        if (
            holder is AddMoreViewHolder &&
            payloads.contains(PAYLOAD_LOADING)
        ) {
            updateAddMoreLoadingState(holder)
            return
        }

        super.onBindViewHolder(
            holder,
            position,
            payloads
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Anime binding
    // ─────────────────────────────────────────────────────────────────────────

    private fun bindAnime(
        holder: AnimeViewHolder
    ) {

        /*
         * IMPORTANT:
         *
         * Do NOT use the position passed to onBindViewHolder inside listeners.
         *
         * RecyclerView positions can change after:
         *
         * prependItems()
         * appendItems()
         * removal
         * animation
         *
         * Always read bindingAdapterPosition when the event actually occurs.
         */

        val bindingPosition =
            holder.bindingAdapterPosition

        if (bindingPosition == RecyclerView.NO_POSITION) {
            return
        }

        val item = items.getOrNull(bindingPosition)
            ?: return

        holder.title?.text = item.title
        holder.rank?.text = item.rank
        holder.cardSub?.text = item.sub
        holder.cardDub?.text = item.dub

        // ── Image ─────────────────────────────────────────────────────────────

        holder.movieImage?.let { imageView ->

            Glide.with(imageView)
                .load(item.imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerInside()
                .into(imageView)
        }

        // ── Click ─────────────────────────────────────────────────────────────

        holder.cardContainer?.setOnClickListener {

            val now = SystemClock.elapsedRealtime()

            if (now - lastClickTime < CLICK_DEBOUNCE_MS) {
                return@setOnClickListener
            }

            lastClickTime = now

            val currentPosition =
                holder.bindingAdapterPosition

            if (currentPosition == RecyclerView.NO_POSITION) {
                return@setOnClickListener
            }

            val currentItem =
                items.getOrNull(currentPosition)
                    ?: return@setOnClickListener

            /*
             * Preferred:
             * let Fragment/Activity handle navigation.
             */
            val clickCallback = onItemClicked

            if (clickCallback != null) {

                clickCallback(currentItem)

            } else {

                /*
                 * Backward-compatible fallback to your existing behavior.
                 */
                val context = holder.itemView.context

                val homeActivity =
                    context as? HomeActivity

                if (homeActivity != null) {

                    val args = Bundle().apply {
                        putString(
                            "anime_code",
                            currentItem.id
                        )

                        putString(
                            "anime_poster",
                            currentItem.imageUrl
                        )
                    }

                    homeActivity.navigateToFragment(
                        WatchAnimeFragment(),
                        args
                    )
                }
            }
        }

        // ── Focus ─────────────────────────────────────────────────────────────

        holder.cardContainer?.setOnFocusChangeListener { _, hasFocus ->

            if (!hasFocus) {
                return@setOnFocusChangeListener
            }

            val currentPosition =
                holder.bindingAdapterPosition

            if (currentPosition == RecyclerView.NO_POSITION) {
                return@setOnFocusChangeListener
            }

            onItemFocused?.invoke(currentPosition)
        }

        // ── TV / DPAD ─────────────────────────────────────────────────────────

        holder.cardContainer?.setOnKeyListener { _, keyCode, event ->

            /*
             * Do NOT consume LEFT/RIGHT.
             *
             * RecyclerView needs those events to move focus naturally,
             * including moving from the final anime to the Add More card.
             */

            if (event.action != KeyEvent.ACTION_DOWN) {
                return@setOnKeyListener false
            }

            when (keyCode) {

                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN -> {

                    false
                }

                else -> false
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Add More footer
    // ─────────────────────────────────────────────────────────────────────────

    private fun bindAddMore(
        holder: AddMoreViewHolder
    ) {

        updateAddMoreLoadingState(holder)

        holder.itemView.setOnClickListener {

            if (isLoadingNext) {
                return@setOnClickListener
            }

            val now = SystemClock.elapsedRealtime()

            if (now - lastClickTime < CLICK_DEBOUNCE_MS) {
                return@setOnClickListener
            }

            lastClickTime = now

            onAddMoreClicked?.invoke()
        }

        /*
         * Keep the footer focusable for Android TV / DPAD.
         */
        holder.itemView.isFocusable = true
        holder.itemView.isFocusableInTouchMode = true
    }

    private fun updateAddMoreLoadingState(
        holder: AddMoreViewHolder
    ) {

        if (isLoadingNext) {

            holder.content?.visibility = View.GONE
            holder.loading?.visibility = View.VISIBLE

            holder.itemView.isClickable = false

        } else {

            holder.content?.visibility = View.VISIBLE
            holder.loading?.visibility = View.GONE

            holder.itemView.isClickable = true
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Recycler cleanup
    // ─────────────────────────────────────────────────────────────────────────

    override fun onViewRecycled(
        holder: RecyclerView.ViewHolder
    ) {

        if (holder is AnimeViewHolder) {

            holder.movieImage?.let { imageView ->

                /*
                 * Prevent an old image request from appearing briefly
                 * on a recycled card.
                 */
                Glide.with(imageView)
                    .clear(imageView)

                imageView.setImageDrawable(null)
            }

            holder.cardContainer?.setOnClickListener(null)
            holder.cardContainer?.onFocusChangeListener = null
            holder.cardContainer?.setOnKeyListener(null)
        }

        if (holder is AddMoreViewHolder) {
            holder.itemView.setOnClickListener(null)
        }

        super.onViewRecycled(holder)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Dataset operations
    // ─────────────────────────────────────────────────────────────────────────

    fun appendItems(
        newItems: List<TrendingAnimeItem>
    ) {

        if (newItems.isEmpty()) {
            return
        }

        /*
         * Prevent duplicate anime when pagination APIs return overlapping pages.
         */
        val existingIds =
            items.asSequence()
                .map { it.id }
                .toHashSet()

        val uniqueItems =
            newItems.filter { it.id !in existingIds }

        if (uniqueItems.isEmpty()) {
            return
        }

        /*
         * Footer currently occupies this position.
         *
         * notifyItemRangeInserted() inserts the new anime BEFORE that footer,
         * so RecyclerView automatically shifts the footer to its new position.
         */
        val insertPosition = items.size

        items.addAll(uniqueItems)

        notifyItemRangeInserted(
            insertPosition,
            uniqueItems.size
        )
    }

    fun prependItems(
        newItems: List<TrendingAnimeItem>
    ) {

        if (newItems.isEmpty()) {
            return
        }

        val existingIds =
            items.asSequence()
                .map { it.id }
                .toHashSet()

        val uniqueItems =
            newItems.filter { it.id !in existingIds }

        if (uniqueItems.isEmpty()) {
            return
        }

        items.addAll(
            0,
            uniqueItems
        )

        notifyItemRangeInserted(
            0,
            uniqueItems.size
        )
    }

    fun addItem(
        item: TrendingAnimeItem
    ) {

        if (items.any { it.id == item.id }) {
            return
        }

        val insertPosition = items.size

        items.add(item)

        notifyItemInserted(insertPosition)
    }

    fun removeItemById(
        id: String
    ): Boolean {

        val position =
            items.indexOfFirst { it.id == id }

        if (position == -1) {
            return false
        }

        items.removeAt(position)

        notifyItemRemoved(position)

        return true
    }

    fun updateItem(
        updatedItem: TrendingAnimeItem
    ): Boolean {

        val position =
            items.indexOfFirst {
                it.id == updatedItem.id
            }

        if (position == -1) {
            return false
        }

        items[position] = updatedItem

        notifyItemChanged(position)

        return true
    }

    fun clearItems() {

        if (items.isEmpty()) {
            return
        }

        val oldSize = items.size

        items.clear()

        /*
         * Better than notifyDataSetChanged():
         * RecyclerView keeps animations and knows exactly what disappeared.
         */
        notifyItemRangeRemoved(
            0,
            oldSize
        )
    }

    fun replaceItems(
        newItems: List<TrendingAnimeItem>
    ) {

        val oldSize = items.size

        if (oldSize > 0) {
            items.clear()

            notifyItemRangeRemoved(
                0,
                oldSize
            )
        }

        if (newItems.isEmpty()) {
            return
        }

        /*
         * Deduplicate replacement dataset too.
         */
        val uniqueItems =
            newItems.distinctBy {
                it.id
            }

        items.addAll(uniqueItems)

        notifyItemRangeInserted(
            0,
            uniqueItems.size
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Read access
    // ─────────────────────────────────────────────────────────────────────────

    fun getItems(): List<TrendingAnimeItem> {
        return items.toList()
    }

    fun getItemAt(
        position: Int
    ): TrendingAnimeItem? {
        return items.getOrNull(position)
    }

    fun getAnimeCount(): Int {
        return items.size
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// Model
// ─────────────────────────────────────────────────────────────────────────────

data class TrendingAnimeItem(
    val id: String,
    val title: String,
    val imageUrl: String,
    val rank: String,
    val sub: String,
    val dub: String
)

////////////////////////////////////////////////////////////////////////////////////////////////////

class AnimeAiringAdapter(
    private val  items: MutableList<AiringAnimeItem>,   // âœ… mutable now,
    private val layoutResId: Int   // ðŸ‘ˆ pass in the layout resource
) :  RecyclerView.Adapter<AnimeAiringAdapter.ViewHolder>() {

    companion object {
        private var lastKeyTime = 0L
        private val KEY_DEBOUNCE_DELAY = 150L // ms
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val CardViewcontiner: CardView = view.findViewById(R.id.CardViewcontiner)
        val Movie_image: ImageView = view.findViewById(R.id.itemImage)

        val title: TextView = view.findViewById(R.id.cardTitle)
        val cardType: TextView = view.findViewById(R.id.cardType)
        val cardDub: TextView = view.findViewById(R.id.cardDub)
        val cardSub: TextView = view.findViewById(R.id.cardSub)




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
        val imdbCode = currentItem.id

        holder.title.text = title
        holder.cardSub.text = currentItem.sub
        holder.cardDub.text = if (currentItem.dub == "null" || currentItem.dub.isEmpty()) "0" else currentItem.dub
        holder.cardType.text = currentItem.type



        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
            .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)

            .centerInside()
            .into(holder.Movie_image)

        holder.CardViewcontiner.setOnClickListener {
            val context = holder.itemView.context
            val args = android.os.Bundle().apply {
    putString("anime_code", imdbCode)
    putString("anime_poster", imageUrl)
}
(context as com.example.onyx.HomeActivity).navigateToFragment(com.example.onyx.WatchAnimeFragment(), args)
        }

        holder.CardViewcontiner.setOnKeyListener { v, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false

            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (position == 0) {
                        // First item - stop focus from moving out to the left
                        //return@setOnKeyListener true
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

    // helper to add items one by one
    fun addItem(item: AiringAnimeItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }
}

data class AiringAnimeItem(
    val id: String,
    val title: String,
    val imageUrl: String,
    val type: String,
    val sub: String,
    val dub: String
)


////////////////////////////////////////////////////////////////////////////////////////////////////




class AnimeSearchAdapter(
    private val  items: MutableList<AnimeSearchItem>,   // âœ… mutable now,
    private val layoutResId: Int   // ðŸ‘ˆ pass in the layout resource
) :  RecyclerView.Adapter<AnimeSearchAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val CardViewcontiner: CardView = view.findViewById(R.id.CardViewcontiner)
        val Movie_image: ImageView = view.findViewById(R.id.itemImage)

        val title: TextView = view.findViewById(R.id.cardTitle)
        val cardType: TextView = view.findViewById(R.id.cardType)
        val cardDub: TextView = view.findViewById(R.id.cardDub)
        val cardSub: TextView = view.findViewById(R.id.cardSub)


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
        val imdbCode = currentItem.id

        holder.title.text = title
        holder.cardSub.text = currentItem.sub
        holder.cardDub.text = if (currentItem.dub == "null" || currentItem.dub.isEmpty()) "0" else currentItem.dub
        holder.cardType.text = currentItem.type





        Glide.with(holder.itemView.context)
            .load(imageUrl)
            //.diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
            .centerInside()
            .into(holder.Movie_image)

        holder.CardViewcontiner.setOnClickListener {
            val context = holder.itemView.context
            val args = android.os.Bundle().apply {
    putString("anime_code", imdbCode)
    putString("anime_poster", imageUrl)
}
(context as com.example.onyx.HomeActivity).navigateToFragment(com.example.onyx.WatchAnimeFragment(), args)
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

    fun addItem(item: AnimeSearchItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun clearItems() {
        items.clear()
        notifyDataSetChanged()
    }

}

data class AnimeSearchItem(
    val id: String,
    val title: String,
    val imageUrl: String,
    val type: String,
    val sub: String,
    val dub: String
)

////////////////////////////////////////////////////////////////////////////////////////////////////

class AnimeGridAdapter(
    private val items: MutableList<AnimeGridItem>,
    private val layoutResId: Int,
) : RecyclerView.Adapter<AnimeGridAdapter.ViewHolder>(), FocusableAdapter<AnimeGridItem> {

    companion object {
        private const val VIEW_TYPE_MOVIE = 0
        private const val VIEW_TYPE_ADD_BUTTON = 1
        private var lastKeyTime = 0L
        private val KEY_DEBOUNCE_DELAY = 430L // ms
    }

    var onAddMoreClicked: (() -> Unit)? = null
    var onPositionFocused: ((position: Int) -> Unit)? = null
    override var onItemFocused: ((View, AnimeGridItem) -> Unit)? = null
    override var onItemFocusLost: (() -> Unit)? = null

    var isLoadingNext = false
        set(value) {
            field = value
            notifyItemChanged(items.size) // refresh the "Add More" item only
        }

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        if (position == items.size) return -1L
        return items[position].id.hashCode().toLong()
    }

    // Unified ViewHolder (Nullable views to safely handle both standard items and the Add button)
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val CardViewcontiner: CardView? = view.findViewById(R.id.CardViewcontiner)
        val Movie_image: ImageView? = view.findViewById(R.id.itemImage)
        val title: TextView? = view.findViewById(R.id.cardTitle)
        val cardType: TextView? = view.findViewById(R.id.cardType)
        val cardDub: TextView? = view.findViewById(R.id.cardDub)
        val cardSub: TextView? = view.findViewById(R.id.cardSub)
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

            if (isLoadingNext) {
                content?.visibility = View.GONE
                loading?.visibility = View.VISIBLE
                holder.itemView.isClickable = false
                holder.itemView.isFocusable = false
            } else {
                content?.visibility = View.VISIBLE
                loading?.visibility = View.GONE
                holder.itemView.isClickable = true
                holder.itemView.isFocusable = true
            }

            // Fallback click listener
            holder.itemView.setOnClickListener {
                if (!isLoadingNext) {
                    onAddMoreClicked?.invoke()
                }
            }
            return
        }

        // ==========================================
        // 2. HANDLE STANDARD ANIME ITEM
        // ==========================================
        val currentItem = items[position]
        val title = currentItem.title
        val imageUrl = currentItem.poster
        val imdbCode = currentItem.id

        holder.title?.text = title
        holder.cardSub?.text = currentItem.sub
        holder.cardDub?.text = if (currentItem.dub == "null" || currentItem.dub.isEmpty()) "0" else currentItem.dub
        holder.cardType?.text = currentItem.type

        holder.Movie_image?.let {
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                .centerInside()
                .into(it)
        }

        // Apply listeners to the root view to match GridAdapter exactly
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val args = android.os.Bundle().apply {
    putString("anime_code", imdbCode)
    putString("anime_poster", imageUrl)
}
(context as com.example.onyx.HomeActivity).navigateToFragment(com.example.onyx.WatchAnimeFragment(), args)
        }

        // ==========================================
        // 3. TV FOCUS PREFETCH LOGIC
        // ==========================================
        holder.itemView.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                onItemFocused?.invoke(v, currentItem) // show popup

                val currentPos = holder.bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    onPositionFocused?.invoke(currentPos)
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

    fun addItem(item: AnimeGridItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun appendItems(newItems: List<AnimeGridItem>) {
        val startPos = items.size
        items.addAll(newItems)
        notifyItemRangeInserted(startPos, newItems.size)
    }

    fun prependItems(newItems: List<AnimeGridItem>) {
        items.addAll(0, newItems)
        notifyItemRangeInserted(0, newItems.size)
    }

    fun clearItems() {
        val size = items.size
        items.clear()
        notifyItemRangeRemoved(0, size) // Smooth removal
    }

    override fun getItem(position: Int): AnimeGridItem? = items.getOrNull(position)
}


data class AnimeGridItem(
    val id: String,
    val anilistId: String,
    val malId: String,
    val title: String,
    val japaneseTitle: String,
    val poster: String,
    val backdropUrl: String?,
    val description: String,
    val releaseDate: String,
    val type: String,
    val quality: String,
    val status: String,
    val genres: List<String>,
    val duration: String,
    val sub: String,
    val dub: String,
    val rating: String
)

////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////

class animeFavAdapter(
    private val  items: MutableList<animeFavItem>,
    private val layoutResId: Int

) :  RecyclerView.Adapter<animeFavAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val Movie_image: ImageView = view.findViewById(R.id.itemImage)
        val itemText: TextView = view.findViewById(R.id.itemText)



        init {
            itemView.setOnFocusChangeListener { _, hasFocus ->

                //Scale animation
                itemView.animate()
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

        val posterUrl = currentItem.posterUrl
        val imdbCode = currentItem.imdbCode
        val type = currentItem.showType



        Glide.with(holder.itemView.context)
            .load(posterUrl)
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
            .centerInside()
            .into(holder.Movie_image)


        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            if(type == "anime"){
                val args = android.os.Bundle().apply {
    putString("anime_code", imdbCode)
    putString("anime_poster", posterUrl)
}
(context as com.example.onyx.HomeActivity).navigateToFragment(com.example.onyx.WatchAnimeFragment(), args)
            }else {
                val intent = Intent(context, Watch_Page::class.java)
                intent.putExtra("imdb_code", imdbCode)
                intent.putExtra("type", type)
                context.startActivity(intent)
            }
        }


    }

    override fun getItemCount() = items.size

    // ðŸ‘‡ helper to add items one by one
    fun addItem(item: animeFavItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)

    }
    fun clearItems() {
        items.clear()
        notifyDataSetChanged()  // Notify RecyclerView that data is cleared
    }

}


data class animeFavItem(
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







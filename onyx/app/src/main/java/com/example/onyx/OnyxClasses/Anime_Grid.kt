package com.example.onyx.OnyxClasses

import android.content.Intent
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
import com.example.onyx.R
import com.example.onyx.WatchAnimeFragment
import com.example.onyx.Watch_Page
import com.bumptech.glide.request.target.Target
import com.google.android.material.card.MaterialCardView


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////

class AnimeTrendingAdapter(
    private val  items: MutableList<TrendingAnimeItem>,   // âœ… mutable now,
    private val layoutResId: Int   // ðŸ‘ˆ pass in the layout resource
) :  RecyclerView.Adapter<AnimeTrendingAdapter.ViewHolder>() {

    companion object {
        private var lastKeyTime = 0L
        private val KEY_DEBOUNCE_DELAY = 100L // ms
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val CardViewcontiner: MaterialCardView = view.findViewById(R.id.CardViewcontiner)
        val Movie_image: ImageView = view.findViewById(R.id.itemImage)
        val rank: TextView = view.findViewById(R.id.rank)
        val title: TextView = view.findViewById(R.id.title)




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
        val rank = currentItem.rank


        holder.title.text = title
        holder.rank.text = rank
        holder.title.text = title

        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
            .centerInside()
            .into(holder.Movie_image)

        holder.CardViewcontiner.setOnClickListener {
            val context = holder.itemView.context
            val args = android.os.Bundle().apply {
                putString("anime_code", imdbCode)
                putString("anime_poster", imageUrl)
            }
            (context as com.example.onyx.HomeActivity).navigateToFragment(WatchAnimeFragment(), args)
        }


        holder.CardViewcontiner.setOnKeyListener { v, keyCode, event ->
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

    // ðŸ‘‡ helper to add items one by one
    fun addItem(item: TrendingAnimeItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)

    }

}

data class TrendingAnimeItem(
    val id: String,
    val title: String,
    val imageUrl: String,
    val rank: String
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
    override var onItemFocused: ((View, AnimeGridItem) -> Unit)? = null
    override var onItemFocusLost: (() -> Unit)? = null

    var isLoadingMore = false
        set(value) {
            field = value
            notifyItemChanged(items.size) // refresh the "Add More" item only
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

            if (isLoadingMore) {
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

                // âœ… Trigger load when focused on the last 6 items
                val prefetchThreshold = 6
                val currentPos = holder.bindingAdapterPosition

                if (!isLoadingMore && currentPos != RecyclerView.NO_POSITION && currentPos >= items.size - prefetchThreshold) {
                    // Defer BOTH the flag flip (which triggers notifyItemChanged)
                    // and the callback until after the current layout/scroll pass
                    // finishes. Flipping isLoadingMore synchronously here was the
                    // crash: focus events can land mid-layout on TV D-pad nav,
                    // and notifyItemChanged() throws if called at that point.
                    v.post {
                        if (!isLoadingMore) {
                            isLoadingMore = true
                            onAddMoreClicked?.invoke()
                        }
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

    fun addItem(item: AnimeGridItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun addItems(newItems: List<AnimeGridItem>) {
        val startPos = items.size
        items.addAll(newItems)
        notifyItemRangeInserted(startPos, newItems.size)
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







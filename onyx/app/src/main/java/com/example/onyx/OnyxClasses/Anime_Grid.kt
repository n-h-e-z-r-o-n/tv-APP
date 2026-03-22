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
import com.example.onyx.Watch_Anime_Page
import com.example.onyx.Watch_Page
import com.bumptech.glide.request.target.Target



////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////

class AnimeTrendingAdapter(
    private val  items: MutableList<TrendingAnimeItem>,   // ✅ mutable now,
    private val layoutResId: Int   // 👈 pass in the layout resource
) :  RecyclerView.Adapter<AnimeTrendingAdapter.ViewHolder>() {

    companion object {
        private var lastKeyTime = 0L
        private val KEY_DEBOUNCE_DELAY = 100L // ms
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val CardViewcontiner: CardView = view.findViewById(R.id.CardViewcontiner)
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
            .centerInside()
            .into(holder.Movie_image)

        holder.CardViewcontiner.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, Watch_Anime_Page::class.java)
            intent.putExtra("anime_code", imdbCode)
            intent.putExtra("anime_poster", imageUrl)
            context.startActivity(intent)
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

    // 👇 helper to add items one by one
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
    private val  items: MutableList<AiringAnimeItem>,   // ✅ mutable now,
    private val layoutResId: Int   // 👈 pass in the layout resource
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
            .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)

            .centerInside()
            .into(holder.Movie_image)

        holder.CardViewcontiner.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, Watch_Anime_Page::class.java)
            intent.putExtra("anime_code", imdbCode)
            intent.putExtra("anime_poster", imageUrl)
            context.startActivity(intent)
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
    private val  items: MutableList<AnimeSearchItem>,   // ✅ mutable now,
    private val layoutResId: Int   // 👈 pass in the layout resource
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
            .centerInside()
            .into(holder.Movie_image)

        holder.CardViewcontiner.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, Watch_Anime_Page::class.java)
            intent.putExtra("anime_code", imdbCode)
            intent.putExtra("anime_poster", imageUrl)
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
    private val items: MutableList<AiringAnimeItem>,
    private val layoutResId: Int
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_MOVIE = 0
        private const val VIEW_TYPE_ADD_BUTTON = 1
        private var lastKeyTime = 0L
        private val KEY_DEBOUNCE_DELAY = 150L // ms
    }

    var onAddMoreClicked: (() -> Unit)? = null

    // ViewHolder for anime items
    inner class AnimeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val CardViewcontiner: CardView = view.findViewById(R.id.CardViewcontiner)
        val Movie_image: ImageView = view.findViewById(R.id.itemImage)
        val title: TextView = view.findViewById(R.id.cardTitle)
        val cardType: TextView = view.findViewById(R.id.cardType)
        val cardDub: TextView = view.findViewById(R.id.cardDub)
        val cardSub: TextView = view.findViewById(R.id.cardSub)
    }

    // ViewHolder for add button
    inner class AddButtonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        init {
            view.setOnClickListener {
                // Move focus to second last item before calling the function
                val secondLastPosition = items.size - 1
                if (secondLastPosition >= 0) {
                    // Find the recyclerView and set focus to second last item
                    val recyclerView = itemView.parent as? RecyclerView
                    recyclerView?.layoutManager?.findViewByPosition(secondLastPosition)?.requestFocus()
                }

                // Then call the add more function
                onAddMoreClicked?.invoke()
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == items.size) VIEW_TYPE_ADD_BUTTON else VIEW_TYPE_MOVIE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ADD_BUTTON -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_add_more, parent, false)
                AddButtonViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(layoutResId, parent, false)
                AnimeViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is AnimeViewHolder -> {
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
                    .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .centerInside()
                    .into(holder.Movie_image)

                holder.CardViewcontiner.setOnClickListener {
                    val context = holder.itemView.context
                    val intent = Intent(context, Watch_Anime_Page::class.java)
                    intent.putExtra("anime_code", imdbCode)
                    intent.putExtra("anime_poster", imageUrl)
                    context.startActivity(intent)
                }

                holder.CardViewcontiner.setOnKeyListener { v, keyCode, event ->
                    if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
                    val now = System.currentTimeMillis()
                    if (now - lastKeyTime < KEY_DEBOUNCE_DELAY) return@setOnKeyListener true
                    lastKeyTime = now

                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (position == 0) return@setOnKeyListener true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (position == items.size) {
                                return@setOnKeyListener true
                            }

                        }
                    }
                    false
                }
            }
            is AddButtonViewHolder -> {
                // Nothing to bind for the add button
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size + 1
    }

    fun addItem(item: AiringAnimeItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun addItems(newItems: List<AiringAnimeItem>) {
        val startPos = items.size
        items.addAll(newItems)
        notifyItemRangeInserted(startPos, newItems.size)
    }

    fun clearItems() {
        items.clear()
        notifyDataSetChanged()
    }
}

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
            .centerInside()
            .into(holder.Movie_image)


        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            if(type == "anime"){
                val intent = Intent(context, Watch_Anime_Page::class.java)
                intent.putExtra("anime_code", imdbCode)
                intent.putExtra("anime_poster", posterUrl)
                context.startActivity(intent)
            }else {
                val intent = Intent(context, Watch_Page::class.java)
                intent.putExtra("imdb_code", imdbCode)
                intent.putExtra("type", type)
                context.startActivity(intent)
            }
        }


    }

    override fun getItemCount() = items.size

    // 👇 helper to add items one by one
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






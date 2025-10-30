package com.example.onyx

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


class AnimeSwiper(
    private val  items: MutableList<AnimeSliderItem>,
    private val layoutResId: Int
) :  RecyclerView.Adapter<AnimeSwiper.ViewHolder>() {


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val CardViewcontiner: CardView = view.findViewById(R.id.spotlightAnimesCard)


        val SliderBackdrop: ImageView = view.findViewById(R.id.SliderBackdrop)

        val cardTitle: TextView = view.findViewById(R.id.cardTitle)

        val cardOverview: TextView = view.findViewById(R.id.cardOverview)

        val SliderButton: LinearLayout = view.findViewById(R.id.cardButton)

        val cardType: TextView = view.findViewById(R.id.cardType)

        val cardRuntime: TextView = view.findViewById(R.id.cardRuntime)

        val cardYear: TextView = view.findViewById(R.id.cardYear)

        val cardQuality: TextView = view.findViewById(R.id.cardQuality)


        val cardDub: TextView = view.findViewById(R.id.cardDub)

        val cardSub: TextView = view.findViewById(R.id.cardSub)




    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(layoutResId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val realPosition = position % items.size      // map to your real data


        val currentItem = items[position]

        val title = currentItem.title
        val imageUrl = currentItem.imageUrl
        val imdbCode = currentItem.id
        val type = currentItem.type
        val overview = currentItem.overview
        val release_date = currentItem.release_date
        val runtime = currentItem.runtime
        val quality = currentItem.quality
        val dub  = currentItem.dub
        val sub  = currentItem.sub


        holder.cardTitle.text = title

        holder.cardType.text = type

        holder.cardRuntime.text = runtime+"m"

        holder.cardYear.text = release_date

        holder.cardQuality.text = quality

        holder.cardSub.text = sub

        holder.cardDub.text = if (currentItem.dub == "null" || currentItem.dub.isEmpty()) "0" else currentItem.dub

        holder.cardOverview.text = overview




        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .centerInside()
            .into(holder.SliderBackdrop)


        holder.SliderButton.setOnClickListener {
            val context = holder.itemView.context
            val intent = android.content.Intent(context, Watch_Anime_Page::class.java)
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

    // 👇 helper to add items one by one
    fun addItem(item: AnimeSliderItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)

    }
}

data class AnimeSliderItem(
    val title: String,
    val imageUrl: String,
    val id: String,
    val type: String,
    val overview: String,
    val release_date: String,
    val runtime: String,
    val quality: String,
    val sub: String,
    val dub: String,
)




class AnimeTrendingAdapter(
    private val  items: MutableList<TrendingAnimeItem>,   // ✅ mutable now,
    private val layoutResId: Int   // 👈 pass in the layout resource
) :  RecyclerView.Adapter<AnimeTrendingAdapter.ViewHolder>() {

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

class AnimeAiringAdapter(
    private val  items: MutableList<AiringAnimeItem>,   // ✅ mutable now,
    private val layoutResId: Int   // 👈 pass in the layout resource
) :  RecyclerView.Adapter<AnimeAiringAdapter.ViewHolder>() {

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

    // 👇 helper to add items one by one
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

}

data class AnimeSearchItem(
    val id: String,
    val title: String,
    val imageUrl: String,
    val type: String,
    val sub: String,
    val dub: String
)




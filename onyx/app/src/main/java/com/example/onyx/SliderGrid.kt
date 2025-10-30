package com.example.onyx

import android.content.Intent
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide


class CardSwiper(
    private val  items: MutableList<SliderItem>,   // ✅ mutable now,
    private val layoutResId: Int   // 👈 pass in the layout resource
) :  RecyclerView.Adapter<CardSwiper.ViewHolder>() {

    private val genreMap = mapOf(
        28 to "Action", 12 to "Adventure", 16 to "Animation", 35 to "Comedy",
        80 to "Crime", 99 to "Documentary", 18 to "Drama", 10751 to "Family",
        14 to "Fantasy", 36 to "History", 27 to "Horror", 10402 to "Music",
        9648 to "Mystery", 10749 to "Romance", 878 to "Science Fiction",
        10770 to "TV Movie", 53 to "Thriller", 10752 to "War", 37 to "Western"
    )
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {


        val SliderBackdrop: ImageView = view.findViewById(R.id.SliderBackdrop)
        val SliderTitle: TextView = view.findViewById(R.id.SliderTitle)
        val SliderOverview: TextView = view.findViewById(R.id.SliderOverview)
        val SliderButton: LinearLayout = view.findViewById(R.id.SliderButton)

        val SliderType: TextView = view.findViewById(R.id.SliderType)
        val SliderRating: TextView = view.findViewById(R.id.SliderRating)
        val SliderYear: TextView = view.findViewById(R.id.SliderYear)
        val SliderGenre: TextView = view.findViewById(R.id.SliderGenre)

        val SliderPg: TextView = view.findViewById(R.id.SliderPg)


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
        val imdbCode = currentItem.imdbCode
        val type = currentItem.type
        val overview = currentItem.overview
        val release_date = currentItem.release_date
        val vote_average = currentItem.vote_average
        val poster_path = currentItem.poster_path
        val genre_ids = currentItem.genre_ids
        val pg_info = currentItem.pg





        holder.SliderTitle.text = title
        holder.SliderOverview.text = overview
        holder.SliderPg.text = pg_info

        if(type=="tv"){
            holder.SliderType.text = "Tv"
        } else{
            holder.SliderType.text = "Movie"
        }

        holder.SliderRating.text = vote_average
        holder.SliderYear.text = release_date


        val genreNames = genre_ids.mapNotNull { id -> genreMap[id] }
        val genreText = genreNames.joinToString(" • ")
        holder.SliderGenre.text = genreText


        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .centerInside()
            .into(holder.SliderBackdrop)


            holder.SliderButton.setOnClickListener {
                val context = holder.itemView.context
                val intent = android.content.Intent(context, Watch_Page::class.java)
                intent.putExtra("imdb_code", imdbCode)
                intent.putExtra("type", type)
                context.startActivity(intent)
            }
    }

    override fun getItemCount() = items.size

    // 👇 helper to add items one by one
    fun addItem(item: SliderItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)

    }
}

data class SliderItem(
    val title: String,
    val imageUrl: String,
    val imdbCode: String,
    val type: String,
    val overview: String,
    val release_date: String,
    val vote_average: String,
    val poster_path: String,
    val genre_ids: List<Int>,
    val pg: String
)

////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////



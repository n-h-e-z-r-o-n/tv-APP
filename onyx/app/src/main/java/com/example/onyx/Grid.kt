package com.example.onyx

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.squareup.picasso.Picasso
import org.json.JSONObject


//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

class GridAdapter(
      private val  items: MutableList<MovieItem>,   // ✅ mutable now,
      private val layoutResId: Int   // 👈 pass in the layout resource
    ) :  RecyclerView.Adapter<GridAdapter.ViewHolder>() {

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




        /*
        Picasso.get()
            .load(imageUrl)
            .fit()
            .centerInside()
            .into(holder.Movie_image)

         */

        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .centerInside()
            .into(holder.Movie_image)

        if(currentItem.type == "Actor"){
            holder.Movie_image.setOnClickListener {
                val context = holder.itemView.context
                val intent = android.content.Intent(context, Actor_Page::class.java)
                intent.putExtra("imdb_code", currentItem.imdbCode)
                intent.putExtra("type", currentItem.type)
                context.startActivity(intent)
            }
        }else {
            holder.itemView.setOnClickListener {
                val context = holder.itemView.context
                val intent = android.content.Intent(context, Watch_Page::class.java)
                intent.putExtra("imdb_code", currentItem.imdbCode)
                intent.putExtra("type", currentItem.type)
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount() = items.size

    // 👇 helper to add items one by one
    fun addItem(item: MovieItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)

    }
}



class OtherAdapter(
    private val  items: MutableList<MovieItem>,   // ✅ mutable now,
    private val layoutResId: Int   // 👈 pass in the layout resource
) :  RecyclerView.Adapter<OtherAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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
            .centerInside()
            .into(holder.Movie_image)


        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = android.content.Intent(context, Watch_Page::class.java)
            intent.putExtra("imdb_code", imdbCode)
            intent.putExtra("type", type)
            context.startActivity(intent)
        }

    }

    override fun getItemCount() = items.size

    // 👇 helper to add items one by one
    fun addItem(item: MovieItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)

    }
}




class CastAdapter(
    private val  items: MutableList<MovieItem>,   // ✅ mutable now,
    private val layoutResId: Int   // 👈 pass in the layout resource
) :  RecyclerView.Adapter<CastAdapter.ViewHolder>() {

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




        /*
        Picasso.get()
            .load(imageUrl)
            .fit()
            .centerInside()
            .into(holder.Movie_image)

         */

        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .centerInside()
            .into(holder.Movie_image)

        if(currentItem.type == "Actor"){
            holder.Movie_image.setOnClickListener {
                val context = holder.itemView.context
                val intent = android.content.Intent(context, Actor_Page::class.java)
                intent.putExtra("imdb_code", currentItem.imdbCode)
                intent.putExtra("type", currentItem.type)
                context.startActivity(intent)
            }
        }else {
            holder.itemView.setOnClickListener {
                val context = holder.itemView.context
                val intent = android.content.Intent(context, Watch_Page::class.java)
                intent.putExtra("imdb_code", currentItem.imdbCode)
                intent.putExtra("type", currentItem.type)
                context.startActivity(intent)
            }
        }


    }

    override fun getItemCount() = items.size

    // 👇 helper to add items one by one
    fun addItem(item: MovieItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)

    }
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

class EqualSpaceItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: android.graphics.Rect,
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


//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////




class EpisodesAdapter(
    private val episodes: MutableList<JSONObject>,
    private val seriesId: String,
    private val type: String
) : RecyclerView.Adapter<EpisodesAdapter.EpisodeViewHolder>() {

    inner class EpisodeViewHolder(view: View) : RecyclerView.ViewHolder(view) {


        val titleView: TextView = view.findViewById(R.id.episode_title)
        val durationView: TextView = view.findViewById(R.id.episode_duration)
        val episode_Rating: TextView = view.findViewById(R.id.episode_Rating)
        val descView: TextView = view.findViewById(R.id.episode_description)
        val epsImg: ImageView = view.findViewById(R.id.Ep_IMG)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_episode, parent, false)
        return EpisodeViewHolder(view)
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        val episode = episodes[position]

        holder.titleView.text = "Eps ${episode.optInt("episode_number")}: ${episode.optString("name")} "
        holder.durationView.text = "⏱ ${episode.optInt("runtime", 0)} min"
        holder.episode_Rating.text = "⬤ Imdb ${episode.optInt("vote_average", 0)}"

        holder.descView.text = episode.optString("overview")

        val stillPath = episode.optString("still_path", null)
        if (!stillPath.isNullOrEmpty()) {
            val url = "https://image.tmdb.org/t/p/w500$stillPath"


            Glide.with(holder.itemView.context)
                .load(url)
                .centerInside()
                .into(holder.epsImg)


        }

        val seasonNo = episode.optInt("season_number")
        val episodeNo = episode.optInt("episode_number")

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = android.content.Intent(context, Play::class.java)


            intent.putExtra("imdb_code", seriesId) // assuming you have it in scope
            intent.putExtra("type", type) // movie/tv show type
            intent.putExtra("seasonNo", seasonNo.toString())
            intent.putExtra("episodeNo", episodeNo.toString())
            context.startActivity(intent)

        }

        fun addEpisode(episode: JSONObject) {
            episodes.add(episode)
            notifyItemInserted(episodes.size - 1)
        }
    }

    override fun getItemCount(): Int = episodes.size
}



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

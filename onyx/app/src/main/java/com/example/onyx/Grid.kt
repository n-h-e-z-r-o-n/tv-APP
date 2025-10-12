package com.example.onyx

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide


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


        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, Watch_Page::class.java)
            intent.putExtra("imdb_code", currentItem.imdbCode)
            intent.putExtra("type", currentItem.type)
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



class OtherAdapter(
    private val  items: MutableList<MovieItem>,   // ✅ mutable now,
    private val layoutResId: Int   // 👈 pass in the layout resource
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



class RecommendAdapter(
    private val  items: MutableList<MovieItem>,   // ✅ mutable now,
    private val layoutResId: Int   // 👈 pass in the layout resource
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
    private val  items: MutableList<CastItem>,   // ✅ mutable now,
    private val layoutResId: Int   // 👈 pass in the layout resource
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





        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .centerInside()
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

    // 👇 helper to add items one by one
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

class FavAdapter(
    private val  items: MutableList<FavItem>,
    private val layoutResId: Int ,
    private val backdropView: ImageView,
    private val favTitleView: TextView,
    private val favGenreView: TextView,
    private val favTypeView: TextView,
    private val favRatingView: TextView,
    private val favYearView: TextView,
    private val favOverviewView: TextView,
    private val RemoveFaveItemBtn: LinearLayout


) :  RecyclerView.Adapter<FavAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val Movie_image: ImageView = view.findViewById(R.id.itemImage)
        val itemText: TextView = view.findViewById(R.id.itemText)



        init {
            itemView.setOnFocusChangeListener { _, hasFocus ->

                /*Scale animation
                itemView.animate()
                    .scaleX(if (hasFocus) 1.02f else 1f)
                    .scaleY(if (hasFocus) 1.02f else 1f)
                    .setDuration(150)
                    .start()

                 */

                if (hasFocus) {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        val item = items[pos]

                        // Backdrop image
                        Glide.with(backdropView.context)
                            .load(item.backdropUrl)
                            .centerCrop()
                            .into(backdropView)

                        // ✅ Set text properties correctly
                        favTitleView.text    = item.title
                        favGenreView.text    = item.genres
                        favTypeView.text     = item.showType          // ensure you have a `type` field
                        favRatingView.text   = "IMDB ${item.voteAverage}"
                        favYearView.text     = item.releaseDate
                        favOverviewView.text = item.overview


                        RemoveFaveItemBtn.setOnClickListener {
                            FavoritesManager.removeFavorite( RemoveFaveItemBtn.context, item.imdbCode, item.showType)
                            val pos = bindingAdapterPosition
                            if (pos != RecyclerView.NO_POSITION) {
                                items.removeAt(pos)
                                notifyItemRemoved(pos)
                            }
                        }




                    }
                }
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
            val intent = android.content.Intent(context, Watch_Page::class.java)
            intent.putExtra("imdb_code", imdbCode)
            intent.putExtra("type", type)
            context.startActivity(intent)
        }


    }

    override fun getItemCount() = items.size

    // 👇 helper to add items one by one
    fun addItem(item: FavItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)

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
    private val episodes: MutableList<EpisodeItem>
) : RecyclerView.Adapter<EpisodesAdapter.EpisodeViewHolder>() {

    inner class EpisodeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val epNoView: TextView = view.findViewById(R.id.episode_Number)
        val titleView: TextView = view.findViewById(R.id.episode_title)
        val durationView: TextView = view.findViewById(R.id.episode_duration)
        val ratingView: TextView = view.findViewById(R.id.episode_Rating)
        val descView: TextView = view.findViewById(R.id.episode_description)
        val epsImg: ImageView = view.findViewById(R.id.Ep_IMG)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_episode, parent, false)
        return EpisodeViewHolder(view)

    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        val ep = episodes[position]


        holder.epNoView.text = "Eps ${ep.episodesNumber}"
        holder.titleView.text = ep.episodesName
        holder.durationView.text = "⏱ ${ep.episodesRuntime} min"
        holder.ratingView.text = "★ ${ep.episodesRating}"
        holder.descView.text = ep.episodesDescription


        val url = "https://image.tmdb.org/t/p/w500${ep.episodesImage}"
        Glide.with(holder.itemView.context)
            .load(url)
            .centerInside()
            .into(holder.epsImg)


        holder.itemView.setOnClickListener {view ->
            view.isEnabled = false
            val context = holder.itemView.context
            val intent = Intent(context, Play::class.java).apply {
                putExtra("imdb_code", ep.seriesId)
                putExtra("type", "tv")
                putExtra("seasonNo", ep.seasonNumber.toString())
                putExtra("episodeNo", ep.episodesNumber.toString())
            }
            context.startActivity(intent)
            view.postDelayed({
                view.isEnabled = true
            }, 5000)
        }
        Log.e("DEBUG_Each E grid", "Eps ${ep.episodesNumber}")
        Log.e("DEBUG_Each E size", "${episodes.size}")


        // ✅ Attach the KeyListener here
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

    fun addEpisode(item: EpisodeItem) {
        episodes.add(item)
        notifyItemInserted(episodes.size - 1)
    }
}

data class EpisodeItem(
    val episodesName: String = "",
    val episodesImage: String= "",
    val episodesNumber: String= "",
    val episodesRating: String = "",
    val episodesRuntime: String = "",
    val episodesDescription: String = "",
    val seriesId: String = "",
    val seasonNumber: String = "",

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
    private val  items: MutableList<NotificationItem>,   // ✅ mutable now,
    private val layoutResId: Int   // 👈 pass in the layout resource
) :  RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        //val image: ImageView = view.findViewById(R.id.notification_title)
        val showTitle: TextView = view.findViewById(R.id.notification_title)
        val message: TextView = view.findViewById(R.id.notification_message)


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


        holder.showTitle.text =  title
        holder.message.text =  info

        /*
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .centerInside()
            .into(holder.Movie_image)

         */


        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = android.content.Intent(context, Watch_Page::class.java)
            intent.putExtra("imdb_code", imdbCode)
            intent.putExtra("type", type)
            context.startActivity(intent)

            NotificationHelper.updateNotification(context, imdbCode, updateSeason, updateEpisode)
            //call updateNotification
        }

    }

    override fun getItemCount() = items.size

    // 👇 helper to add items one by one
    fun addItem(item: NotificationItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }
    
    // 👇 helper to refresh all items
    fun refreshItems(newItems: List<NotificationItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
    
    // 👇 helper to remove specific item by imdbCode
    fun removeItem(imdbCode: String) {
        val index = items.indexOfFirst { it.imdbCode == imdbCode }
        if (index != -1) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}


data class NotificationItem(
    val imdbCode: String,
    val title: String,
    val imageUrl: String?,
    val info: String,
    val type: String = "tv",
    val newSeason: Int,
    val newEpisode: Int
)

package com.example.onyx

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide


class CardSwiper(
    private val  items: MutableList<SliderItem>,   // ✅ mutable now,
    private val layoutResId: Int   // 👈 pass in the layout resource
) :  RecyclerView.Adapter<CardSwiper.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {


        val SliderBackdrop: ImageView = view.findViewById(R.id.SliderBackdrop)
        val SliderTitle: TextView = view.findViewById(R.id.SliderTitle)
        val SliderOverview: TextView = view.findViewById(R.id.SliderOverview)
        val SliderButton: Button = view.findViewById(R.id.SliderButton)

        /*
        init {
            itemView.setOnFocusChangeListener { v, hasFocus ->
                v.animate().scaleX(if (hasFocus) 1.02f else 1f)
                    .scaleY(if (hasFocus) 1.02f else 1f)
                    .setDuration(150)
                    .start()
            }
        }

         */
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
        val overview = currentItem.overview
        val release_date = currentItem.release_date
        val vote_average = currentItem.vote_average
        val poster_path = currentItem.poster_path
        val genre_ids = currentItem.genre_ids





        holder.SliderTitle.text = title
        holder.SliderOverview.text = overview


        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .centerInside()
            .into(holder.SliderBackdrop)

        if(currentItem.type == "Actor"){
            holder.SliderButton.setOnClickListener {
                val context = holder.itemView.context
                val intent = android.content.Intent(context, Actor_Page::class.java)
                intent.putExtra("imdb_code", currentItem.imdbCode)
                intent.putExtra("type", currentItem.type)
                context.startActivity(intent)
            }
        }else {
            holder.SliderButton.setOnClickListener {
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
    val genre_ids: String
)


/*
class CardSwiper(
    private val items: MutableList<MovieItem>,
    private val layoutResId: Int
) : RecyclerView.Adapter<CardSwiper.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val Movie_image: ImageView = view.findViewById(R.id.itemImage)
        val Movie_title: TextView = view.findViewById(R.id.itemText)

        init {
            itemView.setOnFocusChangeListener { v, hasFocus ->
                v.animate().scaleX(if (hasFocus) 1.02f else 1f)
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
        holder.Movie_title.text = currentItem.title

        Glide.with(holder.itemView.context)
            .load(currentItem.imageUrl)
            .centerInside()
            .into(holder.Movie_image)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, Watch_Page::class.java)
            intent.putExtra("imdb_code", currentItem.imdbCode)
            intent.putExtra("type", currentItem.type)
            context.startActivity(intent)
        }

        // 👇 Add focus elevation effect here
        holder.itemView.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.elevation = 20f
                v.translationZ = 20f
                v.bringToFront()
                v.scaleX = 1.1f   // optional: scale for effect
                v.scaleY = 1.1f
            } else {
                v.elevation = 0f
                v.translationZ = 0f
                v.scaleX = 1.0f
                v.scaleY = 1.0f
            }
        }

        // Set focusable for proper D-pad navigation
        holder.itemView.isFocusable = true
        holder.itemView.isFocusableInTouchMode = true
    }

    override fun getItemCount() = items.size

    // 👇 Move top card to back (left swipe)
    fun moveTopToBack(recyclerView: RecyclerView) {
        if (items.isEmpty()) return

        val topViewHolder = recyclerView.findViewHolderForAdapterPosition(0) ?: return
        val topView = topViewHolder.itemView

        topView.animate()
            .translationX(-recyclerView.width.toFloat())
            .alpha(0f)
            .setDuration(400)
            .withEndAction {
                val top = items.removeAt(0)
                items.add(top)
                topView.translationX = 0f
                topView.alpha = 1f
                notifyDataSetChanged()

                // Restore focus to the new top card
                recyclerView.post {
                    recyclerView.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                }
            }
            .start()
    }

    // 👇 Move top card to back (right swipe)
    fun moveTopToBackRight(recyclerView: RecyclerView) {
        if (items.isEmpty()) return

        val topViewHolder = recyclerView.findViewHolderForAdapterPosition(0) ?: return
        val topView = topViewHolder.itemView

        topView.animate()
            .translationX(recyclerView.width.toFloat())
            .alpha(0f)
            .setDuration(400)
            .withEndAction {
                val top = items.removeAt(0)
                items.add(top)
                topView.translationX = 0f
                topView.alpha = 1f
                notifyDataSetChanged()

                // Restore focus to the new top card
                recyclerView.post {
                    recyclerView.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                }
            }
            .start()
    }
}

 */
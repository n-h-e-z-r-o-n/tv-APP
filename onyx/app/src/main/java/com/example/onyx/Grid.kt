package com.example.onyx

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso


class GridAdapter(
      private val items: List<MovieItem>,
      private val layoutResId: Int   // 👈 pass in the layout resource
    ) :  RecyclerView.Adapter<GridAdapter.ViewHolder>() {

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

        val title = currentItem.title
        val imageUrl = currentItem.imageUrl
        val imdbCode = currentItem.imdbCode
        val type = currentItem.type



        holder.Movie_title.text = title

        Picasso.get()
            .load(imageUrl)
            .fit()
            .centerInside()
            .into(holder.Movie_image)


        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = android.content.Intent(context, Watch_Page::class.java)
            intent.putExtra("imdb_code", currentItem.imdbCode)
            intent.putExtra("type", currentItem.type)
            context.startActivity(intent)
        }


    }

    override fun getItemCount() = items.size
}

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

package com.example.tv_s
import com.squareup.picasso.Picasso
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2) // 2 columns

        val data = listOf(
            "Movie EFSDFDFDFD DFDFDFDFD 1" to "https://raw.githubusercontent.com/programmercloud/movies-website/main/img/movie-1.jpg",
            "Movie 2" to "https://raw.githubusercontent.com/programmercloud/movies-website/main/img/movie-2.jpg",
            "Movie 3" to "https://raw.githubusercontent.com/programmercloud/movies-website/main/img/movie-3.jpg",
            "Movie 3" to "https://raw.githubusercontent.com/programmercloud/movies-website/main/img/movie-3.jpg",
            "Movie 3" to "https://raw.githubusercontent.com/programmercloud/movies-website/main/img/movie-3.jpg",
            "Movie 3" to "https://raw.githubusercontent.com/programmercloud/movies-website/main/img/movie-3.jpg",
            "Movie 3" to "https://raw.githubusercontent.com/programmercloud/movies-website/main/img/movie-3.jpg",

            "Movie 3" to "https://raw.githubusercontent.com/programmercloud/movies-website/main/img/movie-3.jpg",
            "Movie 3" to "https://raw.githubusercontent.com/programmercloud/movies-website/main/img/movie-3.jpg",
            "Movie 3" to "https://raw.githubusercontent.com/programmercloud/movies-website/main/img/movie-3.jpg"
        )

        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = GridAdapter(data)
        val spacing = (19 * resources.displayMetrics.density).toInt() // 16dp to px
        recyclerView.addItemDecoration(EqualSpaceItemDecoration(spacing))
    }
}


class GridAdapter(private val items: List<Pair<String, String>>) :
    RecyclerView.Adapter<GridAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.itemImage)
        val text: TextView = view.findViewById(R.id.itemText)

        init {
            itemView.setOnFocusChangeListener { v, hasFocus ->
                v.animate().scaleX(if (hasFocus) 1.1f else 1f)
                    .scaleY(if (hasFocus) 1.1f else 1f)
                    .setDuration(150)
                    .start()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_grid, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (title, imageUrl) = items[position]

        holder.text.text = title

        Picasso.get()
            .load(imageUrl)
            .resize(200, 200) // resize for performance
            .centerCrop()
            .into(holder.image)
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

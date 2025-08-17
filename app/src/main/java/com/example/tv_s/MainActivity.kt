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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.net.HttpURLConnection
import java.net.URL

import android.util.Log

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        //val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        //recyclerView.layoutManager = GridLayoutManager(this, 2) // 2 columns
        //recyclerView.adapter = GridAdapter(data)
        //val spacing = (19 * resources.displayMetrics.density).toInt() // 16dp to px
        //recyclerView.addItemDecoration(EqualSpaceItemDecoration(spacing))

        fetchData()
    }

    private fun fetchData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://yts.mx/api/v2/list_movies.json?page=1&limit=50&sort_by=year"
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val response = connection.inputStream.bufferedReader().use { it.readText() }

                val jsonObject = org.json.JSONObject(response)
                val dataObject = jsonObject.getJSONObject("data")
                val moviesArray = dataObject.getJSONArray("movies")


                val movies = mutableListOf<MovieItem>()

                for (i in 0 until moviesArray.length()) {
                    val item = moviesArray.getJSONObject(i)
                    val title = item.getString("title_english")
                    val imgUrl = item.getString("large_cover_image")
                    val imdb_code = item.getString("imdb_code")
                    val type = "movie"
                    movies.add(MovieItem(title, imgUrl, imdb_code, type))
                }

                withContext(Dispatchers.Main) {
                    val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
                    recyclerView.layoutManager = GridLayoutManager(this@MainActivity, 2)
                    recyclerView.adapter = GridAdapter(movies)
                    val spacing = (19 * resources.displayMetrics.density).toInt() // 16dp to px
                    recyclerView.addItemDecoration(EqualSpaceItemDecoration(spacing))
                }
            } catch (e: Exception) {
                Log.e("DEBUG_TAG", "Error fetching data", e)
            }
        }
    }

}

data class MovieItem(
    val title: String,
    val imageUrl: String,
    val imdbCode: String,
    val type: String
)

class GridAdapter(
    private val items: List<MovieItem> ) :  RecyclerView.Adapter<GridAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val Movie_image: ImageView = view.findViewById(R.id.itemImage)
        val Movie_title: TextView = view.findViewById(R.id.itemText)

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
        Log.e("DEBUG_TAG", items[position].toString())


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

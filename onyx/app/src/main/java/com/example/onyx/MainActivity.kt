package com.example.onyx

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        SSLHelper.trustAllCertificates() // <-- add this line


        // 🔽 Add this block
        val client = UnsafeOkHttpClient.getUnsafeOkHttpClient()
        val picasso = Picasso.Builder(this)
            .downloader(com.squareup.picasso.OkHttp3Downloader(client))
            .build()
        Picasso.setSingletonInstance(picasso)

        setupSidebar()

        fetchData()
    }

    private fun fetchData() {
        CoroutineScope(Dispatchers.IO).launch {
            while(true) {
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
                        recyclerView.layoutManager = GridLayoutManager(this@MainActivity, 4)
                        recyclerView.adapter = GridAdapter(movies)
                        val spacing = (19 * resources.displayMetrics.density).toInt() // 16dp to px
                        recyclerView.addItemDecoration(EqualSpaceItemDecoration(spacing))
                    }


                    break
                } catch (e: Exception) {
                    delay(10_000)
                    Log.e("DEBUG_TAG", "Error fetching data", e)
                    break
                }
            }
        }
    }

    private fun setupSidebar() {
        val btnHome = findViewById<ImageButton>(R.id.btnHome)
        val btnSearch = findViewById<ImageButton>(R.id.btnSearch)
        val btnCategories = findViewById<ImageButton>(R.id.btnCategories)
        val btnWatchlist = findViewById<ImageButton>(R.id.btnWatchlist)
        val btnProfile = findViewById<ImageButton>(R.id.btnProfile)

        val recyclerHome = findViewById<RecyclerView>(R.id.recyclerView)
        val recyclerTv = findViewById<RecyclerView>(R.id.TvShows)
        val recyclerSettings = findViewById<RecyclerView>(R.id.Setting)

        val buttons = listOf(btnHome, btnSearch, btnCategories, btnWatchlist, btnProfile)
        val activeColor = "#4545" // active
        val inactiveColor = "#0000"       // inactive

        fun activate(button: ImageButton, target: RecyclerView) {
            // Reset all icons
            //buttons.forEach { it.inactiveColor }

            // Highlight current
            //button.activeColor

            // Hide all recyclers
            recyclerHome.visibility = View.GONE
            recyclerTv.visibility = View.GONE
            recyclerSettings.visibility = View.GONE

            // Show selected
            target.visibility = View.VISIBLE
        }

        btnHome.setOnClickListener { activate(btnHome, recyclerHome) }
        btnSearch.setOnClickListener { activate(btnSearch, recyclerTv) }  // example
        btnCategories.setOnClickListener { activate(btnCategories, recyclerTv) }
        btnWatchlist.setOnClickListener { activate(btnWatchlist, recyclerSettings) }
        btnProfile.setOnClickListener { activate(btnProfile, recyclerSettings) }

        // default view
        activate(btnHome, recyclerHome)
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

package com.example.onyx

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
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



import android.graphics.Color


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
                        recyclerView.adapter = GridAdapter(movies,  R.layout.item_grid)
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

        // Convert hex color strings to integer color values
        val activeColor = Color.parseColor("#4545FF")
        val inactiveColor = Color.parseColor("#FFFFFF")

        fun activate(button: ImageButton, target: RecyclerView) {
            // Reset all icons
            buttons.forEach { it.setColorFilter(inactiveColor) }

            // Highlight current
            button.setColorFilter(activeColor)

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

package com.example.onyx

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class Movie_Page : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_movie_page)

        NavAction.setupSidebar(this)

        Movies()

    }


    private fun Movies() {
        CoroutineScope(Dispatchers.IO).launch {

            val adapter = GridAdapter(mutableListOf(), R.layout.item_grid)
            withContext(Dispatchers.Main) {
                val recyclerView = findViewById<RecyclerView>(R.id.Movies)


                // Calculate span count dynamically
                val widthInPixels = this@Movie_Page.resources.getDimension(R.dimen.grid_item_width)
                val density = this@Movie_Page.resources.displayMetrics.density
                val widthInDp = widthInPixels / density
                val displayMetrics = resources.displayMetrics
                val screenWidthPx = displayMetrics.widthPixels
                val itemMinWidthPx = ((widthInDp + 15) * displayMetrics.density).toInt() // 160dp per item
                val spanCount = maxOf(1, screenWidthPx / itemMinWidthPx)

                recyclerView.layoutManager = GridLayoutManager(this@Movie_Page, spanCount)

                recyclerView.adapter = adapter
                val spacing = (19 * resources.displayMetrics.density).toInt()
                recyclerView.addItemDecoration(EqualSpaceItemDecoration(spacing))
            }
            while(true) {
                try {
                    val url = "https://yts.mx/api/v2/list_movies.json?page=1&limit=50&sort_by=year"

                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = org.json.JSONObject(response)
                    val dataObject = jsonObject.getJSONObject("data")
                    val moviesArray = dataObject.getJSONArray("movies")
                    Log.e("DEBUG_TAG_Movies 1", moviesArray.toString())


                    val movies = mutableListOf<MovieItem>()

                    for (i in 0 until moviesArray.length()) {
                        val item = moviesArray.getJSONObject(i)
                        val title = item.getString("title_english")
                        val imgUrl = item.getString("large_cover_image")
                        val imdb_code = item.getString("imdb_code")
                        val type = "movie"
                        //movies.add(MovieItem(title, imgUrl, imdb_code, type))
                        val movieItem = MovieItem(title, imgUrl, imdb_code, type)
                        withContext(Dispatchers.Main) {
                            adapter.addItem(movieItem)  // 👈 add one at a time
                        }
                    }

                    /*
                    withContext(Dispatchers.Main) {
                        val recyclerView = findViewById<RecyclerView>(R.id.Movies)
                        recyclerView.layoutManager = GridLayoutManager(this@MainActivity, 5)
                        recyclerView.adapter = GridAdapter(movies,  R.layout.item_grid)
                        val spacing = (19 * resources.displayMetrics.density).toInt() // 16dp to px
                        recyclerView.addItemDecoration(EqualSpaceItemDecoration(spacing))
                    }
                     */

                    break
                } catch (e: Exception) {
                    delay(10_000)
                    Log.e("DEBUG_TAG_Movies 2", "Error fetching data", e)
                    break
                }
            }
        }
    }
}
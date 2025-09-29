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
        GlobalUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_movie_page)

        NavAction.setupSidebar(this)
        loadingAnimation.setup(this@Movie_Page)

        loadingAnimation.show(this@Movie_Page)
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
                        val imdbCode = item.getString("imdb_code")
                        val type = "movie"
                        val year = item.getString("year")
                        val rating = "☆"+ item.getString("rating").substring(0, 3)
                        val runtime = item.getString("runtime") + " min"


                        //movies.add(MovieItem(title, imgUrl, imdb_code, type))
                        val movieItem = MovieItem(title=title, imageUrl=imgUrl, imdbCode=imdbCode, type=type, year = year, rating=rating, runtime=runtime)
                        withContext(Dispatchers.Main) {
                            loadingAnimation.hide(this@Movie_Page)
                            adapter.addItem(movieItem)  // 👈 add one at a time
                        }
                    }

                    break
                } catch (e: Exception) {
                    delay(10_000)
                    loadingAnimation.show(this@Movie_Page)
                    Log.e("DEBUG_TAG_Movies 2", "Error fetching data", e)
                    break
                }
            }
        }

    }
}
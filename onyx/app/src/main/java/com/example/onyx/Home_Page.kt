package com.example.onyx

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class Home_Page : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_page)

        NavAction.setupSidebar(this)

       SliderData()
       TrendingData()
       //PopularData()
    }



    private fun SliderData() {
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                try {
                    val url = "https://api.themoviedb.org/3/discover/movie?"
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("accept", "application/json")
                    connection.setRequestProperty(
                        "Authorization",
                        "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJhZjliMmUyN2MxYTZiYzMyMzNhZjE4MzJmNGFjYzg1MCIsIm5iZiI6MTcxOTY3NDUxNy4xOTYsInN1YiI6IjY2ODAyNjk1ZWZhYTI1ZjBhOGE4NGE3MyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.RTms-g8dzOl3WwCeJ7WNLq3i2kXxl3T7gOTa8POcxcw"
                    )

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = org.json.JSONObject(response)
                    val moviesArray = jsonObject.getJSONArray("results")

                    val movies = mutableListOf<SliderItem>()
                    for (i in 0 until moviesArray.length()) {

                        val item = moviesArray.getJSONObject(i)
                        Log.e("DEBUG_MAIN_Slider", item.toString())
                        val title = item.getString("original_title")
                        val backdrop_path = "https://image.tmdb.org/t/p/w1280" + item.getString("backdrop_path")
                        val id = item.getString("id")
                        val type = "movie"
                        val overview = item.getString("overview")
                        val release_date = item.getString("release_date")
                        val vote_average = item.getString("vote_average")
                        val poster_path = item.getString("poster_path")
                        val genre_ids = item.getString("genre_ids")

                        movies.add(SliderItem(title, backdrop_path, id, type, overview, release_date, vote_average, poster_path, genre_ids ))
                    }

                    withContext(Dispatchers.Main) {
                        val recyclerView = findViewById<RecyclerView>(R.id.Slider_widget)
                        val adapter = CardSwiper(movies, R.layout.card_layout)


                        recyclerView.layoutManager = LinearLayoutManager(
                            this@Home_Page,
                            LinearLayoutManager.HORIZONTAL, // 👈 makes it horizontal
                            false
                        )
                        recyclerView.adapter = adapter


                        /*
                        recyclerView.layoutManager = StackLayoutManager(
                            maxVisible = 6,
                            scaleGap = 0.05f,
                            transGap = 150
                        )
                        // Auto-shift top card every 5 sec
                        Handler(Looper.getMainLooper()).postDelayed(object : Runnable {
                            override fun run() {
                                adapter.moveTopToBack(recyclerView)
                                Handler(Looper.getMainLooper()).postDelayed(this, 5000)
                            }
                        }, 5000)

                         */

                    }

                    break
                } catch (e: Exception) {
                    delay(10_000)
                    Log.e("DEBUG_MAIN_Slider Error", "Error fetching data", e)
                }
            }
        }
    }

    private fun TrendingData() {
        CoroutineScope(Dispatchers.IO).launch {
            while(true) {
                try {

                    val url ="https://api.themoviedb.org/3/trending/all/day?primary_release_year=2025";
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("accept", "application/json")
                    connection.setRequestProperty(
                        "Authorization",
                        "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJhZjliMmUyN2MxYTZiYzMyMzNhZjE4MzJmNGFjYzg1MCIsIm5iZiI6MTcxOTY3NDUxNy4xOTYsInN1YiI6IjY2ODAyNjk1ZWZhYTI1ZjBhOGE4NGE3MyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.RTms-g8dzOl3WwCeJ7WNLq3i2kXxl3T7gOTa8POcxcw"
                    )

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = org.json.JSONObject(response)

                    Log.e("DEBUG_MAIN_Trending 1", jsonObject.toString())
                    val moviesArray = jsonObject.getJSONArray("results")

                    Log.e("DEBUG_MAIN_Trending 2", moviesArray.toString())

                    val movies = mutableListOf<MovieItem>()

                    for (i in 0 until moviesArray.length()) {
                        val item = moviesArray.getJSONObject(i)
                        val title = when {
                            item.has("original_name") && !item.isNull("original_name") -> item.getString("original_name")
                            item.has("original_title") && !item.isNull("original_title") -> item.getString("original_title")
                            item.has("title") && !item.isNull("title") -> item.getString("title")
                            else -> "Untitled"
                        }
                        val imgUrl = "https://image.tmdb.org/t/p/w500" + item.getString("poster_path")
                        val cast_id = item.getString("id")
                        val vote_average  = item.getString("vote_average")

                        val media_type = item.getString("media_type")
                        movies.add(MovieItem(title, imgUrl, cast_id, media_type))
                    }

                    Log.e("DEBUG_MAIN_Trending 3", movies.toString())


                    withContext(Dispatchers.Main) {
                        val recyclerView = findViewById<RecyclerView>(R.id.Trending_widget)
                        recyclerView.layoutManager = LinearLayoutManager(
                            this@Home_Page,
                            LinearLayoutManager.HORIZONTAL, // 👈 makes it horizontal
                            false
                        )
                        recyclerView.adapter = GridAdapter(movies,  R.layout.square_card)
                        val spacing = (5 * resources.displayMetrics.density).toInt() // 16dp to px
                        recyclerView.addItemDecoration(EqualSpaceItemDecoration(spacing))
                    }



                    break
                } catch (e: Exception) {
                    delay(10_000)
                    Log.e("DEBUG_MAIN_Trending 4", "Error fetching data", e)

                }
            }
        }
    }

    /*
    private fun PopularData() {
        CoroutineScope(Dispatchers.IO).launch {
            while(true) {
                try {

                    val url ="https://api.themoviedb.org/3/movie/popular?language=en-US&page=1"

                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("accept", "application/json")
                    connection.setRequestProperty(
                        "Authorization",
                        "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJhZjliMmUyN2MxYTZiYzMyMzNhZjE4MzJmNGFjYzg1MCIsIm5iZiI6MTcxOTY3NDUxNy4xOTYsInN1YiI6IjY2ODAyNjk1ZWZhYTI1ZjBhOGE4NGE3MyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.RTms-g8dzOl3WwCeJ7WNLq3i2kXxl3T7gOTa8POcxcw"
                    )

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = org.json.JSONObject(response)

                    Log.e("DEBUG_MAIN_Popular 1", jsonObject.toString())
                    val moviesArray = jsonObject.getJSONArray("results")

                    Log.e("DEBUG_MAIN_Popular 2", moviesArray.toString())

                    val movies = mutableListOf<MovieItem>()

                    for (i in 0 until moviesArray.length()) {
                        val item = moviesArray.getJSONObject(i)
                        val title = when {
                            item.has("original_name") && !item.isNull("original_name") -> item.getString("original_name")
                            item.has("original_title") && !item.isNull("original_title") -> item.getString("original_title")
                            item.has("title") && !item.isNull("title") -> item.getString("title")
                            else -> "Untitled"
                        }
                        val imgUrl = "https://image.tmdb.org/t/p/w500" + item.getString("poster_path")
                        val cast_id = item.getString("id")
                        val vote_average  = item.getString("vote_average")
                        val media_type = "movie"
                        movies.add(MovieItem(title, imgUrl, cast_id, media_type))
                    }

                    Log.e("DEBUG_MAIN_Popular 3", movies.toString())


                    withContext(Dispatchers.Main) {
                        val recyclerView = findViewById<RecyclerView>(R.id.Popular_widget)
                        recyclerView.layoutManager = LinearLayoutManager(
                            this@Home_Page,
                            LinearLayoutManager.HORIZONTAL, // 👈 makes it horizontal
                            false
                        )
                        recyclerView.adapter = GridAdapter(movies,  R.layout.square_card)
                        val spacing = (19 * resources.displayMetrics.density).toInt() // 16dp to px
                        recyclerView.addItemDecoration(EqualSpaceItemDecoration(spacing))
                    }



                    break
                } catch (e: Exception) {
                    delay(10_000)
                    Log.e("DEBUG_MAIN_Popular 4", "Error fetching data", e)

                }
            }
        }
    }

     */
}
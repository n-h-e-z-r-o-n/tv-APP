package com.example.onyx

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

        SSLHelper.trustAllCertificates() // <-- add this line


        val client = UnsafeOkHttpClient.getUnsafeOkHttpClient()
        val picasso = Picasso.Builder(this)
            .downloader(com.squareup.picasso.OkHttp3Downloader(client))
            .build()
        Picasso.setSingletonInstance(picasso)

        NavAction.setupSidebar(this)

        //SliderData()
        //TrendingData()
        //PopularData()
    }

    private fun SliderData() {
        CoroutineScope(Dispatchers.IO).launch {
            while(true) {
                try {

                    val url ="https://api.themoviedb.org/3/discover/movie?";

                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("accept", "application/json")
                    connection.setRequestProperty(
                        "Authorization",
                        "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJhZjliMmUyN2MxYTZiYzMyMzNhZjE4MzJmNGFjYzg1MCIsIm5iZiI6MTcxOTY3NDUxNy4xOTYsInN1YiI6IjY2ODAyNjk1ZWZhYTI1ZjBhOGE4NGE3MyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.RTms-g8dzOl3WwCeJ7WNLq3i2kXxl3T7gOTa8POcxcw"
                    )

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = org.json.JSONObject(response)

                    Log.e("DEBUG_MAIN_Slider 1", jsonObject.toString())
                    val moviesArray = jsonObject.getJSONArray("results")

                    Log.e("DEBUG_MAIN_Slider 2", moviesArray.toString())

                    val movies = mutableListOf<MovieItem>()

                    for (i in 0 until moviesArray.length()) {
                        val item = moviesArray.getJSONObject(i)
                        val title = item.getString("original_title")
                        val imgUrl = "https://image.tmdb.org/t/p/w500" + item.getString("backdrop_path")
                        val cast_id = item.getString("id")
                        val release_date  = item.getString("release_date")
                        val type = "movie"
                        movies.add(MovieItem(title, imgUrl, cast_id, type))
                    }

                    Log.e("DEBUG_MAIN_Slider 3", movies.toString())


                    withContext(Dispatchers.Main) {
                        val recyclerView = findViewById<RecyclerView>(R.id.Slider_widget)
                        recyclerView.layoutManager = LinearLayoutManager(
                            this@Home_Page,
                            LinearLayoutManager.HORIZONTAL, // 👈 makes it horizontal
                            false
                        )
                        recyclerView.adapter = GridAdapter(movies,  R.layout.card_layout)
                        val spacing = (19 * resources.displayMetrics.density).toInt() // 16dp to px
                        recyclerView.addItemDecoration(EqualSpaceItemDecoration(spacing))
                    }



                    break
                } catch (e: Exception) {
                    delay(10_000)
                    Log.e("DEBUG_MAIN_Slider 4", "Error fetching data", e)

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
                        val spacing = (19 * resources.displayMetrics.density).toInt() // 16dp to px
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
}
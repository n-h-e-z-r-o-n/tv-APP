package com.example.onyx

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class test : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_test)

        NavAction.setupSidebar(this)

        //SliderData()

    }

    private fun Slid_erData() {
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
                        val imgUrl = "https://image.tmdb.org/t/p/w1280" + item.getString("backdrop_path")
                        val cast_id = item.getString("id")
                        val release_date  = item.getString("release_date")
                        val type = "movie"
                        movies.add(MovieItem(title, imgUrl, cast_id, type))
                    }

                    Log.e("DEBUG_MAIN_Slider 3", movies.toString())


                    withContext(Dispatchers.Main) {
                        val recyclerView = findViewById<RecyclerView>(R.id.Slider_widget)
                        recyclerView.layoutManager = LinearLayoutManager(
                            this@test,
                            LinearLayoutManager.HORIZONTAL, // 👈 makes it horizontal
                            false
                        )

                        recyclerView.layoutManager = StackLayoutManager(
                            maxVisible = 6,   // show 6 stacked
                            scaleGap = 0.05f, // shrink each layer slightly
                            transGap = 150     // push sideways
                        )
                        recyclerView.adapter = GridAdapter(movies, R.layout.card_layout) // your adapter

                    }

                    break
                } catch (e: Exception) {
                    delay(10_000)
                    Log.e("DEBUG_MAIN_Slider 4", "Error fetching data", e)
                }
            }
        }
    }

/*
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

                    val movies = mutableListOf<MovieItem>()
                    for (i in 0 until moviesArray.length()) {
                        val item = moviesArray.getJSONObject(i)
                        val title = item.getString("original_title")
                        val imgUrl = "https://image.tmdb.org/t/p/w1280" + item.getString("backdrop_path")

                        val id = item.getString("id")
                        val type = "movie"





                        //movies.add(MovieItem(title, imgUrl, id, type))
                    }

                    withContext(Dispatchers.Main) {
                        val recyclerView = findViewById<RecyclerView>(R.id.Slider_widget)
                        val adapter = CardSwiper(movies, R.layout.card_layout)

                        recyclerView.layoutManager = StackLayoutManager(
                            maxVisible = 6,
                            scaleGap = 0.05f,
                            transGap = 150
                        )
                        recyclerView.adapter = adapter

                        /*
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
                    Log.e("DEBUG_MAIN_Slider", "Error fetching data", e)
                }
            }
        }
    }




 */
}
package com.example.tv_s

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL


class Watch_Page : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_watch_page)

        // Get extras from Intent
        val imdbCode = intent.getStringExtra("imdb_code")
        val type = intent.getStringExtra("type")

        fetchData("1061474", "movie")


        // Show them (for testing)
        //findViewById<TextView>(R.id.watchPage).text =  "IMDB Code: $imdbCode\nType: $type"
        //val recyclerView = findViewById<TextView>(R.id.watchPage)
        //recyclerView.text =  "IMDB Code: $imdbCode\nType: $type"
    }



    private fun fetchData(id:String, type: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {


                val url = "https://api.themoviedb.org/3/$type/$id?language=en-US"
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("accept", "application/json")
                connection.setRequestProperty(
                    "Authorization",
                    "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJhZjliMmUyN2MxYTZiYzMyMzNhZjE4MzJmNGFjYzg1MCIsIm5iZiI6MTcxOTY3NDUxNy4xOTYsInN1YiI6IjY2ODAyNjk1ZWZhYTI1ZjBhOGE4NGE3MyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.RTms-g8dzOl3WwCeJ7WNLq3i2kXxl3T7gOTa8POcxcw"
                )

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.e("DEBUG_Watch", response)

                /*
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

                 */
            } catch (e: Exception) {
                Log.e("DEBUG_TAG", "Error fetching data", e)
            }
        }
    }
}
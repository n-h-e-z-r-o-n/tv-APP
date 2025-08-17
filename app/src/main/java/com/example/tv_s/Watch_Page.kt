package com.example.tv_s

import android.media.Rating
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

import android.view.View



class Watch_Page : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_watch_page)

        // Get extras from Intent
        val imdbCode = intent.getStringExtra("imdb_code")
        val type = intent.getStringExtra("type")

        fetchData(imdbCode.toString(), type.toString())


        // Show them (for testing)
        //findViewById<TextView>(R.id.watchPage).text =  "IMDB Code: $imdbCode\nType: $type"
        //val recyclerView = findViewById<TextView>(R.id.watchPage)
        //recyclerView.text =  "IMDB Code: $imdbCode\nType: $type"
    }



    private fun fetchData(id:String, type: String) {
        CoroutineScope(Dispatchers.IO).launch {
            var tmdbId = id // mutable copy
            try {
                if (id.startsWith("tt")){

                    val url = "https://api.themoviedb.org/3/movie/$id/external_ids"
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("accept", "application/json")
                    connection.setRequestProperty(
                        "Authorization",
                        "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJhZjliMmUyN2MxYTZiYzMyMzNhZjE4MzJmNGFjYzg1MCIsIm5iZiI6MTcxOTY3NDUxNy4xOTYsInN1YiI6IjY2ODAyNjk1ZWZhYTI1ZjBhOGE4NGE3MyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.RTms-g8dzOl3WwCeJ7WNLq3i2kXxl3T7gOTa8POcxcw"
                    )
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = org.json.JSONObject(response)
                    tmdbId = jsonObject.getString("id")

                }

                val url = "https://api.themoviedb.org/3/$type/$tmdbId?language=en-US"
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("accept", "application/json")
                connection.setRequestProperty(
                    "Authorization",
                    "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJhZjliMmUyN2MxYTZiYzMyMzNhZjE4MzJmNGFjYzg1MCIsIm5iZiI6MTcxOTY3NDUxNy4xOTYsInN1YiI6IjY2ODAyNjk1ZWZhYTI1ZjBhOGE4NGE3MyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.RTms-g8dzOl3WwCeJ7WNLq3i2kXxl3T7gOTa8POcxcw"
                )

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.e("DEBUG_Watch", response)
                val jsonObject = org.json.JSONObject(response)
                Log.e("DEBUG_Watch", jsonObject.toString())


                val backdrop_Url = "https://image.tmdb.org/t/p/w500${jsonObject.getString("backdrop_path")}"
                val poster_Url = "https://image.tmdb.org/t/p/w500${jsonObject.getString("backdrop_path")}"
                val original_title  = jsonObject.getString("original_title")
                val overview  = jsonObject.getString("overview")
                val poster_path  = jsonObject.getString("poster_path")
                val release_date  = jsonObject.getString("release_date")
                val runtime  = jsonObject.getString("runtime")
                val vote_average  = jsonObject.getString("vote_average")







                withContext(Dispatchers.Main) {

                            val  backdrop_Widget = findViewById<ImageView>(R.id.backdropImageView)
                            val  poster_widget = findViewById<ImageView>(R.id.posterImageView)
                            val  title_widget = findViewById<TextView>(R.id.title_widget)
                            val  year_widget = findViewById<TextView>(R.id.year_widget)
                            val  Rating_widget = findViewById<TextView>(R.id.Rating_widget)
                            val  Overview_widget = findViewById<TextView>(R.id.overview_widget)



                            title_widget.text = original_title
                            year_widget.text = release_date
                            Rating_widget.text  = "⭐ ${vote_average}/10"
                            Overview_widget.text = overview


                            Picasso.get()
                                .load(backdrop_Url)
                                .fit()
                                .centerInside()
                                .into(backdrop_Widget)

                            Picasso.get()
                                .load(poster_Url)
                                .fit()
                                .centerInside()
                                .into(poster_widget)
                }


            } catch (e: Exception) {
                Log.e("DEBUG_TAG", "Error fetching data", e)
            }
        }
    }
}
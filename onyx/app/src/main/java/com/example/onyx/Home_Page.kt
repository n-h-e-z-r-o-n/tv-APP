package com.example.onyx

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
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
import java.util.Calendar

class Home_Page : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_page)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        loadingAnimation.setup(this@Home_Page)
        NavAction.setupSidebar(this)

        SliderData()
    }


    private fun SliderData() {
        loadingAnimation.show(this@Home_Page)
        CoroutineScope(Dispatchers.IO).launch {

            while (true) {

                try {
                    val url = "https://api.themoviedb.org/3/discover/movie?include_adult=true"
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


                    val url2 = "https://api.themoviedb.org/3/discover/tv?include_adult=true"
                    val connection2 = URL(url2).openConnection() as HttpURLConnection
                    connection2.requestMethod = "GET"
                    connection2.setRequestProperty("accept", "application/json")
                    connection2.setRequestProperty(
                        "Authorization",
                        "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJhZjliMmUyN2MxYTZiYzMyMzNhZjE4MzJmNGFjYzg1MCIsIm5iZiI6MTcxOTY3NDUxNy4xOTYsInN1YiI6IjY2ODAyNjk1ZWZhYTI1ZjBhOGE4NGE3MyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.RTms-g8dzOl3WwCeJ7WNLq3i2kXxl3T7gOTa8POcxcw"
                    )

                    val response2 = connection2.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject2 = org.json.JSONObject(response2)
                    val moviesArray2 = jsonObject2.getJSONArray("results")


                    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                    val url3 =
                        "https://api.themoviedb.org/3/trending/all/day?primary_release_year=$currentYear"
                    val connection3 = URL(url3).openConnection() as HttpURLConnection
                    connection3.requestMethod = "GET"
                    connection3.setRequestProperty("accept", "application/json")
                    connection3.setRequestProperty(
                        "Authorization",
                        "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJhZjliMmUyN2MxYTZiYzMyMzNhZjE4MzJmNGFjYzg1MCIsIm5iZiI6MTcxOTY3NDUxNy4xOTYsInN1YiI6IjY2ODAyNjk1ZWZhYTI1ZjBhOGE4NGE3MyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.RTms-g8dzOl3WwCeJ7WNLq3i2kXxl3T7gOTa8POcxcw"
                    )

                    val response3 = connection3.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject3 = org.json.JSONObject(response3)
                    val moviesArray3 = jsonObject3.getJSONArray("results")


                    var movies = mutableListOf<SliderItem>()
                    for (i in 0 until moviesArray.length()) {

                        val item = moviesArray.getJSONObject(i)
                        Log.e("DEBUG_MAIN_Slider", item.toString())
                        val title = item.getString("title")
                        val backdrop_path =
                            "https://image.tmdb.org/t/p/w1280" + item.getString("backdrop_path")
                        val id = item.getString("id")
                        val type = "movie"
                        val overview = item.getString("overview")
                        val release_date = item.getString("release_date").substring(0, 4)
                        val vote_average = item.getString("vote_average").substring(0, 3)
                        val poster_path = item.getString("poster_path")
                        val genreIdsJson = item.getJSONArray("genre_ids")
                        val genreIds: List<Int> = List(genreIdsJson.length()) { idx ->
                            genreIdsJson.getInt(idx)
                        }

                        movies.add(
                            SliderItem(
                                title,
                                backdrop_path,
                                id,
                                type,
                                overview,
                                release_date,
                                vote_average,
                                poster_path,
                                genreIds
                            )
                        )
                    }

                    for (i in 0 until moviesArray2.length()) {

                        val item = moviesArray2.getJSONObject(i)
                        Log.e("DEBUG_MAIN_Slider", item.toString())
                        val title = item.getString("original_name")
                        val backdrop_path =
                            "https://image.tmdb.org/t/p/w1280" + item.getString("backdrop_path")
                        val id = item.getString("id")
                        val type = "tv"
                        val overview = item.getString("overview")
                        val release_date = item.getString("first_air_date").substring(0, 4)
                        val vote_average = item.getString("vote_average").substring(0, 3)
                        val poster_path = item.getString("poster_path")
                        val genreIdsJson = item.getJSONArray("genre_ids")
                        val genreIds: List<Int> = List(genreIdsJson.length()) { idx ->
                            genreIdsJson.getInt(idx)
                        }

                        movies.add(
                            SliderItem(
                                title,
                                backdrop_path,
                                id,
                                type,
                                overview,
                                release_date,
                                vote_average,
                                poster_path,
                                genreIds
                            )
                        )
                    }

                    for (i in 0 until moviesArray3.length()) {
                        val item = moviesArray3.getJSONObject(i)
                        val title = when {
                            item.has("original_name") && !item.isNull("original_name") -> item.getString(
                                "original_name"
                            )

                            item.has("original_title") && !item.isNull("original_title") -> item.getString(
                                "original_title"
                            )

                            item.has("title") && !item.isNull("title") -> item.getString("title")
                            else -> "Untitled"
                        }

                        val type = item.getString("media_type")
                        if (type != "movie" && type != "tv") {
                            continue   // skip this loop iteration
                        }
                        val backdropPath =
                            "https://image.tmdb.org/t/p/w1280" + item.getString("backdrop_path")
                        val id = item.getString("id")
                        val overview = item.getString("overview")
                        val release_date = try {
                            item.getString("release_date").substring(0, 4)
                        } catch (e: Exception) {
                            item.getString("first_air_date").substring(0, 4)
                        }
                        val vote_average = item.getString("vote_average").substring(0, 3)
                        val poster_path = item.getString("poster_path")
                        val genreIdsJson = item.getJSONArray("genre_ids")
                        val genreIds: List<Int> = List(genreIdsJson.length()) { idx ->
                            genreIdsJson.getInt(idx)
                        }

                        movies.add(
                            SliderItem(
                                title,
                                imageUrl = backdropPath,
                                imdbCode = id,
                                type = type,
                                overview,
                                release_date,
                                vote_average,
                                poster_path,
                                genreIds
                            )
                        )

                    }

                    movies.shuffle()
                    movies = movies.distinctBy { it.imdbCode }.toMutableList()

                    withContext(Dispatchers.Main) {
                        loadingAnimation.hide(this@Home_Page)
                        val recyclerView = findViewById<RecyclerView>(R.id.Slider_widget)
                        val adapter = CardSwiper(movies, R.layout.card_layout)


                        recyclerView.layoutManager = LinearLayoutManager(
                            this@Home_Page,
                            LinearLayoutManager.HORIZONTAL, // 👈 makes it horizontal
                            false
                        )
                        recyclerView.adapter = adapter

                    }

                    break
                } catch (e: Exception) {
                    delay(10_000)
                    Log.e("DEBUG_MAIN_Slider Error", "Error fetching data", e)
                }
            }
        }
    }

}


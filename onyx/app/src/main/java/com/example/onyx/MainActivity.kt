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
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import org.json.JSONArray
import org.json.JSONObject


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
        //SliderData()
        //TrendingData()
        //PopularData()
        fetchData()
        TvShows()
    }

    private fun fetchData() {
        CoroutineScope(Dispatchers.IO).launch {

            val adapter = GridAdapter(mutableListOf(), R.layout.item_grid)
            withContext(Dispatchers.Main) {
                val recyclerView = findViewById<RecyclerView>(R.id.Movies)
                recyclerView.layoutManager = GridLayoutManager(this@MainActivity, 5)
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
                    Log.e("DEBUG_TAG_moviesArray", moviesArray.toString())


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
                    Log.e("DEBUG_TAG_Movies", "Error fetching data", e)
                    break
                }
            }
        }
    }

    private fun TvShows() {
        CoroutineScope(Dispatchers.IO).launch {

            val adapter = GridAdapter(mutableListOf(), R.layout.item_grid)
            withContext(Dispatchers.Main) {
                val recyclerView = findViewById<RecyclerView>(R.id.TvShows)
                recyclerView.layoutManager = GridLayoutManager(this@MainActivity, 5)
                recyclerView.adapter = adapter
                val spacing = (19 * resources.displayMetrics.density).toInt()
                recyclerView.addItemDecoration(EqualSpaceItemDecoration(spacing))
            }
            while(true) {
                try {
                    val url = "https://vidsrc.xyz/episodes/latest/page-1.json"
                    val url2 = "https://vidsrc.xyz/episodes/latest/page-3.json"

                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)
                    val moviesArray = jsonObject.getJSONArray("result")

                    val connection2 = URL(url2).openConnection() as HttpURLConnection
                    connection2.requestMethod = "GET"
                    val response2 = connection2.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject2 = JSONObject(response2)
                    val moviesArray2 = jsonObject2.getJSONArray("result")

                    val finalArray = JSONArray()

                    for (i in 0 until moviesArray.length()) {
                        finalArray.put(moviesArray.getJSONObject(i))
                    }

                    for (i in 0 until moviesArray2.length()) {
                        finalArray.put(moviesArray2.getJSONObject(i))
                    }

                    Log.e("DEBUG_TAG_TvShows 1", finalArray.toString())


                    val movies = mutableListOf<MovieItem>()

                    val movies_temp = mutableListOf<String>()

                    for (i in 0 until finalArray.length()) {
                        val item = finalArray.getJSONObject(i)
                        val imdb_code = item.getString("tmdb_id")
                        if (imdb_code == "null" || imdb_code.isEmpty()) continue
                        movies_temp.add(imdb_code)
                    }
                    val uniqueMovies = movies_temp.toSet().toList()

                    Log.e("DEBUG_TAG_TvShows 2", movies_temp.toString())


                    for (i in 0 until uniqueMovies.size) {
                        val imdb_code = uniqueMovies[i]
                        val url = "https://api.themoviedb.org/3/tv/$imdb_code?"
                        val connection = URL(url).openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.setRequestProperty("accept", "application/json")
                        connection.setRequestProperty(
                            "Authorization",
                            "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJhZjliMmUyN2MxYTZiYzMyMzNhZjE4MzJmNGFjYzg1MCIsIm5iZiI6MTcxOTY3NDUxNy4xOTYsInN1YiI6IjY2ODAyNjk1ZWZhYTI1ZjBhOGE4NGE3MyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.RTms-g8dzOl3WwCeJ7WNLq3i2kXxl3T7gOTa8POcxcw"
                        )
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        val jsonObject = org.json.JSONObject(response)

                        Log.e("DEBUG_TAG_TvShows 3", jsonObject.toString())

                        val title = jsonObject.getString("name")
                        val imgUrl = "https://image.tmdb.org/t/p/w500" + jsonObject.getString("poster_path")
                        val id = jsonObject.getString("id")
                        val type = "tv"
                        movies.add(MovieItem(title, imgUrl, id, type))

                        val movieItem = MovieItem(title, imgUrl, id, type)

                        withContext(Dispatchers.Main) {
                            adapter.addItem(movieItem)  // 👈 add one at a time
                        }


                    }
                    Log.e("DEBUG_TAG_TvShows 4", movies.toString())

                    /*
                     withContext(Dispatchers.Main) {
                        val recyclerView = findViewById<RecyclerView>(R.id.TvShows)
                        recyclerView.layoutManager = GridLayoutManager(this@MainActivity, 5)
                        recyclerView.adapter = GridAdapter(movies,  R.layout.item_grid)
                        val spacing = (19 * resources.displayMetrics.density).toInt() // 16dp to px
                        recyclerView.addItemDecoration(EqualSpaceItemDecoration(spacing))
                    }
                     */
                    break
                } catch (e: Exception) {
                    delay(10_000)
                    Log.e("DEBUG_TAG_TvShows", "Error fetching data", e)
                    break
                }
            }
        }
    }

    private fun setupSidebar() {
        val btnHome = findViewById<ImageButton>(R.id.btnHome)
        val btnMovies = findViewById<ImageButton>(R.id.btnMovies)
        val btnTvShows = findViewById<ImageButton>(R.id.btnTvShow)
        val btnSearch = findViewById<ImageButton>(R.id.btnSearch)
        val btnProfile = findViewById<ImageButton>(R.id.btnProfile)

        val recyclerHome = findViewById<LinearLayout>(R.id.Home)
        val recyclerMovies = findViewById<RecyclerView>(R.id.Movies)
        val recyclerTv = findViewById<RecyclerView>(R.id.TvShows)
        val recyclerSearch = findViewById<LinearLayout>(R.id.SearchShow)

        val recyclerSettings = findViewById<RecyclerView>(R.id.Setting)

        val buttons = listOf(btnHome, btnMovies, btnTvShows, btnSearch, btnProfile)

        // Convert hex color strings to integer color values
        val activeColor = Color.parseColor("#4545FF")
        val inactiveColor = Color.parseColor("#FFFFFF")

        fun activate(button: ImageButton, target: View) {
            // Reset all icons
            buttons.forEach {
                it.setColorFilter(inactiveColor)
                it.setOnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        v.animate().scaleX(1.25f).scaleY(1.25f).setDuration(150).start()
                    } else {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                    }
                }

            }

            // Highlight current
            button.setColorFilter(activeColor)

            // Hide all recyclers
            recyclerHome.visibility = View.GONE
            recyclerMovies.visibility = View.GONE
            recyclerTv.visibility = View.GONE
            recyclerSearch.visibility = View.GONE
            recyclerSettings.visibility = View.GONE

            // Show selected
            target.visibility = View.VISIBLE
        }

        btnHome.setOnClickListener { activate(btnHome, recyclerHome) }
        btnMovies.setOnClickListener { activate(btnMovies, recyclerMovies ) }  // example
        btnTvShows.setOnClickListener { activate(btnTvShows, recyclerTv) }
        btnSearch.setOnClickListener { activate(btnSearch, recyclerSearch) }
        btnProfile.setOnClickListener { activate(btnProfile, recyclerSettings) }

        // default view
        activate(btnHome, recyclerHome)
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
                            this@MainActivity,
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
                            this@MainActivity,
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
                            this@MainActivity,
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


data class MovieItem(
    val title: String,
    val imageUrl: String,
    val imdbCode: String,
    val type: String
)

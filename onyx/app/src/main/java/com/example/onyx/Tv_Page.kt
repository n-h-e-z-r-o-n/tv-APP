package com.example.onyx

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class Tv_Page : AppCompatActivity() {

    private var currentPage = 1
    private var isLoadingMore = false
    private lateinit var adapter: GridAdapter
    private lateinit var recyclerView : RecyclerView

    private var lastRefreshTime: Long = 0L


    override fun onCreate(savedInstanceState: Bundle?) {
        GlobalUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tv_page)

        NavAction.setupSidebar(this@Tv_Page)

        adapter = GridAdapter(mutableListOf(), R.layout.item_grid)
        adapter.onAddMoreClicked = {
            loadMoreMovies()
        }

        // Calculate span count dynamically
        val widthInPixels = this@Tv_Page.resources.getDimension(R.dimen.grid_item_width)
        val density = this@Tv_Page.resources.displayMetrics.density
        val widthInDp = widthInPixels / density
        val displayMetrics = resources.displayMetrics
        val screenWidthPx = displayMetrics.widthPixels
        val itemMinWidthPx = ((widthInDp + 15) * displayMetrics.density).toInt() // 160dp per item
        val spanCount = maxOf(1, screenWidthPx / itemMinWidthPx)

        recyclerView    = findViewById<RecyclerView>(R.id.TvShows_RecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this@Tv_Page, spanCount)
        recyclerView.adapter = adapter
        val spacing = (19 * resources.displayMetrics.density).toInt()
        recyclerView.addItemDecoration(EqualSpaceItemDecoration(spacing))




        fetchTvShows()
    }

    override fun onResume() {
        super.onResume()
        val currentTime = System.currentTimeMillis()
        val oneHourMillis = 60 * 60 * 1000  // 1 hour

        if (currentTime - lastRefreshTime > oneHourMillis) {
            //refreshData()
        }
    }

    private fun fetchTvShows() {
        isLoadingMore = true
        CoroutineScope(Dispatchers.IO).launch {

            repeat(5) { attempt ->
                try {
                    val url = "https://vidsrc.xyz/episodes/latest/page-$currentPage.json"
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)
                    val moviesArray = jsonObject.getJSONArray("result")

                    val finalArray = JSONArray()

                    for (i in 0 until moviesArray.length()) {
                        finalArray.put(moviesArray.getJSONObject(i))
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
                        val numberOfSeasons = try{jsonObject.getJSONObject("last_episode_to_air").getString("season_number")} catch (e: Exception) {""}
                        val episodeNumber = try{jsonObject.getJSONObject("last_episode_to_air").getString("episode_number")} catch (e: Exception) {""}
                        val showD = "SS$numberOfSeasons EPS$episodeNumber"
                        val firstAirDate = jsonObject.getString("first_air_date").substring(0, 4)
                        val voteAverage = "☆" + jsonObject.getString("vote_average").substring(0, 3)

                        val imgUrl = "https://image.tmdb.org/t/p/w500" + jsonObject.getString("poster_path")
                        val id = jsonObject.getString("id")
                        val type = "tv"
                        movies.add(MovieItem(title=title, imageUrl=imgUrl, imdbCode=id, type=type, year="", rating="", runtime=""))

                        val movieItem = MovieItem(title=title, imageUrl=imgUrl, imdbCode=id, type=type, year=firstAirDate, rating=voteAverage, runtime=showD)

                        withContext(Dispatchers.Main) {
                            adapter.addItem(movieItem)
                            isLoadingMore = false
                        }


                    }
                    Log.e("DEBUG_TAG_TvShows 4", movies.toString())

                    return@launch
                } catch (e: Exception) {
                    Log.e("DEBUG_TAG_TvShows", "Attempt ${attempt+1} failed", e)
                    delay(10_000)
                    currentPage--
                }
            }
        }
    }

    private fun loadMoreMovies() {

        if (isLoadingMore) return // Prevent multiple rapid clicks
        currentPage++
        fetchTvShows()

    }

    private fun refreshData() {
        Toast.makeText(this, "Refreshing movies...", Toast.LENGTH_SHORT).show()
        currentPage = 1
        adapter.clearItems()   // we'll add this helper in GridAdapter
        lastRefreshTime = System.currentTimeMillis()
        fetchTvShows()
    }

}
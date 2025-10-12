package com.example.onyx

import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.delay
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import android.util.Log

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class Search_Page : AppCompatActivity() {
    private lateinit var searchInput: EditText
    private lateinit var searchResults: RecyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        GlobalUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_search_page2)

        LoadingAnimation.setup(this@Search_Page)
        NavAction.setupSidebar(this)


        // Initialize views
        searchInput = this.findViewById(R.id.searchInput)
        searchResults = this.findViewById(R.id.SearchResults)

        setupSearchFunctionality()
    }

    private fun setupSearchFunctionality() {
        // Set up the search action when user presses enter/search key
        searchInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                performSearch()
                true
            } else {
                false
            }
        }

    }

    private fun performSearch() {
        LoadingAnimation.show(this@Search_Page)
        val query = searchInput.text.toString().trim()

        val adapter = GridAdapter2(mutableListOf(), R.layout.item_grid)

        CoroutineScope(Dispatchers.IO).launch {
            repeat(3) { attempt ->
                try {

                    if (query.isNotEmpty()) {

                        withContext(Dispatchers.Main) {
                            // Hide keyboard
                            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.hideSoftInputFromWindow(searchInput.windowToken, 0)

                            // Clear focus from EditText
                            searchInput.clearFocus()

                            val recyclerView = findViewById<RecyclerView>(R.id.SearchResults)
                            recyclerView.layoutManager = GridLayoutManager(this@Search_Page, 5)
                            recyclerView.adapter = adapter
                            val spacing = (19 * resources.displayMetrics.density).toInt()
                            recyclerView.addItemDecoration(EqualSpaceItemDecoration(spacing))
                        }



                        Log.e("SEARCH RESULTS", query)

                        // --- Background work (network request) ---
                        val url =
                            "https://api.themoviedb.org/3/search/multi?include_adult=true&query=$query"
                        val connection = URL(url).openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.setRequestProperty("accept", "application/json")
                        connection.setRequestProperty(
                            "Authorization",
                            "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJhZjliMmUyN2MxYTZiYzMyMzNhZjE4MzJmNGFjYzg1MCIsIm5iZiI6MTcxOTY3NDUxNy4xOTYsInN1YiI6IjY2ODAyNjk1ZWZhYTI1ZjBhOGE4NGE3MyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.RTms-g8dzOl3WwCeJ7WNLq3i2kXxl3T7gOTa8POcxcw"
                        )

                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        val jsonObject = JSONObject(response)

                        Log.e("SEARCH RESULTS", jsonObject.toString())
                        val moviesArray = jsonObject.getJSONArray("results")

                        //Log.e("SEARCH RESULTS", moviesArray.toString())

                        for (i in 0 until moviesArray.length()) {
                            val current = moviesArray.getJSONObject(i)
                            current.remove("overview")
                            current.remove("genre_ids")
                            current.remove("popularity")
                            current.remove("video")

                            val mediaType = current.optString("media_type", "movie")

                            var title = "Unknown"
                            var info = ""
                            var date = ""
                            var voteAverage = ""
                            var imgUrl = ""
                            var poster = ""

                            Log.e("SEARCH RESULTS $i", current.toString())
                            when (mediaType) {
                                "person" -> {
                                    title = current.optString(
                                        "name",
                                        current.optString("original_name", "Unknown")
                                    )
                                    imgUrl = "https://image.tmdb.org/t/p/w500" + current.optString(
                                        "profile_path",
                                        ""
                                    )
                                    poster = current.optString("profile_path", "null")
                                    info = current.optString("known_for_department", "")
                                    voteAverage = ""
                                }

                                "tv" -> {
                                    title = current.optString(
                                        "name",
                                        current.optString("original_name", "Unknown")
                                    )
                                    date =
                                        current.optString("first_air_date")
                                            .takeIf { it.isNotEmpty() }
                                            ?.substring(0, 4) ?: ""
                                    imgUrl = "https://image.tmdb.org/t/p/w500" + current.optString(
                                        "poster_path",
                                        ""
                                    )
                                    poster = current.optString("poster_path", "null")
                                    info = ""
                                    voteAverage =
                                        current.optDouble("vote_average", 0.0).toInt()
                                            .toString() + " ★"
                                }

                                "movie" -> {
                                    title = current.optString("original_title", "Unknown")
                                    date =
                                        current.optString("release_date").takeIf { it.isNotEmpty() }
                                            ?.substring(0, 4) ?: ""
                                    imgUrl = "https://image.tmdb.org/t/p/w500" + current.optString(
                                        "poster_path",
                                        ""
                                    )
                                    poster = current.optString("poster_path", "null")
                                    info = "" // TODO: runtime if available
                                    voteAverage = current.optDouble("vote_average", 0.0).toInt()
                                        .toString() + " ★"
                                }
                            }

                            if (poster.isBlank() || poster.endsWith("null")) continue

                            val id = current.getString("id")


                            //movies.add(MovieItem(title, imgUrl, id, type))

                            val movieItem = MovieItem(
                                title = title,
                                imageUrl = imgUrl,
                                imdbCode = id,
                                type = mediaType,
                                year = date,
                                rating = voteAverage,
                                runtime = info
                            )

                            withContext(Dispatchers.Main) {
                                LoadingAnimation.hide(this@Search_Page)
                                adapter.addItem(movieItem)  // 👈 add one at a time
                            }


                        }

                    }
                    return@launch
                } catch (e: Exception) {
                    Log.e("SEARCH RESULTS ERROR", "S ERROR", e)
                    delay(10_000)
                }
            }
        }
    }


}
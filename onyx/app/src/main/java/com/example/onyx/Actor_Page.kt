package com.example.onyx

import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class Actor_Page : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        GlobalUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_actor_page)

        NavAction.setupSidebar(this@Actor_Page)

        val cast_id = intent.getStringExtra("imdb_code")

        Log.e("cast RESULTS start", "id $cast_id ")


        //val cast_id = "500"
        fetchActorShows(cast_id.toString())

    }
    private fun fetchActorShows(id: String){
        val adapter = GridAdapter2(mutableListOf(), R.layout.item_grid)
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                val recyclerView = findViewById<RecyclerView>(R.id.Actor_Results)



                // Calculate span count dynamically
                val widthInPixels = this@Actor_Page.resources.getDimension(R.dimen.grid_item_width)
                val density = this@Actor_Page.resources.displayMetrics.density
                val widthInDp = widthInPixels / density
                val displayMetrics = resources.displayMetrics
                val screenWidthPx = displayMetrics.widthPixels
                val itemMinWidthPx = ((widthInDp + 15) * displayMetrics.density).toInt() // 160dp per item
                val spanCount = maxOf(1, screenWidthPx / itemMinWidthPx)

                recyclerView.layoutManager = GridLayoutManager(this@Actor_Page, spanCount)
                recyclerView.adapter = adapter
                val spacing = (19 * displayMetrics.density).toInt()
                recyclerView.addItemDecoration(EqualSpaceItemDecoration(spacing))


            }

            try {
                val movie_url =
                    "https://api.themoviedb.org/3/person/$id/movie_credits?language=en-US"

                val connection = URL(movie_url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("accept", "application/json")
                connection.setRequestProperty(
                    "Authorization",
                    "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJhZjliMmUyN2MxYTZiYzMyMzNhZjE4MzJmNGFjYzg1MCIsIm5iZiI6MTcxOTY3NDUxNy4xOTYsInN1YiI6IjY2ODAyNjk1ZWZhYTI1ZjBhOGE4NGE3MyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.RTms-g8dzOl3WwCeJ7WNLq3i2kXxl3T7gOTa8POcxcw"
                )
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(response)


                val moviesArray = jsonObject.getJSONArray("cast")
                Log.e("cast RESULTS", moviesArray.toString())




                for (i in 0 until moviesArray.length()) {
                    val current = moviesArray.getJSONObject(i)
                    current.remove("popularity")
                    current.remove("video")

                    val mediaType = "movie"

                    var  title = current.optString("original_title", current.optString("name", current.optString("original_name", "Unknown")))

                    var poster = current.optString("poster_path","null")

                    if ( poster.isBlank() || poster.endsWith("null")) continue

                    val imgUrl  = "https://image.tmdb.org/t/p/w780" + poster

                    val backdrop_path   = "https://image.tmdb.org/t/p/w500" + current.optString("backdrop_path")
                    val overview = current.optString("overview")
                    val date =  current.optString("release_date", current.optString("first_air_date")).substring(0, 4)
                    val info = current.optString("runtime", "")
                    val vote_average  = current.optString("vote_average").substring(0, 3)
                    val id = current.getString("id")



                    val movieItem = MovieItem(title=title, imageUrl=imgUrl, imdbCode=id, type=mediaType, year = date, rating=vote_average, runtime=info)


                    withContext(Dispatchers.Main) {
                        adapter.addItem(movieItem)  // 👈 add one at a time
                    }


                }
                ////////////////////////////////////////////////////////////////////////////////////
                    val tv_url =  "https://api.themoviedb.org/3/person/${id}/tv_credits?language=en-US"

                    val connection2 = URL(tv_url).openConnection() as HttpURLConnection
                connection2.requestMethod = "GET"
                connection2.setRequestProperty("accept", "application/json")
                connection2.setRequestProperty(
                        "Authorization",
                        "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJhZjliMmUyN2MxYTZiYzMyMzNhZjE4MzJmNGFjYzg1MCIsIm5iZiI6MTcxOTY3NDUxNy4xOTYsInN1YiI6IjY2ODAyNjk1ZWZhYTI1ZjBhOGE4NGE3MyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.RTms-g8dzOl3WwCeJ7WNLq3i2kXxl3T7gOTa8POcxcw"
                    )

                val response2 = connection2.inputStream.bufferedReader().use { it.readText() }
                val jsonObject2 = JSONObject(response2)


                val moviesArray2 = jsonObject2.getJSONArray("cast")
                Log.e("cast RESULTS", moviesArray2.toString())
                for (i in 0 until moviesArray2.length()) {
                        val current = moviesArray2.getJSONObject(i)
                        current.remove("popularity")
                        current.remove("video")

                        val mediaType = "tv"

                        var  title = current.optString("original_title", current.optString("name", current.optString("original_name", "Unknown")))

                        var poster = current.optString("poster_path","null")

                        if ( poster.isBlank() || poster.endsWith("null")) continue

                        val imgUrl  = "https://image.tmdb.org/t/p/w500" + poster

                        val backdrop_path   = "https://image.tmdb.org/t/p/w500" + current.optString("backdrop_path")
                        val overview = current.optString("overview")
                        val date =  current.optString("first_air_date").substring(0, 4)
                        val info = current.optString("runtime")
                        val vote_average  = current.optString("vote_average").substring(0, 3)
                        val id = current.getString("id")


                        val movieItem =MovieItem(title=title, imageUrl=imgUrl, imdbCode=id, type=mediaType, year = date, rating=vote_average, runtime=info)


                        withContext(Dispatchers.Main) {
                            adapter.addItem(movieItem)  // 👈 add one at a time
                        }
                }
            }catch (e:Exception){
                Log.e("cast RESULTS", "Error" , e)
            }
        }
    }


}
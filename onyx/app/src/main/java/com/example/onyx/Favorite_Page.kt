package com.example.onyx

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONArray
import kotlin.text.ifEmpty


class Favorite_Page : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        GlobalUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_favorite_page)
        NavAction.setupSidebar(this@Favorite_Page)

        val recyclerView = findViewById<RecyclerView>(R.id.favoritesRecycler)
        val emptyState = findViewById<TextView>(R.id.emptyState)



        recyclerView.layoutManager = GridLayoutManager(this, 6)


        val favorites = FavoritesManager.getFavorites(this)


        val titleFavorites: TextView = findViewById(R.id.titleFavorites)

        if (favorites.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyState.visibility = View.GONE

            val items = favorites.map { obj ->

                try {
                    titleFavorites.text = "$obj"

                    val releaseDate = obj.optString("release_date").ifEmpty {
                        obj.optString("first_air_date")
                    }

                    val type = if (obj.optString("release_date").isNotEmpty()) {
                        "movie"
                    } else if (obj.optString("first_air_date").isNotEmpty()) {
                        "tv"
                    } else {
                        "unknown"
                    }


                    val runtime = obj.optString("runtime").ifEmpty {
                        val arr: JSONArray? = obj.optJSONArray("episode_run_time")
                        if (arr != null && arr.length() > 0) arr.optInt(0).toString() else ""
                    }

                    val overview = obj.optString("overview", "")

                    val backdropUrl = "https://image.tmdb.org/t/p/w1280${obj.optString("backdrop_path", "")}"
                    val posterUrl =    "https://image.tmdb.org/t/p/w780${obj.optString("poster_path", "")}"
                    val originalTitle = obj.optString("name",  obj.optString("title", ""))

                    val voteAverage = obj.optString("vote_average", " ").substring(0, 3)

                    // Genres
                    val genresList = mutableListOf<String>()
                    obj.optJSONArray("genres")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            arr.optJSONObject(i)?.optString("name")?.let { genresList.add(it) }
                        }
                    }
                    val genres = genresList.joinToString(" ~ ")


                    // Production companies
                    val productionList = mutableListOf<String>()
                    obj.optJSONArray("production_companies")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            arr.optJSONObject(i)?.optString("name")?.let { productionList.add(it) }
                        }
                    }
                    val productionC = productionList.joinToString("  ~ ")
                    val pg = if (obj.optBoolean("adult", false)) "18 +" else "13"

                    val id = obj.optString("id", "")



                    FavItem(
                        title = originalTitle,
                        posterUrl = posterUrl,
                        backdropUrl = backdropUrl,
                        releaseDate = releaseDate.substring(0, 4),
                        runtime = runtime,
                        overview = overview,
                        voteAverage = voteAverage,
                        genres = genres,
                        production = productionC,
                        parentalGuide = pg,
                        imdbCode = id,
                        showType = type
                    )


                }catch (e : Exception){
                    //titleFavorites.text = "$e"
                    FavItem(
                        title = "",
                        posterUrl = "",
                        backdropUrl = "",
                        releaseDate = "",
                        runtime = "",
                        overview = "",
                        voteAverage = "",
                        genres = "",
                        production = "",
                        parentalGuide = "",
                        imdbCode = "",
                        showType = ""
                    )
                }


            }.toMutableList()

            val FavBackdrop: ImageView = findViewById(R.id.FavBackdrop)
            val FavTitle: TextView = findViewById(R.id.FavTitle)
            val FavGenre: TextView = findViewById(R.id.FavGenre)
            val FavType: TextView = findViewById(R.id.FavType)
            val FavRating: TextView = findViewById(R.id.FavRating)
            val FavYear: TextView = findViewById(R.id.FavYear)
            val FavOverview: TextView = findViewById(R.id.FavOverview)
            val RemoveFaveItem: LinearLayout = findViewById(R.id.RemoveFaveItem)




            recyclerView.adapter = FavAdapter(items, R.layout.square_card, FavBackdrop, FavTitle, FavGenre, FavType, FavRating, FavYear, FavOverview,RemoveFaveItem )
        }
    }
}
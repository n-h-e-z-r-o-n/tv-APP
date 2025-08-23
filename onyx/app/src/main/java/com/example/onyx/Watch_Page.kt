package com.example.onyx

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.delay
import org.json.JSONArray
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.marginEnd
import org.json.JSONObject

class Watch_Page : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_watch_page)

        // Get extras from Intent
        val imdbCode = intent.getStringExtra("imdb_code")
        val type = intent.getStringExtra("type")

        fetchData(imdbCode.toString(), type.toString())
        //fetchData("95557", "tv")


        /*
        val watchButton = findViewById<Button>(R.id.watchNowButton)
        watchButton.setOnClickListener {
            val intent = Intent(this, Play::class.java)
            intent.putExtra("imdb_code", imdbCode)
            intent.putExtra("type", type)
            startActivity(intent)
        }

         */

        // Show them (for testing)
        //findViewById<TextView>(R.id.watchPage).text =  "IMDB Code: $imdbCode\nType: $type"
        //val recyclerView = findViewById<TextView>(R.id.watchPage)
        //recyclerView.text =  "IMDB Code: $imdbCode\nType: $type"
    }



    private fun fetchData(id:String, type: String) {
        CoroutineScope(Dispatchers.IO).launch {
            var tmdbId = id // mutable copy
            while (true) {
                try {
                    if (id.startsWith("tt")) {

                        val url = "https://api.themoviedb.org/3/movie/$id/external_ids"
                        val connection = URL(url).openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.setRequestProperty("accept", "application/json")
                        connection.setRequestProperty(
                            "Authorization",
                            "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJhZjliMmUyN2MxYTZiYzMyMzNhZjE4MzJmNGFjYzg1MCIsIm5iZiI6MTcxOTY3NDUxNy4xOTYsInN1YiI6IjY2ODAyNjk1ZWZhYTI1ZjBhOGE4NGE3MyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.RTms-g8dzOl3WwCeJ7WNLq3i2kXxl3T7gOTa8POcxcw"
                        )
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        val jsonObject = JSONObject(response)
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
                    val jsonObject = JSONObject(response)
                    Log.e("DEBUG_Watch", jsonObject.toString())


                    val backdrop_Url =
                        "https://image.tmdb.org/t/p/w500${jsonObject.getString("backdrop_path")}"
                    val poster_Url =
                        "https://image.tmdb.org/t/p/w500${jsonObject.getString("poster_path")}"

                    val original_title: String
                    val overview: String
                    val release_date: String
                    val runtime: String
                    val vote_average: String
                    val genres: String
                    val production_C:String
                    val PG: String
                    var no_of_season: Int = 1
                    val validSeasons = mutableListOf<JSONObject>()


                    original_title = if (jsonObject.optString("name").isNotEmpty()) {
                        jsonObject.optString("name")
                    } else {
                        jsonObject.optString("title")
                    }

                    val adult = jsonObject.getString("adult")
                    if(adult == "true"){
                        PG = "18 +"
                    } else {
                        PG = "13"
                    }

                    release_date = if (jsonObject.optString("release_date").isNotEmpty()) {
                        jsonObject.optString("release_date")
                    } else {
                        jsonObject.optString("first_air_date")
                    }

                    runtime = if (jsonObject.optString("runtime").isNotEmpty()) {
                        jsonObject.optString("runtime")
                    } else {
                        jsonObject.optString("episode_run_time")
                    }

                    overview = jsonObject.getString("overview")

                    vote_average = jsonObject.getString("vote_average")

                    val genresArray = jsonObject.getJSONArray("genres") //[{"id":80,"name":"Crime"},{"id":99,"name":"Documentary"}]
                    val genresList = mutableListOf<String>()
                    for (i in 0 until genresArray.length()) {
                        val genreObject = genresArray.getJSONObject(i)
                        val genreName = genreObject.getString("name")
                        genresList.add(genreName)
                    }
                    genres = genresList.joinToString("   ⬤ ")


                    val production_companies = jsonObject.getJSONArray("production_companies") //[{"id":80,"name":"Crime"},{"id":99,"name":"Documentary"}]
                    val productionList = mutableListOf<String>()
                    for (i in 0 until production_companies.length()) {
                        val productionObject = production_companies.getJSONObject(i)
                        val genreName = productionObject.getString("name")
                        productionList.add(genreName)
                    }
                    production_C = productionList.joinToString("  - ")






                    if (type == "tv") {

                        val seasonsArray = jsonObject.getJSONArray("seasons")


                        for (i in 0 until seasonsArray.length()) {
                            val season = seasonsArray.getJSONObject(i)
                            val airDate = season.optString("air_date", "")

                            if (airDate.isNotEmpty()) {
                                validSeasons.add(season)
                            }
                        }
                        no_of_season = validSeasons.size
                        Log.e("DEBUG_Watch Seasons", validSeasons.toString())
                        Log.e("DEBUG_Watch Seasons no", no_of_season.toString())

                    }

                    withContext(Dispatchers.Main) {
                        val backdrop_Widget = findViewById<ImageView>(R.id.backdropImageView)
                        val poster_widget = findViewById<ImageView>(R.id.posterImageView)
                        val title_widget = findViewById<TextView>(R.id.title_widget)
                        val year_widget = findViewById<TextView>(R.id.year_widget)
                        val Rating_widget = findViewById<TextView>(R.id.Rating_widget)
                        val Overview_widget = findViewById<TextView>(R.id.overview_widget)
                        val Runtime_widget = findViewById<TextView>(R.id.Runtime_widget)
                        val Genres_widget = findViewById<TextView>(R.id.Genres_widget)
                        val Production_widget = findViewById<TextView>(R.id.Production_widget)
                        val PG_widget = findViewById<TextView>(R.id.PG_widget)




                        title_widget.text = original_title
                        year_widget.text = release_date

                        Genres_widget.text = genres
                        Production_widget.text = production_C


                        Rating_widget.text = "${vote_average}/10"
                        Runtime_widget.text = "${runtime} min"
                        PG_widget.text = PG

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


                        val watchButton = findViewById<Button>(R.id.watchNowButton)
                        val FaveButton = findViewById<Button>(R.id.favoriteButton)
                        val TrailerButton = findViewById<Button>(R.id.TrailerButton)



                        watchButton.setOnClickListener {
                            val intent = Intent(this@Watch_Page, Play::class.java)
                            intent.putExtra("imdb_code", tmdbId)
                            intent.putExtra("type", type)
                            startActivity(intent)
                        }

                        setupExpandableButton(watchButton, 105, 40, "▶ Play", "▶")
                        setupExpandableButton(
                            FaveButton,
                            130,
                            40,
                            "\uD83E\uDD0D Favourite",
                            "\uD83E\uDD0D"
                        )
                        setupExpandableButton(
                            TrailerButton,
                            130,
                            40,
                            "\uD83C\uDFAC Trailer",
                            "\uD83C\uDFAC"
                        )

                        if(type=="tv"){
                            watchButton.visibility = View.GONE
                            val Season_widget = findViewById<LinearLayout>(R.id.Season_widget)
                            Season_widget.visibility = View.VISIBLE

                            val season_count_widget = findViewById<TextView>(R.id.season_count_text)
                            season_count_widget.text = "$no_of_season Seasons"
                            createSeasonButtons( no_of_season, validSeasons, tmdbId)
                        }

                    }



                    Cast_Data(tmdbId.toString(), type.toString())
                    Watch_Recomendation_Data(tmdbId.toString(), type.toString())
                    break

                } catch (e: Exception) {
                    Log.e("DEBUG_WATCH", "Error fetching data", e)
                }
            }
        }
    }

    private fun createSeasonButtons(noOfSeasons: Int, seasonData : MutableList<JSONObject> , seasonID: String) {
        val container = findViewById<LinearLayout>(R.id.season_selector_container)
        container.removeAllViews() // Clear old buttons if any
        var track = 0
        while (track < noOfSeasons){

            val selectedSeason = seasonData[track]
            val season_no = selectedSeason.optInt("season_number")
            val s_name: String


            if(season_no==0){
                s_name = selectedSeason.getString("name")
            } else{
                s_name = "Season $season_no"
            }

            val SeasonButton = Button(this).apply {
                text = s_name
                textSize = 14f
                isFocusable = true
                isFocusableInTouchMode = true
                isClickable = true
                setTypeface(typeface, Typeface.BOLD) //textStyle =  bold
                stateListAnimator = null

                background = ContextCompat.getDrawable(context, R.drawable.tv_button_selector)

                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(120), // 120dp converted to pixels
                    dpToPx(48)   // 48dp converted to pixels
                ).apply {
                    marginEnd = dpToPx(8) // 8dp margin at the end
                }

                setTextColor(ContextCompat.getColor(context, android.R.color.white))
                //setBackgroundColor(Color.parseColor("#3D5AFE"))
            }

            SeasonButton.setOnClickListener {
                ShowSeasonEpisodes(season_no,  seasonData, seasonID)
            }

            container.addView(SeasonButton)
            track++
        }

    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    @SuppressLint("MissingInflatedId", "SetTextI18n")
    private fun ShowSeasonEpisodes(SelectedSeasons: Int, seasonData : MutableList<JSONObject>, seriesId: String){

        val recyclerView = findViewById<RecyclerView>(R.id.episodes_recycler)
        recyclerView.layoutManager = GridLayoutManager(this@Watch_Page, 5)
        recyclerView.removeAllViews()
        try {
                val selectedSeason = seasonData[SelectedSeasons]
                val episodeCount =  selectedSeason.optInt("episode_count", 0) // selectedSeason.getString("episode_count")
                val current_season_title = findViewById<TextView>(R.id.current_season_title)

                Log.e("DEBUG_Episodes", selectedSeason.toString())


                current_season_title.text = "Season $SelectedSeasons"
                findViewById<TextView>(R.id.episode_count_text).text = "$episodeCount Episodes"
                findViewById<TextView>(R.id.season_year_text).text =  selectedSeason.getString("air_date").substring(0, 4)


                CoroutineScope(Dispatchers.IO).launch {
                    val episodes = mutableListOf<JSONObject>()

                    for (i in 1..episodeCount) {
                        val url =
                            "https://api.themoviedb.org/3/tv/$seriesId/season/${SelectedSeasons}/episode/$i?language=en-US"
                        val connection = URL(url).openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.setRequestProperty("accept", "application/json")
                        connection.setRequestProperty(
                            "Authorization",
                            "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJhZjliMmUyN2MxYTZiYzMyMzNhZjE4MzJmNGFjYzg1MCIsIm5iZiI6MTcxOTY3NDUxNy4xOTYsInN1YiI6IjY2ODAyNjk1ZWZhYTI1ZjBhOGE4NGE3MyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.RTms-g8dzOl3WwCeJ7WNLq3i2kXxl3T7gOTa8POcxcw"
                        )
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        val jsonObject = JSONObject(response)



                        jsonObject.put("episode_number", i)
                        jsonObject.remove("crew")
                        jsonObject.remove("guest_stars")
                        jsonObject.remove("cast")


                        Log.e("DEBUG_Each Updated ", jsonObject.toString())
                        episodes.add(jsonObject)
                    }


                    withContext(Dispatchers.Main) {
                        recyclerView.adapter = EpisodesAdapter(episodes,seriesId, "tv" )
                    }
                }
        }catch (e :Exception){

            Log.e("DEBUG_Each Error ", "", e)
            Log.e("DEBUG_Each E--- S", SelectedSeasons.toString())
            Log.e("DEBUG_Each E--- Data", seasonData.toString())
            Log.e("DEBUG_Each E--- id", seriesId)

        }


    }


    private fun Cast_Data(show_id: String, type: String) {
        CoroutineScope(Dispatchers.IO).launch {
            while(true) {
                try {
                    val url = if (type == "movie") {
                        "https://api.themoviedb.org/3/movie/${show_id}/credits?language=en-US"
                    } else {
                        "https://api.themoviedb.org/3/tv/${show_id}/credits?language=en-US"
                    }

                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("accept", "application/json")
                    connection.setRequestProperty(
                        "Authorization",
                        "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJhZjliMmUyN2MxYTZiYzMyMzNhZjE4MzJmNGFjYzg1MCIsIm5iZiI6MTcxOTY3NDUxNy4xOTYsInN1YiI6IjY2ODAyNjk1ZWZhYTI1ZjBhOGE4NGE3MyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.RTms-g8dzOl3WwCeJ7WNLq3i2kXxl3T7gOTa8POcxcw"
                    )

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)

                    Log.e("DEBUG_WATCH_RECO", jsonObject.toString())
                    val moviesArray = jsonObject.getJSONArray("cast")



                    Log.e("DEBUG_WATCH_Results", jsonObject.toString())

                    val movies = mutableListOf<MovieItem>()

                    for (i in 0 until moviesArray.length()) {
                        val item = moviesArray.getJSONObject(i)
                        val title = item.getString("original_name")
                        val imgUrl = "https://image.tmdb.org/t/p/w500" + item.getString("profile_path")
                        val cast_id = item.getString("id")
                        val type = ""
                        movies.add(MovieItem(title, imgUrl, cast_id, type))
                    }

                    Log.e("DEBUG_WATCH_RECO", movies.toString())


                    withContext(Dispatchers.Main) {
                        val recyclerView = findViewById<RecyclerView>(R.id.Cast_widget)
                        recyclerView.layoutManager = LinearLayoutManager(
                            this@Watch_Page,
                            LinearLayoutManager.HORIZONTAL, // 👈 makes it horizontal
                            false
                        )
                        recyclerView.adapter = GridAdapter(movies,  R.layout.round_grid)
                        val spacing = (9 * resources.displayMetrics.density).toInt() // 16dp to px
                        recyclerView.addItemDecoration(EqualSpaceItemDecoration(spacing))
                    }


                    break
                } catch (e: Exception) {
                    delay(10_000)
                    Log.e("DEBUG_WATCH_RECO", "Error fetching data", e)
                    break
                }
            }
        }
    }

    private fun Watch_Recomendation_Data(show_id: String, type: String) {
        CoroutineScope(Dispatchers.IO).launch {
            while(true) {
                try {

                    val url = if (type == "tv") {
                         "https://api.themoviedb.org/3/tv/${show_id}/recommendations?language=en-US&page=1"
                    } else {
                         "https://api.themoviedb.org/3/movie/${show_id}/recommendations?language=en-US&page=1"
                    }

                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("accept", "application/json")
                    connection.setRequestProperty(
                        "Authorization",
                        "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJhZjliMmUyN2MxYTZiYzMyMzNhZjE4MzJmNGFjYzg1MCIsIm5iZiI6MTcxOTY3NDUxNy4xOTYsInN1YiI6IjY2ODAyNjk1ZWZhYTI1ZjBhOGE4NGE3MyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.RTms-g8dzOl3WwCeJ7WNLq3i2kXxl3T7gOTa8POcxcw"
                    )

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)

                    Log.e("DEBUG_WATCH_RECO", jsonObject.toString())
                    val moviesArray = jsonObject.getJSONArray("results")

                    val dmoviesArray = if (moviesArray.length() ==  0){
                            val fallback = """[{"adult":false,"backdrop_path":"/7HqLLVjdjhXS0Qoz1SgZofhkIpE.jpg","id":1087192,"title":"How to Train Your Dragon","original_title":"How to Train Your Dragon","overview":"On the rugged isle of Berk, where Vikings and dragons have been bitter enemies for generations, Hiccup stands apart, defying centuries of tradition when he befriends Toothless, a feared Night Fury dragon. Their unlikely bond reveals the true nature of dragons, challenging the very foundations of Viking society.","poster_path":"\/q5pXRYTycaeW6dEgsCrd4mYPmxM.jpg","media_type":"movie","original_language":"en","genre_ids":[14,10751,28,12],"popularity":261.0336,"release_date":"2025-06-06","video":false,"vote_average":8.022,"vote_count":1651},{"adult":false,"backdrop_path":"\/zNriRTr0kWwyaXPzdg1EIxf0BWk.jpg","id":1234821,"title":"Jurassic World Rebirth","original_title":"Jurassic World Rebirth","overview":"Five years after the events of Jurassic World Dominion, covert operations expert Zora Bennett is contracted to lead a skilled team on a top-secret mission to secure genetic material from the world's three most massive dinosaurs. When Zora's operation intersects with a civilian family whose boating expedition was capsized, they all find themselves stranded on an island where they come face-to-face with a sinister, shocking discovery that's been hidden from the world for decades.","poster_path":"\/1RICxzeoNCAO5NpcRMIgg1XT6fm.jpg","media_type":"movie","original_language":"en","genre_ids":[878,12,28],"popularity":554.8251,"release_date":"2025-07-01","video":false,"vote_average":6.375,"vote_count":1645},{"adult":false,"backdrop_path":"\/962KXsr09uK8wrmUg9TjzmE7c4e.jpg","id":1119878,"title":"Ice Road: Vengeance","original_title":"Ice Road: Vengeance","overview":"Big rig ice road driver Mike McCann travels to Nepal to scatter his late brother’s ashes on Mt. Everest. While on a packed tour bus traversing the deadly 12,000 ft. terrain of the infamous Road to the Sky, McCann and his mountain guide encounter a group of mercenaries and must fight to save themselves, the busload of innocent travelers, and the local villagers’ homeland.","poster_path":"\/cQN9rZj06rXMVkk76UF1DfBAico.jpg","media_type":"movie","original_language":"en","genre_ids":[28,53,18],"popularity":106.818,"release_date":"2025-06-27","video":false,"vote_average":6.848,"vote_count":174},{"adult":false,"backdrop_path":"\/7Q2CmqIVJuDAESPPp76rWIiA0AD.jpg","id":1011477,"title":"Karate Kid: Legends","original_title":"Karate Kid: Legends","overview":"After a family tragedy, kung fu prodigy Li Fong is uprooted from his home in Beijing and forced to move to New York City with his mother. When a new friend needs his help, Li enters a karate competition – but his skills alone aren't enough. Li's kung fu teacher Mr. Han enlists original Karate Kid Daniel LaRusso for help, and Li learns a new way to fight, merging their two styles into one for the ultimate martial arts showdown.","poster_path":"\/AEgggzRr1vZCLY86MAp93li43z.jpg","media_type":"movie","original_language":"en","genre_ids":[28,12,18],"popularity":133.6611,"release_date":"2025-05-08","video":false,"vote_average":7.151,"vote_count":687},{"adult":false,"backdrop_path":"\/7Zx3wDG5bBtcfk8lcnCWDOLM4Y4.jpg","id":552524,"title":"Lilo & Stitch","original_title":"Lilo & Stitch","overview":"The wildly funny and touching story of a lonely Hawaiian girl and the fugitive alien who helps to mend her broken family.","poster_path":"\/tUae3mefrDVTgm5mRzqWnZK6fOP.jpg","media_type":"movie","original_language":"en","genre_ids":[10751,878,35,12],"popularity":164.7425,"release_date":"2025-05-17","video":false,"vote_average":7.322,"vote_count":1357}]"""
                            JSONArray(fallback)
                    }else{
                         moviesArray
                    }

                    //Log.e("DEBUG_WATCH_Results", jsonObject.toString())

                    val movies = mutableListOf<MovieItem>()

                    for (i in 0 until dmoviesArray.length()) {
                        val item = dmoviesArray.getJSONObject(i)
                        //val title = item.getString("original_title")

                        val title = if (item.optString("name").isNotEmpty()) {
                            jsonObject.optString("name")
                        } else {
                            jsonObject.optString("title")
                        }
                        val imgUrl = "https://image.tmdb.org/t/p/w500" + item.getString("poster_path")
                        val imdb_code = item.getString("id")
                        val type = item.getString("media_type")
                        movies.add(MovieItem(title, imgUrl, imdb_code, type))
                    }

                    Log.e("DEBUG_WATCH_RECO", movies.toString())


                    withContext(Dispatchers.Main) {
                        val recyclerView = findViewById<RecyclerView>(R.id.Recommendation_widget)
                        recyclerView.layoutManager = LinearLayoutManager(
                            this@Watch_Page,
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
                    Log.e("DEBUG_WATCH_RECO", "Error fetching data", e)
                    break
                }
            }
        }
    }

    fun setupExpandableButton(
        button: Button,
        expandedWidthDp: Int,
        collapsedWidthDp: Int,
        expandedText: String,
        collapsedText: String
    ) {
        button.setOnFocusChangeListener { _, hasFocus ->
            val params = button.layoutParams
            if (hasFocus) {
                button.text = expandedText
                params.width = expandedWidthDp.dpToPx(button.context)
            } else {
                button.text = collapsedText
                params.width = collapsedWidthDp.dpToPx(button.context)
            }
            button.layoutParams = params
        }
    }

    // dp → px converter
    fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

}
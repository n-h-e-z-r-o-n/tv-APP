package com.example.onyx

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
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
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import org.json.JSONObject
import java.time.LocalDate
import kotlin.text.ifEmpty

class Watch_Page : AppCompatActivity() {
    
    private var currentServerIndex = 0
    private val servers = listOf(
        "VidSrc.to",
        "Embed API Stream",
        "2Embed",
        "Embed.su",
        "PrimeWire",
        "vidking"
    )

    private lateinit var episodes_recycler : RecyclerView


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        GlobalUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_watch_page)


        episodes_recycler = findViewById<RecyclerView>(R.id.episodes_recycler)
        //episodes_recycler.layoutManager = GridLayoutManager(this@Watch_Page, 4)
        episodes_recycler.layoutManager = LinearLayoutManager(
            this@Watch_Page,
            LinearLayoutManager.HORIZONTAL,
            false
        )


        // Get extras from Intent
        val imdbCode = intent.getStringExtra("imdb_code")
        val type = intent.getStringExtra("type")
        if(!imdbCode.isNullOrEmpty()){
            fetchData(imdbCode.toString(), type.toString())
        }else{
            fetchData("63926 ", "tv")
        }

    }



    @RequiresApi(Build.VERSION_CODES.O)
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


                    val backdrop_Url = if (jsonObject.has("backdrop_path") && !jsonObject.isNull("backdrop_path")) {
                        "https://image.tmdb.org/t/p/w1280${jsonObject.getString("backdrop_path")}"
                    } else if (jsonObject.has("poster_path") && !jsonObject.isNull("poster_path")) {
                        "https://image.tmdb.org/t/p/w780${jsonObject.getString("poster_path")}"
                    } else { "" }

                    val poster_Url =
                        "https://image.tmdb.org/t/p/w780${jsonObject.getString("poster_path")}"

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


                    original_title = jsonObject.optString("name").ifEmpty {
                        jsonObject.optString("title")
                    }

                    val adult = jsonObject.getString("adult")
                    PG = if(adult == "true"){
                        "18 +"
                    } else {
                        "13"
                    }

                    release_date = jsonObject.optString("release_date").ifEmpty {
                        jsonObject.optString("first_air_date")
                    }.substring(0, 4)



                    runtime = if (jsonObject.has("runtime") && !jsonObject.isNull("runtime")) {
                        val runtimeInt = jsonObject.optInt("runtime", 0)
                        if (runtimeInt > 0) GlobalUtils.formatRuntime(runtimeInt) else ""
                    } else {
                        val arr = jsonObject.optJSONArray("episode_run_time")
                        val runtimeInt = if (arr != null && arr.length() > 0) arr.optInt(0) else 0
                        if (runtimeInt > 0) GlobalUtils.formatRuntime(runtimeInt) else ""
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
                    genres = genresList.joinToString("  -  ")


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


                        val watchButton = findViewById<ImageButton>(R.id.watchNowButton)
                        val FaveButton = findViewById<ImageButton>(R.id.favoriteButton)
                        val TrailerButton = findViewById<ImageButton>(R.id.TrailerButton)
                        val serverButton = findViewById<ImageButton>(R.id.serverButton)

                        watchButton.setOnFocusChangeListener { v, hasFocus ->
                            if (hasFocus) {
                                TooltipCompat.setTooltipText(v, "Play")  // ensure tooltip text is set
                                v.post {
                                    v.performLongClick() // 👈 This forces the tooltip to show
                                }
                            }
                        }
                        FaveButton.setOnFocusChangeListener { v, hasFocus ->
                            if (hasFocus) {
                                v.post {
                                    v.performLongClick() // 👈 This forces the tooltip to show
                                }
                            }
                        }
                        TrailerButton.setOnFocusChangeListener { v, hasFocus ->
                            if (hasFocus) {
                                v.post {
                                    v.performLongClick() // 👈 This forces the tooltip to show
                                }
                            }
                        }
                        serverButton.setOnFocusChangeListener { v, hasFocus ->
                            if (hasFocus) {
                                v.post {
                                    v.performLongClick() // 👈 This forces the tooltip to show
                                }
                            }
                        }



                        watchButton.setOnClickListener {
                            val intent = Intent(this@Watch_Page, Play::class.java)
                            intent.putExtra("imdb_code", tmdbId)
                            intent.putExtra("type", type)
                            intent.putExtra("server", servers[currentServerIndex])
                            startActivity(intent)
                        }

                        serverButton.setOnClickListener {
                            showServerDialog()
                        }



                        setupFavoriteButton(
                            button = FaveButton,
                            data = jsonObject
                        )

                        if(type=="tv"){
                            watchButton.visibility = View.GONE
                            val Season_widget = findViewById<LinearLayout>(R.id.Season_widget)
                            Season_widget.visibility = View.VISIBLE

                            val season_count_widget = findViewById<TextView>(R.id.season_count_text)
                            season_count_widget.text = "$no_of_season Seasons"
                            createSeasonButtons( no_of_season, validSeasons, tmdbId, jsonObject)
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



    private var selectedSeasonButton: Button? = null

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createSeasonButtons(
        noOfSeasons: Int,
        seasonData: MutableList<JSONObject>,
        seasonID: String,
        seasonAllData: JSONObject
    ) {
        val container = findViewById<LinearLayout>(R.id.season_selector_container)
        container.removeAllViews()

        var track = 0
        var firstButton: Button? = null  // 👈 Keep a reference to the first

        while (track < noOfSeasons) {
            val selectedSeason = seasonData[track]
            val season_no = selectedSeason.optInt("season_number")
            val s_name = if (season_no == 0) {
                selectedSeason.getString("name")
            } else {
                "Season $season_no"
            }

            val seasonButton = Button(this).apply {
                text = s_name
                textSize = 14f
                isFocusable = true
                isFocusableInTouchMode = true
                isClickable = true
                setTypeface(typeface, Typeface.BOLD)
                stateListAnimator = null
                background = ContextCompat.getDrawable(context, R.drawable.tv_button_selector)
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(120),
                    dpToPx(48)
                ).apply { marginEnd = dpToPx(8) }
                setTextColor(resolveAttrColor(context, R.attr.FG_color))
            }

            seasonButton.setOnClickListener {
                selectedSeasonButton?.let { previous ->
                    previous.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.tv_button_selector
                    )
                    previous.setTextColor(resolveAttrColor(this, R.attr.FG_color))
                }

                seasonButton.setTextColor(resolveAttrColor(this, R.attr.AccentColor))
                selectedSeasonButton = seasonButton

                seasonButton.isEnabled = false
                ShowSeasonEpisodes(season_no, seasonData, seasonID, seasonAllData)
                seasonButton.postDelayed({ seasonButton.isEnabled = true }, 3000)
            }

            // DPAD navigation
            seasonButton.setOnKeyListener { v, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN) {
                    val index = container.indexOfChild(v)
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (index > 0) container.getChildAt(index - 1).requestFocus()
                            return@setOnKeyListener true
                        }

                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (index < container.childCount - 1) container.getChildAt(index + 1).requestFocus()
                            return@setOnKeyListener true
                        }

                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            val episodesRecycler = findViewById<RecyclerView>(R.id.episodes_recycler)
                            val castRecycler = findViewById<RecyclerView>(R.id.Cast_widget)
                            val recommendationRecycler = findViewById<RecyclerView>(R.id.Recommendation_widget)

                            when {
                                episodesRecycler.childCount > 0 -> episodesRecycler.getChildAt(0)?.requestFocus()
                                castRecycler.childCount > 0 -> castRecycler.getChildAt(0)?.requestFocus()
                                recommendationRecycler.childCount > 0 -> recommendationRecycler.getChildAt(0)?.requestFocus()
                            }
                            return@setOnKeyListener true
                        }

                        KeyEvent.KEYCODE_DPAD_UP -> {
                            findViewById<ImageButton>(R.id.serverButton)?.requestFocus()
                            return@setOnKeyListener true
                        }
                    }
                }
                false
            }

            container.addView(seasonButton)

            if (track == 0) {
                firstButton = seasonButton  // 👈 store reference to first button
            }

            track++
        }

        // 👇 Auto-click the first button after layout is done
        firstButton?.post {
            firstButton?.performClick()
            firstButton?.requestFocus()  // optional: also focus it visually
        }
    }



    //setBackgroundColor(Color.parseColor("#3D5AFE"))
    private fun resolveAttrColor(context: Context, attr: Int): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ShowSeasonEpisodes(SelectedSeasons: Int, seasonData : MutableList<JSONObject>, seriesId: String, seasonAllData:  JSONObject) {

        LoadingAnimation.setup(this, R.raw.grey)
        LoadingAnimation.show(this)

        val overviewWidget = findViewById<TextView>(R.id.overview_widget)
        val ratingWidget = findViewById<TextView>(R.id.Rating_widget)
        val posterWidget = findViewById<ImageView>(R.id.posterImageView)
        val backdropWidget = findViewById<ImageView>(R.id.backdropImageView)
        val season_CWidget = findViewById<TextView>(R.id.season_C)


        val currentSeasonTitle = findViewById<TextView>(R.id.current_season_title)
        val episodeCountText = findViewById<TextView>(R.id.episode_count_text)
        val seasonYearText = findViewById<TextView>(R.id.season_year_text)

        //Log.e("DEBUG_Each Selecteds", SelectedSeasons.toString())
        //Log.e("DEBUG_Each seasonData", seasonData.toString())

        // val selectedSeason = seasonData[SelectedSeasons]
        val selectedSeason = seasonData.firstOrNull {
            it.optInt("season_number") == SelectedSeasons
        }
        if (selectedSeason == null) {
            return
        }

        val episodeCount = selectedSeason.optInt("episode_count", 0)
        val airDate = selectedSeason.optString("air_date", "")
        var selectedSeasonPoster = selectedSeason.optString("poster_path", "")
        val selectedSeasonOverview = selectedSeason.optString("overview", "")
        val selectedSeasonNumber = selectedSeason.optString("season_number", "")
        val selectedSeasonRating = selectedSeason.optDouble("vote_average", 0.0)
        var stillPath = selectedSeason.optString("still_path", "")

        Log.e("DEBUG_Each  stillPath", stillPath.toString())





        currentSeasonTitle.text = "Season $SelectedSeasons"
        season_CWidget.text = "Season $SelectedSeasons"
        episodeCountText.text = "$episodeCount Episodes"
        seasonYearText.text = airDate.take(4)


        ratingWidget.text = "$selectedSeasonRating/10"

        if (selectedSeasonOverview !== "") {
            overviewWidget.text = selectedSeasonOverview
        }
        if (selectedSeasonPoster !== "") {
            selectedSeasonPoster = "https://image.tmdb.org/t/p/w780$selectedSeasonPoster"
            Glide.with(posterWidget.context)
                .load(selectedSeasonPoster)
                .centerCrop()
                .into(posterWidget)
        }




        Log.e("DEBUG_Each E--- S 1", seasonData.toString())
        Log.e("DEBUG_Each E--- S 2", seasonAllData.toString())

        val tempImg = seasonAllData.getString("backdrop_path")


        CoroutineScope(Dispatchers.IO).launch {
            while (true) {

                try {

                    val url =
                        "https://api.themoviedb.org/3/tv/$seriesId/season/${SelectedSeasons}?language=en-US"
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("accept", "application/json")
                    connection.setRequestProperty(
                        "Authorization",
                        "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJhZjliMmUyN2MxYTZiYzMyMzNhZjE4MzJmNGFjYzg1MCIsIm5iZiI6MTcxOTY3NDUxNy4xOTYsInN1YiI6IjY2ODAyNjk1ZWZhYTI1ZjBhOGE4NGE3MyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.RTms-g8dzOl3WwCeJ7WNLq3i2kXxl3T7gOTa8POcxcw"
                    )
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)


                    val episodesArray = jsonObject.getJSONArray("episodes")
                    val today = LocalDate.now()

                    Log.e("DEBUG_Each E json", jsonObject.toString())
                    Log.e("DEBUG_Each E data", episodesArray.toString())
                    Log.e("DEBUG_Each E leng", "${episodesArray.length()}")


                    val episodesList = mutableListOf<EpisodeItem>()
                    for (i in 0 until episodesArray.length()) {
                        val episodes = episodesArray.getJSONObject(i)


                        /*
                        if (episodes.optString("still_path", "").isBlank() || episodes.optString("still_path", "").equals("null", true) ||
                            episodes.optString("runtime", "").isBlank() || episodes.optString("runtime", "").equals("null", true)) {
                            continue
                        }

                         */

                        if (airDate.isNullOrEmpty() || airDate.equals("null", true) || episodeCount == 0) {
                            continue // Skip unreleased
                        }

                        val stillPathRaw = episodes.optString("still_path", "")
                        val runtimeRaw = episodes.optString("runtime", "")

                        val stillPath = if (stillPathRaw.isNullOrEmpty() || runtimeRaw == "null") tempImg  else stillPathRaw
                        val runtime = if (runtimeRaw.isNullOrEmpty() || runtimeRaw == "null") "0" else runtimeRaw




                        episodesList.add(

                            EpisodeItem(
                                episodesName = episodes.optString("name", ""),
                                episodesImage = stillPath,
                                episodesNumber = episodes.optString("episode_number", ""),
                                episodesRating = episodes.optString("vote_average", "0.0"),
                                episodesRuntime = runtime,
                                episodesDescription = episodes.optString("overview", ""),
                                seriesId = seriesId,
                                seasonNumber = episodes.optString("season_number", ""),
                            )
                        )


                    }


                    withContext(Dispatchers.Main) {
                        Log.e("DEBUG_Each E list", "${episodesList.size}")
                        episodes_recycler.removeAllViews()
                        episodes_recycler.adapter = EpisodesAdapter(episodesList)
                        LoadingAnimation.hide(this@Watch_Page)

                    }
                    break
                } catch (e: Exception) {
                }

            }

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

                    val movies = mutableListOf<CastItem>()

                    for (i in 0 until moviesArray.length()) {
                        val item = moviesArray.getJSONObject(i)
                        val title = item.getString("original_name")
                        val imgUrl = "https://image.tmdb.org/t/p/h632" + item.getString("profile_path")
                        val cast_id = item.getString("id")
                        val type = "Actor"
                        movies.add(CastItem(title, imgUrl, cast_id, type))
                    }



                    withContext(Dispatchers.Main) {
                        val recyclerView = findViewById<RecyclerView>(R.id.Cast_widget)
                        recyclerView.layoutManager = LinearLayoutManager(
                            this@Watch_Page,
                            LinearLayoutManager.HORIZONTAL, // 👈 makes it horizontal
                            false
                        )
                        recyclerView.adapter = CastAdapter(movies,  R.layout.round_grid)
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

                        val title = if (item.has("name") && !item.isNull("name")) {
                            item.optString("name")
                        } else {
                            item.optString("title")
                        }
                        //val imgUrl = "https://image.tmdb.org/t/p/w780" + item.getString("backdrop_path")

                        val imgUrl = if (item.has("backdrop_path") && !item.isNull("backdrop_path")) {
                            "https://image.tmdb.org/t/p/w1280${item.getString("backdrop_path")}"
                        } else if (item.has("poster_path") && !item.isNull("poster_path")) {
                            "https://image.tmdb.org/t/p/w780${item.getString("poster_path")}"
                        } else { "" }

                        val imdb_code = item.getString("id")
                        val type = item.getString("media_type")
                        movies.add(MovieItem(title, imgUrl, imdb_code, type))
                    }


                    withContext(Dispatchers.Main) {
                        val recyclerView = findViewById<RecyclerView>(R.id.Recommendation_widget)



                        recyclerView.layoutManager = GridLayoutManager(this@Watch_Page, 3)
                        recyclerView.adapter = RecommendAdapter(movies,  R.layout.recomendation_card)
                        val spacing = (19 * resources.displayMetrics.density).toInt() // 16dp to px
                        recyclerView.addItemDecoration(EqualSpaceItemDecoration(spacing))


                    }


                    break
                } catch (e: Exception) {
                    delay(10_000)
                    break
                }
            }
        }
    }



    private fun setupFavoriteButton(
        button: ImageButton,   // 👈 Changed to ImageButton
        data: JSONObject
    ) {
        val id = data.optString("id")
        val inferredType = if (data.has("first_air_date")) "tv" else "movie"

        @RequiresApi(Build.VERSION_CODES.O)
        fun applyIcon() {
            val isFav = FavoritesManager.isFavorite(this@Watch_Page, id, inferredType)
            if (isFav) {
                button.setImageResource(R.drawable.ic_tickfave)  // ❤️ e.g. filled heart icon
                button.tooltipText = "Remove from Favorites"
            } else {
                button.setImageResource(R.drawable.ic_addfave) // 🤍 outline heart icon
                button.tooltipText = "Add to Favorites"
            }
        }

        applyIcon()

        button.setOnClickListener {
            val isFav = FavoritesManager.isFavorite(this@Watch_Page, id, inferredType)
            if (isFav) {
                FavoritesManager.removeFavorite(this@Watch_Page, id, inferredType)
            } else {
                FavoritesManager.addFavorite(this@Watch_Page, data)
            }
            applyIcon()
        }
    }


    // dp → px converter
    fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    private fun showServerDialog() {
        val builder = android.app.AlertDialog.Builder(this, R.style.CustomDialogTheme)
        builder.setTitle("Select a Streaming Server (Powered by Third Parties)")
            .setSingleChoiceItems(servers.toTypedArray(), currentServerIndex) { dialog, which ->
                currentServerIndex = which
                // Update server button display
                val serverButton = findViewById<ImageButton>(R.id.serverButton)

                Toast.makeText(this, "Server changed to: ${servers[currentServerIndex]}", Toast.LENGTH_SHORT).show()
                dialog.dismiss() // Auto-close dialog when option is selected
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

}
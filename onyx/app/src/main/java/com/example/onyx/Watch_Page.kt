package com.example.onyx

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import android.os.Handler
import android.os.Looper

import org.json.JSONObject
import java.time.LocalDate
import kotlin.text.ifEmpty

import com.example.onyx.FetchData.TMDBapi
import com.example.onyx.Database.AppDatabase
import com.example.onyx.Database.SessionManger
import com.example.onyx.OnyxClasses.CastAdapter
import com.example.onyx.OnyxClasses.CastItem
import com.example.onyx.OnyxClasses.EpisodeItem
import com.example.onyx.OnyxClasses.EpisodesAdapter
import com.example.onyx.OnyxClasses.EqualSpaceItemDecoration
import com.example.onyx.OnyxClasses.MovieItem
import com.example.onyx.OnyxClasses.RecommendAdapter
import com.example.onyx.OnyxObjects.GlobalUtils
import com.example.onyx.OnyxObjects.LoadingAnimation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import android.graphics.Color
import android.view.LayoutInflater
import android.widget.ScrollView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.example.onyx.OnyxObjects.StreamingLinks
import com.example.onyx.OnyxObjects.StreamingLinks.extractStreamFromServer
import com.example.onyx.OnyxObjects.StreamingLinks.getServerUrls
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import com.bumptech.glide.request.target.Target

class Watch_Page : AppCompatActivity() {

    private lateinit var  fetch: TMDBapi
    private lateinit var db: AppDatabase
    private lateinit var  sm: SessionManger

    private var userId: Int = -1
    private var showId: String = ""
    private var showType: String = ""
    private var showTitle: String = ""
    private var showPoster: String = ""
    private var showBackdrop: String = ""
    private var streamLinksFetched = false
    private var currentServerIndex = 0
    private lateinit var episodes_recycler : RecyclerView
    private lateinit var episodesAdapter: EpisodesAdapter
    private lateinit var watchButton : LinearLayout
    private lateinit var faveButton : LinearLayout
    private lateinit var trailerButton :LinearLayout
    private lateinit var serverButton: LinearLayout
    private lateinit var UIsection1: FrameLayout
    private var episodesJob: Job? = null
    private var streamingJob: Job? = null
    private var trailerJob: Job? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        GlobalUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_watch_page)

        setupBackPressedCallback()


        fetch = TMDBapi(this)
        db = AppDatabase(this)
        sm = SessionManger(this)

        userId = sm.getUserId()

        episodes_recycler = findViewById<RecyclerView>(R.id.episodes_recycler)
        episodes_recycler.layoutManager = LinearLayoutManager(
            this@Watch_Page,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        episodesAdapter = EpisodesAdapter(mutableListOf(), db, sm.getUserId())
        episodes_recycler.adapter = episodesAdapter


        //-------- ---------------------------------------------------------------------------------

        UIsection1 = findViewById<FrameLayout>(R.id.widget_1)
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels

        /*
        val params = UIsection1.layoutParams
        params.height = (screenHeight * 1).toInt()
        UIsection1.layoutParams = params
         */

        UIsection1.minimumHeight  = screenHeight

        //-------- ---------------------------------------------------------------------------------


        faveButton = findViewById<LinearLayout>(R.id.favoriteButton)
        watchButton = findViewById<LinearLayout>(R.id.watchNowButton)
        trailerButton = findViewById<LinearLayout>(R.id.TrailerButton)
        serverButton = findViewById<LinearLayout>(R.id.serverButton)

        GlobalUtils.enableFullViewOnDescendantFocus( UIsection1, faveButton )
        GlobalUtils.enableFullViewOnDescendantFocus( UIsection1, serverButton )
        GlobalUtils.enableFullViewOnDescendantFocus( UIsection1, trailerButton )
        GlobalUtils.enableFullViewOnDescendantFocus( UIsection1, watchButton )


        //-------- Get extras from Intent-----------------------------------------------------------
        showId = intent.getStringExtra("imdb_code")?: ""
        showType = intent.getStringExtra("type")?: ""
        Log.e("DEBUG_WATCH", "imdbCode: $showId, type: $showType")

        //------------------------------------------------------------------------------------------
        val continuePlay = intent.getBooleanExtra("continue_play", false)
        if(continuePlay){

            val seasonNumber = intent.getStringExtra("seasonNo") ?: "1"
            val episodeNumber = intent.getStringExtra("EpisodeNo") ?: "1"
            val type = intent.getStringExtra("type") ?: ""
            val title = intent.getStringExtra("title") ?: ""
            val posterUrl = intent.getStringExtra("poster") ?: ""
            val backdropUrl = intent.getStringExtra("backdrop") ?: ""
            val itemId = intent.getStringExtra("imdb_code") ?: ""

            val intent = Intent(this@Watch_Page, Play::class.java)
            intent.putExtra("imdb_code", itemId)
            intent.putExtra("type", type)
            intent.putExtra("title", title)
            intent.putExtra("poster", posterUrl)
            intent.putExtra("backdrop", backdropUrl)
            intent.putExtra("seasonNo", seasonNumber)
            intent.putExtra("episodeNo", episodeNumber)

            startActivity(intent)
        }
        //------------------------------------------------------------------------------------------




        if(showId.isNullOrEmpty()){
            showId = "89456"
            showType = "tv"
        }

        lifecycleScope.launch {

            try {

                val jsonObject = withContext(Dispatchers.IO) {
                    fetch.fetchShowData(showId, showType)
                }

                if (jsonObject == null) {
                    //LoadingAnimation.setup(this@Watch_Page, R.raw.error)
                    return@launch
                }

                showData(jsonObject)

            } catch (e: Exception) {
                Log.e("Watch_Page", "Failed to fetch data", e)
               // LoadingAnimation.setup(this@Watch_Page, R.raw.error)
            }
        }

    }



    private fun showData(jsonObject: JSONObject) {

            // ---------- LOGOS ------------------------------------------------------------------------
            val cShowLogo = findViewById<ImageView>(R.id.cShowLogo)
            val textLogo = findViewById<TextView>(R.id.title_widget)
            fetch.fetchLogos(showType, showId, cShowLogo, textLogo)

            // ---------- DATA -------------------------------------------------------------------------

            Log.e("DEBUG_Watch", jsonObject.toString())


            val overview = jsonObject.optString("overview", "")
            val voteAverage = jsonObject.optString("vote_average", "0")
            val voteCount = jsonObject.optString("vote_count", "0")

            val originalTitle = jsonObject.optString("name").ifEmpty {
                jsonObject.optString("title")
            }


            val releaseDate = jsonObject.optString("release_date").ifEmpty {
                jsonObject.optString("first_air_date")
            }.take(4)

            val PG = if (jsonObject.optBoolean("adult", false)) "18 +" else "13"

            val backdropUrl =
                if (jsonObject.has("backdrop_path") && !jsonObject.isNull("backdrop_path")) {
                    "https://image.tmdb.org/t/p/original/${jsonObject.optString("backdrop_path", "")}"
                } else if (jsonObject.has("poster_path") && !jsonObject.isNull("poster_path")) {
                    "https://image.tmdb.org/t/p/original/${jsonObject.optString("poster_path", "")}"
                } else {
                    ""
                }

            val posterUrl =
                "https://image.tmdb.org/t/p/original/${jsonObject.optString("poster_path", "")}"


            val tmdbId = jsonObject.optString("id")
            showId = tmdbId




            val runtime = if (jsonObject.has("runtime") && !jsonObject.isNull("runtime")) {
                val runtimeInt = jsonObject.optInt("runtime", 0)
                if (runtimeInt > 0) GlobalUtils.formatRuntime(runtimeInt) else ""
            } else {
                val arr = jsonObject.optJSONArray("episode_run_time")
                val runtimeInt =
                    if (arr != null && arr.length() > 0) arr.optInt(0) else 0
                if (runtimeInt > 0) GlobalUtils.formatRuntime(runtimeInt) else ""
            }

            val genresArray =
                jsonObject.getJSONArray("genres") //[{"id":80,"name":"Crime"},{"id":99,"name":"Documentary"}]
            val genresList = mutableListOf<String>()
            for (i in 0 until genresArray.length()) {
                val genreObject = genresArray.getJSONObject(i)
                val genreName = genreObject.getString("name")
                genresList.add(genreName)
            }

            val genres = jsonObject.optJSONArray("genres")
                ?.let { array ->
                    (0 until array.length()).mapNotNull {
                        array.optJSONObject(it)?.optString("name")
                    }.joinToString("  -  ")
                } ?: ""


            val productionCompanies = jsonObject.optJSONArray("production_companies")
                ?.let { array ->
                    (0 until array.length()).mapNotNull {
                        array.optJSONObject(it)?.optString("name")
                    }.joinToString("  -  ")
                } ?: ""





            showTitle = originalTitle
            showPoster = posterUrl
            showBackdrop = backdropUrl

            // ---------- UI BINDS -----------------------------------------------------------------

            findViewById<TextView>(R.id.title_widget).text = originalTitle
            findViewById<TextView>(R.id.year_widget).text = releaseDate
            findViewById<TextView>(R.id.overview_widget).text = overview
            findViewById<TextView>(R.id.Genres_widget).text = genres
            findViewById<TextView>(R.id.Production_widget).text = productionCompanies
            findViewById<TextView>(R.id.Rating_widget).text = voteAverage
            findViewById<TextView>(R.id.Runtime_widget).text = "$runtime min"
            findViewById<TextView>(R.id.PG_widget).text = PG

            val poster_widget = findViewById<ImageView>(R.id.posterImageView)
            val backdrop_Widget = findViewById<ImageView>(R.id.backdropImageView)

            extractAndApplyDynamicColor(posterUrl)

            Glide.with(this@Watch_Page)
                .load(GlobalUtils.getOptimizedBackdropUrl(backdropUrl))
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                .centerCrop()
                .into(backdrop_Widget)

            // Start Ken Burns Effect (Slow Zoom & Pan)
            backdrop_Widget.post {
                // Ensure it's slightly zoomed in from the start so edges don't show when panning
                backdrop_Widget.scaleX = 1.05f
                backdrop_Widget.scaleY = 1.05f
                
                val scaleX = android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 1.05f, 1.12f)
                val scaleY = android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.05f, 1.12f)
                val panX = android.animation.PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0f, -30f)
                val panY = android.animation.PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, -10f)

                val animator = android.animation.ObjectAnimator.ofPropertyValuesHolder(backdrop_Widget, scaleX, scaleY, panX, panY)
                animator.duration = 25000 // 25 seconds for a very slow, cinematic movement
                animator.repeatCount = android.animation.ValueAnimator.INFINITE
                animator.repeatMode = android.animation.ValueAnimator.REVERSE
                animator.interpolator = android.view.animation.LinearInterpolator()
                animator.start()
            }
            
            extractAndApplyDynamicColor(posterUrl)

            Glide.with(this@Watch_Page)
                .load(GlobalUtils.getOptimizedPosterUrl(posterUrl))
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                .centerInside()
                .into(poster_widget)


        //------------------------------------------------------------------------------------------

            val validSeasons = mutableListOf<JSONObject>()
            var no_of_season: Int = 0
            var lastSeason: Int = 0
            var lastEpisode: Int = 0

            if (showType == "tv") {
                try {
                    lastEpisode = jsonObject.getJSONObject("last_episode_to_air")
                        .optInt("episode_number", 0)
                    lastSeason = jsonObject.getJSONObject("last_episode_to_air")
                        .optInt("season_number", 0)
                } catch (e: Exception) {}

                val seasonsArray = jsonObject.getJSONArray("seasons")
                for (i in 0 until seasonsArray.length()) {
                    val season = seasonsArray.getJSONObject(i)
                    val airDate = season.optString("air_date", "")

                    if (airDate.isNotEmpty()) {
                        validSeasons.add(season)
                    }
                }

                no_of_season = validSeasons.size



                watchButton.visibility = View.GONE
                val Season_widget = findViewById<LinearLayout>(R.id.Season_widget)
                Season_widget.visibility = View.VISIBLE

                //val season_count_widget = findViewById<TextView>(R.id.season_count_text)
                //season_count_widget.text = "$no_of_season Seasons"
                createSeasonButtons(no_of_season, validSeasons, showId)
            }else{
                //fetchStreamLinks(showId, showType, showTitle, showPoster, showBackdrop, showSno="0", showEno="0")
            }

            // ---------- BUTTONS ------------------------------------------------------------------

            watchButton.setOnClickListener {

                val intent = Intent(this@Watch_Page, Play::class.java)
                intent.putExtra("imdb_code", showId)
                intent.putExtra("type", showType)
                intent.putExtra("title", showTitle)
                intent.putExtra("poster", showPoster)
                intent.putExtra("backdrop", showBackdrop)
                intent.putExtra("seasonNo", "0")
                intent.putExtra("EpisodeNo", "0")
                startActivity(intent)

                /*

                findViewById<FrameLayout>(R.id.stream_main).visibility = View.VISIBLE
                fetchStreamLinks(showId, showType, showTitle, showPoster, showBackdrop, showSno="0", showEno="0")

               */
            }

            serverButton.setOnClickListener {
                showServerDialog()
            }

            trailerButton.setOnClickListener {
                val trailerLabel = findViewById<TextView>(R.id.trailer_text)

                trailerJob?.cancel()
                trailerButton.isEnabled = false
                trailerLabel.text = "Opening..."

                trailerJob = lifecycleScope.launch {
                        val opened = openTrailerInYouTube(showId, showType, showTitle)
                     trailerButton.isEnabled = true

                        if (!opened) {
                            trailerLabel.text = "No Trailer Found"
                            kotlinx.coroutines.delay(2500)
                        }

                        trailerLabel.text = "Trailer"
                    }
            }


            setupFavoriteButton(
                showId = showId,
                type = showType,
                title = originalTitle,
                voteAverage = voteAverage,
                genres = genres,
                overview = overview,
                runtime = runtime,
                year = releaseDate,
                voteCount = voteCount,
                pg = PG,
                poster = posterUrl,
                backdrop = backdropUrl,
                noOfSeason = no_of_season,
                lastSeason = lastSeason,
                lastEpisode = lastEpisode
            )

            //--------------------------------------------------------------------------------------

            //LoadingAnimation.hide(this@Watch_Page)
            Cast_Data(showId.toString(), showType.toString())
            Watch_Recomendation_Data(showId.toString(), showType.toString())

    }

    private fun extractAndApplyDynamicColor(imageUrl: String) {
        GlobalUtils.extractDynamicColor(this@Watch_Page, imageUrl) { color ->
            val mainBox = findViewById<ScrollView>(R.id.mainBox)
            val blurContainerLeft = findViewById<LinearLayout>(R.id.blurContainer_left)
            val blurContainerBottom = findViewById<LinearLayout>(R.id.blurContainer_bottom)

            mainBox.setBackgroundColor(color)

            // Create a smooth gradient fading left to right
            val baseColor = (color and 0x00FFFFFF) or -0x1000000
            val gradientDrawableLeft = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(baseColor, Color.TRANSPARENT, Color.TRANSPARENT)
            )

            val gradientDrawableBottom = android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.BOTTOM_TOP,
                intArrayOf(baseColor, Color.TRANSPARENT, Color.TRANSPARENT)
            )

            blurContainerLeft.background = gradientDrawableLeft
            blurContainerBottom.background = gradientDrawableBottom
        }
    }

    private suspend fun openTrailerInYouTube(showId: String, showType: String, showTitle: String): Boolean {
        val trailerUrl = withContext(Dispatchers.IO) {
            val jsonObject = fetch.fetchVideoData(showId, showType) ?: return@withContext null
            val results = jsonObject.optJSONArray("results") ?: return@withContext null

            val youtubeVideos = (0 until results.length())
                .mapNotNull { results.optJSONObject(it) }
                .filter { it.optString("site").equals("YouTube", ignoreCase = true) }

            val video = youtubeVideos.firstOrNull {
                it.optString("type").equals("Trailer", ignoreCase = true) && it.optBoolean("official")
            } ?: youtubeVideos.firstOrNull {
                it.optString("type").equals("Trailer", ignoreCase = true)
            } ?: youtubeVideos.firstOrNull {
                it.optString("type").equals("Teaser", ignoreCase = true)
            } ?: youtubeVideos.firstOrNull()

            video?.optString("key")
                ?.takeIf { it.isNotBlank() }
                ?.let { "https://www.youtube.com/watch?v=$it" }
                ?: buildYouTubeSearchUrl(showTitle)
        }

        return withContext(Dispatchers.Main) {
            openYouTubeIntent(trailerUrl)
        }
    }

    private fun buildYouTubeSearchUrl(showTitle: String): String? {
        val query = showTitle.trim().takeIf { it.isNotBlank() } ?: return null
        return "https://www.youtube.com/results?search_query=" + Uri.encode("$query trailer")
    }

    private fun openYouTubeIntent(url: String?): Boolean {
        if (url.isNullOrBlank()) {
            return false
        }

        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage("com.google.android.youtube")
        }

        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

        return try {
            startActivity(appIntent)
            true
        } catch (_: Exception) {
            try {
                startActivity(webIntent)
                true
            } catch (error: Exception) {
                Log.e("Watch_Page", "Unable to open trailer", error)
                Toast.makeText(this@Watch_Page, "Unable to open trailer", Toast.LENGTH_SHORT).show()
                false
            }
        }
    }



    override fun onDestroy() {
        // Cancel all tracked jobs and coroutines
        trailerJob?.cancel()
        streamingJob?.cancel()
        lifecycleScope.coroutineContext.cancelChildren()

        episodesAdapter.clear()

        super.onDestroy()
    }





    private var selectedSeasonButton: Button? = null


    private fun createSeasonButtons(
        noOfSeasons: Int,
        seasonData: MutableList<JSONObject>,
        seasonID: String,
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
                isAllCaps = false
                typeface = ResourcesCompat.getFont(context, R.font.p)
                background = ContextCompat.getDrawable(context, R.drawable.season_selector)
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(120),
                    dpToPx(38)
                ).apply { marginEnd = dpToPx(0) }

                setTextColor(Color.WHITE)                // android:textColor="#FFFFFF"
                setShadowLayer(
                    2f,                                  // android:shadowRadius="4"
                    1f,                                  // android:shadowDx="1"
                    1f,                                  // android:shadowDy="3"
                    Color.BLACK                          // android:shadowColor="#000000"
                )
            }

            seasonButton.setOnClickListener {

                if (selectedSeasonButton == seasonButton) return@setOnClickListener

                selectedSeasonButton?.isSelected = false
                seasonButton.isSelected = true


                selectedSeasonButton = seasonButton
                ShowSeasonEpisodes(season_no, seasonData, seasonID)
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
                    }
                }
                false
            }

            container.addView(seasonButton)

            GlobalUtils.enableFullViewOnDescendantFocus( UIsection1, seasonButton )

            if (track == 0) {
                firstButton = seasonButton  // 👈 store reference to first button
            }

            track++
        }

        // Auto-click the first button after layout is done

        /*
        firstButton?.post {
            firstButton?.performClick()
            firstButton?.requestFocus()  // optional: also focus it visually
        }
         */

    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }


    private val seasonEpisodesCache = mutableMapOf<String, List<EpisodeItem>>()
    private fun ShowSeasonEpisodes(SelectedSeasons: Int, seasonData : MutableList<JSONObject>, seriesId: String) {


        // Create a unique cache key
        val cacheKey = "${seriesId}_${SelectedSeasons}"
        seasonEpisodesCache[cacheKey]?.let { cachedEpisodes ->
            episodesAdapter.updateData(cachedEpisodes)
            Log.e("DEBUG_Each E", "cache key")
            return
        }



        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE

        val selectedSeason = seasonData.firstOrNull {
            it.optInt("season_number") == SelectedSeasons
        }
        if (selectedSeason == null) {
            return
        }

        val airDate = selectedSeason.optString("air_date", "")
        var selectedSeasonPoster = selectedSeason.optString("poster_path", "")
        val selectedSeasonOverview = selectedSeason.optString("overview", "")
        val selectedSeasonRating = selectedSeason.optDouble("vote_average", 0.0)

        findViewById<TextView>(R.id.season_C).text = "Season $SelectedSeasons"
        findViewById<TextView>(R.id.Rating_widget).text = "$selectedSeasonRating"

        if (selectedSeasonOverview.isNotEmpty()) {
            findViewById<TextView>(R.id.overview_widget).text = selectedSeasonOverview
        }
        if (selectedSeasonPoster.isNotEmpty()) {
            selectedSeasonPoster = "https://image.tmdb.org/t/p/original/$selectedSeasonPoster"
            val posterWidget = findViewById<ImageView>(R.id.posterImageView)
            Glide.with(posterWidget)
                .load(GlobalUtils.getOptimizedPosterUrl(selectedSeasonPoster))
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                .centerCrop()
                .into(posterWidget)
        }

        episodesJob?.cancel()
        episodesJob = lifecycleScope.launch {
            try {
                val episodesList = withContext(Dispatchers.IO) {
                    val list = mutableListOf<EpisodeItem>()
                    val jsonObject = fetch.fetchSeasonInfo(seriesId.toString(), SelectedSeasons.toString())
                    
                    if (jsonObject != null) {
                        val episodesArray = jsonObject.getJSONArray("episodes")

                        for (i in 0 until episodesArray.length()) {
                            val episodes = episodesArray.getJSONObject(i)?: continue

                            val episodesAirDate = episodes.optString("air_date", "")
                            if (episodesAirDate.isNotBlank() && episodesAirDate != "null") {
                                try {
                                    val airLocalDate = LocalDate.parse(episodesAirDate, formatter)
                                    if (airLocalDate.isAfter(today)) continue
                                } catch (_: Exception) {}
                            }

                            val stillPathRaw = episodes.optString("still_path", "")
                            val runtimeRaw = episodes.optString("runtime", "")

                            val stillPath = if (stillPathRaw.isNullOrEmpty() || stillPathRaw  == "null") "" else stillPathRaw
                            val runtime = if (runtimeRaw.isNullOrEmpty() || runtimeRaw == "null") "0" else runtimeRaw

                            list.add(
                                EpisodeItem(
                                    showTitle = showTitle,
                                    showPoster = showPoster,
                                    showBackdrop = showBackdrop,
                                    episodesName = episodes.optString("name", ""),
                                    episodesImage = stillPath,
                                    episodesNumber = episodes.optString("episode_number", ""),
                                    episodesRating = episodes.optString("vote_average", "0.0"),
                                    episodesRuntime = runtime,
                                    episodesDescription = episodes.optString("overview", ""),
                                    seriesId = seriesId,
                                    seasonNumber = episodes.optString("season_number", ""),
                                    parentView = UIsection1
                                )
                            )
                        }
                    }
                    list
                }

                if (episodesList.isNotEmpty()) {
                    seasonEpisodesCache[cacheKey] = episodesList
                    episodesAdapter.updateData(episodesList)
                }
            } catch (e: Exception){
                Log.e("DEBUG_Each E Crush", "${e}")
                Toast.makeText(this@Watch_Page, "Episode $e", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun fetchStreamLinks(
        showId: String, showType: String, showTitle: String,
        showPoster: String, showBackdrop: String, showSno: String, showEno: String
    ) {
        // If already fetched, just focus the first result
        val stream_main = findViewById<FrameLayout>(R.id.stream_main)
        val container_server = findViewById<LinearLayout>(R.id.container_server)
        val container = findViewById<LinearLayout>(R.id.stream_links_container)

        if (streamLinksFetched) {
            if (container.childCount > 0) container.getChildAt(0).requestFocus()
            return
        }

        streamLinksFetched = true

        val loadingImageView = findViewById<ImageView>(R.id.stream_links_animation)
        Glide.with(this@Watch_Page)
            .asGif()
            .load(R.raw.flame_loading)
            .into(loadingImageView)

        container.removeAllViews()
        val inflater = LayoutInflater.from(this@Watch_Page)
        val servers = StreamingLinks.getServerUrls(showId, showType, showSno, showEno)

        // Cap concurrent WebViews at 2 to protect TV RAM
        val semaphore = Semaphore(2)
        var pendingCount = servers.size

        // Register the focus-change listener once upfront
        container.viewTreeObserver.addOnGlobalFocusChangeListener { _, newFocus ->
            stream_main.visibility =
                if (newFocus != null && container.indexOfChild(newFocus) >= 0) View.VISIBLE
                else View.GONE
        }

        // Helper: build and wire a stream button on the Main thread
        fun addStreamButton(serverName: String, streamUrl: com.example.onyx.OnyxObjects.StreamData) {
            val streamBtn = inflater.inflate(R.layout.item_stream, container, false) as LinearLayout
            streamBtn.findViewById<TextView>(R.id.stream_title).text = "$serverName Server"
            streamBtn.findViewById<TextView>(R.id.stream_message).text = showTitle

            streamBtn.setOnClickListener {
                Video_payer.playVideoExternally(
                    this@Watch_Page,
                    streamUrl.url, streamUrl.referer, streamUrl.userAgent,
                    showId, showType, showTitle, showPoster, showBackdrop, showSno, showEno
                )
            }

            streamBtn.setOnKeyListener { v, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN) {
                    val index = container.indexOfChild(v)
                    val count  = container.childCount
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            container.getChildAt((index + 1) % count).requestFocus(); true
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            container.getChildAt((index - 1 + count) % count).requestFocus(); true
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            stream_main.visibility = View.GONE
                            watchButton.requestFocus(); true
                        }
                        else -> false
                    }
                } else false
            }

            container.addView(streamBtn)

            // First result: hide loading and focus automatically
            if (container.childCount == 1) {
                loadingImageView.visibility = View.GONE
                container_server.removeAllViews()
                if (stream_main.visibility == View.VISIBLE) streamBtn.requestFocus()
            }
        }

        // Helper: show "not available" card if nothing found after all servers finish
        fun showUnavailable() {
            loadingImageView.visibility = View.GONE
            if (container.childCount > 0) return  // at least one server worked
            val streamBtn = inflater.inflate(R.layout.item_stream, container, false) as LinearLayout
            streamBtn.findViewById<TextView>(R.id.stream_title).apply {
                text = "VIDEO NOT AVAILABLE"
                setTextColor(Color.RED)
            }
            streamBtn.findViewById<TextView>(R.id.stream_message).text = showTitle
            streamBtn.isClickable = false
            streamBtn.setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
                        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            stream_main.visibility = View.GONE
                            watchButton.requestFocus(); true
                        }
                        else -> false
                    }
                } else false
            }
            container.addView(streamBtn)
        }

        // Launch all servers in parallel on the Main thread (WebView requires Main).
        // The Semaphore caps how many are actively loading at once.
        streamingJob = lifecycleScope.launch {
            val jobs = servers.map { (serverName, webUrl) ->
                async {
                    semaphore.withPermit {
                        try {
                            val streamUrl = StreamingLinks.extractStreamFromServer(
                                this@Watch_Page, webUrl, container_server
                            )
                            if (streamUrl != null) {
                                // Already on Main thread — update UI immediately
                                addStreamButton(serverName, streamUrl)
                                Log.d("Stream-Result", "Server resolved: $serverName")
                            }
                        } catch (e: Exception) {
                            Log.e("Stream-Result", "Error for $serverName: $e")
                        } finally {
                            pendingCount--
                            if (pendingCount <= 0) showUnavailable()
                        }
                    }
                }
            }
            jobs.forEach { it.join() }
        }
    }








    private fun Cast_Data(show_id: String, type: String) {
        lifecycleScope.launch {
            val movies = withContext(Dispatchers.IO) {
                val list = mutableListOf<CastItem>()
                val jsonObject = fetch.fetchShowCast(show_id, type)

                if (jsonObject != null) {
                    val moviesArray = jsonObject.getJSONArray("cast")
                    for (i in 0 until moviesArray.length()) {
                        val item = moviesArray.getJSONObject(i)
                        val title = item.optString("original_name", "")
                        val imgUrl = "https://image.tmdb.org/t/p/original/" + item.optString("profile_path", "")
                        val cast_id = item.optString("id", "")
                        list.add(CastItem(title, imgUrl, cast_id, "Actor"))
                    }
                }
                list
            }

            if (movies.isNotEmpty()) {
                val recyclerView: RecyclerView = if (type == "tv") {
                    findViewById(R.id.Cast_widget_tv)
                } else {
                    findViewById(R.id.Cast_widget_mv)
                }

                recyclerView.visibility = View.VISIBLE
                recyclerView.layoutManager = LinearLayoutManager(
                    this@Watch_Page,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
                recyclerView.adapter = CastAdapter(movies, R.layout.round_grid)
            }
        }
    }

       private fun Watch_Recomendation_Data(show_id: String, type: String) {
        lifecycleScope.launch {
            val movies = withContext(Dispatchers.IO) {
                val list = mutableListOf<MovieItem>()
                val jsonObject = fetch.fetchShowRecommendation(show_id, type)

                if (jsonObject != null) {
                    val moviesArray = jsonObject.getJSONArray("results")
                    for (i in 0 until moviesArray.length()) {
                        val item = moviesArray.getJSONObject(i)

                        val title = if (item.has("name") && !item.isNull("name")) {
                            item.optString("name")
                        } else {
                            item.optString("title")
                        }

                        val imgUrl = if (item.has("backdrop_path") && !item.isNull("backdrop_path")) {
                            "https://image.tmdb.org/t/p/original${item.optString("backdrop_path", "")}"
                        } else if (item.has("poster_path") && !item.isNull("poster_path")) {
                            "https://image.tmdb.org/t/p/original${item.optString("poster_path", "")}"
                        } else { "" }

                        val imdb_code = item.optString("id", "")
                        val recType = item.optString("media_type", "")
                        list.add(MovieItem(title, imgUrl, imdb_code, recType))
                    }
                }
                list
            }

            if (movies.isNotEmpty()) {

                        val recyclerView = findViewById<RecyclerView>(R.id.Recommendation_widget)
                        recyclerView.isNestedScrollingEnabled = false

                        // Calculate number of rows
                        val columns = 3
                        val itemCount = movies.size
                        val rows = Math.ceil(itemCount.toDouble() / columns).toInt()
                        val dpPerRow = 172f
                        val spacingBetweenRows = 19f // dp (from your item decoration)
                        val totalHeightDp = (dpPerRow * rows) + (spacingBetweenRows * (rows - 1))
                        // Convert dp to pixels
                        val density = resources.displayMetrics.density
                        val totalHeightPx = (totalHeightDp * density).toInt()
                        // Set the calculated height
                        recyclerView.layoutParams.height = totalHeightPx
                        recyclerView.requestLayout() // Important: request layout update

                        recyclerView.layoutManager = GridLayoutManager(this@Watch_Page, 3)
                        recyclerView.adapter = RecommendAdapter(movies, R.layout.recomendation_card)
                        val spacing = (19 * resources.displayMetrics.density).toInt() // 16dp to px
                        recyclerView.addItemDecoration(EqualSpaceItemDecoration(spacing))
                }
        }
    }


    private fun setupFavoriteButton(
        showId :String,
        type :String,
        title:String,
        voteAverage :String,
        genres :String,
        overview :String,
        runtime :String,
        year:String,
        voteCount :String,
        pg:String,
        poster:String,
        backdrop:String,
        noOfSeason:Int,
        lastSeason: Int,
        lastEpisode:Int
    ) {

        val faveButtonImg = findViewById<ImageView>(R.id.favoriteButtonImg)
        val userId = sm.getUserId()

        @RequiresApi(Build.VERSION_CODES.O)
        fun applyIcon() {
            lifecycleScope.launch(Dispatchers.Main) {
                val isFav = withContext(Dispatchers.IO) { db.isFavoriteShow(userId, showId, type) }
                if (isFav) {
                    faveButtonImg.setImageResource(R.drawable.ic_tickfave)
                    faveButtonImg.imageTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(this@Watch_Page, R.color.fav))
                } else {
                    faveButtonImg.setImageResource(R.drawable.ic_addfave)
                    faveButtonImg.imageTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(this@Watch_Page, R.color.white))
                }
            }
        }

        applyIcon()

        faveButton.setOnClickListener {
            GlobalUtils.favoritesStateHasChanged  = true
            lifecycleScope.launch(Dispatchers.Main) {
                withContext(Dispatchers.IO) {
                    val isFav = db.isFavoriteShow(userId, showId, type)
                    if (isFav) {
                        db.removeFavoriteShow(userId, showId, type)
                    } else {
                        db.addFavoriteShow(
                            userId = userId,
                            showId = showId,
                            type = type,
                            title = title,
                            rating = voteAverage,
                            genres = genres,
                            overview = overview,
                            runtime = runtime,
                            year = year,
                            voteCount = voteCount,
                            pg = pg,
                            poster = poster,
                            backdrop = backdrop ,
                            noOfSeason = noOfSeason,
                            lastSeason =lastSeason,
                            lastEpisode =lastEpisode
                        )
                    }
                }
                applyIcon()
            }
        }
    }

////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////

    private fun showServerDialog() {

        val servers = StreamingLinks.serversList

        val builder = android.app.AlertDialog.Builder(this, R.style.CustomDialogTheme)
        builder.setTitle("Select a Streaming Server (Powered by Third Parties)")
            .setSingleChoiceItems(servers.toTypedArray(), StreamingLinks.getSavedServerIndex(this)) { dialog, which ->
                // Save selection immediately
                StreamingLinks.saveServerIndex(this, which)
                currentServerIndex = which

                Toast.makeText(this, "Server: ${servers[currentServerIndex]}", Toast.LENGTH_SHORT).show()
                dialog.dismiss() // Auto-close dialog
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                     // Cancel any running coroutines
                     lifecycleScope.coroutineContext.cancelChildren()
                    // If controls are hidden, exit the video player
                    finish()

            }
        })
    }


}

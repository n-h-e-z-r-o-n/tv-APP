package com.example.onyx

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import com.example.onyx.Database.AppDatabase
import com.example.onyx.Database.SessionManger
import com.example.onyx.FetchData.AnimeApi
import com.example.onyx.FetchData.TMDBapi
import com.example.onyx.OnyxClasses.AiringAnimeItem
import com.example.onyx.OnyxClasses.AnimeAiringAdapter
import com.example.onyx.OnyxClasses.EqualSpaceItemDecoration
import com.example.onyx.OnyxObjects.GlobalUtils
import com.example.onyx.OnyxObjects.LoadingAnimation
import kotlinx.coroutines.cancelChildren
import kotlin.collections.mutableListOf


class Watch_Anime_Page : AppCompatActivity() {
    private var urlHome = BuildConfig.A_K
    private lateinit var db: AppDatabase
    private lateinit var  sm: SessionManger
    private lateinit var  fetchAnime: AnimeApi
    private lateinit var  fetchTMDB: TMDBapi

    private  var userId = -1


    private lateinit var poster :String
    private lateinit var animeId :String

    private lateinit var mainSection: FrameLayout

    private lateinit var SeasonIMGArray: MutableList<String>

    private  var screenHeight = 0

    private var selectedSeasonView: FrameLayout? = null





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GlobalUtils.applyTheme(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_watch_anime_page)

        setupBackPressedCallback()

        db = AppDatabase(this)
        sm = SessionManger(this)
        fetchAnime = AnimeApi(this)
        fetchTMDB = TMDBapi(this)

        userId = sm.getUserId()

        val displayMetrics = resources.displayMetrics
        screenHeight = displayMetrics.heightPixels

        //------------------------------------------------------------------------------------------

        mainSection = findViewById(R.id.mainSection)

        //------------------------------------------------------------------------------------------

        val params = mainSection.layoutParams
        params.height = (screenHeight * 1).toInt()
        mainSection.layoutParams = params



        LoadingAnimation.setup(this@Watch_Anime_Page, R.raw.line_loading)
        SeasonIMGArray = mutableListOf<String>()
        //------------------------------------------------------------------------------------------
        animeId = intent.getStringExtra("anime_code")?: ""

        /*
        Log.e("ANIME_Watch id", animeId)
        if (animeId.isNotEmpty()){
            getInfo(animeId)
        }else{
            getInfo("one-punch-man-season-3-19932")
        }
         */

        CoroutineScope(Dispatchers.Main).launch {

            Log.e("ANIME_Watch id", animeId)
            if (animeId.isNotEmpty()){
                getInfo(animeId)
            }else{
                getInfo("one-punch-man-season-3-19932")
            }

        }
    }


    override fun onDestroy() {
        super.onDestroy()

        // Cancel any running coroutines
        lifecycleScope.coroutineContext.cancelChildren()

        // Remove all handler callbacks
        Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null)

        finish()
    }




    private fun getInfo(Id: String){

        LoadingAnimation.show(this@Watch_Anime_Page)

        val jsonObject = fetchAnime.animeInfo(Id)

        Log.e("ANIME_Watch Data", jsonObject.toString())
        if (jsonObject==null){
            return
        }

        val data = jsonObject.getJSONObject("data")

        val id = data.getJSONObject("anime").getJSONObject("info").getString("id")
        poster = data.getJSONObject("anime").getJSONObject("info").getString("poster")

        val anilistId = data.getJSONObject("anime").getJSONObject("info").getString("anilistId")
        val malId = data.getJSONObject("anime").getJSONObject("info").getString("malId")

        val name = data.getJSONObject("anime").getJSONObject("info").getString("name")

        val japaneseName = data.getJSONObject("anime").getJSONObject("moreInfo").getString("japanese")

        val description = data.getJSONObject("anime").getJSONObject("info").getString("description")
        val rating = data.getJSONObject("anime").getJSONObject("info").getJSONObject("stats").getString("rating")
        val quality = data.getJSONObject("anime").getJSONObject("info").getJSONObject("stats").getString("quality")
        val type = data.getJSONObject("anime").getJSONObject("info").getJSONObject("stats").getString("type")
        val duration = data.getJSONObject("anime").getJSONObject("info").getJSONObject("stats").getString("duration")
        val sub = data.getJSONObject("anime").getJSONObject("info").getJSONObject("stats").getJSONObject("episodes").optString("sub", "")
        val dub = data.getJSONObject("anime").getJSONObject("info").getJSONObject("stats").getJSONObject("episodes").optString("dub", "")
        val aired = data.getJSONObject("anime").getJSONObject("moreInfo").getString("aired")
        //val status = data.getJSONObject("anime").getJSONObject("moreInfo").getString("status")
        //val studios = data.getJSONObject("anime").getJSONObject("moreInfo").getString("studios")
        val genresArray = data.getJSONObject("anime").getJSONObject("moreInfo").getJSONArray("genres")
        var genre = ""
        for (i in 0 until genresArray.length()) {
            genre = genre +" ~ " +genresArray.getString(i)
        }

        tmdbRelation(japaneseName, type)

        val  seasons = data.getJSONArray("seasons")?: JSONArray()
        val  relatedAnimes = data.getJSONArray("relatedAnimes")?: JSONArray()
        val  recommendedAnime = data.getJSONArray("recommendedAnimes")?: JSONArray()

        val saveData  = jsonObject.getJSONObject("data")
        saveData.remove("recommendedAnimes")
        saveData.remove("relatedAnimes")
        saveData.remove("mostPopularAnimes")


        findViewById<TextView>(R.id.watchTitle).text = name
        findViewById<TextView>(R.id.watchRating).text = rating
        findViewById<TextView>(R.id.watchRuntime).text = duration
        findViewById<TextView>(R.id.watchType).text = type
        findViewById<TextView>(R.id.watchQuality).text = quality
        findViewById<TextView>(R.id.watchSub).text = sub
        findViewById<TextView>(R.id.watchDub).text = dub
        findViewById<TextView>(R.id.watchYear).text = aired
        findViewById<TextView>(R.id.watchOverview).text = description
        findViewById<TextView>(R.id.watchGenres).text = genre

        val posterWidget = findViewById<ImageView>(R.id.WatchImage)
        Glide.with(posterWidget.context)
            .load(poster)
            .fitCenter()
            .into(posterWidget)


        if (seasons.length() > 0){
            createSeasonButtons(seasons.length(), seasons )
            findViewById<TextView>(R.id.SeasonHeadline).visibility = View.VISIBLE
        }else{
            findViewById<TextView>(R.id.SeasonHeadline).visibility = View.GONE
            findViewById<TextView>(R.id.selected_seasonShow).text = "EPISODES"

            getEpisodes(id)
        }

        showRecommendation(relatedAnimes, recommendedAnime)
        LoadingAnimation.hide(this@Watch_Anime_Page)


        setupFavoriteButton(
            animeId = animeId,
            name = name,
            type = type,
            anilistId = anilistId,
            malId = malId,
            description = description,
            rating = rating,
            quality = quality,
            duration = duration,
            posterF = poster,
            sub=sub,
            dub=dub,
            aired=aired,
            genre=genre,
            seasons = seasons.toString()
        )
    }

    private fun tmdbRelation(animeName:String, animeType:String ) {
        val type = if (animeType.equals("tv", ignoreCase = true)) {
            "tv"
        } else {
            "movie"
        }
        CoroutineScope(Dispatchers.IO).launch {

            val url_s =
                "https://api.themoviedb.org/3/discover/$type?with_keywords=210024|287501&include_adult=true&with_text_query=${animeName}"

            val Connection = URL(url_s).openConnection() as HttpURLConnection

            Connection.requestMethod = "GET"
            Connection.setRequestProperty("accept", "application/json")
            Connection.setRequestProperty(
                "Authorization",
                "Bearer ${BuildConfig.TM_K}"
            )

            val logosResponse = Connection.inputStream.bufferedReader().use { it.readText() }
            val jsonObjectImg = JSONObject(logosResponse)

            val resultArray = jsonObjectImg.getJSONArray("results")

            Log.e("DEBUG_Watch_Images", "$animeName, AType: $animeType, UrlTYPE: $type")
            Log.e("DEBUG_Watch_Images", jsonObjectImg.toString())
            Log.e("DEBUG_Watch_Images", resultArray.toString())

            if(resultArray.length() > 0){
                val item = resultArray.getJSONObject(0)
                val id = item.getString("id")
                val backdrop = item.optString("backdrop_path", item.optString("poster_path", ""))
                val title = item.getString("name")
                //poster = "https://image.tmdb.org/t/p/original/$backdrop"

                SeasonIMGArray.add("https://image.tmdb.org/t/p/original/$backdrop")

                withContext(Dispatchers.Main) {
                    val cShowLogo = findViewById<ImageView>(R.id.cShowLogo)
                    val textLogo = findViewById<TextView>(R.id.watchTitle)
                    fetchTMDB.fetchLogos(type, id, cShowLogo, textLogo)

                    Log.e("DEBUG_Watch_Images", resultArray.toString())

                    val backdrop_Widget = findViewById<ImageView>(R.id.backdropWidget)

                    findViewById<ImageView>(R.id.WatchImage).visibility = View.GONE

                    Glide.with(this@Watch_Anime_Page)
                        .load("https://image.tmdb.org/t/p/original/$backdrop")
                        .centerInside()
                        .into(backdrop_Widget)

                }
            }
        }
    }

    private fun createSeasonButtons(
        noOfSeasons: Int,
        seasonData: JSONArray
    ) {
        val container = findViewById<LinearLayout>(R.id.anime_season_selector_container)
        container.removeAllViews()

        val inflater = LayoutInflater.from(this)

        for (i in 0 until noOfSeasons) {
            val season = seasonData.getJSONObject(i)

            val seasonBtn = inflater.inflate(R.layout.anime_season_item2, container, false) as FrameLayout
            val seasonTitle = seasonBtn.findViewById<TextView>(R.id.SeasonTitle)

            val title = season.optString("title", "Season ${i + 1}")
            val imageUrl = season.optString("poster", "")
            val season_id = season.optString("id", "")

            seasonTitle.text = title

            SeasonIMGArray.add(imageUrl)

            /*
            val params = FrameLayout.LayoutParams(
                GlobalUtils.dpToPx(120, seasonBtn.context),   // width
                GlobalUtils.dpToPx(38, seasonBtn.context),    // height
            )
            params.marginEnd =  GlobalUtils.dpToPx(0, this)
            seasonBtn.layoutParams = params
             */



            /*
            val seasonImage = cardView.findViewById<ImageView>(R.id.SeasonImage)

            if (imageUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(imageUrl)
                    .centerCrop()
                    .into(seasonImage)
            }
             */




            seasonBtn.setOnClickListener {

                selectedSeasonView?.isSelected = false
                seasonBtn.isSelected = true

                selectedSeasonView = seasonBtn

                findViewById<TextView>(R.id.selected_seasonShow).text = "List of episodes ($title)"

                getEpisodes(season_id)
            }

            container.addView(seasonBtn)
            if (i == 0) seasonBtn.performClick()
        }
    }
    @OptIn(UnstableApi::class)
    private fun getEpisodes(seasonId: String){
        val container = findViewById<LinearLayout>(R.id.anime_episodes_selector_container)
        container.removeAllViews()

        val jsonObject = fetchAnime.animeEpisodes(seasonId)

        Log.e("ANIME_Watch EPISODES", jsonObject.toString())
        if (jsonObject==null){
            return
        }

        val data = jsonObject.getJSONObject("data")
        val  episodes = data.getJSONArray("episodes")


        val inflater = LayoutInflater.from(this@Watch_Anime_Page)

        for (i in 0 until episodes.length()) {
            val episode = episodes.getJSONObject(i)

            val cardView = inflater.inflate(R.layout.anime_item_episode, container, false) as CardView
            val epTitle = cardView.findViewById<TextView>(R.id.episode_name)
            val epNumber = cardView.findViewById<TextView>(R.id.episode_Number)
            val epImg = cardView.findViewById<ImageView>(R.id.episode_image)
            val cWatchSeek_bar = cardView.findViewById<SeekBar>(R.id.cWatchSeek_bar)


            /*try{
                val imageUrl = SeasonIMGArray.random()
                Glide.with(this)
                    .load(imageUrl)
                    .centerCrop()
                    .into(epImg)
            }catch (e:Exception){}
             */



            val eTitle = episode.optString("title", "${i + 1}")
            val eNumber = episode.optString("number", "")
            val episodeId = episode.optString("episodeId", "")


            epTitle.text = eTitle
            epNumber.text = "E$eNumber "

            val lastPos = db.getResumePosition(userId, episodeId, "anime").toLong()
            val durationPos = db.getDurationPosition(userId, episodeId, "anime").toLong()
            val progress = ((lastPos.toDouble() / durationPos.toDouble()) * 1000).toInt()
            cWatchSeek_bar.progress = progress.coerceIn(0, 1000)


            cardView.setOnClickListener {
                Log.e("ANIME_episodeId ", "episodeId: $episodeId")

                //Anime_Video_Player.playVideoExternally(this@Watch_Anime_Page, episodeId, eNumber, seasonId, eTitle)
                Anime_Video_Player.playVideoExternally(this@Watch_Anime_Page, episodeId, eNumber, seasonId)
            }
            container.addView(cardView)
        }


    }


    private fun showRecommendation(data: JSONArray, data2: JSONArray) {

        // ✅ Merge the two JSONArrays
        val Airing = JSONArray()
        for (i in 0 until data.length()) {
            Airing.put(data.getJSONObject(i))
        }
        for (i in 0 until data2.length()) {
            Airing.put(data2.getJSONObject(i))
        }

        var RecommendationItems = mutableListOf<AiringAnimeItem>()

        for (i in 0 until Airing.length()) {


            val item = Airing.getJSONObject(i)

            val title = item.getString("name")

            val imageUrl = item.getString("poster")

            val id = item.getString("id")

            val type = item.getString("type")

            val sub = item.getJSONObject("episodes").optString("sub", "")
            val dub = item.getJSONObject("episodes").optString("dub", "")


            RecommendationItems.add(
                AiringAnimeItem(
                    id,
                    title,
                    imageUrl,
                    type,
                    sub,
                    dub
                )
            )

        }


        val recyclerView = findViewById<RecyclerView>(R.id.animeWatchRecommendation)
        recyclerView.layoutManager = GridLayoutManager(this@Watch_Anime_Page, GlobalUtils.calculateSpanCount(this@Watch_Anime_Page, 170))
        recyclerView.adapter = AnimeAiringAdapter(RecommendationItems, R.layout.anime_airing_item)

        val spacing = (19 * resources.displayMetrics.density).toInt() // 16dp to px
        recyclerView.addItemDecoration(EqualSpaceItemDecoration(spacing))

    }


    private fun setupFavoriteButton(
        animeId :String,
        name:String,
        type :String,
        anilistId :String,
        malId :String,
        description:String,
        rating :String,
        quality :String,
        duration :String,
        posterF :String,
        sub:String,
        dub:String,
        aired:String,
        genre:String,
        seasons:String,


    ) {
        val userId = sm.getUserId()
        val favoriteButton =  findViewById<LinearLayout>(R.id.favoriteButton)
        val favoriteButtonImg =  findViewById<ImageView>(R.id.favoriteButtonImg)
        val favoriteButtonText =  findViewById<TextView>(R.id.favoriteButtonText)





        @RequiresApi(Build.VERSION_CODES.O)
        fun applyIcon() {
            val isFav = db.isFavoriteAnime(userId, animeId)
            if (isFav) {
                favoriteButtonImg.setImageResource(R.drawable.ic_tickfave)
                favoriteButtonText.text = "Remove-Fav "
            } else {
                favoriteButtonImg.setImageResource(R.drawable.ic_addfave)
                favoriteButtonText.text= "Add-to-Fav "
            }
        }

        applyIcon()

        favoriteButton.setOnClickListener {
            val isFav = db.isFavoriteAnime(userId, animeId)
            if (isFav) {
                db.removeFavoriteAnime(userId, animeId)
            } else {
                db.addFavoriteAnime(
                    userId,
                    animeId,
                    name,
                    type,
                    anilistId,
                    malId,
                    description,
                    rating,
                    quality,
                    duration,
                    poster,
                    sub,
                    dub,
                    aired,
                    genre,
                    seasons
                )
            }
            applyIcon()
        }
    }


    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {


                // Cancel any running coroutines
                lifecycleScope.coroutineContext.cancelChildren()

                // Remove all handler callbacks
                Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null)


                // If controls are hidden, exit the video player
                finish()

            }
        })
    }



}
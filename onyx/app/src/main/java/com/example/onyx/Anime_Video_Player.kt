

package com.example.onyx

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.media.AudioManager
import android.net.Uri
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.ui.PlayerView
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import com.bumptech.glide.Glide
import org.json.JSONArray

import com.example.onyx.Database.AppDatabase
import com.example.onyx.Database.SessionManger
import com.example.onyx.FetchData.AnimeApi
import com.example.onyx.OnyxObjects.GlobalUtils
import com.example.onyx.OnyxObjects.MiruroScraper
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@UnstableApi
class Anime_Video_Player : AppCompatActivity(), Player.Listener {

    private lateinit var db: AppDatabase
    private lateinit var  sm: SessionManger
    private lateinit var  fetchAnime: AnimeApi
    private  var  userId  = -1
    private var resumePosition: Long = 0L


    private var currentEpisodeId = ""
    private var currentEpisodeNumber  = ""
    private var currentPoster  = ""
    private var currentSeasonId  = ""
    private var currentSeasonTitle  = ""


    private var holdSeasonTitle  = ""
    private var holdPoster  = ""
    private var holdSeasonId  = ""

    private var holdEpisodeNo  = ""
    private val episodeButtons = mutableListOf<FrameLayout>()
    private lateinit var EpisodeContiner: LinearLayout
    private lateinit var SeasonsContainer: LinearLayout
    private lateinit var seasonTitleWidget: TextView
    private var selectedEpisodeView: FrameLayout? = null
    private var isEpisodeLoading = false
    private var isSeasonLoading = false
    private var firstEpisodeLoaded = false
    private var isAutoNextEnabled = true
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private lateinit var playerView: PlayerView
    private lateinit var progressBar: ProgressBar
    private lateinit var overlayContainer: View
    private lateinit var bottomBar: LinearLayout
    private lateinit var moreSectionMenu: FrameLayout
    private lateinit var moreSectionScrollView: ScrollView
    private lateinit var centerOverlay: FrameLayout

    // Control buttons
    private lateinit var videoUrl:String
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnRewind: ImageButton
    private lateinit var btnFastForward: ImageButton
    private lateinit var btnMute: ImageButton
    private lateinit var btnSpeed: MaterialCardView
    private lateinit var btnSpeedText: TextView
    private lateinit var btnSubtitles: ImageButton
    private lateinit var btnQuality: TextView
    private lateinit var btnRefresh: ImageButton
    private lateinit var btnSettings: ImageButton
    private lateinit var btnClose: ImageButton
    private lateinit var btnFullscreen: ImageButton


    // Seek bar and time displays
    private lateinit var seekBar: SeekBar
    private lateinit var txtCurrentTime: TextView
    private lateinit var txtDuration: TextView

    private var exoPlayer: ExoPlayer? = null
    private var isControlsVisible = true
    private var isMenuVisible = true

    private var isMuted = false
    private var currentSpeed = 1.0f
    private var lastTapTime = 0L
    private var tapCount = 0
    private var progressHandler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null
    private var progressUpdateCount = 0

    // Playback speeds
    private val playbackSpeeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    private var currentSpeedIndex = 2 // Default to 1.0x

    // Quality options - will be populated from tracks
    private var qualityOptions = listOf("Auto")
    private var currentQualityIndex = 0

    // Player management variables (merged from PlayerManager)
    private var currentVideoUrl: String? = null
    private var availableQualities: List<String> = listOf("Auto")
    private var pendingEmbedSource: StreamSource? = null
    private var activePlaybackContext: EpisodePlaybackContext? = null
    private var pendingPlaybackContext: EpisodePlaybackContext? = null
    private val animeStreamResolverLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val pendingSource = pendingEmbedSource
            val pendingContext = pendingPlaybackContext
            pendingEmbedSource = null
            pendingPlaybackContext = null

            if (result.resultCode != Activity.RESULT_OK) {
                val failureMessage = result.data
                    ?.getStringExtra(AnimeStreamResolver.EXTRA_ERROR_MESSAGE)
                    ?.takeIf { it.isNotBlank() }
                    ?: "Could not resolve embed stream"
                Toast.makeText(this, failureMessage, Toast.LENGTH_SHORT).show()
                if (pendingSource != null) {
                    Log.e(
                        "ANIME_PLAYER",
                        "Embed resolver failed for ${pendingSource.serverName}: $failureMessage"
                    )
                }
                return@registerForActivityResult
            }

            val data = result.data
            val resolvedUrl = data?.getStringExtra(AnimeStreamResolver.EXTRA_RESOLVED_URL).orEmpty()
            if (resolvedUrl.isBlank()) {
                Toast.makeText(this, "Embed resolver returned no stream", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            val resolvedEpisodeId = data?.getStringExtra(AnimeStreamResolver.EXTRA_EPISODE_ID)
                ?: pendingSource?.episodeId
                ?: currentEpisodeId
            val resolvedServerName = data?.getStringExtra(AnimeStreamResolver.EXTRA_SERVER_NAME)
                ?: pendingSource?.serverName
                ?: ""
            val resolvedCategory = data?.getStringExtra(AnimeStreamResolver.EXTRA_CATEGORY)
                ?: pendingSource?.category
                ?: "sub"
            val resolvedReferer = data?.getStringExtra(AnimeStreamResolver.EXTRA_REFERER)
            val resolvedOrigin = data?.getStringExtra(AnimeStreamResolver.EXTRA_ORIGIN)
            val resolvedUserAgent = data?.getStringExtra(AnimeStreamResolver.EXTRA_USER_AGENT)

            fetchServerSources(
                resolvedEpisodeId,
                resolvedServerName,
                resolvedCategory,
                resolvedUrl,
                resolvedReferer,
                resolvedOrigin,
                resolvedUserAgent,
                sourceType = "resolved",
                playbackContext = pendingContext
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        GlobalUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_anime_video_player)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // Prevent screen from sleeping while this Activity is visible

        db = AppDatabase(this)         // Initialize database
        sm = SessionManger(this)
        fetchAnime = AnimeApi(this)

        userId = sm.getUserId()


        initializeViews()
        setupPlayer()
        setupControls()
        setupGestures()
        setupBackPressedCallback()

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                lifecycleScope.launch(Dispatchers.Main) {
                    if (episodeButtons.isEmpty() && holdSeasonId.isNotBlank()) {
                        getEpisodes(holdSeasonId)
                    } else if (exoPlayer?.playbackState == Player.STATE_IDLE || exoPlayer?.playerError != null) {
                        if (currentEpisodeId.isNotBlank()) {
                            fetchStreamingLinks(currentEpisodeId)
                        }
                    }
                }
            }
        }
        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
        } catch (e: Exception) {
            Log.e("ANIME_PLAYER", "Failed to register network callback", e)
        }
    }

    private suspend fun fetchResumePosition(episodeId: String = currentEpisodeId): Long = withContext(Dispatchers.IO) {
        if (episodeId.isNotBlank()) {
            db.getResumePosition(userId, episodeId, "anime").toLong()
        } else {
            0L
        }
    }

    private fun buildPlaybackContext(episodeId: String): EpisodePlaybackContext {
        return EpisodePlaybackContext(
            episodeId = episodeId,
            episodeNumber = holdEpisodeNo.ifBlank { currentEpisodeNumber },
            seasonId = holdSeasonId.ifBlank { currentSeasonId },
            seasonTitle = holdSeasonTitle.ifBlank { currentSeasonTitle },
            poster = holdPoster.ifBlank { currentPoster }
        )
    }

    private fun applyPlaybackContext(context: EpisodePlaybackContext) {
        currentEpisodeId = context.episodeId
        currentEpisodeNumber = context.episodeNumber
        currentSeasonId = context.seasonId
        currentSeasonTitle = context.seasonTitle
        currentPoster = context.poster
    }

    private fun clearSavedProgressForContext(context: EpisodePlaybackContext?) {
        val playbackContext = context ?: return
        if (userId <= 0 || playbackContext.episodeId.isBlank()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.removeContinueWatching(userId, playbackContext.episodeId, "anime")
            } catch (e: Exception) {
                Log.e("ANIME_PLAYER", "Error clearing progress for ended episode", e)
            }
        }
    }

    private fun initializeViews() {
        playerView = findViewById(R.id.player_view)
        progressBar = findViewById(R.id.progress_bar)
        overlayContainer = findViewById(R.id.overlay_container)
        bottomBar = findViewById(R.id.bottom_bar)
        centerOverlay = findViewById(R.id.center_overlay)

        // Control buttons
        btnPlayPause = findViewById(R.id.btn_play_pause)
        btnRewind = findViewById(R.id.btn_rewind)
        btnFastForward = findViewById(R.id.btn_fast_forward)
        btnMute = findViewById(R.id.btn_mute)
        btnSpeed = findViewById(R.id.btn_speed)
        btnSpeedText = findViewById(R.id.btn_speed_text)
        btnSubtitles = findViewById(R.id.btn_subtitles)
        btnQuality = findViewById(R.id.btn_quality)
        btnRefresh = findViewById(R.id.btn_refresh)
        btnSettings = findViewById(R.id.btn_settings)
        btnClose = findViewById(R.id.btn_close)
        btnFullscreen = findViewById(R.id.btn_fullscreen)

        // Seek bar and time displays
        seekBar = findViewById(R.id.seek_bar)
        txtCurrentTime = findViewById(R.id.txt_current_time)
        txtDuration = findViewById(R.id.txt_duration)

        moreSectionMenu = findViewById(R.id.MoreSection)
        moreSectionScrollView = findViewById(R.id.moreSectionScrollView)
        seasonTitleWidget = findViewById(R.id.PlayingSeasonTitle)
        SeasonsContainer = findViewById(R.id.SeasonsContainer)
        EpisodeContiner = findViewById(R.id.EpisodeContiner)

        val prefs = getSharedPreferences("AnimePrefs", Context.MODE_PRIVATE)
        isAutoNextEnabled = prefs.getBoolean("isAutoNextEnabled", true)
        val btnAutoNext = findViewById<TextView>(R.id.btn_autoNext)
        btnAutoNext.text = if (isAutoNextEnabled) "Auto-next: On" else "Auto-next: Off"
    }



    private fun setupPlayer() {


        val seasonId = intent.getStringExtra("seasonId")
        val episodeId = intent.getStringExtra("episodeId")
        val episodesNumber = intent.getStringExtra("episodesNumber")

        Log.e("ANIME_Player_SId", "${seasonId}")
        Log.e("ANIME_Player_EId", "${episodeId}")
        Log.e("ANIME_Player_EN", "${episodesNumber}")


        currentEpisodeId = episodeId.toString()
        currentEpisodeNumber = episodesNumber.toString()
        currentSeasonId = seasonId.toString()

        showData(seasonId.toString())

    }

    private fun showData(SeasonId: String){
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val jsonObject = withContext(Dispatchers.IO) { fetchAnime.animeInfo(SeasonId)}

                if (jsonObject==null) return@launch

                val data =  jsonObject.getJSONObject("data")
                val name = data.getString("name")
                val seasons = data.getJSONArray("seasons")?: JSONArray()
                val poster = data.getString("poster")

                seasonTitleWidget.text  = name

                currentPoster = poster
                currentSeasonTitle = name

                //fetchStreamingLinks(currentEpisodeId) //start playing the selected episode video

                val inflater = LayoutInflater.from(this@Anime_Video_Player)
                Log.e("ANIME_Player_SN", "${seasons.length()}")
                if (seasons.length() > 0){
                    SeasonsContainer.removeAllViews()
                    for (i in 0 until seasons.length()) {

                        val seasonBtn = inflater.inflate(R.layout.anime_season_item2, SeasonsContainer, false) as FrameLayout
                        val seasonTitle = seasonBtn.findViewById<TextView>(R.id.SeasonTitle)


                        val season = seasons.getJSONObject(i)
                        val title = season.optString("title", "Season ${i + 1}")
                        val seasonsNo = season.optString("season", "Season ${i + 1}")
                        val imageUrl = season.optString("poster", "")
                        val season_id = season.optString("id", "")

                        seasonTitle.text = seasonsNo //title

                        /*
                        if (imageUrl.isNotEmpty()) {
                            //val seasonImage = seasonBtn.findViewById<ImageView>(R.id.SeasonImage)
                            Glide.with(this@Anime_Video_Player)
                                .load(imageUrl)
                                .centerCrop()
                                .into(seasonImage)
                        }
                        */

                        seasonBtn.setOnClickListener {
                            if (isSeasonLoading) return@setOnClickListener
                            isSeasonLoading = true

                            lifecycleScope.launch(Dispatchers.Main) {
                                try {
                                    val jsonObject = withContext(Dispatchers.IO) { fetchAnime.animeInfo(season_id) }
                                    if (jsonObject!=null){
                                        val data =  jsonObject.getJSONObject("data")
                                        val name = data.getString("name")
                                        val poster = data.getString("poster")
                                        val id = data.getString("id")

                                        seasonTitleWidget.text  = name

                                        holdSeasonTitle = name
                                        holdPoster = poster
                                        holdSeasonId = id
                                    }

                                    getEpisodes(season_id)
                                } catch (e: Exception) {
                                    Log.e("ANIME_PLAYER", "Error in season click", e)
                                } finally {
                                    isSeasonLoading = false
                                }
                            }
                        }

                        SeasonsContainer.addView(seasonBtn)


                        if (currentSeasonId == season_id){
                            Log.e("ANIME_PLAYER", "currentSeasonId: $currentSeasonId , season_id: $season_id")
                            seasonBtn.performClick()
                        }
                    }
                }else{
                    holdSeasonTitle = name
                    holdPoster = poster
                    holdSeasonId = SeasonId

                    getEpisodes(SeasonId)
                }
            } catch (e: Exception) {
                Log.e("ANIME_PLAYER", "Error showing data", e)
            }
        }
    }


    private suspend fun getEpisodes(season_id: String){
        EpisodeContiner.removeAllViews()

        val inflater = LayoutInflater.from(this)
        try {
            val EpisodesjsonObject = withContext(Dispatchers.IO) { fetchAnime.animeEpisodes(season_id) }

            if (EpisodesjsonObject != null){
                val data = EpisodesjsonObject.getJSONObject("data")
                val  episodes = data.getJSONArray("episodes")
                for (i in 0 until episodes.length()) {

                    val episode = episodes.getJSONObject(i)
                    val eTitle = episode.optString("title", "${i + 1}")
                    val eNumber = episode.optString("number", "")
                    val episodeId = episode.optString("episodeId", "")
                    val eImageUrl = episode.optString("image", "")

                    val episodeBtn = inflater.inflate(R.layout.anime_item_episode2, EpisodeContiner, false) as FrameLayout
                    val epTitleWidget = episodeBtn.findViewById<TextView>(R.id.episode_name)
                    val epNumberWidget = episodeBtn.findViewById<TextView>(R.id.episode_Number)
                    val epImg = episodeBtn.findViewById<ImageView>(R.id.episode_image)
                    val cWatchSeek_bar = episodeBtn.findViewById<SeekBar>(R.id.cWatchSeek_bar)

                    epTitleWidget.text = eTitle
                    epNumberWidget.text = "$eNumber: "

                    episodeBtn.setOnFocusChangeListener { view, hasFocus ->

                        view.animate()
                            .scaleX(if (hasFocus) 1.08f else 1.0f)
                            .scaleY(if (hasFocus) 1.08f else 1.0f)
                            .setDuration(150)
                            .start()

                        view.elevation = if (hasFocus) 12f else 0f

                        if (hasFocus) {
                            GlobalUtils.centerChild(
                                moreSectionScrollView,
                                view
                            )
                        }
                    }

                    try{
                        Glide.with(this)
                            .load(eImageUrl)
                            .centerCrop()
                            .into(epImg)
                    }catch (e:Exception){}

                    withContext(Dispatchers.IO) {
                        try {
                            val lastPos = db.getResumePosition(userId, episodeId, "anime").toLong()
                            val durationPos = db.getDurationPosition(userId, episodeId, "anime").toLong()
                            withContext(Dispatchers.Main) {
                                val progress = if (durationPos > 0) ((lastPos.toDouble() / durationPos.toDouble()) * 1000).toInt() else 0
                                cWatchSeek_bar.progress = progress.coerceIn(0, 1000)
                            }
                        }catch(e:Exception){}
                    }

                    episodeBtn.setOnClickListener {

                        if (currentEpisodeId == episodeId) return@setOnClickListener
                        if (isEpisodeLoading) return@setOnClickListener

                        Log.e("ANIME_PLAYER", "EPISODE: $eNumber  , ID: $episodeId, CLICKED")

                        saveContinueWatching()


                            holdEpisodeNo = eNumber
                            fetchStreamingLinks(episodeId)

                            selectedEpisodeView?.isSelected = false
                            episodeBtn.isSelected = true
                            selectedEpisodeView = episodeBtn

                    }

                    EpisodeContiner.addView(episodeBtn)

                    if(currentEpisodeNumber == eNumber && currentEpisodeId == episodeId){
                        episodeBtn.isSelected = true
                        selectedEpisodeView = episodeBtn

                        if(!firstEpisodeLoaded){
                            Log.e("ANIME_PLAYER", "DEFAULT EPISODE: $eNumber  , ID: $episodeId, CLICKED")

                            //episodeBtn.performClick()

                            selectedEpisodeView?.isSelected = false
                            episodeBtn.isSelected = true
                            selectedEpisodeView = episodeBtn
                            holdEpisodeNo = eNumber
                            saveContinueWatching()


                            fetchStreamingLinks(episodeId)

                            firstEpisodeLoaded = true
                        }
                    }

                    if (currentSeasonId == season_id){
                        episodeButtons.add(episodeBtn)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ANIME_PLAYER", "Error getting episodes", e)
        }

    }

    private fun fetchStreamingLinks_api(episodeId: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            isEpisodeLoading = true
            val maxAttempts = 3
            var attempt = 0

            while (attempt < maxAttempts) {
                attempt++
                var jsonObjectServerInfo: org.json.JSONObject? = null
                try {
                    jsonObjectServerInfo = withContext(Dispatchers.IO) { fetchAnime.animeEpisodeServers(episodeId) }
                } catch (e: Exception) {
                    Log.e("ANIME_PLAYER", "Error fetching episode servers", e)
                }
                
                if (jsonObjectServerInfo != null) {

                    Log.d("ANIME_PLAYER", "StreamingLinks=$jsonObjectServerInfo FETCHED")

                    val dataServers = jsonObjectServerInfo.getJSONObject("data")

                    val subServersRaw = dataServers.optJSONArray("sub") ?: JSONArray()
                    val dubServersRaw = dataServers.optJSONArray("dub") ?: JSONArray()

                    fun cleanArray(input: JSONArray): JSONArray {
                        val result = JSONArray()

                        for (i in 0 until input.length()) {
                            val obj = input.optJSONObject(i) ?: continue
                            val link = obj.optString("link").lowercase()
                            val type = obj.optString("type")
                            
                            val isSupported = link.contains(".m3u8") || link.contains(".mp4") || link.contains(".mkv") || link.contains(".mpd")

                            if (type == "embed" || isSupported) {
                                result.put(obj)
                            }
                        }
                        return result
                    }

                    val subServers = cleanArray(subServersRaw)
                    val dubServers = cleanArray(dubServersRaw)

                    val prefs = getSharedPreferences("AnimePrefs", Context.MODE_PRIVATE)
                    val prefCategory = prefs.getString("pref_server_category", null)
                    val prefServerName = prefs.getString("pref_server_name", null)

                    var preferredSource: StreamSource? = null

                    if (prefCategory != null) {
                        val targetServers = if (prefCategory == "sub") subServers else dubServers
                        if (targetServers.length() > 0) {
                            if (prefServerName != null) {
                                for (i in 0 until targetServers.length()) {
                                    val server = targetServers.getJSONObject(i)
                                    if (server.getString("server") == prefServerName) {
                                        preferredSource = StreamSource(
                                            serverName = server.getString("server"),
                                            category = prefCategory,
                                            videoLink = server.getString("link"),
                                            referer = server.optJSONObject("headers")?.optString("Referer"),
                                            origin = server.optJSONObject("headers")?.optString("Origin"),
                                            userAgent = server.optJSONObject("headers")?.optString("User-Agent"),
                                            sourceType = server.optString("type")
                                        )
                                        break
                                    }
                                }
                            }
                            if (preferredSource == null) {
                                val server = targetServers.getJSONObject(0)
                                preferredSource = StreamSource(
                                    serverName = server.getString("server"),
                                    category = prefCategory,
                                    videoLink = server.getString("link"),
                                    referer = server.optJSONObject("headers")?.optString("Referer"),
                                    origin = server.optJSONObject("headers")?.optString("Origin"),
                                    userAgent = server.optJSONObject("headers")?.optString("User-Agent"),
                                    sourceType = server.optString("type")
                                )
                            }
                        }
                    }

                    // Determine initial default
                    val defaultSource = preferredSource ?: when {
                        dubServers.length() > 0 -> {
                            val server = dubServers.getJSONObject(0)
                            StreamSource(
                                serverName = server.getString("server"),
                                category = "dub",
                                videoLink = server.getString("link"),
                                referer = server.optJSONObject("headers")?.optString("Referer"),
                                origin = server.optJSONObject("headers")?.optString("Origin"),
                                userAgent = server.optJSONObject("headers")?.optString("User-Agent"),
                                sourceType = server.optString("type")
                            )
                        }

                        subServers.length() > 0 -> {
                            val server = subServers.getJSONObject(0)
                            StreamSource(
                                serverName = server.getString("server"),
                                category = "sub",
                                videoLink = server.getString("link"),
                                referer = server.optJSONObject("headers")?.optString("Referer"),
                                origin = server.optJSONObject("headers")?.optString("Origin"),
                                userAgent = server.optJSONObject("headers")?.optString("User-Agent"),
                                sourceType = server.optString("type")
                            )
                        }
                        else -> return@launch
                    }

                    //fetchServerSources(episodeId, defaultServerName, defaultCategory, videoLink)

                    fetchServerSources(
                        episodeId,
                        defaultSource.serverName,
                        defaultSource.category,
                        defaultSource.videoLink,
                        referer =  defaultSource.referer, // Make sure to pass this!
                        origin = defaultSource.origin,
                        userAgent = defaultSource.userAgent,
                        sourceType = defaultSource.sourceType
                    )

                    val btnServer = findViewById<TextView>(R.id.btn_server)

                    btnServer.setOnClickListener {
                        val builder = android.app.AlertDialog.Builder(
                            this@Anime_Video_Player,
                            R.style.CustomDialogTheme
                        )
                        builder.setTitle("Select Server")

                        val container = LinearLayout(this@Anime_Video_Player).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(40, 24, 40, 24)
                            setBackgroundColor(Color.TRANSPARENT)
                        }

                        val scrollView = ScrollView(this@Anime_Video_Player).apply {
                            addView(container)
                        }

                        var dialog: android.app.AlertDialog? = null

                        fun addServerSection(title: String, servers: JSONArray) {
                            val label = TextView(this@Anime_Video_Player).apply {
                                text = "$title:"
                                setTextColor(ContextCompat.getColor(context, android.R.color.white))
                                textSize = 18f
                                setTypeface(typeface, Typeface.BOLD)
                                setPadding(0, 20, 0, 10)
                            }
                            container.addView(label)

                            for (i in 0 until servers.length()) {
                                val server = servers.getJSONObject(i)
                                val rawServerName = server.getString("server")
                                val sourceType = server.optString("type")
                                val serverName = "$rawServerName ($sourceType)"
                                val vlink = server.getString("link")
                                val referer = server.getJSONObject("headers").optString("Referer")
                                val orign = server.getJSONObject("headers").optString("Origin")
                                val userAgent = server.optJSONObject("headers")?.optString("User-Agent")

                                val serverBtn = Button(this@Anime_Video_Player).apply {
                                    text = serverName
                                    setAllCaps(false)
                                    textSize = 14f
                                    background = ContextCompat.getDrawable(
                                        context,
                                        R.drawable.item_anime_episode_focus
                                    )
                                    setTextColor(
                                        ContextCompat.getColor(
                                            context,
                                            android.R.color.white
                                        )
                                    )
                                    setOnClickListener {
                                        Toast.makeText(
                                            context,
                                            "Switching to $title → $serverName",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        dialog?.dismiss()

                                        if (vlink.isNotBlank()) {
                                            val prefs = getSharedPreferences("AnimePrefs", Context.MODE_PRIVATE)
                                            prefs.edit()
                                                .putString("pref_server_category", title)
                                                .putString("pref_server_name", rawServerName)
                                                .apply()
                                            fetchServerSources(episodeId, rawServerName, title, vlink, referer,  orign, userAgent, sourceType)
                                        }
                                    }
                                }
                                container.addView(serverBtn)
                            }
                        }

                        // Add sections
                        addServerSection("sub", subServers)
                        addServerSection("dub", dubServers)

                        builder.setView(scrollView)
                        builder.setNegativeButton("Cancel", null)

                        dialog = builder.create()
                        dialog.show()
                    }
                    isEpisodeLoading = false

                    break
                }

                Log.e("ANIME_PLAYER", "StreamingLinks=$episodeId FAILED")

                if (attempt < maxAttempts) {
                    delay(3000) // Non-blocking wait (standard for Coroutines)
                }

            }
            isEpisodeLoading = false

        }
    }


    private fun fetchStreamingLinks(episodeId: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            isEpisodeLoading = true
            val maxAttempts = 3
            var attempt = 0

            while (attempt < maxAttempts) {
                attempt++

                var streamData: Map<String, List<Map<String, Any>>>? = null
                try {
                    // CALL THE LOCAL SCRAPER DIRECTLY INSTEAD OF THE NODE.JS API
                    streamData = withContext(Dispatchers.IO) {
                        MiruroScraper.fetchMiruroStreamingLinks(episodeId)
                    }
                } catch (e: Exception) {
                    Log.e("ANIME_PLAYER", "Error fetching streaming links", e)

                }

                // Check if we got valid data back
                if (streamData != null && streamData.isNotEmpty() && (streamData["sub"]?.isNotEmpty() == true || streamData["dub"]?.isNotEmpty() == true)) {

                    Log.d("ANIME_PLAYER", "StreamingLinks=$episodeId FETCHED")


                    val subServersRaw = streamData["sub"] ?: emptyList()
                    val dubServersRaw = streamData["dub"] ?: emptyList()

                    Log.d("ANIME_PLAYER", "\n\nsubServersRaw=$subServersRaw \n\n")
                    Log.d("ANIME_PLAYER", "\n\ndubServersRaw=$dubServersRaw \n\n")

                    // Replaced cleanArray with a clean native Kotlin list filter
                    fun cleanList(input: List<Map<String, Any>>): List<Map<String, Any>> {
                        return input.filter { 
                            val type = it["type"] as? String
                            val link = (it["link"] as? String)?.lowercase() ?: ""
                            val isSupported = link.contains(".m3u8") || link.contains(".mp4") || link.contains(".mkv") || link.contains(".mpd")
                            type != "embed" && isSupported
                        }
                    }

                    //val subServers = cleanList(subServersRaw)
                    //val dubServers = cleanList(dubServersRaw)

                    val subServers = subServersRaw
                    val dubServers = dubServersRaw

                    val prefs = getSharedPreferences("AnimePrefs", Context.MODE_PRIVATE)
                    val prefCategory = prefs.getString("pref_server_category", null)
                    val prefServerName = prefs.getString("pref_server_name", null)

                    var preferredSource: StreamSource? = null

                    if (prefCategory != null) {
                        val targetServers = if (prefCategory == "sub") subServers else dubServers
                        if (targetServers.isNotEmpty()) {
                            if (prefServerName != null) {
                                for (server in targetServers) {
                                    if (server["server"] as? String == prefServerName) {
                                        val headers = server["headers"] as? Map<*, *>
                                        preferredSource = StreamSource(
                                            serverName = server["server"] as? String ?: "",
                                            category = prefCategory,
                                            videoLink = server["link"] as? String ?: "",
                                            referer = headers?.get("Referer") as? String ?: "",
                                            origin = headers?.get("Origin") as? String ?: "",
                                            userAgent = headers?.get("User-Agent") as? String ?: "",
                                            sourceType = server["type"] as? String ?: ""
                                        )
                                        break
                                    }
                                }
                            }
                            if (preferredSource == null) {
                                val server = targetServers[0]
                                val headers = server["headers"] as? Map<*, *>
                                preferredSource = StreamSource(
                                    serverName = server["server"] as? String ?: "",
                                    category = prefCategory,
                                    videoLink = server["link"] as? String ?: "",
                                    referer = headers?.get("Referer") as? String ?: "",
                                    origin = headers?.get("Origin") as? String ?: "",
                                    userAgent = headers?.get("User-Agent") as? String ?: "",
                                    sourceType = server["type"] as? String ?: ""
                                )
                            }
                        }
                    }

                    // Determine initial default
                    val defaultSource = preferredSource ?: when {
                        dubServers.isNotEmpty() -> {
                            val server = dubServers[0]
                            val headers = server["headers"] as? Map<*, *>
                            StreamSource(
                                serverName = server["server"] as? String ?: "",
                                category = "dub",
                                videoLink = server["link"] as? String ?: "",
                                referer = headers?.get("Referer") as? String ?: "",
                                origin = headers?.get("Origin") as? String ?: "",
                                userAgent = headers?.get("User-Agent") as? String ?: "",
                                sourceType = server["type"] as? String ?: ""
                            )
                        }

                        subServers.isNotEmpty() -> {
                            val server = subServers[0]
                            val headers = server["headers"] as? Map<*, *>
                            StreamSource(
                                serverName = server["server"] as? String ?: "",
                                category = "sub",
                                videoLink = server["link"] as? String ?: "",
                                referer = headers?.get("Referer") as? String ?: "",
                                origin = headers?.get("Origin") as? String ?: "",
                                userAgent = headers?.get("User-Agent") as? String ?: "",
                                sourceType = server["type"] as? String ?: ""
                            )
                        }

                        else -> return@launch
                    }

                    fetchServerSources(
                        episodeId,
                        defaultSource.serverName,
                        defaultSource.category,
                        defaultSource.videoLink,
                        referer = defaultSource.referer,
                        origin = defaultSource.origin,
                        userAgent = defaultSource.userAgent,
                        sourceType = defaultSource.sourceType
                    )

                    val btnServer = findViewById<TextView>(R.id.btn_server)

                    btnServer.setOnClickListener {
                        val builder = android.app.AlertDialog.Builder(
                            this@Anime_Video_Player,
                            R.style.CustomDialogTheme
                        )
                        builder.setTitle("Select Server")

                        val container = LinearLayout(this@Anime_Video_Player).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(40, 24, 40, 24)
                            setBackgroundColor(Color.TRANSPARENT)
                        }

                        val scrollView = ScrollView(this@Anime_Video_Player).apply {
                            addView(container)
                        }

                        var dialog: android.app.AlertDialog? = null

                        // Replaced JSONArray with List<Map<String, Any>>
                        fun addServerSection(title: String, servers: List<Map<String, Any>>) {
                            if (servers.isEmpty()) return

                            val label = TextView(this@Anime_Video_Player).apply {
                                text = "$title:"
                                setTextColor(ContextCompat.getColor(context, android.R.color.white))
                                textSize = 18f
                                setTypeface(typeface, Typeface.BOLD)
                                setPadding(0, 20, 0, 10)
                            }
                            container.addView(label)

                            for (server in servers) {
                                val rawServerName = server["server"] as? String ?: ""
                                val sourceType = server["type"] as? String ?: ""
                                val serverName = "${rawServerName.uppercase()} ($sourceType)"
                                val vlink = server["link"] as? String ?: ""
                                val headers = server["headers"] as? Map<*, *>

                                val referer = headers?.get("Referer") as? String ?: ""
                                val origin = headers?.get("Origin") as? String ?: ""
                                val userAgent = headers?.get("User-Agent") as? String ?: ""

                                val serverBtn = Button(this@Anime_Video_Player).apply {
                                    text = serverName
                                    setAllCaps(false)
                                    textSize = 14f
                                    background = ContextCompat.getDrawable(
                                        context,
                                        R.drawable.item_anime_episode_focus
                                    )
                                    setTextColor(
                                        ContextCompat.getColor(
                                            context,
                                            android.R.color.white
                                        )
                                    )
                                    setOnClickListener {
                                        Toast.makeText(
                                            context,
                                            "Switching to $title → $serverName",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        dialog?.dismiss()

                                        if (vlink.isNotBlank()) {
                                            val prefs = getSharedPreferences("AnimePrefs", Context.MODE_PRIVATE)
                                            prefs.edit()
                                                .putString("pref_server_category", title)
                                                .putString("pref_server_name", rawServerName)
                                                .apply()
                                            fetchServerSources(episodeId, rawServerName, title, vlink, referer, origin, userAgent, sourceType)
                                        }
                                    }
                                }
                                container.addView(serverBtn)
                            }
                        }

                        // Add sections
                        addServerSection("sub", subServers)
                        addServerSection("dub", dubServers)

                        builder.setView(scrollView)
                        builder.setNegativeButton("Cancel", null)

                        dialog = builder.create()
                        dialog.show()
                    }
                    isEpisodeLoading = false

                    break
                }

                Log.e("ANIME_PLAYER", "StreamingLinks=$episodeId FAILED")

                if (attempt < maxAttempts) {
                    delay(3000) // Non-blocking wait (standard for Coroutines)
                }

            }
            isEpisodeLoading = false

        }
    }

    private fun fetchServerSources(
        episodeId: String,
        serverName: String,
        category: String,
        vidUrl: String,
        referer: String?,
        origin: String?,
        userAgent: String?,
        sourceType: String? = null,
        playbackContext: EpisodePlaybackContext? = null
    ) {
        lifecycleScope.launch(Dispatchers.Main) {
                    val targetPlaybackContext = playbackContext
                        ?: pendingPlaybackContext?.takeIf { it.episodeId == episodeId }
                        ?: buildPlaybackContext(episodeId)

                    Log.e(
                        "ANIME_PLAYER",
                        "PlAYER STARTED: $episodeId  , \nserverName: $serverName , \ncategory: $category, \nsourceType: $sourceType, \nvidUrl: $vidUrl , \nReferer: $referer , \norigin: $origin, userAgent: $userAgent\n\n"
                    )


                    saveContinueWatching()  // SAVE CURRENT PROGRESS BEFORE SWITCHING
                    stopProgressTracking()
                    exoPlayer?.pause()


                    applyPlaybackContext(targetPlaybackContext)



                    videoUrl = vidUrl
                    //val referer = "https://www.miruro.tv/" //data.getJSONObject("headers").optString("Referer")




                    val btnServer = findViewById<TextView>(R.id.btn_server)
                    btnServer.text = "$category: $serverName"

                    if (sourceType.equals("embed", ignoreCase = true)) {
                        pendingPlaybackContext = targetPlaybackContext
                        pendingEmbedSource = StreamSource(
                            episodeId = episodeId,
                            serverName = serverName,
                            category = category,
                            videoLink = vidUrl,
                            referer = referer,
                            origin = origin,
                            userAgent = userAgent,
                            sourceType = sourceType
                        )
                        Toast.makeText(
                            this@Anime_Video_Player,
                            "Resolving $serverName stream...",
                            Toast.LENGTH_SHORT
                        ).show()
                        animeStreamResolverLauncher.launch(
                            AnimeStreamResolver.createIntent(
                                context = this@Anime_Video_Player,
                                episodeId = episodeId,
                                serverName = serverName,
                                category = category,
                                embedUrl = vidUrl,
                                referer = referer,
                                origin = origin,
                                userAgent = userAgent
                            )
                        )
                        return@launch
                    }

                    resumePosition =
                        fetchResumePosition(targetPlaybackContext.episodeId)
                    hasAppliedResumePosition = false
                    pendingPlaybackContext = null
                    // (Re)initialize player with new stream
                    exoPlayer?.release()
                    exoPlayer = initializePlayer(vidUrl, referer, origin, userAgent)
                    activePlaybackContext = targetPlaybackContext
                    //exoPlayer = initializePlayer(vidUrl)
                    exoPlayer?.let { player ->
                        playerView.player = player
                        player.addListener(this@Anime_Video_Player)
                        updatePlayPauseButton()
                        updateMuteButton()
                        updateSpeedButton()
                        updateQualityButton()
                    }


        }
    }


    private fun focusPlayingEpisode() {
        val episodeContainer = findViewById<LinearLayout>(R.id.EpisodeContiner)
        val childCount = episodeContainer.childCount
        if (childCount == 0) return

        var episodeNum = currentEpisodeNumber.toIntOrNull() ?: 0
        //var episodeNum = currentEpisodeNumber?.toIntOrNull() ?: 0

        episodeNum = episodeNum-1
        episodeContainer.getChildAt(episodeNum)?.requestFocus()
    }

    private fun playNextEpisode(){
        val currentIndex = episodeButtons.indexOf(selectedEpisodeView)
        if (currentIndex != -1 && currentIndex < episodeButtons.size - 1) {
            val nextEpisodeBtn = episodeButtons[currentIndex + 1]
            nextEpisodeBtn.requestFocus()
            nextEpisodeBtn.performClick()
        } else {
            Toast.makeText(this, "No more episodes.", Toast.LENGTH_SHORT).show()
        }
    }



    private fun setupControls() {
        val btnAutoNext = findViewById<TextView>(R.id.btn_autoNext)
        btnAutoNext.setOnClickListener {
            isAutoNextEnabled = !isAutoNextEnabled
            btnAutoNext.text = if (isAutoNextEnabled) "Auto-next: On" else "Auto-next: Off"
            val prefs = getSharedPreferences("AnimePrefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("isAutoNextEnabled", isAutoNextEnabled).apply()
        }

        // Play/Pause button
        btnPlayPause.setOnClickListener {
            togglePlayPause()
        }


        btnMute.setOnClickListener {
            toggleMute()
        }

        // Fast-forward button (10 seconds)
        btnFastForward.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    if (event.isLongPress) seekRelative(10000) // repeat every long press interval
                    else seekRelative(10000)
                }
                true
            } else false
        }

        btnRewind.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    if (event.isLongPress) seekRelative(-10000)
                    else seekRelative(-10000)
                }
                true
            } else false
        }

        // Speed button
        btnSpeed.setOnClickListener {
            showSpeedDialog()
        }

        // Quality button
        btnQuality.setOnClickListener {
            showQualityDialog()
        }

        // Refresh button
        btnRefresh.setOnClickListener {
            refreshVideo()
        }

        // Settings button
        btnSettings.setOnClickListener {
            showSettingsDialog()
        }

        // Close button
        btnClose.setOnClickListener {
            finish()
        }



        // Seek bar
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val duration = exoPlayer?.duration ?: 0L
                    val position = (progress * duration / 1000).toLong()
                    txtCurrentTime.text = formatTime(position)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // No auto-hide during seek
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val duration = exoPlayer?.duration ?: 0L
                val position = (seekBar?.progress ?: 0) * duration / 1000
                exoPlayer?.seekTo(position)
            }
        })
    }

    private fun setupGestures() {
        playerView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastTapTime < 300) {
                        tapCount++
                        if (tapCount == 2) {
                            // Double tap - seek forward/backward
                            val x = event.x
                            val width = playerView.width
                            if (x < width / 2) {
                                seekRelative(-10000) // Seek back 10 seconds
                                showSeekFeedback("-10s")
                            } else {
                                seekRelative(10000) // Seek forward 10 seconds
                                showSeekFeedback("+10s")
                            }
                            tapCount = 0
                        }
                    } else {
                        tapCount = 1
                    }
                    lastTapTime = currentTime
                }
                MotionEvent.ACTION_UP -> {
                    if (tapCount == 1) {
                        // Single tap - toggle controls
                        toggleControls()
                    }
                }
            }
            true
        }
    }


    private var lastBackPressTime = 0L
    private val DOUBLE_BACK_PRESS_INTERVAL = 1000L // 2 seconds

    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {
                    val currentTime = System.currentTimeMillis()

                    // If menu is visible → hide it on single back
                    if (isMenuVisible) {
                        hideMenu()
                        return
                    }

                    // Handle double back press to exit
                    if (currentTime - lastBackPressTime < DOUBLE_BACK_PRESS_INTERVAL) {
                        finish() // or finishAffinity()
                    } else {
                        lastBackPressTime = currentTime
                        showMenu() // optional: or just show a toast
                    }
                }
            }
        )
    }


    private fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }

    private fun seekRelative(offsetMs: Long) {
        exoPlayer?.let { player ->
            val currentPosition = player.currentPosition
            val newPosition = (currentPosition + offsetMs).coerceAtLeast(0)
            player.seekTo(newPosition)
        }
    }

    private fun toggleMute() {
        exoPlayer?.let { player ->
            isMuted = !isMuted
            player.volume = if (isMuted) 0f else 1f
            updateMuteButton()
        }
    }

    private fun showSpeedDialog() {
        val speedOptions = arrayOf("0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "2.0x")
        val builder = android.app.AlertDialog.Builder(this, R.style.CustomDialogTheme)
        builder.setTitle("Playback Speed")
            .setSingleChoiceItems(speedOptions, currentSpeedIndex) { dialog, which ->
                currentSpeedIndex = which
                currentSpeed = playbackSpeeds[which]
                exoPlayer?.setPlaybackSpeed(currentSpeed)
                updateSpeedButton()
                dialog.dismiss() // Auto-close dialog when option is selected
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showQualityDialog() {
        val builder = android.app.AlertDialog.Builder(this, R.style.CustomDialogTheme)
        builder.setTitle("Video Quality")
            .setSingleChoiceItems(qualityOptions.toTypedArray(), currentQualityIndex) { dialog, which ->
                currentQualityIndex = which
                setVideoQuality(which)
                updateQualityButton()
                Toast.makeText(this, "Quality changed to ${qualityOptions[which]}", Toast.LENGTH_SHORT).show()
                dialog.dismiss() // Auto-close dialog when option is selected
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSettingsDialog() {
        val settings = arrayOf("Subtitles", "Video Info")
        val builder = android.app.AlertDialog.Builder(this, R.style.CustomDialogTheme)
        builder.setTitle("Settings")
            .setItems(settings) { dialog, which ->
                when (which) {
                    0 -> Toast.makeText(this, "Subtitles not available", Toast.LENGTH_SHORT).show()
                    1 -> showVideoInfo()
                }
                dialog.dismiss() // Auto-close dialog when option is selected
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showVideoInfo() {
        exoPlayer?.let { player ->
            val duration = formatTime(player.duration)
            val currentPos = formatTime(player.currentPosition)
            val quality = getCurrentVideoQuality()
            val videoInfo = getVideoInfo()
            val info = "Duration: $duration\nCurrent: $currentPos\nSpeed: ${currentSpeed}x\nQuality: $quality\n\n$videoInfo"
            Toast.makeText(this, info, Toast.LENGTH_LONG).show()
        }
    }


    private fun refreshVideo() {
        exoPlayer?.let { player ->
            // Save current position
            saveContinueWatching()

            val currentItem = player.currentMediaItem ?: return

            player.stop()
            player.clearMediaItems()

            lifecycleScope.launch(Dispatchers.Main) {
                // Re-fetch resume position
                resumePosition = fetchResumePosition()

                player.setMediaItem(currentItem)
                player.prepare()
                player.play()
            }
        }
    }

    private fun toggleControls() {
        if (isControlsVisible) {
            hideControls()
        } else {
            showControls()
        }
    }

    private fun showControls() {
        isControlsVisible = true
        bottomBar.visibility = View.VISIBLE
        moreSectionMenu.visibility = View.GONE
    }

    private fun hideControls() {
        isControlsVisible = false
        bottomBar.visibility = View.GONE
    }

    private fun showMenu() {
        isMenuVisible = true
        moreSectionMenu.visibility = View.VISIBLE

        isControlsVisible = false
        bottomBar.visibility = View.GONE

        focusPlayingEpisode()
    }

    private fun hideMenu() {
        isMenuVisible = false
        moreSectionMenu.visibility = View.GONE
    }



    private fun showSeekFeedback(text: String) {
        centerOverlay.removeAllViews()
        val textView = TextView(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 24f
            setPadding(40, 20, 40, 20)
            setBackgroundColor(Color.parseColor("#80000000"))
        }
        centerOverlay.addView(textView)
        centerOverlay.visibility = View.VISIBLE

        val fadeOut = AlphaAnimation(1.0f, 0.0f).apply {
            duration = 1000
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    centerOverlay.visibility = View.GONE
                }
                override fun onAnimationRepeat(animation: Animation?) {}
            })
        }
        textView.startAnimation(fadeOut)
    }

    private fun updatePlayPauseButton() {
        exoPlayer?.let { player ->
            btnPlayPause.setImageResource(
                if (player.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )
        }
    }

    private fun updateMuteButton() {
        btnMute.setImageResource(
            if (isMuted) android.R.drawable.ic_lock_silent_mode else android.R.drawable.ic_lock_silent_mode_off
        )
    }

    private fun updateSpeedButton() {
        btnSpeedText.text = "${currentSpeed}x"
    }

    private fun updateQualityButton() {
        // Get the current quality from the player
        val currentQuality = getCurrentVideoQuality()
        btnQuality.text = currentQuality
    }

    private fun formatTime(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    // Player.Listener implementation
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        runOnUiThread {
            updatePlayPauseButton()
            if (isPlaying) {
                progressBar.visibility = View.GONE
                startProgressTracking()
            } else {
                stopProgressTracking()
            }
        }
    }

    private var hasAppliedResumePosition = false
    override fun onPlaybackStateChanged(playbackState: Int) {
        runOnUiThread {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    progressBar.visibility = View.VISIBLE
                    stopProgressTracking()
                }
                Player.STATE_READY -> {
                    progressBar.visibility = View.GONE
                    val duration = exoPlayer?.duration ?: 0L
                    txtDuration.text = formatTime(duration)
                    seekBar.max = 1000

                    // Update quality display when video is ready
                    updateQualityButton()

                    val resumeThreshold = (duration / 100).coerceAtLeast(5_000L).coerceAtMost(30_000L)
                    val hasValidResumePosition =
                        resumePosition > 0 &&
                            duration > resumeThreshold &&
                            resumePosition < duration - resumeThreshold

                    if (hasValidResumePosition && !hasAppliedResumePosition) {
                        exoPlayer?.seekTo(resumePosition)
                        hasAppliedResumePosition = true
                        Log.d("ANIME_PLAYER", "Applied resume position: $resumePosition")
                    } else if (!hasAppliedResumePosition) {
                        resumePosition = 0L
                    }

                    if (exoPlayer?.isPlaying == true) {
                        startProgressTracking()
                    }
                }

                Player.STATE_ENDED -> {
                    // Video ended, could restart or show next video
                    stopProgressTracking()
                    clearSavedProgressForContext(activePlaybackContext)
                    resumePosition = 0L
                    if (isAutoNextEnabled) {
                        playNextEpisode()
                    } else {
                        Toast.makeText(this@Anime_Video_Player, "Video ended", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        runOnUiThread {
            updateSeekBar()
        }
    }

    override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
        runOnUiThread {
            // Update quality display when video size changes
            updateQualityButton()
        }
    }

    override fun onTracksChanged(tracks: Tracks) {
        runOnUiThread {
            // Update quality options when tracks are available
            updateAvailableQualities(tracks)
            qualityOptions = availableQualities
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        Log.e(
            "ANIME_PLAYER",
            "Playback failed code=${error.errorCodeName} message=${error.message}",
            error
        )

        val httpError = error.cause as? HttpDataSource.InvalidResponseCodeException
        if (httpError != null) {
            Log.e(
                "ANIME_PLAYER",
                "HTTP ${httpError.responseCode} for ${httpError.dataSpec.uri} headers=${httpError.headerFields}"
            )
        }
    }

    private fun startProgressTracking() {
        stopProgressTracking() // Stop any existing tracking
        progressUpdateCount = 0
        progressRunnable = object : Runnable {
            override fun run() {
                updateSeekBar()
                progressUpdateCount++
                // Track continue watching every 10 seconds (approx)
                if (progressUpdateCount % 10 == 0) {
                    saveContinueWatching()
                }
                progressHandler.postDelayed(this, 1000) // Update every second
            }
        }
        progressHandler.post(progressRunnable!!)
    }

    private fun stopProgressTracking() {
        progressRunnable?.let { progressHandler.removeCallbacks(it) }
        progressRunnable = null
    }

    private fun updateSeekBar() {
        exoPlayer?.let { player ->
            val duration = player.duration
            if (duration > 0) {
                val position = player.currentPosition
                val progress = (position * 1000 / duration).toInt()
                seekBar.progress = progress
                txtCurrentTime.text = formatTime(position)
            }
        }
    }

    private fun saveContinueWatching() {
        exoPlayer?.let { player ->
            val duration = player.duration.toInt()
            val lastPosition = player.currentPosition.toInt()
            val playbackContext = activePlaybackContext ?: return@let

            // Capture all necessary values immediately to avoid race conditions
            val savedUserId = userId
            val savedEpisodeId = playbackContext.episodeId
            val savedSeasonTitle = playbackContext.seasonTitle
            val savedPoster = playbackContext.poster
            val savedSeasonId = playbackContext.seasonId
            val savedEpisodeNumber = playbackContext.episodeNumber

            if (duration > 0 && lastPosition >= 5000 && savedUserId != 0 && savedEpisodeId.isNotBlank()) {
                // Run in background thread using independent scope to prevent cancellation when activity dies
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        db.addOrUpdateContinueWatching(
                            userId = savedUserId,
                            itemId = savedEpisodeId,
                            type = "anime",
                            title = savedSeasonTitle,
                            poster = savedPoster,
                            backdrop = savedPoster,
                            seasonNumber = savedSeasonId,
                            episodeNumber = savedEpisodeNumber,
                            lastPosition = lastPosition,
                            duration = duration
                        )
                    } catch (e: Exception) {
                        Log.e("ANIME_PLAYER", "Error saving progress", e)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        networkCallback?.let {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.unregisterNetworkCallback(it)
        }
        networkCallback = null
        saveContinueWatching()
        stopProgressTracking()
        releasePlayerWithAudioFocus()
        finish()
    }

    override fun onPause() {
        super.onPause()
        saveContinueWatching()
        exoPlayer?.pause()
        stopProgressTracking()
    }

    override fun onResume() {
        super.onResume()
        exoPlayer?.play()
        // Progress tracking will start automatically when onIsPlayingChanged is called
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                // Only show controls if they are hidden
                 if (!isControlsVisible and !isMenuVisible) {
                    showControls()
                    return true
                }
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (isControlsVisible and !isMenuVisible) {
                    hideControls()
                    return true
                }
            }

        }
        return super.onKeyDown(keyCode, event)
    }

    // ===== PlayerManager functionality merged into this class =====

    companion object {
        fun playVideoExternally(context: Context, episodeId: String, episodesNumber: String, seasonId: String) {
            val intent = Intent(context, Anime_Video_Player::class.java).apply {
                putExtra("episodeId", episodeId)
                putExtra("episodesNumber", episodesNumber.toString())
                putExtra("seasonId", seasonId)


                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(intent)
        }
    }

    private fun initializePlayer(videoUrl: String, referer: String? = null, origin: String?, userAgent: String?): ExoPlayer {
        releasePlayer()

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val result = audioManager.requestAudioFocus(
            null,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )

        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.w("Video_player", "Audio focus not granted, continuing playback")
        }

        val trackSelector = DefaultTrackSelector(this).apply {
            setParameters(
                buildUponParameters()
                    .setMaxVideoSize(1920, 1080)
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .setAllowVideoNonSeamlessAdaptiveness(true)
                    .setMaxAudioChannelCount(2)
                    .setPreferredAudioLanguage("en")
                    .setSelectUndeterminedTextLanguage(true)
                    .setForceHighestSupportedBitrate(false)
            )
        }

        val sanitizedReferer = referer?.trim().takeUnless { it.isNullOrEmpty() }
        val sanitizedUserAgent = userAgent?.trim().takeUnless { it.isNullOrEmpty() }
        val sanitizedOrigin = origin?.trim().takeUnless { it.isNullOrEmpty() }
            ?: sanitizedReferer?.let(::deriveOriginFromReferer)

        val headers = buildMap<String, String> {
            sanitizedUserAgent?.let { put("User-Agent", it) }
            sanitizedReferer?.let { put("Referer", it) }
            sanitizedOrigin?.let { put("Origin", it) }
        }

        Log.d(
            "ANIME_PLAYER",
            "Initializing player url=$videoUrl referer=$sanitizedReferer origin=$sanitizedOrigin ua=$sanitizedUserAgent"
        )

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(headers)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)

        val renderersFactory = DefaultRenderersFactory(this)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setEnableDecoderFallback(true)

        val mediaSourceFactory = DefaultMediaSourceFactory(
            DefaultDataSource.Factory(this, httpDataSourceFactory)
        ).setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(6))

        val loadControl = DefaultLoadControl.Builder()
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val player = ExoPlayer.Builder(this)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(videoUrl))
                prepare()
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onTracksChanged(tracks: Tracks) {
                        updateAvailableQualities(tracks)
                    }
                })
            }

        currentVideoUrl = videoUrl
        return player
    }

    private fun deriveOriginFromReferer(referer: String): String? {
        return runCatching {
            val uri = Uri.parse(referer)
            val scheme = uri.scheme
            val host = uri.host
            if (scheme.isNullOrBlank() || host.isNullOrBlank()) {
                null
            } else {
                "$scheme://$host"
            }
        }.getOrNull()
    }


    private fun releasePlayer() {
        exoPlayer?.let { player ->
            player.release()
            exoPlayer = null
            currentVideoUrl = null
            availableQualities = listOf("Auto")
        }
        activePlaybackContext = null
    }

    private fun releasePlayerWithAudioFocus() {
        exoPlayer?.let { player ->
            player.release()
            exoPlayer = null
            currentVideoUrl = null
            availableQualities = listOf("Auto")
        }
        activePlaybackContext = null

        // Abandon audio focus when releasing player
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.abandonAudioFocus(null)
    }

    private fun setVideoQuality(qualityIndex: Int) {
        exoPlayer?.let { player ->
            val trackSelector = player.trackSelector as? DefaultTrackSelector ?: return

            Log.d("Video_payer", "Setting video quality to index: $qualityIndex")

            if (qualityIndex >= availableQualities.size) {
                Log.w("Video_payer", "Quality index $qualityIndex out of bounds")
                return
            }

            val selectedQuality = availableQualities[qualityIndex]

            when (selectedQuality) {
                "Auto" -> {
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .clearVideoSizeConstraints()
                            .setMaxVideoSize(1920, 1080)
                            .setAllowVideoMixedMimeTypeAdaptiveness(true)
                            .setAllowVideoNonSeamlessAdaptiveness(true)
                    )
                }
                "1080p" -> {
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .setMaxVideoSize(1920, 1080)
                            .setMinVideoSize(1920, 1080)
                            .setAllowVideoMixedMimeTypeAdaptiveness(false)
                            .setAllowVideoNonSeamlessAdaptiveness(false)
                    )
                }
                "720p" -> {
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .setMaxVideoSize(1280, 720)
                            .setMinVideoSize(1280, 720)
                            .setAllowVideoMixedMimeTypeAdaptiveness(false)
                            .setAllowVideoNonSeamlessAdaptiveness(false)
                    )
                }
                "480p" -> {
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .setMaxVideoSize(854, 480)
                            .setMinVideoSize(854, 480)
                            .setAllowVideoMixedMimeTypeAdaptiveness(false)
                            .setAllowVideoNonSeamlessAdaptiveness(false)
                    )
                }
                "360p" -> {
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .setMaxVideoSize(640, 360)
                            .setMinVideoSize(640, 360)
                            .setAllowVideoMixedMimeTypeAdaptiveness(false)
                            .setAllowVideoNonSeamlessAdaptiveness(false)
                    )
                }
                "240p" -> {
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .setMaxVideoSize(426, 240)
                            .setMinVideoSize(426, 240)
                            .setAllowVideoMixedMimeTypeAdaptiveness(false)
                            .setAllowVideoNonSeamlessAdaptiveness(false)
                    )
                }
                else -> {
                    // Handle custom resolutions (e.g., "1440p", "2160p", etc.)
                    val resolution = selectedQuality.replace("p", "").toIntOrNull()
                    if (resolution != null) {
                        val width = when {
                            resolution >= 2160 -> 3840 // 4K
                            resolution >= 1440 -> 2560 // 1440p
                            resolution >= 1080 -> 1920 // 1080p
                            resolution >= 720 -> 1280  // 720p
                            resolution >= 480 -> 854   // 480p
                            resolution >= 360 -> 640   // 360p
                            else -> 426                // 240p
                        }
                        val height = resolution

                        trackSelector.setParameters(
                            trackSelector.buildUponParameters()
                                .setMaxVideoSize(width, height)
                                .setMinVideoSize(width, height)
                                .setAllowVideoMixedMimeTypeAdaptiveness(false)
                                .setAllowVideoNonSeamlessAdaptiveness(false)
                        )
                    }
                }
            }

            Log.d("Video_payer", "Quality parameters applied successfully for: $selectedQuality")
        }
    }

    private fun getCurrentVideoQuality(): String {
        exoPlayer?.let { player ->
            val videoFormat = player.videoFormat
            if (videoFormat != null) {
                val width = videoFormat.width
                val height = videoFormat.height

                return when {
                    width >= 1920 && height >= 1080 -> "1080p"
                    width >= 1280 && height >= 720 -> "720p"
                    width >= 854 && height >= 480 -> "480p"
                    width >= 640 && height >= 360 -> "360p"
                    width >= 426 && height >= 240 -> "240p"
                    else -> "Auto"
                }
            }
        }
        return "Auto"
    }

    private fun getVideoInfo(): String {
        exoPlayer?.let { player ->
            val videoFormat = player.videoFormat
            if (videoFormat != null) {
                return "Resolution: ${videoFormat.width}x${videoFormat.height}\n" +
                        "Codec: ${videoFormat.codecs}\n" +
                        "Bitrate: ${videoFormat.bitrate / 1000} kbps\n" +
                        "Frame Rate: ${videoFormat.frameRate} fps"
            }
        }
        return "Video info not available"
    }

    private fun updateAvailableQualities(tracks: Tracks) {
        val qualities = mutableListOf("Auto")

        // Get video tracks
        val videoTrackGroup = tracks.groups.find { it.type == C.TRACK_TYPE_VIDEO }

        if (videoTrackGroup != null) {
            val uniqueResolutions = mutableSetOf<Pair<Int, Int>>()

            // Extract unique resolutions from available tracks
            for (i in 0 until videoTrackGroup.length) {
                val format = videoTrackGroup.getTrackFormat(i)
                val width = format.width
                val height = format.height

                if (width > 0 && height > 0) {
                    uniqueResolutions.add(Pair(width, height))
                }
            }

            // Convert resolutions to quality labels and sort by resolution (highest first)
            val qualityLabels = uniqueResolutions.map { (width, height) ->
                when {
                    width >= 1920 && height >= 1080 -> "1080p"
                    width >= 1280 && height >= 720 -> "720p"
                    width >= 854 && height >= 480 -> "480p"
                    width >= 640 && height >= 360 -> "360p"
                    width >= 426 && height >= 240 -> "240p"
                    else -> "${width}p"
                }
            }.distinct().sortedWith(compareByDescending {
                when (it) {
                    "1080p" -> 1080
                    "720p" -> 720
                    "480p" -> 480
                    "360p" -> 360
                    "240p" -> 240
                    else -> it.replace("p", "").toIntOrNull() ?: 0
                }
            })

            qualities.addAll(qualityLabels)
        }

        availableQualities = qualities
        Log.d("Video_payer", "Available qualities updated: $availableQualities")
    }
}

data class StreamSource(
    val episodeId: String = "",
    val serverName: String,
    val category: String,
    val videoLink: String,
    val referer: String?,
    val origin: String?,
    val userAgent: String?,
    val sourceType: String? = null
)

data class EpisodePlaybackContext(
    val episodeId: String,
    val episodeNumber: String,
    val seasonId: String,
    val seasonTitle: String,
    val poster: String
)

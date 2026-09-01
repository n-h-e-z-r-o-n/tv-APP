package com.example.onyx

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import com.example.onyx.Database.AppDatabase
import com.example.onyx.Database.SessionManger
import com.example.onyx.FetchData.TMDBapi
import com.example.onyx.OnyxObjects.GlobalUtils
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.media3.datasource.HttpDataSource
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@UnstableApi
class Video_payer : AppCompatActivity(), Player.Listener {

    // ── Show metadata ──────────────────────────────────────────────────────────
    private lateinit var db: AppDatabase
    private lateinit var sm: SessionManger
    private lateinit var fetch: TMDBapi
    private var userId = -1
    private var resumePosition: Long = 0L
    private var showId: String = ""
    private var showType: String = ""
    private var showTitle: String = ""
    private var showPoster: String = ""
    private var showBackdrop: String = ""
    private var showSNo: String = ""
    private var showENo: String = ""
    private var videoUrl: String = ""
    private var userAgent: String = ""
    private var referer: String = ""
    private var isAutoNextEnabled = true
    private var isAutoNextLaunching = false

    // ── Views ──────────────────────────────────────────────────────────────────
    private lateinit var playerView: PlayerView
    private lateinit var progressBar: ProgressBar
    private lateinit var overlayContainer: View
    private lateinit var bottomBar: LinearLayout
    private lateinit var centerOverlay: FrameLayout
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnRewind: ImageButton
    private lateinit var btnFastForward: ImageButton
    private lateinit var btnMute: ImageButton
    private lateinit var btnSpeed: MaterialCardView

    private lateinit var btn_speed_text: TextView
    private lateinit var btnSubtitles: ImageButton
    private lateinit var btnAutoNext: TextView
    private lateinit var btnQuality: TextView
    private lateinit var btnRefresh: ImageButton
    private lateinit var btnSettings: ImageButton
    private lateinit var btnClose: ImageButton
    private lateinit var btnFullscreen: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var txtCurrentTime: TextView
    private lateinit var txtDuration: TextView

    // ── Player state ───────────────────────────────────────────────────────────
    private var exoPlayer: ExoPlayer? = null
    private var currentVideoUrl: String? = null

    private var isControlsVisible = true
    private var isFullscreen = false
    private var isMuted = false
    private var currentSpeed = 1.0f
    private var currentSpeedIndex = 2         // index into playbackSpeeds; 2 = 1.0x
    private var currentQualityIndex = 0
    private var availableQualities: List<String> = listOf("Auto")
    private var qualityOptions: List<String> = listOf("Auto")

    // ── Gesture detection ──────────────────────────────────────────────────────
    private var lastTapTime = 0L
    private var tapCount = 0

    // ── Seek-feedback animation (guarded against overlap) ──────────────────────
    private var seekFeedbackAnimation: Animation? = null

    // ── Progress tracking ──────────────────────────────────────────────────────
    /** Cached duration to avoid repeated player calls inside the 1-s tick */
    private var cachedDuration = 0L
    private val progressHandler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null
    private var saveCounter = 0

    // ── Playback retry / recovery ─────────────────────────────────────────────
    private val playbackRetryHandler = Handler(Looper.getMainLooper())
    private var playbackRetryRunnable: Runnable? = null
    private var playbackRetryCount = 0
    private val maxPlaybackRetries = 4

    // ── Audio focus ────────────────────────────────────────────────────────────
    private lateinit var audioManager: AudioManager
    private lateinit var connectivityManager: ConnectivityManager
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                val player = exoPlayer
                shouldResumeAfterAudioFocusGain =
                    shouldResumePlaybackWhenReady && (player?.playWhenReady == true || player?.isPlaying == true)
                player?.pause()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                val player = exoPlayer
                if (
                    shouldResumeAfterAudioFocusGain &&
                    shouldResumePlaybackWhenReady &&
                    !waitingForNetworkRecovery
                ) {
                    player?.play()
                }
                shouldResumeAfterAudioFocusGain = false
            }
        }
    }
    private var isNetworkCallbackRegistered = false
    private var waitingForNetworkRecovery = false
    private var shouldResumeAfterNetworkRecovery = false
    private var shouldResumePlaybackWhenReady = true
    private var shouldResumeAfterAudioFocusGain = false
    private var lastKnownPlaybackPosition = 0L
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            runOnUiThread { handleNetworkAvailable() }
        }

        override fun onLost(network: Network) {
            runOnUiThread {
                if (!isNetworkAvailable()) {
                    handleNetworkLost()
                }
            }
        }
    }

    // ── Constants ──────────────────────────────────────────────────────────────
    private val playbackSpeeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    // ──────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        GlobalUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_payer)

        // Keep screen on while playing
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        db = AppDatabase(this)
        sm = SessionManger(this)
        fetch = TMDBapi(this)
        userId = sm.getUserId()

        initializeViews()
        setupPlayer()
        setupControls()
        setupGestures()
        setupBackPressedCallback()
        registerNetworkCallback()
    }

    override fun onPause() {
        super.onPause()
        saveContinueWatching()
        shouldResumePlaybackWhenReady =
            exoPlayer?.playWhenReady == true ||
                    (waitingForNetworkRecovery && shouldResumeAfterNetworkRecovery)
        exoPlayer?.pause()
        stopProgressTracking()
    }

    override fun onResume() {
        super.onResume()
        if (
            waitingForNetworkRecovery &&
            shouldResumePlaybackWhenReady &&
            isNetworkAvailable()
        ) {
            handleNetworkAvailable()
        } else if (shouldResumePlaybackWhenReady) {
            // Tracking resumes automatically via onIsPlayingChanged
            exoPlayer?.play()
        }
    }

    override fun onDestroy() {
        saveContinueWatching()
        stopProgressTracking()
        stopBufferingWatchdog()
        bufferingWatchdogHandler.removeCallbacksAndMessages(null)
        bufferingWatchdogRunnable = null

        // Cancel any in-flight seek-feedback animation so its listener can't
        // touch views after the Activity starts tearing down.
        seekFeedbackAnimation?.setAnimationListener(null)
        seekFeedbackAnimation = null

        playerView.setOnTouchListener(null)

        progressHandler.removeCallbacksAndMessages(null)
        playbackRetryHandler.removeCallbacksAndMessages(null)
        playbackRetryRunnable = null
        lifecycleScope.coroutineContext.cancelChildren()
        unregisterNetworkCallback()
        releasePlayer()
        playerView.player = null
        // Note: do NOT call finish() here — already being destroyed
        super.onDestroy()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Setup
    // ──────────────────────────────────────────────────────────────────────────

    private fun initializeViews() {
        playerView       = findViewById(R.id.player_view)
        progressBar      = findViewById(R.id.progress_bar)
        overlayContainer = findViewById(R.id.overlay_container)
        bottomBar        = findViewById(R.id.bottom_bar)
        centerOverlay    = findViewById(R.id.center_overlay)
        btnPlayPause     = findViewById(R.id.btn_play_pause)
        btnRewind        = findViewById(R.id.btn_rewind)
        btnFastForward   = findViewById(R.id.btn_fast_forward)
        btnMute          = findViewById(R.id.btn_mute)
        btnSpeed         = findViewById(R.id.btn_speed)
        btn_speed_text   = findViewById(R.id.btn_speed_text)
        btnSubtitles     = findViewById(R.id.btn_subtitles)
        btnAutoNext      = findViewById(R.id.btn_autoNext)
        btnQuality       = findViewById(R.id.btn_quality)
        btnRefresh       = findViewById(R.id.btn_refresh)
        btnSettings      = findViewById(R.id.btn_settings)
        btnClose         = findViewById(R.id.btn_close)
        btnFullscreen    = findViewById(R.id.btn_fullscreen)
        seekBar          = findViewById(R.id.seek_bar)
        txtCurrentTime   = findViewById(R.id.txt_current_time)
        txtDuration      = findViewById(R.id.txt_duration)
    }

    private fun setupPlayer() {
        videoUrl     = intent.getStringExtra("video_url")   ?: ""
        referer      = intent.getStringExtra("referer")     ?: ""
        userAgent    = intent.getStringExtra("userAgent")   ?: ""
        showId       = intent.getStringExtra("showId")      ?: ""
        showType     = intent.getStringExtra("showType")    ?: ""
        showTitle    = intent.getStringExtra("showTitle")   ?: ""
        showPoster   = intent.getStringExtra("showPoster")  ?: ""
        showBackdrop = intent.getStringExtra("showBackdrop")?: ""
        showSNo      = intent.getStringExtra("showSNo")     ?: ""
        showENo      = intent.getStringExtra("showENo")     ?: ""
        isAutoNextEnabled = getAutoNextPreference()

        if (BuildConfig.DEBUG) {
            Log.d("Video_payer", "videoUrl=$videoUrl")
            Log.d("Video_payer", "referer=$referer")
            Log.d("Video_payer", "userAgent=$userAgent")
            Log.d("Video_payer", "showId=$showId")
            Log.d("Video_payer", "showType=$showType")
            Log.d("Video_payer", "showTitle=$showTitle")
            Log.d("Video_payer", "showSNo=$showSNo")
            Log.d("Video_payer", "showENo=$showENo")
            Log.d("Video_payer", "autoNext=$isAutoNextEnabled")
        }

        if (videoUrl.isEmpty()) {
            Toast.makeText(this, "No video URL provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch(Dispatchers.Main) {
            resumePosition = withContext(Dispatchers.IO) { fetchResumePosition() }

            // Guard against the Activity being torn down while the DB lookup
            // was in flight (e.g. rapid back-press).
            if (isDestroyed || isFinishing) return@launch

            exoPlayer = buildPlayer(videoUrl)
            exoPlayer?.let { player ->
                playerView.player = player
                player.addListener(this@Video_payer)
                updatePlayPauseButton()
                updateMuteButton()
                updateSpeedButton()
                updateQualityButton()
                qualityOptions = availableQualities
            }
        }
    }

    private fun setupControls() {
        btnPlayPause.setOnClickListener { togglePlayPause() }
        btnMute.setOnClickListener      { toggleMute() }
        btnSpeed.setOnClickListener     { showSpeedDialog() }
        btnAutoNext.setOnClickListener  { toggleAutoNext() }
        btnQuality.setOnClickListener   { showQualityDialog() }
        btnRefresh.setOnClickListener   { refreshVideo(resetAutoRefreshAttempts = true) }
        btnSettings.setOnClickListener  { showSettingsDialog() }
        btnClose.setOnClickListener     { finish() }
        btnFullscreen.setOnClickListener{ toggleFullscreen() }

        updateAutoNextButton()

        // Touch taps for FF/rewind, in addition to the existing D-pad support below,
        // so this also works on non-remote (phone/tablet) layouts.
        btnFastForward.setOnClickListener { seekRelative(10_000); showSeekFeedback("+10s") }
        btnRewind.setOnClickListener      { seekRelative(-10_000); showSeekFeedback("-10s") }

        btnFastForward.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER && event.action == KeyEvent.ACTION_DOWN) {
                seekRelative(10_000); true
            } else false
        }
        btnRewind.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER && event.action == KeyEvent.ACTION_DOWN) {
                seekRelative(-10_000); true
            } else false
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && cachedDuration > 0) {
                    txtCurrentTime.text = formatTime(progress * cachedDuration / 1000)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (cachedDuration > 0) {
                    exoPlayer?.seekTo((seekBar?.progress ?: 0) * cachedDuration / 1000)
                }
            }
        })
    }

    private fun setupGestures() {
        playerView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val now = System.currentTimeMillis()
                    if (now - lastTapTime < 300) {
                        tapCount++
                        if (tapCount == 2) {
                            val seekAmt = if (event.x < playerView.width / 2) -10_000L else 10_000L
                            seekRelative(seekAmt)
                            showSeekFeedback(if (seekAmt < 0) "-10s" else "+10s")
                            tapCount = 0
                        }
                    } else {
                        tapCount = 1
                    }
                    lastTapTime = now
                }
                MotionEvent.ACTION_UP -> {
                    if (tapCount == 1) toggleControls()
                }
            }
            true
        }
    }

    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isControlsVisible) hideControls() else finish()
            }
        })
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Player construction
    // ──────────────────────────────────────────────────────────────────────────

    private fun buildPlayer(url: String): ExoPlayer {
        releasePlayer()
        autoRefreshCount = 0
        shouldResumePlaybackWhenReady = true
        shouldResumeAfterAudioFocusGain = false

        val headers = buildMap<String, String> {
            put("User-Agent", userAgent)
            if (referer.isNotEmpty()) {
                put("Referer", referer)
                val uri = Uri.parse(referer)
                val origin = "${uri.scheme}://${uri.host}"
                if (origin != "null://null") put("Origin", origin)
            }
        }

        if (BuildConfig.DEBUG) {
            Log.d("Video_payer", "UA=$userAgent  referer=$referer")
        }

        val httpFactory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(headers)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)       // fail fast on bad connections
            .setReadTimeoutMs(30_000)

        val loadErrorPolicy = DefaultLoadErrorHandlingPolicy(6)

        val mediaSourceFactory = DefaultMediaSourceFactory(
            DefaultDataSource.Factory(this, httpFactory)
        ).setLoadErrorHandlingPolicy(loadErrorPolicy)

        // Request audio focus with proper listener so we pause on phone calls etc.
        val focusResult = audioManager.requestAudioFocus(
            audioFocusChangeListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
        if (focusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.w("Video_payer", "Audio focus not granted — continuing anyway")
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
                    // Let ABR choose bitrate freely — forceHighest kills adaptive quality
                    .setForceHighestSupportedBitrate(false)
            )
        }

        val renderersFactory = DefaultRenderersFactory(this)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            // Allow more threads for software decode fallback
            .setEnableDecoderFallback(true)

        val loadControl = DefaultLoadControl.Builder()
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBufferDurationsMs(
                /* minBufferMs */ MIN_BUFFER_MS,
                /* maxBufferMs */ MAX_BUFFER_MS,
                /* bufferForPlayback */ BUFFER_FOR_PLAYBACK_MS,
                /* bufferForPlaybackAfterRebuffer */ BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .build()

        val player = ExoPlayer.Builder(this)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(mediaSourceFactory)
            // Buffer more aggressively before starting or resuming playback.
            .setLoadControl(loadControl)
            .build()

        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true
        currentVideoUrl = url
        return player
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Player controls
    // ──────────────────────────────────────────────────────────────────────────

    private fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying || player.playWhenReady) {
                shouldResumePlaybackWhenReady = false
                shouldResumeAfterNetworkRecovery = false
                shouldResumeAfterAudioFocusGain = false
                player.pause()
            } else {
                shouldResumePlaybackWhenReady = true
                player.play()
            }
        }
    }

    private fun seekRelative(offsetMs: Long) {
        val player = exoPlayer ?: return
        val duration = cachedDuration.takeIf { it > 0 } ?: player.duration.takeIf { it > 0 && it != C.TIME_UNSET }
        val target = (player.currentPosition + offsetMs).coerceAtLeast(0)
        player.seekTo(if (duration != null) target.coerceAtMost(duration) else target)
    }

    private fun toggleMute() {
        isMuted = !isMuted
        exoPlayer?.volume = if (isMuted) 0f else 1f
        updateMuteButton()
    }

    private fun refreshVideo(resetAutoRefreshAttempts: Boolean = false) {
        val restartPosition = exoPlayer?.currentPosition ?: resumePosition
        if (resetAutoRefreshAttempts) {
            autoRefreshCount = 0
        }
        shouldResumePlaybackWhenReady = true
        shouldResumeAfterNetworkRecovery = true
        exoPlayer?.let { player ->
            saveContinueWatching()
            reloadVideo(player, restartPosition, autoPlay = true)
        }
    }

    ///////////////////////////// Auto Buffering Section ///////////////////////////////////////////////////////////////////

    private val bufferingWatchdogHandler = Handler(Looper.getMainLooper())
    private var bufferingWatchdogRunnable: Runnable? = null
    private var isAutoRefreshing = false
    private var autoRefreshCount = 0
    private val maxAutoRefreshAttempts = 3
    private val AUTO_REFRESH_BUFFERING_TIMEOUT_MS = 80_000L
    private val BUFFERING_WATCHDOG_POLL_MS = 5_000L

    private var lastWatchdogBufferedPosition = -1L
    private var lastBufferProgressTimeMs = 0L
    private val BUFFER_PROGRESS_THRESHOLD_MS = 500L

    private fun startBufferingWatchdog() {
        val player = exoPlayer ?: return

        if (!player.playWhenReady) {
            stopBufferingWatchdog()
            return
        }

        if (bufferingWatchdogRunnable != null) {
            return
        }

        lastWatchdogBufferedPosition = player.bufferedPosition
        lastBufferProgressTimeMs = SystemClock.elapsedRealtime()

        bufferingWatchdogRunnable = Runnable {
            val currentPlayer = exoPlayer ?: return@Runnable

            if (currentPlayer.playbackState != Player.STATE_BUFFERING || !currentPlayer.playWhenReady) {
                stopBufferingWatchdog()
                return@Runnable
            }

            if (waitingForNetworkRecovery) {
                bufferingWatchdogHandler.postDelayed(bufferingWatchdogRunnable!!, BUFFERING_WATCHDOG_POLL_MS)
                return@Runnable
            }

            val currentBufferedPosition = currentPlayer.bufferedPosition
            val nowMs = SystemClock.elapsedRealtime()
            val bufferHasAdvanced =
                currentBufferedPosition > lastWatchdogBufferedPosition + BUFFER_PROGRESS_THRESHOLD_MS

            if (bufferHasAdvanced) {
                Log.d(
                    "Auto_refresh",
                    "Buffer progressing: " +
                            "${lastWatchdogBufferedPosition / 1000}s -> " +
                            "${currentBufferedPosition / 1000}s"
                )

                lastWatchdogBufferedPosition = currentBufferedPosition
                lastBufferProgressTimeMs = nowMs
            } else {
                val stalledForMs = nowMs - lastBufferProgressTimeMs

                if (stalledForMs >= AUTO_REFRESH_BUFFERING_TIMEOUT_MS && !isAutoRefreshing) {
                    if (autoRefreshCount >= maxAutoRefreshAttempts) {
                        Log.d("Auto_refresh", "Auto-refresh limit reached")
                        stopBufferingWatchdog()

                        Toast.makeText(
                            this,
                            "Stream is taking too long. Tap refresh to retry.",
                            Toast.LENGTH_LONG
                        ).show()

                        return@Runnable
                    }

                    autoRefreshCount++
                    isAutoRefreshing = true

                    Log.d(
                        "Auto_refresh",
                        "Buffer stalled for ${stalledForMs}ms, auto refresh " +
                                "$autoRefreshCount/$maxAutoRefreshAttempts"
                    )

                    refreshVideo()

                    bufferingWatchdogHandler.postDelayed(
                        {
                            isAutoRefreshing = false
                            lastWatchdogBufferedPosition = exoPlayer?.bufferedPosition ?: -1L
                            lastBufferProgressTimeMs = SystemClock.elapsedRealtime()
                        },
                        BUFFERING_WATCHDOG_POLL_MS
                    )
                }
            }

            bufferingWatchdogHandler.postDelayed(bufferingWatchdogRunnable!!, BUFFERING_WATCHDOG_POLL_MS)
        }

        bufferingWatchdogHandler.postDelayed(bufferingWatchdogRunnable!!, BUFFERING_WATCHDOG_POLL_MS)
    }

    private fun stopBufferingWatchdog() {
        isAutoRefreshing = false
        lastWatchdogBufferedPosition = -1L
        lastBufferProgressTimeMs = 0L
        bufferingWatchdogRunnable?.let {
            bufferingWatchdogHandler.removeCallbacks(it)
        }
        bufferingWatchdogRunnable = null
    }

    // Player.Listener callbacks  (always called on the main thread by ExoPlayer)
    // ──────────────────────────────────────────────────────────────────────────

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        updatePlayPauseButton()
        if (isPlaying) {
            progressBar.visibility = View.GONE
            startProgressTracking()
        } else {
            stopProgressTracking()
        }
    }

    override fun onPlaybackStateChanged(state: Int) {
        when (state) {
            Player.STATE_BUFFERING -> {
                progressBar.visibility = View.VISIBLE
                stopProgressTracking()
                startBufferingWatchdog()
            }
            Player.STATE_READY -> {
                stopBufferingWatchdog()
                playbackRetryCount = 0
                playbackRetryRunnable?.let { playbackRetryHandler.removeCallbacks(it) }
                playbackRetryRunnable = null

                progressBar.visibility = View.GONE
                cachedDuration = exoPlayer?.duration ?: 0L
                if (cachedDuration > 0) {
                    txtDuration.text = formatTime(cachedDuration)
                    seekBar.max = 1000
                }
                updateQualityButton()
                if (exoPlayer?.isPlaying == true) startProgressTracking()

                // Resume from saved DB position once — then clear it
                if (resumePosition > 0) {
                    exoPlayer?.seekTo(resumePosition)
                    resumePosition = 0
                }
            }
            Player.STATE_ENDED -> {
                stopBufferingWatchdog()
                isAutoRefreshing = false
                autoRefreshCount = 0
                playbackRetryCount = 0

                waitingForNetworkRecovery = false
                shouldResumeAfterNetworkRecovery = false
                stopProgressTracking()
                saveContinueWatching()
                if (shouldAutoPlayNextEpisode()) {
                    tryAutoPlayNextEpisode()
                } else {
                    Toast.makeText(this, "Video ended", Toast.LENGTH_SHORT).show()
                }
            }
            Player.STATE_IDLE -> {
                stopBufferingWatchdog()
            }
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        progressBar.visibility = View.VISIBLE
        stopProgressTracking()

        val httpCode = findHttpResponseCode(error)

        Log.e(
            "Video_payer",
            """
            Playback failed
            errorCode=${error.errorCode}
            errorName=${error.errorCodeName}
            httpCode=$httpCode
            message=${error.message}
            cause=${error.cause}
            url=$videoUrl
            """.trimIndent(),
            error
        )

        if (!isNetworkAvailable()) {
            handleNetworkLost()
            return
        }

        when {
            isRecoverablePlaybackError(error, httpCode) -> {
                schedulePlaybackRetry()
            }

            httpCode == 401 || httpCode == 403 || httpCode == 410 -> {
                // Source token/link has expired or is unauthorized. Retrying the
                // *same* URL would just fail again, so we deliberately stop here
                // instead of calling refreshVideo() — the caller needs to supply
                // a freshly-signed URL (e.g. via a new Intent) rather than us
                // looping on a dead link.
                Log.d(
                    "onPlayerError",
                    "Source probably expired or is no longer authorized: HTTP $httpCode"
                )

                Toast.makeText(this, "Video Not Available.", Toast.LENGTH_SHORT).show()
            }

            else -> {
                Toast.makeText(
                    this,
                    "Playback error: ${error.errorCodeName}. Tap refresh to get a new source.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onTracksChanged(tracks: Tracks) {
        updateAvailableQualities(tracks)
        qualityOptions = availableQualities
        updateQualityButton()
    }

    override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
        updateQualityButton()
    }

    override fun onPositionDiscontinuity(
        old: Player.PositionInfo,
        new: Player.PositionInfo,
        reason: Int
    ) {
        updateSeekBar()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Progress tracking  (1-second tick on main thread)
    // ──────────────────────────────────────────────────────────────────────────

    private fun startProgressTracking() {
        stopProgressTracking()
        progressRunnable = object : Runnable {
            override fun run() {
                updateSeekBar()
                if (++saveCounter >= 10) {
                    saveContinueWatching()
                    saveCounter = 0
                }
                progressHandler.postDelayed(this, 1_000)
            }
        }
        progressHandler.post(progressRunnable!!)
    }

    private fun stopProgressTracking() {
        progressRunnable?.let { progressHandler.removeCallbacks(it) }
        progressRunnable = null
    }

    private fun updateSeekBar() {
        val player = exoPlayer ?: return
        val duration = cachedDuration.takeIf { it > 0 } ?: return

        val position = player.currentPosition
        val bufferedPosition = player.bufferedPosition
        val bufferedAhead = (bufferedPosition - position).coerceAtLeast(0)

        lastKnownPlaybackPosition = position

        seekBar.progress = (position * 1000 / duration).toInt()

        seekBar.secondaryProgress =
            (bufferedPosition * 1000 / duration)
                .coerceIn(0, 1000)
                .toInt()

        txtCurrentTime.text = formatTime(position)

        if (BuildConfig.DEBUG) {
            Log.d(
                "VideoBuffer",
                "position=${position / 1000}s | " +
                        "buffered=${bufferedPosition / 1000}s | " +
                        "ahead=${bufferedAhead / 1000}s"
            )
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Persist progress
    // ──────────────────────────────────────────────────────────────────────────

    private fun findHttpResponseCode(error: Throwable): Int? {
        var current: Throwable? = error

        while (current != null) {
            if (current is HttpDataSource.InvalidResponseCodeException) {
                return current.responseCode
            }
            current = current.cause
        }

        return null
    }

    private fun isRecoverablePlaybackError(
        error: PlaybackException,
        httpCode: Int?
    ): Boolean {
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> true

            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> {
                when {
                    httpCode == 408 -> true
                    httpCode == 429 -> true
                    httpCode != null && httpCode in 500..599 -> true
                    else -> false
                }
            }

            else -> false
        }
    }

    private fun schedulePlaybackRetry() {
        val player = exoPlayer ?: return

        if (playbackRetryCount >= maxPlaybackRetries) {
            Log.w("Video_payer", "Maximum playback retries reached")
            playbackRetryCount = 0

            Toast.makeText(
                this,
                "Stream is not responding. Tap refresh to get a new source.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val restartPosition =
            player.currentPosition.takeIf { it > 0 } ?: lastKnownPlaybackPosition

        val shouldAutoPlay = player.playWhenReady

        val delayMs = when (playbackRetryCount) {
            0 -> 1_000L
            1 -> 2_000L
            2 -> 4_000L
            else -> 8_000L
        }

        playbackRetryCount++

        Log.d(
            "Video_payer",
            "Scheduling retry $playbackRetryCount/$maxPlaybackRetries " +
                    "after ${delayMs}ms at ${restartPosition}ms"
        )

        playbackRetryRunnable?.let {
            playbackRetryHandler.removeCallbacks(it)
        }

        playbackRetryRunnable = Runnable {
            if (isDestroyed || isFinishing) {
                return@Runnable
            }

            if (!isNetworkAvailable()) {
                handleNetworkLost()
                return@Runnable
            }

            val currentPlayer = exoPlayer ?: return@Runnable

            progressBar.visibility = View.VISIBLE

            runCatching {
                /*
                 * A fatal playback error puts ExoPlayer in STATE_IDLE.
                 * prepare() retries the current media item without rebuilding
                 * the entire player.
                 */
                currentPlayer.prepare()

                if (restartPosition > 0) {
                    currentPlayer.seekTo(restartPosition)
                }

                currentPlayer.playWhenReady = shouldAutoPlay

                if (shouldAutoPlay) {
                    currentPlayer.play()
                }
            }.onFailure { retryError ->
                Log.e("Video_payer", "Playback retry failed", retryError)
            }
        }

        playbackRetryHandler.postDelayed(
            playbackRetryRunnable!!,
            delayMs
        )
    }

    private fun reloadVideo(player: ExoPlayer, startPositionMs: Long, autoPlay: Boolean) {
        resumePosition = 0
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(MediaItem.fromUri(videoUrl))
        player.prepare()
        if (startPositionMs > 0) {
            player.seekTo(startPositionMs)
        }
        player.playWhenReady = autoPlay
        if (autoPlay) {
            player.play()
        }
    }

    private fun handleNetworkLost() {
        if (waitingForNetworkRecovery) return

        val player = exoPlayer
        shouldResumeAfterNetworkRecovery =
            shouldResumePlaybackWhenReady &&
                    player != null &&
                    player.playbackState != Player.STATE_ENDED
        lastKnownPlaybackPosition = player?.currentPosition ?: lastKnownPlaybackPosition
        waitingForNetworkRecovery = true

        progressBar.visibility = View.VISIBLE
        stopProgressTracking()
        player?.pause()
        updatePlayPauseButton()
        Toast.makeText(this, "Connection lost. Waiting for network...", Toast.LENGTH_SHORT).show()
    }

    private fun handleNetworkAvailable() {
        if (!waitingForNetworkRecovery || isDestroyed || isFinishing) return
        if (!lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) return

        val player = exoPlayer ?: return
        waitingForNetworkRecovery = false
        val restartPosition = player.currentPosition.takeIf { it > 0 } ?: lastKnownPlaybackPosition
        reloadVideo(player, restartPosition, autoPlay = shouldResumeAfterNetworkRecovery)
        shouldResumeAfterAudioFocusGain = false
        Toast.makeText(this, "Connection restored. Resuming video...", Toast.LENGTH_SHORT).show()
    }

    private fun registerNetworkCallback() {
        if (isNetworkCallbackRegistered) return

        runCatching {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        }.onSuccess {
            isNetworkCallbackRegistered = true
        }.onFailure { error ->
            Log.w("Video_payer", "Failed to register network callback", error)
        }
    }

    private fun unregisterNetworkCallback() {
        if (!isNetworkCallbackRegistered) return

        runCatching {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }.onFailure { error ->
            Log.w("Video_payer", "Failed to unregister network callback", error)
        }
        isNetworkCallbackRegistered = false
    }

    private fun isNetworkAvailable(): Boolean {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun fetchResumePosition(): Long {
        return if (showType == "movie") {
            db.getResumePosition(userId, showId, showType).toLong()
        } else {
            db.getResumePosition(userId, "${showId}_S${showSNo}_E${showENo}", showType).toLong()
        }
    }

    private fun saveContinueWatching() {
        val player = exoPlayer ?: return
        val duration     = player.duration.toInt()
        val lastPosition = player.currentPosition.toInt()
        if (lastPosition < 5_000 || duration <= 0) return

        val itemId = if (showType == "movie") showId
        else "${showId}_S${showSNo}_E${showENo}"

        // Fire-and-forget on IO thread — does not hold a reference to the Activity
        lifecycleScope.launch(Dispatchers.IO) {
            db.addOrUpdateContinueWatching(
                userId        = userId,
                itemId        = itemId,
                type          = showType,
                title         = showTitle,
                poster        = showPoster,
                backdrop      = showBackdrop,
                seasonNumber  = showSNo,
                episodeNumber = showENo,
                lastPosition  = lastPosition,
                duration      = duration
            )
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    private fun getAutoNextPreference(): Boolean {
        val prefs = getSharedPreferences("video_player_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("auto_next_enabled", true)
    }

    private fun setAutoNextPreference(enabled: Boolean) {
        val prefs = getSharedPreferences("video_player_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("auto_next_enabled", enabled).apply()
    }

    private fun isShowEpisode(): Boolean {
        if (!showType.equals("tv", ignoreCase = true)) return false
        val seasonNumber = showSNo.toIntOrNull() ?: return false
        val episodeNumber = showENo.toIntOrNull() ?: return false
        return seasonNumber > 0 && episodeNumber > 0
    }

    private fun shouldAutoPlayNextEpisode(): Boolean {
        return isShowEpisode() && isAutoNextEnabled && !isAutoNextLaunching
    }

    private fun toggleAutoNext() {
        if (!isShowEpisode()) return
        isAutoNextEnabled = !isAutoNextEnabled
        setAutoNextPreference(isAutoNextEnabled)
        updateAutoNextButton()
        Toast.makeText(
            this,
            if (isAutoNextEnabled) "Auto-next enabled" else "Auto-next disabled",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun updateAutoNextButton() {
        if (!isShowEpisode()) {
            btnAutoNext.visibility = View.GONE
            return
        }

        btnAutoNext.visibility = View.VISIBLE
        btnAutoNext.text = if (isAutoNextEnabled) "Auto-next: On" else "Auto-next: Off"
    }

    private fun tryAutoPlayNextEpisode() {
        if (isAutoNextLaunching) return
        isAutoNextLaunching = true
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.Main) {
            val nextEpisode = withContext(Dispatchers.IO) { findNextEpisodeTarget() }

            if (isDestroyed || isFinishing) return@launch

            if (nextEpisode == null) {
                isAutoNextLaunching = false
                progressBar.visibility = View.GONE
                Toast.makeText(this@Video_payer, "No next episode available", Toast.LENGTH_SHORT).show()
                return@launch
            }

            Toast.makeText(
                this@Video_payer,
                "Playing S${nextEpisode.seasonNumber} E${nextEpisode.episodeNumber}",
                Toast.LENGTH_SHORT
            ).show()
            launchPlayForEpisode(nextEpisode)
        }
    }

    private fun findNextEpisodeTarget(): NextEpisodeTarget? {
        val currentSeason = showSNo.toIntOrNull() ?: return null
        val currentEpisode = showENo.toIntOrNull() ?: return null
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE

        fun findFirstAiredEpisode(seasonNo: Int, afterEpisode: Int = 0): Int? {
            val seasonInfo = fetch.fetchSeasonInfo(showId, seasonNo.toString()) ?: return null
            val episodes = seasonInfo.optJSONArray("episodes") ?: return null

            var bestEpisodeNumber: Int? = null
            for (index in 0 until episodes.length()) {
                val episode = episodes.optJSONObject(index) ?: continue
                val episodeNumber = episode.optInt("episode_number", 0)
                if (episodeNumber <= afterEpisode) continue
                if (!isEpisodeAired(episode.optString("air_date", ""), today, formatter)) continue

                if (bestEpisodeNumber == null || episodeNumber < bestEpisodeNumber) {
                    bestEpisodeNumber = episodeNumber
                }
            }
            return bestEpisodeNumber
        }

        findFirstAiredEpisode(currentSeason, currentEpisode)?.let { nextEpisode ->
            return NextEpisodeTarget(currentSeason, nextEpisode)
        }

        val showData = fetch.fetchShowData(showId, "tv") ?: return null
        val seasons = showData.optJSONArray("seasons") ?: return null
        val nextSeasons = mutableListOf<Int>()

        for (index in 0 until seasons.length()) {
            val season = seasons.optJSONObject(index) ?: continue
            val seasonNumber = season.optInt("season_number", -1)
            if (seasonNumber > currentSeason) {
                nextSeasons.add(seasonNumber)
            }
        }

        nextSeasons.sorted().forEach { seasonNumber ->
            val nextEpisode = findFirstAiredEpisode(seasonNumber)
            if (nextEpisode != null) {
                return NextEpisodeTarget(seasonNumber, nextEpisode)
            }
        }

        return null
    }

    private fun isEpisodeAired(
        airDate: String,
        today: LocalDate,
        formatter: DateTimeFormatter
    ): Boolean {
        if (airDate.isBlank() || airDate == "null") return true
        return try {
            !LocalDate.parse(airDate, formatter).isAfter(today)
        } catch (_: Exception) {
            true
        }
    }

    private fun launchPlayForEpisode(nextEpisode: NextEpisodeTarget) {
        shouldResumePlaybackWhenReady = false
        val intent = Intent(this, Play::class.java).apply {
            putExtra("imdb_code", showId)
            putExtra("type", "tv")
            putExtra("title", showTitle)
            putExtra("poster", showPoster)
            putExtra("backdrop", showBackdrop)
            putExtra("showBackdrop", showBackdrop)
            putExtra("seasonNo", nextEpisode.seasonNumber.toString())
            putExtra("episodeNo", nextEpisode.episodeNumber.toString())
        }
        startActivity(intent)
        finish()
    }

    // UI helpers
    // ──────────────────────────────────────────────────────────────────────────

    private fun toggleControls()  { if (isControlsVisible) hideControls() else showControls() }
    private fun showControls()    { isControlsVisible = true;  bottomBar.visibility = View.VISIBLE }
    private fun hideControls()    { isControlsVisible = false; bottomBar.visibility = View.GONE }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        @Suppress("DEPRECATION")
        if (isFullscreen) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            btnFullscreen.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            btnFullscreen.setImageResource(android.R.drawable.ic_menu_crop)
        }
    }

    /**
     * Shows a brief "+10s"/"-10s" style overlay. Guarded so rapid double-taps
     * don't stack multiple TextViews/animations with dangling listeners.
     */
    private fun showSeekFeedback(text: String) {
        // Cancel any animation still in flight so its onAnimationEnd callback
        // can't fire after we've already swapped in a new view.
        seekFeedbackAnimation?.setAnimationListener(null)
        seekFeedbackAnimation = null

        centerOverlay.removeAllViews()
        val tv = TextView(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 24f
            setPadding(40, 20, 40, 20)
            setBackgroundColor(Color.parseColor("#80000000"))
        }
        centerOverlay.addView(tv)
        centerOverlay.visibility = View.VISIBLE

        val animation = AlphaAnimation(1f, 0f).apply {
            duration = 1000
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(a: Animation?) = Unit
                override fun onAnimationRepeat(a: Animation?) = Unit
                override fun onAnimationEnd(a: Animation?) {
                    if (!isDestroyed && !isFinishing) {
                        centerOverlay.visibility = View.GONE
                    }
                    seekFeedbackAnimation = null
                }
            })
        }
        seekFeedbackAnimation = animation
        tv.startAnimation(animation)
    }

    private fun updatePlayPauseButton() {
        btnPlayPause.setImageResource(
            if (exoPlayer?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    private fun updateMuteButton() {
        btnMute.setImageResource(
            if (isMuted) android.R.drawable.ic_lock_silent_mode
            else         android.R.drawable.ic_lock_silent_mode_off
        )
    }

    private fun updateSpeedButton()   { btn_speed_text.text = "${currentSpeed}x" }
    private fun updateQualityButton() { btnQuality.text = getCurrentVideoQuality() }

    private fun formatTime(ms: Long): String {
        val s  = ms / 1000
        val h  = s / 3600
        val m  = (s % 3600) / 60
        val sc = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sc)
        else       "%02d:%02d".format(m, sc)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Dialogs
    // ──────────────────────────────────────────────────────────────────────────

    private fun showSpeedDialog() {
        val options = arrayOf("0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "2.0x")
        android.app.AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setTitle("Playback Speed")
            .setSingleChoiceItems(options, currentSpeedIndex) { dialog, which ->
                currentSpeedIndex = which
                currentSpeed = playbackSpeeds[which]
                exoPlayer?.setPlaybackSpeed(currentSpeed)
                updateSpeedButton()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showQualityDialog() {
        android.app.AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setTitle("Video Quality")
            .setSingleChoiceItems(qualityOptions.toTypedArray(), currentQualityIndex) { dialog, which ->
                currentQualityIndex = which
                setVideoQuality(which)
                updateQualityButton()
                Toast.makeText(this, "Quality: ${qualityOptions[which]}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSettingsDialog() {
        android.app.AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setTitle("Settings")
            .setItems(arrayOf("Subtitles", "Video Info")) { _, which ->
                when (which) {
                    0 -> Toast.makeText(this, "Subtitles not available", Toast.LENGTH_SHORT).show()
                    1 -> showVideoInfo()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showVideoInfo() {
        val player = exoPlayer ?: return
        val fmt = player.videoFormat
        val info = buildString {
            append("Duration: ${formatTime(player.duration)}\n")
            append("Position: ${formatTime(player.currentPosition)}\n")
            append("Speed: ${currentSpeed}x\n")
            append("Quality: ${getCurrentVideoQuality()}\n")
            if (fmt != null) {
                append("\nResolution: ${fmt.width}×${fmt.height}\n")
                append("Codec: ${fmt.codecs}\n")
                append("Bitrate: ${fmt.bitrate / 1000} kbps\n")
                append("Frame rate: ${fmt.frameRate} fps")
            }
        }
        Toast.makeText(this, info, Toast.LENGTH_LONG).show()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Quality management
    // ──────────────────────────────────────────────────────────────────────────
    //
    // labelForResolution() is the single source of truth for width/height ->
    // "NNNp" bucketing, used both when enumerating available tracks and when
    // reporting the currently-playing format. Previously this logic was
    // duplicated (and could silently drift) across two functions.

    private fun labelForResolution(width: Int, height: Int): String = when {
        width >= 1920 && height >= 1080 -> "1080p"
        width >= 1280 && height >= 720  -> "720p"
        width >= 854  && height >= 480  -> "480p"
        width >= 640  && height >= 360  -> "360p"
        width >= 426  && height >= 240  -> "240p"
        else -> "${height}p"
    }

    private fun setVideoQuality(index: Int) {
        val player = exoPlayer ?: return
        val selector = player.trackSelector as? DefaultTrackSelector ?: return
        if (index >= availableQualities.size) return

        val label = availableQualities[index]
        val height = label.removeSuffix("p").toIntOrNull()

        val params = selector.buildUponParameters()

        if (label == "Auto" || height == null) {
            selector.setParameters(
                params.clearVideoSizeConstraints()
                    .setMaxVideoSize(1920, 1080)
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .setAllowVideoNonSeamlessAdaptiveness(true)
            )
        } else {
            val width = when {
                height >= 2160 -> 3840
                height >= 1440 -> 2560
                height >= 1080 -> 1920
                height >= 720  -> 1280
                height >= 480  -> 854
                height >= 360  -> 640
                else           -> 426
            }
            selector.setParameters(
                params.setMaxVideoSize(width, height)
                    .setMinVideoSize(width, height)
                    .setAllowVideoMixedMimeTypeAdaptiveness(false)
                    .setAllowVideoNonSeamlessAdaptiveness(false)
            )
        }
        if (BuildConfig.DEBUG) Log.d("Video_payer", "Quality set to $label")
    }

    private fun getCurrentVideoQuality(): String {
        val fmt = exoPlayer?.videoFormat ?: return "Auto"
        if (fmt.width <= 0 || fmt.height <= 0) return "Auto"
        return labelForResolution(fmt.width, fmt.height)
    }

    private fun updateAvailableQualities(tracks: Tracks) {
        val videoGroup = tracks.groups.find { it.type == C.TRACK_TYPE_VIDEO } ?: run {
            availableQualities = listOf("Auto")
            return
        }

        val labels = (0 until videoGroup.length)
            .map { videoGroup.getTrackFormat(it) }
            .filter { it.width > 0 && it.height > 0 }
            .map { fmt -> labelForResolution(fmt.width, fmt.height) }
            .distinct()
            .sortedByDescending { it.removeSuffix("p").toIntOrNull() ?: 0 }

        availableQualities = listOf("Auto") + labels
        if (BuildConfig.DEBUG) Log.d("Video_payer", "Qualities: $availableQualities")
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Player release
    // ──────────────────────────────────────────────────────────────────────────

    private fun releasePlayer() {
        exoPlayer?.let { player ->
            player.removeListener(this)
            player.stop()
            player.clearMediaItems()
            player.release()
            exoPlayer = null
            shouldResumePlaybackWhenReady = false
            shouldResumeAfterAudioFocusGain = false
            waitingForNetworkRecovery = false
            shouldResumeAfterNetworkRecovery = false
            currentVideoUrl = null
            availableQualities = listOf("Auto")
        }
        audioManager.abandonAudioFocus(audioFocusChangeListener)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Key events
    // ──────────────────────────────────────────────────────────────────────────

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_UP && !isControlsVisible) {
            showControls()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Companion — static launcher
    // ──────────────────────────────────────────────────────────────────────────

    companion object {

        private const val MIN_BUFFER_MS = 60_000          // 1 minute
        private const val MAX_BUFFER_MS = 180_000         // 3 minutes
        private const val BUFFER_FOR_PLAYBACK_MS = 15_000 // wait ~15 sec before starting
        private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 30_000 // wait ~30 sec after stall

        fun playVideoExternally(
            context: Context,
            videoUrl: String,
            referer: String?,
            userAgent: String?,
            showId: String,
            showType: String,
            showTitle: String,
            showPoster: String,
            showBackdrop: String,
            showSNo: String,
            showENo: String
        ) {
            val intent = Intent(context, Video_payer::class.java).apply {
                putExtra("video_url",   videoUrl)
                putExtra("referer",     referer)
                putExtra("userAgent",   userAgent)
                putExtra("showId",      showId)
                putExtra("showType",    showType)
                putExtra("showTitle",   showTitle)
                putExtra("showPoster",  showPoster)
                putExtra("showBackdrop",showBackdrop)
                putExtra("showSNo",     showSNo)
                putExtra("showENo",     showENo)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(intent)
        }
    }
}

private data class NextEpisodeTarget(
    val seasonNumber: Int,
    val episodeNumber: Int
)

package com.example.onyx

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.ui.TrackSelectionDialogBuilder
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionParameters

// Singleton to manage active player instance
object PlayerManager {
    private var activePlayer: ExoPlayer? = null
    private var activeContext: Context? = null

    fun setActivePlayer(player: ExoPlayer, context: Context) {
        // Release any previously active player
        activePlayer?.release()
        activePlayer = player
        activeContext = context
    }

    fun clearActivePlayer() {
        activePlayer?.release()
        activePlayer = null
        activeContext = null
    }

    fun getActivePlayer(): ExoPlayer? = activePlayer
    fun getActiveContext(): Context? = activeContext
}

class Video_payer : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private var playerView: StyledPlayerView? = null
    private var progressBar: ProgressBar? = null
    private var overlayContainer: View? = null
    private var bottomBar: View? = null
    private var centerOverlay: View? = null
    private var playPauseButton: ImageButton? = null
    private var rewindButton: ImageButton? = null
    private var fastForwardButton: ImageButton? = null
    private var speedButton: ImageButton? = null
    private var muteButton: ImageButton? = null
    private var subtitlesButton: ImageButton? = null
    private var qualityButton: TextView? = null
    private var refreshButton: ImageButton? = null
    private var settingsButton: ImageButton? = null
    private var closeButton: ImageButton? = null
    private var seekBar: SeekBar? = null
    private var currentTimeText: TextView? = null
    private var durationText: TextView? = null
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition = 0L
    private var previousVolume = 1f
    private var controlsVisible = false
    private var isSeeking = false
    private val uiHandler = Handler(Looper.getMainLooper())
    private val controlsAutoHideRunnable = Runnable { hideControls() }
    private val progressUpdateRunnable = object : Runnable {
        override fun run() {
            updateProgress()
            uiHandler.postDelayed(this, 500)
        }
    }
    private var trackSelector: DefaultTrackSelector? = null
    private var currentSpeed = 1.0f
    private var currentQuality = "Auto"
    private var isMuted = false
    private var subtitlesEnabled = false

    // Speed options
    private val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    private val speedLabels = listOf("0.5x", "0.75x", "1x", "1.25x", "1.5x", "2x")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_payer)
        
        // Prevent screen from sleeping while this Activity is visible
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        initializeViews()
        setupControls()
        setupDpadNavigation()
        setupBackPressHandler()
        hideSystemUi()
    }

    private fun initializeViews() {
        playerView = findViewById(R.id.player_view)
        progressBar = findViewById(R.id.progress_bar)
        overlayContainer = findViewById(R.id.overlay_container)
        bottomBar = findViewById(R.id.bottom_bar)
        centerOverlay = findViewById(R.id.center_overlay)
        playPauseButton = findViewById(R.id.btn_play_pause)
        rewindButton = findViewById(R.id.btn_rewind)
        fastForwardButton = findViewById(R.id.btn_fast_forward)
        speedButton = findViewById(R.id.btn_speed)
        muteButton = findViewById(R.id.btn_mute)
        subtitlesButton = findViewById(R.id.btn_subtitles)
        qualityButton = findViewById(R.id.btn_quality)
        refreshButton = findViewById(R.id.btn_refresh)
        settingsButton = findViewById(R.id.btn_settings)
        closeButton = findViewById(R.id.btn_close)
        seekBar = findViewById(R.id.seek_bar)
        currentTimeText = findViewById(R.id.txt_current_time)
        durationText = findViewById(R.id.txt_duration)
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (controlsVisible) {
                    hideControls()
                } else {
                finish()
                }
            }
        })
    }

    private fun initializePlayer() {
        val videoUrl = intent.getStringExtra("video_url") ?: return

        if (trackSelector == null) {
            trackSelector = DefaultTrackSelector(this).apply {
                setParameters(
                    parameters
                        .buildUpon()
                        .setForceHighestSupportedBitrate(true)
                        .setPreferredVideoMimeTypes("video/avc", "video/hevc", "video/av1")
                        .build()
                )
            }
        }

        // Check if there's already an active player
        val existingPlayer = PlayerManager.getActivePlayer()
        if (existingPlayer != null && PlayerManager.getActiveContext() != this) {
            // Another activity has the player, release it first
            PlayerManager.clearActivePlayer()
        }

        // Create new player if none exists
        if (player == null) {
            player = ExoPlayer.Builder(this)
                .setTrackSelector(trackSelector!!)
                .build()
            PlayerManager.setActivePlayer(player!!, this)
        }

        // Set up player listener
        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> showLoading()
                    Player.STATE_READY -> {
                        hideLoading()
                        updateDuration()
                        updateQualityButton()
                    }
                    Player.STATE_ENDED -> {
                        hideLoading()
                        player?.seekTo(0)
                        player?.pause()
                    }
                    Player.STATE_IDLE -> hideLoading()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlayPauseIcon(isPlaying)
            }

            override fun onPlayerError(error: PlaybackException) {
                hideLoading()
                showError("Playback error: ${error.message}")
            }
        })

        // Attach media item
        val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
        player?.apply {
            setMediaItem(mediaItem)
            playWhenReady = this@Video_payer.playWhenReady
            seekTo(currentWindow, playbackPosition)
            prepare()
        }

        playerView?.player = player
        playerView?.setOnClickListener { toggleControls() }

        uiHandler.post(progressUpdateRunnable)
        updatePlayPauseIcon(player?.isPlaying == true)
    }

    @SuppressLint("InlinedApi")
    private fun hideSystemUi() {
        playerView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }

    private fun showLoading() {
        progressBar?.isVisible = true
    }

    private fun hideLoading() {
        progressBar?.isVisible = false
    }

    private fun showError(message: String) {
        progressBar?.isVisible = false
        // You could show a toast or error dialog here
    }

    private fun setupControls() {
        playPauseButton?.setOnClickListener {
            player?.let { p ->
                if (p.isPlaying) p.pause() else p.play()
                showCenterFeedback(if (p.isPlaying) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause, "")
                resetAutoHide()
            }
        }

        rewindButton?.setOnClickListener {
            player?.let { p ->
                p.seekTo((p.currentPosition - 10_000L).coerceAtLeast(0L))
                showCenterFeedback(android.R.drawable.ic_media_rew, "-10s")
                resetAutoHide()
            }
        }

        fastForwardButton?.setOnClickListener {
            player?.let { p ->
                val duration = if (p.duration > 0) p.duration else Long.MAX_VALUE
                p.seekTo((p.currentPosition + 10_000L).coerceAtMost(duration))
                showCenterFeedback(android.R.drawable.ic_media_ff, "+10s")
                resetAutoHide()
            }
        }

        speedButton?.setOnClickListener {
            showSpeedDialog()
            resetAutoHide()
        }

        muteButton?.setOnClickListener {
            toggleMute()
            resetAutoHide()
        }

        subtitlesButton?.setOnClickListener {
            toggleSubtitles()
            resetAutoHide()
        }

        qualityButton?.setOnClickListener {
            showQualityDialog()
            resetAutoHide()
        }

        refreshButton?.setOnClickListener {
            refreshVideo()
            resetAutoHide()
        }

        settingsButton?.setOnClickListener {
            showSettingsDialog()
            resetAutoHide()
        }

        closeButton?.setOnClickListener {
            finish()
        }

        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val duration = player?.duration ?: 0L
                    if (duration > 0L) {
                        val position = duration * progress / 1000L
                        currentTimeText?.text = formatTime(position)
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isSeeking = true
                resetAutoHide()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isSeeking = false
                val duration = player?.duration ?: 0L
                if (duration > 0L && seekBar != null) {
                    val position = duration * seekBar.progress / 1000L
                    player?.seekTo(position)
                }
                resetAutoHide()
            }
        })
    }

    private fun showSpeedDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Playback Speed")
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_single_choice, speedLabels)
        val listView = ListView(this)
        listView.adapter = adapter
        listView.choiceMode = ListView.CHOICE_MODE_SINGLE
        
        // Set current selection
        val currentIndex = speedOptions.indexOf(currentSpeed)
        if (currentIndex >= 0) {
            listView.setItemChecked(currentIndex, true)
        }
        
        builder.setView(listView)
        builder.setPositiveButton("OK") { _, _ ->
            val selectedIndex = listView.checkedItemPosition
            if (selectedIndex >= 0 && selectedIndex < speedOptions.size) {
                setPlaybackSpeed(speedOptions[selectedIndex])
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun setPlaybackSpeed(speed: Float) {
        currentSpeed = speed
        player?.setPlaybackSpeed(speed)
        speedButton?.contentDescription = "${speed}x"
        showCenterFeedback(android.R.drawable.ic_popup_sync, "${speed}x Speed")
    }

    private fun toggleMute() {
        player?.let { p ->
            if (isMuted) {
                p.volume = if (previousVolume <= 0f) 1f else previousVolume
                isMuted = false
                muteButton?.setImageResource(android.R.drawable.ic_lock_silent_mode_off)
                showCenterFeedback(android.R.drawable.ic_lock_silent_mode_off, "Unmuted")
            } else {
                previousVolume = p.volume
                p.volume = 0f
                isMuted = true
                muteButton?.setImageResource(android.R.drawable.ic_lock_silent_mode)
                showCenterFeedback(android.R.drawable.ic_lock_silent_mode, "Muted")
            }
        }
    }

    private fun toggleSubtitles() {
        subtitlesEnabled = !subtitlesEnabled
        if (subtitlesEnabled) {
            showCaptionsDialog()
        } else {
            // Disable subtitles
            trackSelector?.parameters?.buildUpon()
                ?.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                ?.build()?.let {
                    trackSelector?.setParameters(
                        it
                    )
                }
            subtitlesButton?.setImageResource(android.R.drawable.ic_menu_agenda)
            showCenterFeedback(android.R.drawable.ic_menu_agenda, "Subtitles Off")
        }
    }

    private fun showCaptionsDialog() {
        val player = player ?: return
        val builder = TrackSelectionDialogBuilder(
            this,
            "Subtitles",
            player,
            C.TRACK_TYPE_TEXT
        )
        builder.setAllowMultipleOverrides(false)
            .setShowDisableOption(true)
            .build()
            .show()
    }

    private fun showQualityDialog() {
        val selector = trackSelector ?: return
        val player = player ?: return
        
        // Use TrackSelectionDialogBuilder for quality selection
        val builder = TrackSelectionDialogBuilder(
            this,
            "Video Quality",
            player,
            C.TRACK_TYPE_VIDEO
        )
        builder.setAllowMultipleOverrides(false)
            .setShowDisableOption(true)
            .build()
            .show()
    }

    private fun setVideoQuality(quality: String) {
        currentQuality = quality
        qualityButton?.text = quality
        showCenterFeedback(android.R.drawable.ic_popup_sync, "Quality: $quality")
    }

    private fun updateQualityButton() {
        qualityButton?.text = currentQuality
    }

    private fun refreshVideo() {
        player?.prepare()
        showCenterFeedback(android.R.drawable.ic_popup_sync, "Refreshed")
    }

    private fun showSettingsDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Settings")
        builder.setMessage("Additional playback settings will be available here.")
        builder.setPositiveButton("OK", null)
        builder.show()
    }

    private fun toggleControls() {
        if (controlsVisible) hideControls() else showControls()
    }

    private fun showControls() {
        controlsVisible = true
        bottomBar?.isVisible = true
        playPauseButton?.requestFocus()
        resetAutoHide()
    }

    private fun hideControls() {
        controlsVisible = false
        bottomBar?.isVisible = false
        uiHandler.removeCallbacks(controlsAutoHideRunnable)
    }

    private fun resetAutoHide() {
        uiHandler.removeCallbacks(controlsAutoHideRunnable)
        uiHandler.postDelayed(controlsAutoHideRunnable, 3000)
    }

    private fun updateProgress() {
        if (isSeeking) return

        val p = player ?: return
        val duration = p.duration
        val position = p.currentPosition
        if (duration > 0L) {
            val progress = (position * 1000L / duration).toInt().coerceIn(0, 1000)
            seekBar?.progress = progress
            currentTimeText?.text = formatTime(position)
            durationText?.text = formatTime(duration)
        } else {
            seekBar?.progress = 0
            currentTimeText?.text = formatTime(position)
        }
    }

    private fun updateDuration() {
        val duration = player?.duration ?: 0L
        if (duration > 0L) {
            durationText?.text = formatTime(duration)
        }
    }

    private fun updatePlayPauseIcon(isPlaying: Boolean) {
        playPauseButton?.setImageResource(
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )
    }

    private fun setupDpadNavigation() {
        // Ensure all controls are focusable for TV
        playPauseButton?.isFocusable = true
        rewindButton?.isFocusable = true
        fastForwardButton?.isFocusable = true
        speedButton?.isFocusable = true
        muteButton?.isFocusable = true
        subtitlesButton?.isFocusable = true
        qualityButton?.isFocusable = true
        refreshButton?.isFocusable = true
        settingsButton?.isFocusable = true
        closeButton?.isFocusable = true
        seekBar?.isFocusable = true

        // Set up focus chain
        playPauseButton?.nextFocusRightId = R.id.btn_speed
        speedButton?.nextFocusLeftId = R.id.btn_play_pause
        speedButton?.nextFocusRightId = R.id.btn_mute
        muteButton?.nextFocusLeftId = R.id.btn_speed
        muteButton?.nextFocusRightId = R.id.btn_subtitles
        subtitlesButton?.nextFocusLeftId = R.id.btn_mute
        subtitlesButton?.nextFocusRightId = R.id.btn_quality
        qualityButton?.nextFocusLeftId = R.id.btn_subtitles
        qualityButton?.nextFocusRightId = R.id.btn_refresh
        refreshButton?.nextFocusLeftId = R.id.btn_quality
        refreshButton?.nextFocusRightId = R.id.btn_settings
        settingsButton?.nextFocusLeftId = R.id.btn_refresh
        settingsButton?.nextFocusRightId = R.id.btn_close
        closeButton?.nextFocusLeftId = R.id.btn_settings

        // Add focus change listeners
        val focusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (v is ImageButton || v is TextView) {
                v.scaleX = if (hasFocus) 1.2f else 1.0f
                v.scaleY = if (hasFocus) 1.2f else 1.0f
            }
        }

        playPauseButton?.setOnFocusChangeListener(focusChangeListener)
        rewindButton?.setOnFocusChangeListener(focusChangeListener)
        fastForwardButton?.setOnFocusChangeListener(focusChangeListener)
        speedButton?.setOnFocusChangeListener(focusChangeListener)
        muteButton?.setOnFocusChangeListener(focusChangeListener)
        subtitlesButton?.setOnFocusChangeListener(focusChangeListener)
        qualityButton?.setOnFocusChangeListener(focusChangeListener)
        refreshButton?.setOnFocusChangeListener(focusChangeListener)
        settingsButton?.setOnFocusChangeListener(focusChangeListener)
        closeButton?.setOnFocusChangeListener(focusChangeListener)
        seekBar?.setOnFocusChangeListener { v, hasFocus ->
            v.scaleX = if (hasFocus) 1.05f else 1.0f
            v.scaleY = if (hasFocus) 1.05f else 1.0f
        }
    }

    private fun showCenterFeedback(iconRes: Int, text: String) {
        centerOverlay?.apply {
            alpha = 0f
            isVisible = true
            animate().alpha(1f).setDuration(100).withEndAction {
                uiHandler.postDelayed({
                    centerOverlay?.animate()?.alpha(0f)?.setDuration(150)?.withEndAction {
                        centerOverlay?.isVisible = false
                    }?.start()
                }, 700)
            }.start()
        }
    }

    private fun formatTime(millis: Long): String {
        var seconds = millis / 1000
        val hours = seconds / 3600
        seconds %= 3600
        val minutes = seconds / 60
        seconds %= 60
        return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds) else String.format("%02d:%02d", minutes, seconds)
    }

    public override fun onStart() {
        super.onStart()
        if (Util.SDK_INT >= 24) {
            initializePlayer()
        }
    }

    public override fun onPause() {
        super.onPause()
        // Auto-pause when going to background
        if (player?.isPlaying == true) {
            player?.pause()
            playWhenReady = true
        }
        if (Util.SDK_INT < 24) {
            releasePlayer()
        }
    }

    public override fun onStop() {
        super.onStop()
        if (player?.isPlaying == true) {
            player?.pause()
            playWhenReady = true
        }
        if (Util.SDK_INT >= 24) {
            releasePlayer()
        }
    }

    public override fun onResume() {
        super.onResume()
        if (Util.SDK_INT < 24 || player == null) {
            initializePlayer()
        } else {
            if (playWhenReady) {
                player?.play()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus && player?.isPlaying == true) {
            player?.pause()
            playWhenReady = true
        }
    }

    private fun releasePlayer() {
        player?.let {
            playbackPosition = it.currentPosition
            currentWindow = it.currentMediaItemIndex
            playWhenReady = it.playWhenReady

            if (PlayerManager.getActivePlayer() == it) {
                PlayerManager.clearActivePlayer()
            }

            it.release()
        }
        player = null
        uiHandler.removeCallbacks(progressUpdateRunnable)
        uiHandler.removeCallbacks(controlsAutoHideRunnable)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!controlsVisible) {
            showControls()
        } else {
            resetAutoHide()
        }

        return when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_SPACE -> {
                player?.let {
                    if (it.isPlaying) it.pause() else it.play()
                }
                true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                player?.play()
                true
            }
            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                player?.pause()
                true
            }
            KeyEvent.KEYCODE_MEDIA_STOP -> {
                player?.stop()
                true
            }
            KeyEvent.KEYCODE_VOLUME_MUTE -> {
                toggleMute()
                true
            }
            KeyEvent.KEYCODE_CAPTIONS -> {
                toggleSubtitles()
                true
            }
            KeyEvent.KEYCODE_MENU -> {
                showQualityDialog()
                true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                when (getCurrentFocusId()) {
                    R.id.btn_play_pause -> {
                        player?.let { p -> if (p.isPlaying) p.pause() else p.play() }
                    }
                    R.id.btn_rewind -> {
                        player?.let { p ->
                            p.seekTo((p.currentPosition - 10_000L).coerceAtLeast(0L))
                            showCenterFeedback(android.R.drawable.ic_media_rew, "-10s")
                        }
                    }
                    R.id.btn_fast_forward -> {
                        player?.let { p ->
                            val duration = if (p.duration > 0) p.duration else Long.MAX_VALUE
                            p.seekTo((p.currentPosition + 10_000L).coerceAtMost(duration))
                            showCenterFeedback(android.R.drawable.ic_media_ff, "+10s")
                        }
                    }
                    R.id.btn_speed -> {
                        showSpeedDialog()
                    }
                    R.id.btn_mute -> {
                        toggleMute()
                    }
                    R.id.btn_subtitles -> {
                        toggleSubtitles()
                    }
                    R.id.btn_quality -> {
                        showQualityDialog()
                    }
                    R.id.btn_refresh -> {
                        refreshVideo()
                    }
                    R.id.btn_settings -> {
                        showSettingsDialog()
                    }
                    R.id.btn_close -> {
                        finish()
                    }
                    R.id.seek_bar -> {
                        isSeeking = !isSeeking
                    }
                    else -> {
                        player?.let { p -> if (p.isPlaying) p.pause() else p.play() }
                    }
                }
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun getCurrentFocusId(): Int {
        return currentFocus?.id ?: View.NO_ID
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }
}
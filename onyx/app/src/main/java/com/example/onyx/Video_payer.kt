package com.example.onyx

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
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

    fun setActivePlayer(player: ExoPlayer) {
        // Release any previously active player
        activePlayer?.release()
        activePlayer = player
    }

    fun clearActivePlayer() {
        activePlayer = null
    }

    fun getActivePlayer(): ExoPlayer? = activePlayer
}

class Video_payer : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private var playerView: StyledPlayerView? = null
    private var progressBar: ProgressBar? = null
    private var overlayContainer: View? = null
    private var bottomBar: View? = null
    private var topBar: View? = null
    private var centerOverlay: View? = null
    private var playPauseButton: ImageButton? = null
    private var rewindButton: ImageButton? = null
    private var fastForwardButton: ImageButton? = null
    private var favoriteButton: ImageButton? = null
    private var speedButton: ImageButton? = null
    private var volumeButton: ImageButton? = null
    private var subtitlesButton: ImageButton? = null
    private var autoButton: android.widget.Button? = null
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_payer)
        // Prevent screen from sleeping while this Activity is visible
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        playerView = findViewById(R.id.player_view)
        progressBar = findViewById(R.id.progress_bar)
        overlayContainer = findViewById(R.id.overlay_container)
        bottomBar = findViewById(R.id.bottom_bar)
        centerOverlay = findViewById(R.id.center_overlay)
        playPauseButton = findViewById(R.id.btn_play_pause)
        rewindButton = findViewById(R.id.btn_rewind)
        fastForwardButton = findViewById(R.id.btn_fast_forward)
        favoriteButton = findViewById(R.id.btn_favorite)
        speedButton = findViewById(R.id.btn_speed)
        volumeButton = findViewById(R.id.btn_volume)
        subtitlesButton = findViewById(R.id.btn_subtitles)
        autoButton = findViewById(R.id.btn_auto)
        refreshButton = findViewById(R.id.btn_refresh)
        settingsButton = findViewById(R.id.btn_settings)
        closeButton = findViewById(R.id.btn_close)
        seekBar = findViewById(R.id.seek_bar)
        currentTimeText = findViewById(R.id.txt_current_time)
        durationText = findViewById(R.id.txt_duration)

        setupControls()
        setupDpadNavigation()

        // Setup back press handling
        setupBackPressHandler()

        // Hide system UI for immersive experience
        hideSystemUi()
    }

    private fun setupBackPressHandler() {
        // Handle back navigation - simply finish the activity
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
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

        // Reuse the existing global player if available
        player = PlayerManager.getActivePlayer()
        if (player == null) {
            player = ExoPlayer.Builder(this)
                .setTrackSelector(trackSelector!!)
                .build().apply {
                    PlayerManager.setActivePlayer(this)
                }
        }

        // Attach media item (replace the source if new video is requested)
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
        updateDuration()
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

        favoriteButton?.setOnClickListener {
            // Toggle favorite status
            showCenterFeedback(android.R.drawable.btn_star_big_on, "Added to Favorites")
            resetAutoHide()
        }

        speedButton?.setOnClickListener {
            showQualityDialog()
            resetAutoHide()
        }

        volumeButton?.setOnClickListener {
            player?.let { p ->
                if (p.volume > 0f) {
                    previousVolume = p.volume
                    p.volume = 0f
                } else {
                    p.volume = if (previousVolume <= 0f) 1f else previousVolume
                }
                updateVolumeIcon(p.volume == 0f)
                showCenterFeedback(
                    if (p.volume == 0f) android.R.drawable.ic_lock_silent_mode else android.R.drawable.ic_lock_silent_mode_off,
                    if (p.volume == 0f) "Muted" else "Unmuted"
                )
                resetAutoHide()
            }
        }

        subtitlesButton?.setOnClickListener {
            showCaptionsDialog()
            resetAutoHide()
        }

        autoButton?.setOnClickListener {
            // Toggle auto mode
            showCenterFeedback(android.R.drawable.ic_popup_sync, "Auto Mode")
            resetAutoHide()
        }

        refreshButton?.setOnClickListener {
            // Refresh/reload video
            player?.prepare()
            showCenterFeedback(android.R.drawable.ic_popup_sync, "Refreshed")
            resetAutoHide()
        }

        settingsButton?.setOnClickListener {
            showQualityDialog()
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

    private fun showQualityDialog() {
        val selector = trackSelector ?: return
        val player = player ?: return
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

    private fun showCaptionsDialog() {
        val player = player ?: return
        val builder = TrackSelectionDialogBuilder(
            this,
            "Captions",
            player,
            C.TRACK_TYPE_TEXT
        )
        builder.setAllowMultipleOverrides(false)
            .setShowDisableOption(true)
            .build()
            .show()
    }

    private fun toggleControls() {
        if (controlsVisible) hideControls() else showControls()
    }

    private fun showControls() {
        controlsVisible = true
        bottomBar?.isVisible = true
        topBar?.isVisible = true
        // Move focus to primary control for TV remotes
        playPauseButton?.requestFocus()
        resetAutoHide()
    }

    private fun hideControls() {
        controlsVisible = false
        bottomBar?.isVisible = false
        topBar?.isVisible = false
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

    private fun updateVolumeIcon(isMuted: Boolean) {
        volumeButton?.setImageResource(
            if (isMuted) android.R.drawable.ic_lock_silent_mode else android.R.drawable.ic_lock_silent_mode_off
        )
    }

    private fun toggleFullscreen() {
        val view = playerView ?: return
        val isFullscreen = (view.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN) != 0
        if (isFullscreen) {
            view.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        } else {
            hideSystemUi()
        }
    }

    private fun setupDpadNavigation() {
        // Ensure all controls are focusable for TV
        playPauseButton?.isFocusable = true
        rewindButton?.isFocusable = true
        fastForwardButton?.isFocusable = true
        favoriteButton?.isFocusable = true
        speedButton?.isFocusable = true
        volumeButton?.isFocusable = true
        subtitlesButton?.isFocusable = true
        autoButton?.isFocusable = true
        refreshButton?.isFocusable = true
        settingsButton?.isFocusable = true
        closeButton?.isFocusable = true
        seekBar?.isFocusable = true

        // Set up a complete focus chain for TV navigation
        favoriteButton?.nextFocusRightId = R.id.btn_speed
        favoriteButton?.nextFocusDownId = R.id.seek_bar

        speedButton?.nextFocusLeftId = R.id.btn_favorite
        speedButton?.nextFocusRightId = R.id.btn_volume
        speedButton?.nextFocusDownId = R.id.seek_bar

        volumeButton?.nextFocusLeftId = R.id.btn_speed
        volumeButton?.nextFocusRightId = R.id.btn_subtitles
        volumeButton?.nextFocusDownId = R.id.seek_bar

        subtitlesButton?.nextFocusLeftId = R.id.btn_volume
        subtitlesButton?.nextFocusRightId = R.id.btn_auto
        subtitlesButton?.nextFocusDownId = R.id.seek_bar

        autoButton?.nextFocusLeftId = R.id.btn_subtitles
        autoButton?.nextFocusRightId = R.id.btn_refresh
        autoButton?.nextFocusDownId = R.id.seek_bar

        refreshButton?.nextFocusLeftId = R.id.btn_auto
        refreshButton?.nextFocusRightId = R.id.btn_settings
        refreshButton?.nextFocusDownId = R.id.seek_bar

        settingsButton?.nextFocusLeftId = R.id.btn_refresh
        settingsButton?.nextFocusRightId = R.id.btn_close
        settingsButton?.nextFocusDownId = R.id.seek_bar

        closeButton?.nextFocusLeftId = R.id.btn_settings
        closeButton?.nextFocusDownId = R.id.seek_bar

        seekBar?.nextFocusUpId = R.id.btn_favorite
        seekBar?.nextFocusRightId = R.id.txt_duration

        durationText?.nextFocusLeftId = R.id.seek_bar
        durationText?.nextFocusUpId = R.id.btn_favorite

        // Add focus change listeners to show visual feedback
        val focusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (v is ImageButton || v is android.widget.Button) {
                v.scaleX = if (hasFocus) 1.2f else 1.0f
                v.scaleY = if (hasFocus) 1.2f else 1.0f
            }
        }

        playPauseButton?.setOnFocusChangeListener(focusChangeListener)
        rewindButton?.setOnFocusChangeListener(focusChangeListener)
        fastForwardButton?.setOnFocusChangeListener(focusChangeListener)
        favoriteButton?.setOnFocusChangeListener(focusChangeListener)
        speedButton?.setOnFocusChangeListener(focusChangeListener)
        volumeButton?.setOnFocusChangeListener(focusChangeListener)
        subtitlesButton?.setOnFocusChangeListener(focusChangeListener)
        autoButton?.setOnFocusChangeListener(focusChangeListener)
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
            playWhenReady = true // Remember we want to resume later
        }

        if (Util.SDK_INT < 24) {
            releasePlayer()
        }
    }

    public override fun onStop() {
        super.onStop()

        // Double-check in case onPause wasn't called or didn't work
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
            // Auto-resume if we were playing before backgrounding
            if (playWhenReady) {
                player?.play()
            }
        }
    }

    // Optional: Handle focus changes for immediate response
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (!hasFocus && player?.isPlaying == true) {
            // Immediately pause when losing focus (home button, recent apps, etc.)
            player?.pause()
            playWhenReady = true
        }
    }

    private fun releasePlayer() {
        player?.let {
            playbackPosition = it.currentPosition
            currentWindow = it.currentMediaItemIndex
            playWhenReady = it.playWhenReady

            // Only clear from manager if this is the active player
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
        // Show controls when any key is pressed
        if (!controlsVisible) {
            showControls()
        } else {
            resetAutoHide()
        }

        // Handle media controls for TV remote
        return when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_SPACE -> {
                player?.let {
                    if (it.isPlaying) {
                        it.pause()
                    } else {
                        it.play()
                    }
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
            // Mute toggle
            KeyEvent.KEYCODE_VOLUME_MUTE -> {
                player?.let { p ->
                    if (p.volume > 0f) {
                        previousVolume = p.volume
                        p.volume = 0f
                    } else {
                        p.volume = if (previousVolume <= 0f) 1f else previousVolume
                    }
                    updateVolumeIcon(p.volume == 0f)
                }
                true
            }
            // Open captions dialog (often KEYCODE_CAPTIONS on some remotes)
            KeyEvent.KEYCODE_CAPTIONS -> {
                showCaptionsDialog()
                true
            }
            // Use MENU key for Quality dialog on TV remotes
            KeyEvent.KEYCODE_MENU -> {
                showQualityDialog()
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (isSeeking) {
                    // If seeking, adjust the seek position
                    val progress = (seekBar?.progress ?: 0) - 10
                    seekBar?.progress = progress.coerceAtLeast(0)
                    val duration = player?.duration ?: 0L
                    if (duration > 0L) {
                        val position = duration * progress / 1000L
                        currentTimeText?.text = formatTime(position)
                    }
                    return true
                }

                // If not seeking, check if we're on the first control in a row
                when (getCurrentFocusId()) {
                    R.id.btn_favorite -> {
                        // Already at leftmost control, do favorite action
                        showCenterFeedback(android.R.drawable.btn_star_big_on, "Added to Favorites")
                    }
                    else -> {
                        // Let the system handle focus navigation
                        return false
                    }
                }
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (isSeeking) {
                    // If seeking, adjust the seek position
                    val progress = (seekBar?.progress ?: 0) + 10
                    seekBar?.progress = progress.coerceAtMost(1000)
                    val duration = player?.duration ?: 0L
                    if (duration > 0L) {
                        val position = duration * progress / 1000L
                        currentTimeText?.text = formatTime(position)
                    }
                    return true
                }

                // If not seeking, check if we're on the last control in a row
                when (getCurrentFocusId()) {
                    R.id.btn_close -> {
                        // Already at rightmost control, do close action
                        finish()
                    }
                    else -> {
                        // Let the system handle focus navigation
                        return false
                    }
                }
                true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (isSeeking) {
                    // Exit seeking mode and move focus up
                    isSeeking = false
                    playPauseButton?.requestFocus()
                    return true
                }

                // If focus is on bottom row, move to top row
                when (getCurrentFocusId()) {
                    R.id.seek_bar, R.id.txt_duration -> {
                        favoriteButton?.requestFocus()
                        return true
                    }
                    else -> {
                        // Let the system handle focus navigation
                        return false
                    }
                }
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                // Move focus to seek bar
                seekBar?.requestFocus()
                return true
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
                    R.id.btn_favorite -> {
                        showCenterFeedback(android.R.drawable.btn_star_big_on, "Added to Favorites")
                    }
                    R.id.btn_speed -> {
                        showQualityDialog()
                    }
                    R.id.btn_volume -> {
                        player?.let { p ->
                            if (p.volume > 0f) {
                                previousVolume = p.volume
                                p.volume = 0f
                            } else {
                                p.volume = if (previousVolume <= 0f) 1f else previousVolume
                            }
                            updateVolumeIcon(p.volume == 0f)
                        }
                    }
                    R.id.btn_subtitles -> {
                        showCaptionsDialog()
                    }
                    R.id.btn_auto -> {
                        showCenterFeedback(android.R.drawable.ic_popup_sync, "Auto Mode")
                    }
                    R.id.btn_refresh -> {
                        player?.prepare()
                        showCenterFeedback(android.R.drawable.ic_popup_sync, "Refreshed")
                    }
                    R.id.btn_settings -> {
                        showQualityDialog()
                    }
                    R.id.btn_close -> {
                        finish()
                    }
                    R.id.seek_bar -> {
                        isSeeking = !isSeeking
                    }
                    else -> {
                        // Default action for center button
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
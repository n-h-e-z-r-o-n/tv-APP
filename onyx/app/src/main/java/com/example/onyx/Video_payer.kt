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
    private var errorTextView: TextView? = null
    private var overlayContainer: View? = null
    private var bottomBar: View? = null
    private var topBar: View? = null
    private var centerOverlay: View? = null
    private var centerIcon: ImageView? = null
    private var skipText: TextView? = null
    private var playPauseButton: ImageButton? = null
    private var rewindButton: ImageButton? = null
    private var fastForwardButton: ImageButton? = null
    private var muteButton: ImageButton? = null
    private var qualityButton: ImageButton? = null
    private var captionsButton: ImageButton? = null
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
        errorTextView = findViewById(R.id.error_text)
        overlayContainer = findViewById(R.id.overlay_container)
        bottomBar = findViewById(R.id.bottom_bar)
        centerOverlay = findViewById(R.id.center_overlay)
        centerIcon = findViewById(R.id.center_icon)
        skipText = findViewById(R.id.skip_text)
        playPauseButton = findViewById(R.id.btn_play_pause)
        rewindButton = findViewById(R.id.btn_rewind)
        fastForwardButton = findViewById(R.id.btn_fast_forward)
        muteButton = findViewById(R.id.btn_mute)
        qualityButton = findViewById(R.id.btn_quality)
        captionsButton = findViewById(R.id.btn_captions)
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
        // Handle back navigation properly
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (controlsVisible) {
                    hideControls()
                } else {
                    if (player?.isPlaying == true) {
                        player?.pause()
                    }
                    finish()
                }
            }
        })
    }

    private fun initializePlayer() {
        val videoUrl = intent.getStringExtra("video_url") ?: return

        // Check if there's already an active player from another instance
        val existingPlayer = PlayerManager.getActivePlayer()
        if (existingPlayer != null && existingPlayer != player) {
            // Release the existing player to prevent multiple active players
            existingPlayer.release()
            PlayerManager.clearActivePlayer()
        }

        trackSelector = DefaultTrackSelector(this).apply {
            // Configure to prefer highest quality
            setParameters(
                parameters
                    .buildUpon()
                    .setForceHighestSupportedBitrate(true)
                    .setMaxVideoSizeSd()
                    .setPreferredVideoMimeTypes("video/avc", "video/hevc", "video/av1")
                    .build()
            )
        }

        player = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector!!)
            .build().apply {
                // Register this player as the active one
                PlayerManager.setActivePlayer(this)

                playWhenReady = this@Video_payer.playWhenReady
                seekTo(currentWindow, playbackPosition)

                val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
                setMediaItem(mediaItem)
                prepare()

                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> showLoading()
                            Player.STATE_READY -> hideLoading()
                            Player.STATE_ENDED -> finish()
                            Player.STATE_IDLE -> showError("Video not available")
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        showError("Playback error: ${error.message}")
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        updatePlayPauseIcon(isPlaying)
                        // If this player stops playing, clear it from active manager
                        if (!isPlaying && PlayerManager.getActivePlayer() == this@apply) {
                            PlayerManager.clearActivePlayer()
                        }
                    }
                })
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
        errorTextView?.isVisible = false
    }

    private fun hideLoading() {
        progressBar?.isVisible = false
        errorTextView?.isVisible = false
    }

    private fun showError(message: String) {
        progressBar?.isVisible = false
        errorTextView?.apply {
            text = message
            isVisible = true
        }
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

        muteButton?.setOnClickListener {
            player?.let { p ->
                if (p.volume > 0f) {
                    previousVolume = p.volume
                    p.volume = 0f
                } else {
                    p.volume = if (previousVolume <= 0f) 1f else previousVolume
                }
                updateMuteIcon(p.volume == 0f)
                showCenterFeedback(
                    if (p.volume == 0f) android.R.drawable.ic_lock_silent_mode else android.R.drawable.ic_lock_silent_mode_off,
                    if (p.volume == 0f) "Muted" else "Unmuted"
                )
                resetAutoHide()
            }
        }


        qualityButton?.setOnClickListener {
            showQualityDialog()
            resetAutoHide()
        }

        captionsButton?.setOnClickListener {
            showCaptionsDialog()
            resetAutoHide()
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

    private fun updateMuteIcon(isMuted: Boolean) {
        muteButton?.setImageResource(
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
        muteButton?.isFocusable = true
        qualityButton?.isFocusable = true
        captionsButton?.isFocusable = true
        seekBar?.isFocusable = true

        // Set up a complete focus chain for TV navigation
        rewindButton?.nextFocusRightId = R.id.btn_play_pause
        rewindButton?.nextFocusDownId = R.id.seek_bar

        playPauseButton?.nextFocusLeftId = R.id.btn_rewind
        playPauseButton?.nextFocusRightId = R.id.btn_fast_forward
        playPauseButton?.nextFocusDownId = R.id.seek_bar

        fastForwardButton?.nextFocusLeftId = R.id.btn_play_pause
        fastForwardButton?.nextFocusRightId = R.id.btn_mute
        fastForwardButton?.nextFocusDownId = R.id.seek_bar

        muteButton?.nextFocusLeftId = R.id.btn_fast_forward
        muteButton?.nextFocusRightId = R.id.btn_fullscreen
        muteButton?.nextFocusDownId = R.id.seek_bar


        qualityButton?.nextFocusLeftId = R.id.btn_fullscreen
        qualityButton?.nextFocusRightId = R.id.btn_captions
        qualityButton?.nextFocusDownId = R.id.seek_bar

        captionsButton?.nextFocusLeftId = R.id.btn_quality
        captionsButton?.nextFocusDownId = R.id.seek_bar

        seekBar?.nextFocusUpId = R.id.btn_rewind
        seekBar?.nextFocusRightId = R.id.txt_duration

        durationText?.nextFocusLeftId = R.id.seek_bar
        durationText?.nextFocusUpId = R.id.btn_rewind

        // Add focus change listeners to show visual feedback
        val focusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (v is ImageButton) {
                v.scaleX = if (hasFocus) 1.2f else 1.0f
                v.scaleY = if (hasFocus) 1.2f else 1.0f
            }
        }

        playPauseButton?.setOnFocusChangeListener(focusChangeListener)
        rewindButton?.setOnFocusChangeListener(focusChangeListener)
        fastForwardButton?.setOnFocusChangeListener(focusChangeListener)
        muteButton?.setOnFocusChangeListener(focusChangeListener)
        qualityButton?.setOnFocusChangeListener(focusChangeListener)
        captionsButton?.setOnFocusChangeListener(focusChangeListener)
        seekBar?.setOnFocusChangeListener { v, hasFocus ->
            v.scaleX = if (hasFocus) 1.05f else 1.0f
            v.scaleY = if (hasFocus) 1.05f else 1.0f
        }
    }

    private fun showCenterFeedback(iconRes: Int, text: String) {
        centerIcon?.setImageResource(iconRes)
        skipText?.text = text
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
                    updateMuteIcon(p.volume == 0f)
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
                    R.id.btn_rewind -> {
                        // Already at leftmost control, do rewind action
                        val newPos = (player?.currentPosition ?: 0L) - 10_000L
                        player?.seekTo(newPos.coerceAtLeast(0L))
                        showCenterFeedback(android.R.drawable.ic_media_rew, "-10s")
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
                    R.id.btn_captions -> {
                        // Already at rightmost control, do fast forward action
                        val duration = player?.duration ?: Long.MAX_VALUE
                        val newPos = (player?.currentPosition ?: 0L) + 10_000L
                        player?.seekTo(newPos.coerceAtMost(duration))
                        showCenterFeedback(android.R.drawable.ic_media_ff, "+10s")
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
                        playPauseButton?.requestFocus()
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
                    R.id.btn_mute -> {
                        player?.let { p ->
                            if (p.volume > 0f) {
                                previousVolume = p.volume
                                p.volume = 0f
                            } else {
                                p.volume = if (previousVolume <= 0f) 1f else previousVolume
                            }
                            updateMuteIcon(p.volume == 0f)
                        }
                    }
                    R.id.btn_fullscreen -> {
                        toggleFullscreen()
                    }
                    R.id.btn_quality -> {
                        showQualityDialog()
                    }
                    R.id.btn_captions -> {
                        showCaptionsDialog()
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
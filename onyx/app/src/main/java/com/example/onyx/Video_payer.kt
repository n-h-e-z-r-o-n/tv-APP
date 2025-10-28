package com.example.onyx

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import androidx.media3.common.util.UnstableApi
import java.text.SimpleDateFormat
import java.util.*

@UnstableApi
class Video_payer : AppCompatActivity(), Player.Listener {

    private lateinit var playerView: PlayerView
    private lateinit var progressBar: ProgressBar
    private lateinit var overlayContainer: View
    private lateinit var bottomBar: LinearLayout
    private lateinit var centerOverlay: FrameLayout

    // Control buttons
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnRewind: ImageButton
    private lateinit var btnFastForward: ImageButton
    private lateinit var btnMute: ImageButton
    private lateinit var btnSpeed: Button
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
    private var isFullscreen = false
    private var isMuted = false
    private var currentSpeed = 1.0f
    private var lastTapTime = 0L
    private var tapCount = 0
    private var progressHandler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null

    // Playback speeds
    private val playbackSpeeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    private var currentSpeedIndex = 2 // Default to 1.0x

    // Quality options - will be populated from tracks
    private var qualityOptions = listOf("Auto")
    private var currentQualityIndex = 0
    
    // Player management variables (merged from PlayerManager)
    private var currentVideoUrl: String? = null
    private var availableQualities: List<String> = listOf("Auto")

    override fun onCreate(savedInstanceState: Bundle?) {
        GlobalUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_payer)

        // Prevent screen from sleeping while this Activity is visible
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        initializeViews()
        setupPlayer()
        setupControls()
        setupGestures()
        setupBackPressedCallback()
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
    }

    private fun setupPlayer() {
        val videoUrl = intent.getStringExtra("video_url")
        if (videoUrl != null) {
            exoPlayer = initializePlayer(videoUrl)
            
            exoPlayer?.let { player ->
                playerView.player = player
                player.addListener(this)
                updatePlayPauseButton()
                updateMuteButton()
                updateSpeedButton()
                updateQualityButton()
                // Initial quality options - will be updated when tracks are available
                qualityOptions = availableQualities
            }
        } else {
            Toast.makeText(this, "No video URL provided", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupControls() {
        // Play/Pause button
        btnPlayPause.setOnClickListener {
            togglePlayPause()
        }

        // Rewind button (10 seconds)
        btnRewind.setOnClickListener {
            seekRelative(-10000)
        }

        // Fast forward button (10 seconds)
        btnFastForward.setOnClickListener {
            seekRelative(10000)
        }

        // Mute button
        btnMute.setOnClickListener {
            toggleMute()
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

        // Fullscreen button
        btnFullscreen.setOnClickListener {
            toggleFullscreen()
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

    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isControlsVisible) {
                    // If controls are visible, hide them
                    hideControls()
                } else {
                    // If controls are hidden, exit the video player
                    finish()
                }
            }
        })
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
        val videoUrl = intent.getStringExtra("video_url")
        if (videoUrl != null) {
            exoPlayer?.let { player ->
                player.stop()
                player.clearMediaItems()
                player.setMediaItem(MediaItem.fromUri(videoUrl))
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
    }

    private fun hideControls() {
        isControlsVisible = false
        bottomBar.visibility = View.GONE
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        if (isFullscreen) {
            // Enter fullscreen
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            btnFullscreen.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        } else {
            // Exit fullscreen
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            btnFullscreen.setImageResource(android.R.drawable.ic_menu_crop)
        }
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
        btnSpeed.text = "${currentSpeed}x"
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
                    // Start progress tracking if playing
                    if (exoPlayer?.isPlaying == true) {
                        startProgressTracking()
                    }
                }
                Player.STATE_ENDED -> {
                    // Video ended, could restart or show next video
                    stopProgressTracking()
                    Toast.makeText(this, "Video ended", Toast.LENGTH_SHORT).show()
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

    private fun startProgressTracking() {
        stopProgressTracking() // Stop any existing tracking
        progressRunnable = object : Runnable {
            override fun run() {
                updateSeekBar()
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

    override fun onDestroy() {
        super.onDestroy()
        stopProgressTracking()
        releasePlayerWithAudioFocus()
        finish()
    }

    override fun onPause() {
        super.onPause()
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
                if (!isControlsVisible) {
                    showControls()
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }
    
    // ===== PlayerManager functionality merged into this class =====
    
    companion object {
        fun playVideoExternally(context: Context, videoUrl: String) {
            val intent = Intent(context, Video_payer::class.java).apply {
                putExtra("video_url", videoUrl)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(intent)
        }
    }
    
    private fun initializePlayer(videoUrl: String): ExoPlayer {
        releasePlayer()
        
        // Request audio focus to prevent multiple audio streams
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val result = audioManager.requestAudioFocus(
            null,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
        
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.w("Video_payer", "Audio focus not granted, but continuing with playback")
        }

        val trackSelector = DefaultTrackSelector(this).apply {
            setParameters(
                buildUponParameters()
                    .setMaxVideoSize(1920, 1080) // Allow up to 1080p by default
                    .setPreferredVideoMimeType("video/mp4")
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .setAllowVideoNonSeamlessAdaptiveness(true)
                    .setMaxAudioChannelCount(2) // Limit to stereo audio
                    .setPreferredAudioLanguage("en") // Prefer English audio
                    .setSelectUndeterminedTextLanguage(true) // Select first available audio track
                    .setForceHighestSupportedBitrate(true) // Force single track selection
            )
        }

        val renderersFactory = DefaultRenderersFactory(this)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        val player = ExoPlayer.Builder(this)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .build()
            .apply {
                val mediaItem = MediaItem.fromUri(videoUrl)
                setMediaItem(mediaItem)
                addListener(object : Player.Listener {
                    override fun onTracksChanged(tracks: Tracks) {
                        updateAvailableQualities(tracks)
                    }
                })
                prepare()
                playWhenReady = true
            }

        currentVideoUrl = videoUrl
        return player
    }
    
    private fun releasePlayer() {
        exoPlayer?.let { player ->
            player.release()
            exoPlayer = null
            currentVideoUrl = null
            availableQualities = listOf("Auto")
        }
    }
    
    private fun releasePlayerWithAudioFocus() {
        exoPlayer?.let { player ->
            player.release()
            exoPlayer = null
            currentVideoUrl = null
            availableQualities = listOf("Auto")
        }
        
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
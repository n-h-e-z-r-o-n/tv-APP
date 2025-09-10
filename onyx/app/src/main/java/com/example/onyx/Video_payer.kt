package com.example.onyx

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.common.util.UnstableApi
import java.text.SimpleDateFormat
import java.util.*

@UnstableApi
class Video_payer : AppCompatActivity(), Player.Listener {
    
    private lateinit var playerView: PlayerView
    private lateinit var progressBar: ProgressBar
    private lateinit var overlayContainer: View
    private lateinit var topBar: LinearLayout
    private lateinit var bottomBar: LinearLayout
    private lateinit var centerOverlay: FrameLayout
    
    // Control buttons
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnRewind: ImageButton
    private lateinit var btnFastForward: ImageButton
    private lateinit var btnMute: ImageButton
    private lateinit var btnSpeed: ImageButton
    private lateinit var btnSubtitles: ImageButton
    private lateinit var btnQuality: TextView
    private lateinit var btnRefresh: ImageButton
    private lateinit var btnSettings: ImageButton
    private lateinit var btnClose: ImageButton
    private lateinit var btnFullscreen: ImageButton
    
    // Text views
    private lateinit var txtVideoTitle: TextView
    
    // Seek bar and time displays
    private lateinit var seekBar: SeekBar
    private lateinit var txtCurrentTime: TextView
    private lateinit var txtDuration: TextView
    
    private var exoPlayer: ExoPlayer? = null
    private var isControlsVisible = true
    private var isFullscreen = false
    private var isMuted = false
    private var currentSpeed = 1.0f
    private var controlsHandler = Handler(Looper.getMainLooper())
    private var controlsRunnable: Runnable? = null
    private var lastTapTime = 0L
    private var tapCount = 0
    
    // Playback speeds
    private val playbackSpeeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    private var currentSpeedIndex = 2 // Default to 1.0x
    
    // Quality options
    private val qualityOptions = listOf("Auto", "1080p", "720p", "480p", "360p", "240p")
    private var currentQualityIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_payer)

        // Prevent screen from sleeping while this Activity is visible
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        initializeViews()
        setupPlayer()
        setupControls()
        setupGestures()
    }
    
    private fun initializeViews() {
        playerView = findViewById(R.id.player_view)
        progressBar = findViewById(R.id.progress_bar)
        overlayContainer = findViewById(R.id.overlay_container)
        topBar = findViewById(R.id.top_bar)
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
        
        // Text views
        txtVideoTitle = findViewById(R.id.txt_video_title)
        
        // Seek bar and time displays
        seekBar = findViewById(R.id.seek_bar)
        txtCurrentTime = findViewById(R.id.txt_current_time)
        txtDuration = findViewById(R.id.txt_duration)
    }
    
    private fun setupPlayer() {
        val videoUrl = intent.getStringExtra("video_url")
        if (videoUrl != null) {
            exoPlayer = PlayerManager.initializePlayer(this, videoUrl)
            exoPlayer?.let { player ->
                playerView.player = player
                player.addListener(this)
                updatePlayPauseButton()
                updateMuteButton()
                updateSpeedButton()
                updateQualityButton()
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
                controlsHandler.removeCallbacks(controlsRunnable!!)
            }
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val duration = exoPlayer?.duration ?: 0L
                val position = (seekBar?.progress ?: 0) * duration / 1000
                exoPlayer?.seekTo(position)
                showControlsTemporarily()
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
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Playback Speed")
            .setSingleChoiceItems(speedOptions, currentSpeedIndex) { _, which ->
                currentSpeedIndex = which
                currentSpeed = playbackSpeeds[which]
                exoPlayer?.setPlaybackSpeed(currentSpeed)
                updateSpeedButton()
            }
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun showQualityDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Video Quality")
            .setSingleChoiceItems(qualityOptions.toTypedArray(), currentQualityIndex) { _, which ->
                currentQualityIndex = which
                updateQualityButton()
                // Note: Quality change would require reinitializing the player with different track selection
            }
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun showSettingsDialog() {
        val settings = arrayOf("Subtitles", "Audio Track", "Video Info")
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Settings")
            .setItems(settings) { _, which ->
                when (which) {
                    0 -> Toast.makeText(this, "Subtitles not available", Toast.LENGTH_SHORT).show()
                    1 -> Toast.makeText(this, "Audio track selection not available", Toast.LENGTH_SHORT).show()
                    2 -> showVideoInfo()
                }
            }
            .show()
    }
    
    private fun showVideoInfo() {
        exoPlayer?.let { player ->
            val duration = formatTime(player.duration)
            val currentPos = formatTime(player.currentPosition)
            val info = "Duration: $duration\nCurrent: $currentPos\nSpeed: ${currentSpeed}x"
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
        topBar.visibility = View.VISIBLE
        bottomBar.visibility = View.VISIBLE
        showControlsTemporarily()
    }
    
    private fun hideControls() {
        isControlsVisible = false
        topBar.visibility = View.GONE
        bottomBar.visibility = View.GONE
        controlsHandler.removeCallbacks(controlsRunnable!!)
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
    
    private fun showControlsTemporarily() {
        controlsHandler.removeCallbacks(controlsRunnable!!)
        controlsRunnable = Runnable { hideControls() }
        controlsHandler.postDelayed(controlsRunnable!!, 3000) // Hide after 3 seconds
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
                if (player.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            )
        }
    }
    
    private fun updateMuteButton() {
        btnMute.setImageResource(
            if (isMuted) android.R.drawable.ic_lock_silent_mode else android.R.drawable.ic_lock_silent_mode_off
        )
    }
    
    private fun updateSpeedButton() {
        //btnSpeed.text = "${currentSpeed}x"
    }
    
    private fun updateQualityButton() {
        btnQuality.text = qualityOptions[currentQualityIndex]
    }
    
    private fun formatTime(timeMs: Long): String {
        val date = Date(timeMs)
        val formatter = SimpleDateFormat("mm:ss", Locale.getDefault())
        return formatter.format(date)
    }
    
    // Player.Listener implementation
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        runOnUiThread {
            updatePlayPauseButton()
            if (isPlaying) {
                progressBar.visibility = View.GONE
            }
        }
    }
    
    override fun onPlaybackStateChanged(playbackState: Int) {
        runOnUiThread {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    progressBar.visibility = View.VISIBLE
                }
                Player.STATE_READY -> {
                    progressBar.visibility = View.GONE
                    val duration = exoPlayer?.duration ?: 0L
                    txtDuration.text = formatTime(duration)
                    seekBar.max = 1000
                }
                Player.STATE_ENDED -> {
                    // Video ended, could restart or show next video
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
        controlsHandler.removeCallbacks(controlsRunnable!!)
        PlayerManager.releasePlayer()
    }
    
    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
    }
    
    override fun onResume() {
        super.onResume()
        exoPlayer?.play()
    }
}
package com.example.onyx

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.util.Util

class Video_payer : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private var playerView: StyledPlayerView? = null
    private var progressBar: ProgressBar? = null
    private var errorTextView: TextView? = null
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_payer)

        playerView = findViewById(R.id.player_view)
        progressBar = findViewById(R.id.progress_bar)
        errorTextView = findViewById(R.id.error_text)

        // Setup back press handling
        setupBackPressHandler()

        // Hide system UI for immersive experience
        hideSystemUi()
    }

    private fun setupBackPressHandler() {
        // Handle back navigation properly
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (player?.isPlaying == true) {
                    player?.pause()
                }
                finish()
            }
        })
    }

    private fun initializePlayer() {
        val videoUrl = intent.getStringExtra("video_url") ?: return

        player = ExoPlayer.Builder(this).build().apply {
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
            })
        }

        playerView?.player = player
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

    public override fun onStart() {
        super.onStart()
        if (Util.SDK_INT >= 24) {
            initializePlayer()
        }
    }

    public override fun onResume() {
        super.onResume()
        if (Util.SDK_INT < 24 || player == null) {
            initializePlayer()
        }
    }

    public override fun onPause() {
        super.onPause()
        if (Util.SDK_INT < 24) {
            releasePlayer()
        }
    }

    public override fun onStop() {
        super.onStop()
        if (Util.SDK_INT >= 24) {
            releasePlayer()
        }
    }

    private fun releasePlayer() {
        player?.let {
            playbackPosition = it.currentPosition
            currentWindow = it.currentMediaItemIndex
            playWhenReady = it.playWhenReady
            it.release()
        }
        player = null
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
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
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }
}
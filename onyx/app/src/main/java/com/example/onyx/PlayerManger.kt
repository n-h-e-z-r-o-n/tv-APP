package com.example.onyx

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource

@UnstableApi
object PlayerManager {
    
    private var exoPlayer: ExoPlayer? = null
    private var currentVideoUrl: String? = null
    
    fun playVideoExternally(context: Context, videoUrl: String) {
        Log.d("PlayerManager", "Playing video: $videoUrl")
        
        val intent = Intent(context, Video_payer::class.java).apply {
            putExtra("video_url", videoUrl)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }
    
    fun initializePlayer(context: Context, videoUrl: String): ExoPlayer {
        releasePlayer()
        
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setMaxVideoSizeSd()
                    .setPreferredVideoMimeType("video/mp4")
            )
        }
        
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        
        exoPlayer = ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setTrackSelector(trackSelector)
            .build()
            .apply {
                val mediaItem = MediaItem.fromUri(videoUrl)
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
            }
        
        currentVideoUrl = videoUrl
        return exoPlayer!!
    }
    
    fun getPlayer(): ExoPlayer? = exoPlayer
    
    fun isPlayerInitialized(): Boolean = exoPlayer != null
    
    fun getCurrentPosition(): Long = exoPlayer?.currentPosition ?: 0L
    
    fun getDuration(): Long = exoPlayer?.duration ?: 0L
    
    fun isPlaying(): Boolean = exoPlayer?.isPlaying ?: false
    
    fun pause() {
        exoPlayer?.pause()
    }
    
    fun play() {
        exoPlayer?.play()
    }
    
    fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
    }
    
    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.setPlaybackSpeed(speed)
    }
    
    fun setVolume(volume: Float) {
        exoPlayer?.volume = volume
    }
    
    fun getVolume(): Float = exoPlayer?.volume ?: 1.0f
    
    fun addPlayerListener(listener: Player.Listener) {
        exoPlayer?.addListener(listener)
    }
    
    fun removePlayerListener(listener: Player.Listener) {
        exoPlayer?.removeListener(listener)
    }
    
    fun releasePlayer() {
        exoPlayer?.let { player ->
            player.release()
            exoPlayer = null
            currentVideoUrl = null
        }
    }
    
    fun getCurrentVideoUrl(): String? = currentVideoUrl
}

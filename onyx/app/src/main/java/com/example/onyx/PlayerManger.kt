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
                    .setMaxVideoSize(1920, 1080) // Allow up to 1080p by default
                    .setPreferredVideoMimeType("video/mp4")
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .setAllowAudioMixedMimeTypeAdaptiveness(true)
                    .setAllowVideoNonSeamlessAdaptiveness(true)
                    .setAllowAudioNonSeamlessAdaptiveness(true)
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
    
    fun setVideoQuality(qualityIndex: Int) {
        exoPlayer?.let { player ->
            val trackSelector = player.trackSelector as? DefaultTrackSelector ?: return
            
            Log.d("PlayerManager", "Setting video quality to index: $qualityIndex")
            
            when (qualityIndex) {
                0 -> { // Auto
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .clearVideoSizeConstraints()
                            .setMaxVideoSize(1920, 1080)
                            .setAllowVideoMixedMimeTypeAdaptiveness(true)
                            .setAllowVideoNonSeamlessAdaptiveness(true)
                    )
                }
                1 -> { // 1080p
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .setMaxVideoSize(1920, 1080)
                            .setMinVideoSize(1920, 1080)
                            .setAllowVideoMixedMimeTypeAdaptiveness(false)
                            .setAllowVideoNonSeamlessAdaptiveness(false)
                    )
                }
                2 -> { // 720p
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .setMaxVideoSize(1280, 720)
                            .setMinVideoSize(1280, 720)
                            .setAllowVideoMixedMimeTypeAdaptiveness(false)
                            .setAllowVideoNonSeamlessAdaptiveness(false)
                    )
                }
                3 -> { // 480p
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .setMaxVideoSize(854, 480)
                            .setMinVideoSize(854, 480)
                            .setAllowVideoMixedMimeTypeAdaptiveness(false)
                            .setAllowVideoNonSeamlessAdaptiveness(false)
                    )
                }
                4 -> { // 360p
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .setMaxVideoSize(640, 360)
                            .setMinVideoSize(640, 360)
                            .setAllowVideoMixedMimeTypeAdaptiveness(false)
                            .setAllowVideoNonSeamlessAdaptiveness(false)
                    )
                }
                5 -> { // 240p
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .setMaxVideoSize(426, 240)
                            .setMinVideoSize(426, 240)
                            .setAllowVideoMixedMimeTypeAdaptiveness(false)
                            .setAllowVideoNonSeamlessAdaptiveness(false)
                    )
                }
            }
            
            Log.d("PlayerManager", "Quality parameters applied successfully")
        }
    }
    
    fun getCurrentVideoQuality(): String {
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
    
    fun getVideoInfo(): String {
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
    
    fun getAvailableQualities(): List<String> {
        // This would ideally get the actual available qualities from the media
        // For now, return the standard options
        return listOf("Auto", "1080p", "720p", "480p", "360p", "240p")
    }
}

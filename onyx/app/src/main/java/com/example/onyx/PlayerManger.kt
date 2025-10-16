package com.example.onyx

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.media.AudioManager
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
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
    private var availableQualities: List<String> = listOf("Auto")

    fun playVideoExternally(context: Context, videoUrl: String) {
        // If a player is already active for the same URL, don't create a new one
        if (isPlayerActive() && currentVideoUrl == videoUrl) {
            Log.d("PlayerManager", "Player already active for this URL, not creating new instance")
            return
        }
        
        // Release any existing player before starting a new one
        releasePlayer()
        
        val intent = Intent(context, Video_payer::class.java).apply {
            putExtra("video_url", videoUrl)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(intent)
    }

    fun initializePlayer(context: Context, videoUrl: String): ExoPlayer {
        releasePlayer()
        
        // Request audio focus to prevent multiple audio streams
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val result = audioManager.requestAudioFocus(
            null,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
        
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.w("PlayerManager", "Audio focus not granted, but continuing with playback")
        }

        val trackSelector = DefaultTrackSelector(context).apply {
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

        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        exoPlayer = ExoPlayer.Builder(context)
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
        return exoPlayer!!
    }



    fun play() {
        exoPlayer?.play()
    }


    fun releasePlayer() {
        exoPlayer?.let { player ->
            player.release()
            exoPlayer = null
            currentVideoUrl = null
            availableQualities = listOf("Auto")
        }
    }
    
    fun releasePlayerWithAudioFocus(context: Context) {
        exoPlayer?.let { player ->
            player.release()
            exoPlayer = null
            currentVideoUrl = null
            availableQualities = listOf("Auto")
        }
        
        // Abandon audio focus when releasing player
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.abandonAudioFocus(null)
    }

    fun getCurrentVideoUrl(): String? = currentVideoUrl
    
    fun getCurrentPlayer(): ExoPlayer? = exoPlayer
    
    fun isPlayerActive(): Boolean = exoPlayer != null

    fun setVideoQuality(qualityIndex: Int) {
        exoPlayer?.let { player ->
            val trackSelector = player.trackSelector as? DefaultTrackSelector ?: return

            Log.d("PlayerManager", "Setting video quality to index: $qualityIndex")

            if (qualityIndex >= availableQualities.size) {
                Log.w("PlayerManager", "Quality index $qualityIndex out of bounds")
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

            Log.d("PlayerManager", "Quality parameters applied successfully for: $selectedQuality")
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
        Log.d("PlayerManager", "Available qualities updated: $availableQualities")
    }

    fun getAvailableQualities(): List<String> {
        return availableQualities
    }
}

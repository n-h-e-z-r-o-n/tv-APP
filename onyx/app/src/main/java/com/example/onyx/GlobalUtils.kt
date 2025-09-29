package com.example.onyx

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Global utility class containing shared functions and data management
 * that can be used across all activities in the Onyx app
 * 
 * USAGE EXAMPLES:
 * 
 * // Statistics tracking
 * GlobalUtils.incrementMoviesWatched(this)
 * GlobalUtils.incrementSeriesWatched(this)
 * val moviesCount = GlobalUtils.getMoviesWatched(this)
 * 
 * // Settings management
 * GlobalUtils.setAutoPlay(this, true)
 * val isAutoPlayEnabled = GlobalUtils.isAutoPlayEnabled(this)
 * GlobalUtils.setVideoQuality(this, "1080p")
 * 
 * // Cache management
 * val success = GlobalUtils.clearAppCache(this)
 * 
 * // App info
 * val version = GlobalUtils.getAppVersion(this)
 * val deviceInfo = GlobalUtils.getDeviceInfo(this)
 * 
 * // Utility functions
 * val formattedTime = GlobalUtils.formatDuration(125) // "02:05"
 * val formattedSize = GlobalUtils.formatFileSize(1024000) // "1.0 MB"
 * val isValidEmail = GlobalUtils.isValidEmail("user@example.com")
 */
object GlobalUtils {
    
    // SharedPreferences key constants
    private const val PREF_NAME = "OnyxProfile"
    private const val KEY_MOVIES_WATCHED = "movies_watched"
    private const val KEY_SERIES_WATCHED = "series_watched"
    private const val KEY_AUTO_PLAY = "auto_play"
    private const val KEY_NOTIFICATIONS = "notifications"
    private const val KEY_VIDEO_QUALITY = "video_quality"
    
    // Default values
    private const val DEFAULT_VIDEO_QUALITY = "1080p"
    
    /**
     * Get SharedPreferences instance
     */
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    // ==================== STATISTICS MANAGEMENT ====================
    
    /**
     * Increment movies watched counter
     */
    fun incrementMoviesWatched(context: Context) {
        val prefs = getSharedPreferences(context)
        val currentCount = prefs.getInt(KEY_MOVIES_WATCHED, 0)
        prefs.edit().putInt(KEY_MOVIES_WATCHED, currentCount + 1).apply()
        Log.d("GlobalUtils", "Movies watched incremented to: ${currentCount + 1}")
    }
    
    /**
     * Increment series watched counter
     */
    fun incrementSeriesWatched(context: Context) {
        val prefs = getSharedPreferences(context)
        val currentCount = prefs.getInt(KEY_SERIES_WATCHED, 0)
        prefs.edit().putInt(KEY_SERIES_WATCHED, currentCount + 1).apply()
        Log.d("GlobalUtils", "Series watched incremented to: ${currentCount + 1}")
    }
    
    /**
     * Get movies watched count
     */
    fun getMoviesWatched(context: Context): Int {
        return getSharedPreferences(context).getInt(KEY_MOVIES_WATCHED, 0)
    }
    
    /**
     * Get series watched count
     */
    fun getSeriesWatched(context: Context): Int {
        return getSharedPreferences(context).getInt(KEY_SERIES_WATCHED, 0)
    }


    
    /**
     * Reset all statistics
     */
    fun resetStatistics(context: Context) {
        val prefs = getSharedPreferences(context)
        prefs.edit()
            .putInt(KEY_MOVIES_WATCHED, 0)
            .putInt(KEY_SERIES_WATCHED, 0)
            .apply()
        Log.d("GlobalUtils", "Statistics reset")
    }
    
    // ==================== SETTINGS MANAGEMENT ====================
    
    /**
     * Set auto-play setting
     */
    fun setAutoPlay(context: Context, enabled: Boolean) {
        getSharedPreferences(context).edit().putBoolean(KEY_AUTO_PLAY, enabled).apply()
        Log.d("GlobalUtils", "Auto-play set to: $enabled")
    }
    
    /**
     * Get auto-play setting
     */
    fun isAutoPlayEnabled(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(KEY_AUTO_PLAY, true)
    }
    
    /**
     * Set notifications setting
     */
    fun setNotifications(context: Context, enabled: Boolean) {
        getSharedPreferences(context).edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
        Log.d("GlobalUtils", "Notifications set to: $enabled")
    }
    
    /**
     * Get notifications setting
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(KEY_NOTIFICATIONS, true)
    }
    
    /**
     * Set video quality setting
     */
    fun setVideoQuality(context: Context, quality: String) {
        getSharedPreferences(context).edit().putString(KEY_VIDEO_QUALITY, quality).apply()
        Log.d("GlobalUtils", "Video quality set to: $quality")
    }
    
    /**
     * Get video quality setting
     */
    fun getVideoQuality(context: Context): String {
        return getSharedPreferences(context).getString(KEY_VIDEO_QUALITY, DEFAULT_VIDEO_QUALITY) ?: DEFAULT_VIDEO_QUALITY
    }
    
    // ==================== FAVORITES MANAGEMENT ====================
    
    /**
     * Add item to favorites
     */
    fun addToFavorites(context: Context, itemId: String, itemType: String, itemTitle: String) {
        val prefs = getSharedPreferences(context)
        val favorites = getFavorites(context).toMutableSet()
        val favoriteItem = "$itemId|$itemType|$itemTitle"
        favorites.add(favoriteItem)
        prefs.edit().putStringSet("favorites", favorites).apply()
        Log.d("GlobalUtils", "Added to favorites: $itemTitle")
    }
    
    /**
     * Remove item from favorites
     */
    fun removeFromFavorites(context: Context, itemId: String) {
        val prefs = getSharedPreferences(context)
        val favorites = getFavorites(context).toMutableSet()
        favorites.removeAll { it.startsWith("$itemId|") }
        prefs.edit().putStringSet("favorites", favorites).apply()
        Log.d("GlobalUtils", "Removed from favorites: $itemId")
    }
    
    /**
     * Check if item is in favorites
     */
    fun isFavorite(context: Context, itemId: String): Boolean {
        val favorites = getFavorites(context)
        return favorites.any { it.startsWith("$itemId|") }
    }
    
    /**
     * Get all favorites
     */
    fun getFavorites(context: Context): Set<String> {
        return getSharedPreferences(context).getStringSet("favorites", emptySet()) ?: emptySet()
    }
    
    // ==================== CACHE MANAGEMENT ====================
    
    /**
     * Clear app cache
     */
    fun clearAppCache(context: Context): Boolean {
        return try {
            // Clear internal cache
            val cacheDir = context.cacheDir
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
            }
            
            // Clear external cache if available
            val externalCacheDir = context.externalCacheDir
            if (externalCacheDir?.exists() == true) {
                externalCacheDir.deleteRecursively()
            }
            
            Log.d("GlobalUtils", "Cache cleared successfully")
            true
        } catch (e: Exception) {
            Log.e("GlobalUtils", "Failed to clear cache", e)
            false
        }
    }
    
    // ==================== UTILITY FUNCTIONS ====================
    
    /**
     * Get app version name

    fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (e: Exception) {
            "1.0.0"
        }
    }*/
    
    /**
     * Format time duration in MM:SS format
     */
    fun formatDuration(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
    
    /**
     * Format file size in human readable format
     */
    fun formatFileSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0
        
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        
        return String.format("%.1f %s", size, units[unitIndex])
    }
    
    /**
     * Validate email format
     */
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    /**
     * Get device info for debugging
     */
    fun getDeviceInfo(context: Context): String {
        val displayMetrics = context.resources.displayMetrics
        return "Screen: ${displayMetrics.widthPixels}x${displayMetrics.heightPixels}, " +
                "Density: ${displayMetrics.density}, " +
                "Android: ${android.os.Build.VERSION.RELEASE}"
    }
}

package com.example.onyx.OnyxObjects

import android.animation.ValueAnimator
import android.app.Activity
import android.app.UiModeManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.MediaStore
import android.text.SpannableStringBuilder
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.PathInterpolator
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.webkit.WebViewAssetLoader
import com.example.onyx.FetchData.TMDBapi
import com.example.onyx.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.Float
import kotlin.random.Random
import android.webkit.CookieManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


private val interpolator = AccelerateDecelerateInterpolator()

object GlobalUtils {

    // SharedPreferences key constants
    private const val PREF_NAME = "OnyxProfile"
    private const val KEY_MOVIES_WATCHED = "movies_watched"
    private const val KEY_SERIES_WATCHED = "series_watched"
    private const val KEY_AUTO_PLAY = "auto_play"
    private const val KEY_NOTIFICATIONS = "notifications"
    private const val KEY_VIDEO_QUALITY = "video_quality"
    private const val KEY_APP_THEME  = "app_theme"

    // Default values
    private const val DEFAULT_VIDEO_QUALITY = "1080p"
    private const val DEFAULT_THEME = "dark"


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
     * Set autoplay setting
     */
    fun setAutoPlay(context: Context, enabled: Boolean) {
        getSharedPreferences(context).edit().putBoolean(KEY_AUTO_PLAY, enabled).apply()
        Log.d("GlobalUtils", "Auto-play set to: $enabled")
    }

    /**
     * Get autoplay setting
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

    // ==================== THEME MANAGEMENT ====================

    // List of your theme keys
    private val availableThemes = listOf(
        "Default",
        "Brown",
        "dark",
        "Yellow",
        "ghost",
        "green",
        "red",
        "purple"
    )
    fun getAvailableThemes(): List<String> = availableThemes

    fun getAppTheme(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_APP_THEME, "Default") ?: "Default"
    }

    fun setAppTheme(context: Context, theme: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_APP_THEME, theme).apply()
    }


    fun applyTheme(activity: Activity) {
        when (getAppTheme(activity)) {
            "Default"-> activity.setTheme(R.style.Theme_Onyx_Default)
            "Brown"  -> activity.setTheme(R.style.Theme_Onyx_Brown)
            "dark" -> activity.setTheme(R.style.Theme_Onyx_Dark)
            "Yellow" -> activity.setTheme(R.style.Theme_Onyx_Yellow)
            "ghost" -> activity.setTheme(R.style.Theme_Onyx_Ghost)
            "green" -> activity.setTheme(R.style.Theme_Onyx_Green)
            "red" -> activity.setTheme(R.style.Theme_Onyx_Red)
            "purple" -> activity.setTheme(R.style.Theme_Onyx_Purple)
            else     -> activity.setTheme(R.style.Theme_Onyx_Default)
        }
    }

    // ==================== FAVORITES MANAGEMENT ====================



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

    ///  Get app version name

    fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"   // fallback if null
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    // ==================== APP MANAGEMENT ====================



    ///Restart the application
    fun restartApp(context: Context) {
        try {
            Log.d("GlobalUtils", "Restarting application...")

            // Get the main activity class
            val packageManager = context.packageManager
            val intent = packageManager.getLaunchIntentForPackage(context.packageName)

            if (intent != null) {
                // Clear the task stack and start fresh
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)

                // Kill the current process
                Process.killProcess(Process.myPid())
            } else {
                Log.e("GlobalUtils", "Could not get launch intent for package: ${context.packageName}")
            }
        } catch (e: Exception) {
            Log.e("GlobalUtils", "Error restarting app", e)
        }
    }


    fun formatRuntime(totalMinutes: Int): String {
        if (totalMinutes <= 0) return ""

        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return buildString {
            if (hours > 0) append("${hours}h ")
            if (minutes > 0) append("${minutes} min")
        }.trim()
    }


    ///Checks if the app is running on a TV device.
    fun isTv(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    fun calculateSpanCount(context: Context, itemWidthDp: Int): Int {
        val displayMetrics = context.resources.displayMetrics
        val screenWidthPx = displayMetrics.widthPixels
        val itemWidthPx = (itemWidthDp * displayMetrics.density).toInt()
        return (screenWidthPx / itemWidthPx).coerceAtLeast(1)
    }

    fun calculateSpanCountV2(
        context: Context,
        itemWidthDp: Int,
        reservedWidthDp: Int = 0
    ): Int {
        val displayMetrics = context.resources.displayMetrics

        val screenWidthPx = displayMetrics.widthPixels

        // Convert dp → px
        val itemWidthPx = (itemWidthDp * displayMetrics.density).toInt()
        val reservedWidthPx = (reservedWidthDp * displayMetrics.density).toInt()

        // Available screen width after subtracting sidebar / margin / extra UI
        val availableWidthPx = (screenWidthPx - reservedWidthPx).coerceAtLeast(0)

        // Calculate span count
        return (availableWidthPx / itemWidthPx).coerceAtLeast(1)
    }

     fun dpToPx(dp: Int, context: Context): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

////////////////////////////////////////////////////////////////////////////////////////////////////

    fun scrambleToText(
        textView: TextView,
        finalText: String,
        speed: Int = 400
    ) {
        val chars = "▫▪■□"
        val handler = Handler(Looper.getMainLooper())

        data class QueueItem(
            val to: Char,
            var char: Char = ' ',
            var done: Boolean = false,
            var frame: Int = 0,
            val maxFrames: Int
        )

        val queue = finalText.map {
            QueueItem(
                to = it,
                maxFrames = Random.Default.nextInt(speed, speed * 2)
            )
        }

        fun randomChar(): Char =
            chars[Random.Default.nextInt(chars.length)]

        fun update() {
            val sb = SpannableStringBuilder()
            var complete = 0

            queue.forEach { q ->
                if (q.done) {
                    sb.append(q.to)
                    complete++
                } else {
                    if (q.frame >= q.maxFrames) {
                        q.done = true
                        sb.append(q.to)
                    } else {
                        if (q.frame == 0 || Random.Default.nextFloat() < 0.5f) {
                            q.char = randomChar()
                        }

                        val start = sb.length
                        sb.append(q.char)

                        q.frame++
                    }
                }
            }

            textView.text = sb

            if (complete < queue.size) {
                handler.postDelayed({ update() }, 16L) // ~60fps
            }
        }

        update()
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////

    fun getServerUrl(context: Context, urlType: String?, showId: String?, seasonNo: String?, episodeNo: String?): String {
        val serverIndex = getSavedServerIndex(context)

        return if (urlType == "movie") {
            when (serverIndex) {
                0 -> "https://vidsrc.to/embed/movie/$showId"
                1 -> "https://player.embed-api.stream/?id=$showId&type=movie"
                2 -> "https://embedmaster.link/movie/$showId"
                3 -> "https://www.primewire.si/embed/movie?tmdb=$showId"
                4 -> "https://www.vidking.net/embed/movie/$showId"
                else -> "https://vidsrc.to/embed/movie/$showId"
            }
        } else {
            when (serverIndex) {
                0 -> "https://vidsrc.to/embed/tv/$showId/$seasonNo/$episodeNo"
                1 -> "https://player.embed-api.stream/?id=$showId&s=$seasonNo&e=$episodeNo"
                2 -> "https://embedmaster.link/tv/$showId/$seasonNo/$episodeNo"
                3 -> "https://www.primewire.si/embed/tv?tmdb=$showId&season=$seasonNo&episode=$episodeNo"
                4 -> "https://www.vidking.net/embed/tv/$showId/$seasonNo/$episodeNo"
                else -> "https://vidsrc.to/embed/tv/$showId/$seasonNo/$episodeNo"
            }
        }
    }

    fun saveServerIndex(context: Context, index: Int) {
        val prefs = context.getSharedPreferences("server_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("selected_server_index", index).apply()
    }

    fun getSavedServerIndex(context: Context): Int {
        val prefs = context.getSharedPreferences("server_prefs", Context.MODE_PRIVATE)
        Log.e("DEBUG_SERVER", prefs.getInt("selected_server_index", 0).toString())
        return prefs.getInt("selected_server_index", 0)
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////


    private var dp50: Float = 0f
    private var dp90: Float = 0f
    private var dp120: Float = 0f
    private var dp140: Float = 0f
    private var dp150: Float = 0f
    private var dp250: Float = 0f

    private lateinit var elevations: FloatArray
    private lateinit var scales : FloatArray
    private lateinit var translations  : FloatArray

    private var animationD:Long = 200

    private var isAnimating = false


    fun setupCardStackFromContainer(
        container: FrameLayout,
        autoSwipeDelay: Long = 10000L
    ) {

        // Ensure container has CardView children
        val cards = (0 until container.childCount)
            .mapNotNull { container.getChildAt(it) as? CardView }

        if (cards.isEmpty()) return

        // ---------------- Auto Swipe ----------------
        val autoSwipeHandler = Handler(Looper.getMainLooper())
        var autoSwipeRunnable: Runnable? = null
        var autoSwipeRunning = false

        fun stopAutoSwipe() {
            autoSwipeRunning = false
            autoSwipeRunnable?.let { autoSwipeHandler.removeCallbacks(it) }
            autoSwipeRunnable = null
        }

        fun startAutoSwipe() {
            if (autoSwipeRunning) return
            autoSwipeRunning = true

            autoSwipeRunnable = object : Runnable {
                override fun run() {
                    if (!container.hasFocus()) {
                        swapRight(container, keepFocus = false)
                        autoSwipeHandler.postDelayed(this, autoSwipeDelay)
                    } else stopAutoSwipe()
                }
            }

            autoSwipeHandler.postDelayed(autoSwipeRunnable!!, autoSwipeDelay)
        }


        val context = container.context
        dp50 = dp(context, 50f)
        dp90 = dp(context, 90f)
        dp120 = dp(context, 120f)
        dp140 = dp(context, 140f)
        dp150 = dp(context, 150f)
        dp250 = dp(context, 250f)

        translations = floatArrayOf(
            0f, dp50, dp90, dp120, dp140, dp150
        )
        scales = floatArrayOf(1.0f, 0.95f, 0.9f, 0.85f, 0.8f, 0.7f)
        elevations = floatArrayOf(6f, 5f, 4f, 3f, 2f, 1f)

        var autoSwipeResumeRunnable: Runnable? = null

        // ---------------- Setup Card Listeners ----------------

        cards.forEach { card ->

            card.setLayerType(View.LAYER_TYPE_HARDWARE, null)             // Set a persistent hardware layer for smooth animations


            card.isFocusable = true
            card.isFocusableInTouchMode = true


            card.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    stopAutoSwipe() // stop any ongoing auto-swipe
                    autoSwipeResumeRunnable?.let { container.removeCallbacks(it) } // cancel pending resumes
                    autoSwipeResumeRunnable = null

                    v.animate().cancel()
                    v.bringToFront()
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .translationX(0f)
                        .setDuration(animationD)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
                    v.elevation = 7f
                } else {

                    // Schedule auto-swipe restart after 300ms
                    autoSwipeResumeRunnable?.let { container.removeCallbacks(it) }
                    autoSwipeResumeRunnable = Runnable {
                        if (!container.hasFocus()) startAutoSwipe()
                    }
                    container.postDelayed(autoSwipeResumeRunnable!!, 300)
                }
            }

            card.setOnKeyListener { _, keyCode, event ->
                if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> { swapLeft(container); true }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> { swapRight(container); true }
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> false
                    else -> false
                }
            }
        }

        // ---------------- Cleanup on View Detach -------------------------------------------------
        container.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {}

            override fun onViewDetachedFromWindow(v: View) {
                stopAutoSwipe()
                container.removeOnAttachStateChangeListener(this)

                // Clear all listeners to prevent memory leaks
                cards.forEach { card ->
                    card.setOnFocusChangeListener(null)
                    card.setOnKeyListener(null)
                }
            }
        })

        layoutStack(container)
        container.postDelayed({ if (!container.hasFocus()) startAutoSwipe() }, 2000)
    }

    private val MAX_VISIBLE_CARDS = 4

    private fun layoutStack(container: FrameLayout) {
        val count = container.childCount
        if (count == 0) return

        for (i in 0 until count) {
            val card = container.getChildAt(i)

            val posFromTop = count - 1 - i

            if (posFromTop >= MAX_VISIBLE_CARDS) {
                // Hide cards below the visible stack
                card.visibility = View.INVISIBLE
                card.translationX = 0f
                card.scaleX = 0.7f
                card.scaleY = 0.7f
                card.elevation = 0f
            } else {
                val index = posFromTop
                val targetTranslation = translations.getOrElse(index) { translations.last() }
                val targetScale = scales.getOrElse(index) { scales.last() }
                val targetElevation = elevations.getOrElse(index) { elevations.last() }

                card.visibility = View.VISIBLE

                // Only animate if changed
                val needsTranslation = card.translationX != targetTranslation
                val needsScale = card.scaleX != targetScale || card.scaleY != targetScale
                val needsElevation = card.elevation != targetElevation

                card.animate().cancel()
                if (needsTranslation || needsScale) {
                    card.animate()
                        .translationX(targetTranslation)
                        .scaleX(targetScale)
                        .scaleY(targetScale)
                        .setDuration(animationD)
                        .setInterpolator(interpolator)
                        .start()
                }

                if (needsElevation) {
                    card.elevation = targetElevation
                }
            }
        }
    }


    private fun swapRight(container: FrameLayout, keepFocus: Boolean = true) {

        if (isAnimating) return
        isAnimating = true

        if (container.childCount == 0) return
        val top = container.getChildAt(container.childCount - 1)

        top.animate()
            .translationXBy(-dp250)
            .scaleX(0.85f)
            .scaleY(0.85f)
            .rotation(-5f)
            .setDuration(animationD)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withLayer()
            .withEndAction {
                top.rotation = 0f

                // Optimized view reordering
                val parent = top.parent as? ViewGroup

                parent?.removeView(top)
                parent?.addView(top, 0)


                layoutStack(container) // Re-layout stack positions

                if (keepFocus) {
                    container.getChildAt(container.childCount - 1)?.requestFocus()
                }

                container.postDelayed({
                    isAnimating = false
                }, animationD)
            }
            .start()
    }


    private fun swapLeft(container: FrameLayout, keepFocus: Boolean = true) {

        if (isAnimating) return
        isAnimating = true

        if (container.childCount == 0) return
        val bottom = container.getChildAt(0)

        bottom.animate()
            .translationXBy(-dp250)
            .scaleX(0.85f)
            .scaleY(0.85f)
            .rotation(-5f)
            .setDuration(animationD)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withLayer()
            .withEndAction {
                bottom.rotation = 0f

                // Optimized view reordering
                val parent = bottom.parent as? ViewGroup
                parent?.removeView(bottom)
                parent?.addView(bottom)

                layoutStack(container)

                if (keepFocus) {
                    container.getChildAt(container.childCount - 1)?.requestFocus()
                }


                container.postDelayed({
                    isAnimating = false
                }, animationD)
            }
            .start()
    }

    private fun dp(context: Context, value: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            context.resources.displayMetrics
        )
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    fun expandParentOnChildFocus(
        parent: View,
        expandedWidthDp: Float,
        collapsedWidthDp: Float,
        animationDuration: Long = 10L
    ) {
        val context = parent.context
        val expandedWidthPx = dp(context, expandedWidthDp).toInt()
        val collapsedWidthPx = dp(context, collapsedWidthDp).toInt()

        fun animateWidth(targetWidth: Int) {
            val startWidth = parent.width
            if (startWidth == targetWidth) return

            ValueAnimator.ofInt(startWidth, targetWidth).apply {
                duration = animationDuration // <--- FIXED
                addUpdateListener {
                    parent.layoutParams = parent.layoutParams.apply {
                        width = it.animatedValue as Int
                    }
                    parent.requestLayout()
                }
                start()
            }
        }

        fun checkFocus() {
            if (parent.hasFocus()) {
                animateWidth(expandedWidthPx)
            } else {
                animateWidth(collapsedWidthPx)
            }
        }

        // Watch all children
        fun attachFocusListeners(view: View) {
            view.setOnFocusChangeListener { _, _ ->
                parent.post { checkFocus() }
            }

            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    attachFocusListeners(view.getChildAt(i))
                }
            }
        }

        attachFocusListeners(parent)
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    //designed to force a container (like a section or a row) to be fully visible on the screen whenever an item inside it is focused.
    fun enableFullViewOnDescendantFocus(
        parent: ViewGroup,
        descendant: View
    ) {
        descendant.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) return@setOnFocusChangeListener

            Log.d("FULL_FOCUS", "Focused: $v inside $parent")

            parent.post {
                scrollParentFullyIntoView(parent)
            }
        }
    }

    fun enableFullViewOnDescendantFocus(
        parent: ViewGroup
    ) {
        // Set container height to match the screen height
        val displayMetrics = parent.context.resources.displayMetrics
        val params = parent.layoutParams
        if (params != null) {
            params.height = displayMetrics.heightPixels
            parent.layoutParams = params
        }

        parent.viewTreeObserver.addOnGlobalFocusChangeListener { _, newFocus ->
            if (newFocus != null) {
                var p = newFocus.parent
                while (p != null) {
                    if (p === parent) {
                        parent.post {
                            scrollParentFullyIntoView(parent)
                        }
                        break
                    }
                    p = p.parent
                }
            }
        }
    }

    fun expandAndScrollIntoView(parent: ViewGroup) {
        val displayMetrics = parent.context.resources.displayMetrics

        parent.layoutParams = parent.layoutParams.apply {
            height = displayMetrics.heightPixels
        }

        parent.post {
            scrollParentFullyIntoView(parent)
        }
    }

    private fun scrollParentFullyIntoView(parent: View) {
        val scrollContainer = findScrollParent(parent) ?: return

        val rect = Rect()
        parent.getDrawingRect(rect)

        // Convert rect to scroll container coordinates
        scrollContainer.offsetDescendantRectToMyCoords(parent, rect)

        scrollContainer.requestChildRectangleOnScreen(
            parent,
            rect,
            true
        )
    }


    private fun findScrollParent(view: View): ViewGroup? {
        var parent = view.parent
        while (parent is ViewGroup) {
            when (parent) {
                is ScrollView,
                is HorizontalScrollView,
                is NestedScrollView -> return parent
            }
            parent = parent.parent
        }
        return null
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    // Function to hide system UI (status bar and navigation bar)
    fun hideSystemUI(activity: Activity) {
        val window = activity.window

        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11 (API 30) and above
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // For older versions
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    )
        }
    }

    ///////////////////////////////////////////////////////////

    val movieGenreMap = mapOf(
        28 to "Action",
        12 to "Adventure",
        16 to "Animation",
        35 to "Comedy",
        80 to "Crime",
        99 to "Documentary",
        18 to "Drama",
        10751 to "Family",
        14 to "Fantasy",
        36 to "History",
        27 to "Horror",
        10402 to "Music",
        9648 to "Mystery",
        10749 to "Romance",
        878 to "Sci-Fi",
        10770 to "TV Movie",
        53 to "Thriller",
        10752 to "War",
        37 to "Western"
    )

    val tvGenreMap = mapOf(
        10759 to "Action & Adventure",
        16 to "Animation",
        35 to "Comedy",
        80 to "Crime",
        99 to "Documentary",
        18 to "Drama",
        10751 to "Family",
        10762 to "Kids",
        9648 to "Mystery",
        10763 to "News",
        10764 to "Reality",
        10765 to "Sci-Fi & Fantasy",
        10766 to "Soap",
        10767 to "Talk",
        10768 to "War & Politics",
        37 to "Western"
    )
    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    fun exitApp(activity: Activity) {
        // Finish all activities
        activity.finishAffinity()
        // Remove from recent apps
        activity.finishAndRemoveTask()
        // Kill process
        android.os.Process.killProcess(android.os.Process.myPid())
        System.exit(0)
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    suspend fun ipCheck(context: Context): Boolean {
        Log.e("Login_Page IP_CHECK", "Starting...")

        // 1️⃣ Check if already saved
        val savedCountry = getSavedCountryCode(context)
        if (savedCountry.isNotEmpty()) {
            Log.e("Login_Page IP_CHECK", "Using cached country: $savedCountry")
            return savedCountry.equals("KE", ignoreCase = true)
        }

        // 2️⃣ If not saved → call network
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL("https://ipapi.co/json/")
                connection = url.openConnection() as HttpURLConnection
                connection.setRequestProperty("User-Agent", "Android-TV-App")
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext false
                }

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = org.json.JSONObject(response)
                val countryCode = json.optString("country", "")

                Log.e("Login_Page IP_CHECK", "Detected Country: $countryCode")

                if (countryCode.isNotEmpty()) {
                    saveCountryCode(context, countryCode)
                }

                countryCode.equals("KE", ignoreCase = true)

            } catch (e: Exception) {
                Log.e("Login_Page IP_CHECK", "Error: ${e.message}")
                false
            } finally {
                connection?.disconnect()
            }
        }
    }

    fun saveCountryCode(context: Context, country: String) {
        Log.e("Login_Page IP_CHECK", "Saved Country: $country")
        val prefs = context.getSharedPreferences("country_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("detected_country_code", country).apply()
    }

    fun getSavedCountryCode(context: Context): String {
        val prefs = context.getSharedPreferences("country_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getString("detected_country_code", "") ?: ""
        Log.e("Login_Page IP_CHECK", "Retrieved Country: $saved")
        return saved
    }



    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    suspend fun playTrailer(
        context: Context,
        idPlay: String,
        showType: String,
        webView: WebView,
        muted: Int = 1
    ) {

        val fetch = TMDBapi(context)

        val jsonObject = withContext(Dispatchers.IO) {
            fetch.fetchVideoData(idPlay, showType)
        }

        var videoId = ""

        if (jsonObject != null) {
            val results = jsonObject.optJSONArray("results") ?: return

            if (results.length() == 0) return

            for (i in 0 until results.length()) {
                val obj = results.getJSONObject(i)

                if (obj.getString("site") == "YouTube" &&
                    obj.getString("type") == "Trailer" &&
                    obj.getBoolean("official")
                ) {

                    videoId = obj.getString("key")

                    if (videoId.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            setupWebView(context, webView, videoId, muted)
                        }
                    }

                    break
                }
            }
        }
    }


     fun setupWebView(context: Context, webView: WebView, videoId: String, muted:Int=1) {

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()

        webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.userAgentString =
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.loadsImagesAutomatically = false
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest
                ): WebResourceResponse? {
                    return assetLoader.shouldInterceptRequest(request.url)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d("WebView", "Player Loaded")
                }
            }
        }

         val typedValue = TypedValue()
         context.theme.resolveAttribute(R.attr.BG_color, typedValue, true)
         webView.setBackgroundColor(typedValue.data)

        // Build HTML dynamically and save to assets (optional)
         //src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&rel=0&mute=1"
        val html = """
        <!DOCTYPE html>
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body { margin: 0; padding: 0; background-color: #000; }
                .container { position: relative; width: 100vw; height: 100vh; }
                iframe { position: absolute; top: 0; left: 0; width: 100%; height: 100%; }
            </style>
        </head>
        <body>
            <div class="container">
                <iframe
                    
                    src="https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&rel=0&mute=$muted&controls=0&modestbranding=1&playsinline=1&loop=1&playlist=$videoId"
                    frameborder="0"
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                    allowfullscreen>
                </iframe>
            </div>
        </body>
        </html>
    """.trimIndent()

        // Load HTML from fake HTTPS domain via asset loader

        webView.loadDataWithBaseURL(
            "https://appassets.androidplatform.net/assets/",
            html,
            "text/html",
            "utf-8",
            null
        )
         webView.visibility = View.VISIBLE
    }


    fun StopTrailer(webView: WebView) {

        webView.apply {
            onPause() // pause video/audio
            // stop video playback
            loadUrl("about:blank")
            stopLoading()
            onPause() // pause video/audio
            // clear temporary data
            clearHistory()
            clearFormData()
            clearCache(false)

            // hide player
            visibility = View.GONE
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////


    private const val DATABASE_NAME = "app_data.db"
    private const val DATABASE_VERSION = 1

    fun autoBackupDatabase(context: Context) {
        try {
            val dbHelper = object : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
                override fun onCreate(db: SQLiteDatabase) {}
                override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            }
            dbHelper.writableDatabase.close()

            val dbFile = context.getDatabasePath(DATABASE_NAME)
            if (!dbFile.exists()) {
                Log.e("Database_backup", "Database file not found")
                return
            }

            val resolver = context.contentResolver
            val backupName = "app_data_backup.db"
            val backupPath = "Documents/OnyxBackup/"

            // 1️⃣ Check for existing backup and delete it
            val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME}=? AND ${MediaStore.Files.FileColumns.RELATIVE_PATH}=?"
            val selectionArgs = arrayOf(backupName, backupPath)

            resolver.query(
                MediaStore.Files.getContentUri("external"),
                arrayOf(MediaStore.Files.FileColumns._ID),
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                while (cursor.moveToNext()) {
                    val existingUri = ContentUris.withAppendedId(
                        MediaStore.Files.getContentUri("external"),
                        cursor.getLong(idIndex)
                    )
                    resolver.delete(existingUri, null, null)
                    Log.d("Database_backup", "Deleted existing backup")
                }
            }

            // 2️⃣ Insert new backup
            val contentValues = ContentValues().apply {
                put(MediaStore.Files.FileColumns.DISPLAY_NAME, backupName)
                put(MediaStore.Files.FileColumns.MIME_TYPE, "application/octet-stream")
                put(MediaStore.Files.FileColumns.RELATIVE_PATH, backupPath)
                put(MediaStore.Files.FileColumns.IS_PENDING, 1)
            }

            val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                ?: run {
                    Log.e("Database_backup", "Failed to create MediaStore entry")
                    return
                }

            resolver.openOutputStream(uri)?.use { output ->
                FileInputStream(dbFile).use { input ->
                    input.copyTo(output)
                }
            }

            // Mark as complete
            contentValues.clear()
            contentValues.put(MediaStore.Files.FileColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)

            Log.d("Database_backup", "Database backed up successfully via MediaStore")

        } catch (e: Exception) {
            Log.e("Database_backup", "Backup failed: ${e.message}")
        }
    }

    fun autoRestoreDatabaseIfNeeded(context: Context) {
        try {
            val dbHelper = object : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
                override fun onCreate(db: SQLiteDatabase) {}
                override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            }

            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery("SELECT COUNT(*) FROM users", null)
            cursor.moveToFirst()
            val userCount = cursor.getInt(0)
            cursor.close()
            db.close()

            if (userCount > 0) {
                Log.d("Database_backup", "Database already has data, skipping restore")
                return
            }

            val resolver = context.contentResolver

            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.RELATIVE_PATH
            )

            val selection = """
            ${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND
            ${MediaStore.MediaColumns.RELATIVE_PATH} = ?
        """.trimIndent()

            val selectionArgs = arrayOf(
                "app_data_backup.db",
                "Documents/OnyxBackup/"
            )

            val queryCursor = resolver.query(
                MediaStore.Files.getContentUri("external"),
                projection,
                selection,
                selectionArgs,
                null
            )

            if (queryCursor == null || !queryCursor.moveToFirst()) {
                Log.d("Database_backup", "Backup NOT found in MediaStore")
                queryCursor?.close()
                return
            }

            val id = queryCursor.getLong(
                queryCursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            )

            queryCursor.close()

            val contentUri = ContentUris.withAppendedId(
                MediaStore.Files.getContentUri("external"),
                id
            )

            val dbFile = context.getDatabasePath(DATABASE_NAME)
            dbFile.parentFile?.mkdirs()

            dbHelper.close()

            resolver.openInputStream(contentUri)?.use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            } ?: run {
                Log.e("Database_backup", "Could not open backup stream")
                return
            }

            Log.d("Database_backup", "Database restored successfully")

        } catch (e: Exception) {
            Log.e("Database_backup", "Restore failed: ${e.message}")
        }
    }

    // give your app that premium, Netflix-style vertical scrolling behavior.
    fun snapRowToTopOnFocus_(scrollView: ScrollView, rowView: View) {
        rowView.viewTreeObserver.addOnGlobalFocusChangeListener { oldFocus, newFocus ->
            val oldWasInRow = oldFocus?.let { isViewAncestor(rowView, it) } ?: false
            val newIsInRow = newFocus?.let { isViewAncestor(rowView, it) } ?: false

            if (!oldWasInRow && newIsInRow) {
                scrollView.post {
                    scrollView.smoothScrollTo(0, rowView.top)
                }
            }
        }
    }

    fun snapRowToTopOnFocus(scrollView: ScrollView, rowView: View) {
        rowView.viewTreeObserver.addOnGlobalFocusChangeListener { oldFocus, newFocus ->
            val oldWasInRow = oldFocus?.let { isViewAncestor(rowView, it) } ?: false
            val newIsInRow = newFocus?.let { isViewAncestor(rowView, it) } ?: false

            if (!oldWasInRow && newIsInRow) {
                scrollView.post {
                    // ✅ FIX 1: Get the true Y position relative to the ScrollView
                    val targetY = getRelativeTop(rowView, scrollView)

                    // ✅ FIX 2: Use our custom interpolator and cancelable animation
                    animateScrollTo(scrollView, targetY.coerceAtLeast(0))
                }
            }
        }
    }



    private fun isViewAncestor(parent: View, child: View): Boolean {
        var current: android.view.ViewParent? = child.parent
        while (current != null) {
            if (current === parent) return true
            current = current.parent
        }
        return false
    }



    private var activeScrollAnimator: ValueAnimator? = null

    fun centerParentOnFocus(scrollView: ScrollView, parentView: View) {
        // Note: If you call this in a Fragment/Activity, ensure you remove the listener in onDestroyView
        parentView.viewTreeObserver.addOnGlobalFocusChangeListener { oldFocus, newFocus ->

            val oldWasInParent = oldFocus?.let { isViewAncestor(parentView, it) } ?: false
            val newIsInParent = newFocus?.let { isViewAncestor(parentView, it) } ?: false

            if (!oldWasInParent && newIsInParent) {
                scrollView.post {
                    // ✅ FIX 1: Calculate the true Y position relative to the ScrollView
                    val relativeTop = getRelativeTop(parentView, scrollView)

                    val targetY = relativeTop - (scrollView.height / 2) + (parentView.height / 2)

                    animateScrollTo(scrollView, targetY.coerceAtLeast(0))
                }
            }
        }
    }

    // Helper to get the absolute Top of a view relative to a specific ancestor
    private fun getRelativeTop(child: View, ancestor: ViewGroup): Int {
        var top = child.top
        var currentParent = child.parent as? View

        while (currentParent != null && currentParent != ancestor) {
            top += currentParent.top
            currentParent = currentParent.parent as? View
        }
        return top
    }

    private fun animateScrollTo(
        scrollView: ScrollView,
        targetY: Int
    ) {
        val startY = scrollView.scrollY
        if (startY == targetY) return // Already there

        // ✅ FIX 2: Cancel any currently running scroll animation before starting a new one
        activeScrollAnimator?.cancel()

        activeScrollAnimator = ValueAnimator.ofInt(startY, targetY).apply {
            duration = 450L

            interpolator = PathInterpolator(
                0.22f,
                1f,
                0.36f,
                1f
            ) // Prime/Netflix style easing

            addUpdateListener {
                scrollView.scrollTo(
                    0,
                    it.animatedValue as Int
                )
            }

            start()
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    fun setHeightToMatchScreen(view: View) {
        val displayMetrics = view.context.resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val params = view.layoutParams
        if (params != null) {
            params.height = screenHeight
            view.layoutParams = params
        } else {
            view.layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                screenHeight
            )
        }
    }

}

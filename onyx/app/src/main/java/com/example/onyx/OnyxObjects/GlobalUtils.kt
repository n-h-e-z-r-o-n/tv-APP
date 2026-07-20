package com.example.onyx.OnyxObjects

import java.time.DayOfWeek
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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.time.format.DateTimeParseException

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

    fun extractDynamicColor(
        context: Context,
        imageUrl: String,
        onColorExtracted: (Int) -> Unit
    ) {
        val sm = com.example.onyx.Database.SessionManger(context)
        if (!sm.isDynamicColorEnabled()) return

        Glide.with(context)
            .asBitmap()
            .load(imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(object : com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
                override fun onResourceReady(
                    resource: android.graphics.Bitmap,
                    transition: com.bumptech.glide.request.transition.Transition<in android.graphics.Bitmap>?
                ) {
                    androidx.palette.graphics.Palette.from(resource).generate { palette ->
                        val dominantColor = palette?.darkVibrantSwatch?.rgb
                            ?: palette?.darkMutedSwatch?.rgb
                            ?: palette?.dominantSwatch?.rgb
                            ?: android.graphics.Color.parseColor("#121212")

                        onColorExtracted(dominantColor)
                    }
                }
                override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
            })
    }

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


    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////




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
    ): Boolean {

        val fetch = TMDBapi(context)

        val jsonObject = withContext(Dispatchers.IO) {
            fetch.fetchVideoData(idPlay, showType)
        } ?: return false

        val results = jsonObject.optJSONArray("results") ?: return false
        if (results.length() == 0) return false

        // Build a list of all YouTube entries for priority fallback
        val youtubeVideos = (0 until results.length())
            .mapNotNull { results.optJSONObject(it) }
            .filter { it.optString("site") == "YouTube" && it.optString("key").isNotEmpty() }

        if (youtubeVideos.isEmpty()) return false

        // Priority: official Trailer → any Trailer → Teaser → first available
        val video = youtubeVideos.firstOrNull { it.optString("type") == "Trailer" && it.optBoolean("official") }
            ?: youtubeVideos.firstOrNull { it.optString("type") == "Trailer" }
            ?: youtubeVideos.firstOrNull { it.optString("type") == "Teaser" }
            ?: youtubeVideos.first()

        val videoId = video.optString("key")

        withContext(Dispatchers.Main) {
            try {
                setupWebView(context, webView, videoId, muted)
            } catch (e: Exception) {
                Log.e("playTrailer", "Failed to setup webview", e)
            }
        }

        return true
    }


     fun setupWebView(context: Context, webView: WebView, videoId: String, muted:Int=1) {

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()

        webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            // We DO NOT set a desktop User-Agent here.
            // Using a desktop User-Agent forces YouTube to load its heavy desktop player,
            // which causes speed changes, audio drops, and OOM crashes on TVs under network stress.
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
            onPause()           // pause video/audio
            stopLoading()
            loadUrl("about:blank")
            clearHistory()
            clearFormData()
            clearCache(false)
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



    private val activeScrollAnimators = java.util.WeakHashMap<ScrollView, ValueAnimator>()

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

            parentView.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    parentView.post {
                        val next = parentView.focusSearch(View.FOCUS_DOWN)
                        next?.requestFocus()
                    }
                }
            }
        }
    }

    // Helper to get the absolute Top of a view relative to a specific ancestor
    private  fun getRelativeTop(child: View, ancestor: ViewGroup): Int {
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

        // cancel any currently running scroll animation before starting a new one
        activeScrollAnimators[scrollView]?.cancel()

        val distance = kotlin.math.abs(targetY - startY)
        
        // Dynamic duration: fast for short scrolls, up to 450ms for large jumps
        val calculatedDuration = (250L + (distance / 5f)).toLong().coerceIn(250L, 450L)

        val animator = ValueAnimator.ofInt(startY, targetY).apply {
            duration = calculatedDuration

            interpolator = PathInterpolator(
                0.1f,
                0.9f,
                0.2f,
                1.0f
            ) // Premium TV-style decelerate curve

            addUpdateListener {
                scrollView.scrollTo(
                    0,
                    it.animatedValue as Int
                )
            }

            start()
        }
        activeScrollAnimators[scrollView] = animator
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


    fun formatDateString(dateString: String): String {
        return try {
            // The professional, thread-safe parser
            val parsedDate = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val standardFormat = parsedDate.format(DateTimeFormatter.ofPattern("MMM yyyy"))

            when {
                // 1. Future Releases
                parsedDate.isAfter(LocalDate.now()) -> "$standardFormat 🚀 (Upcoming)"

                // 2. Specific Pop Culture & Cinematic Dates
                parsedDate.monthValue == 5 && parsedDate.dayOfMonth == 4 -> "$standardFormat ⚔️ (May the 4th)"
                parsedDate.monthValue == 10 && parsedDate.dayOfMonth == 31 -> "$standardFormat 🎃 (Spooky Premiere)"
                parsedDate.monthValue == 12 && parsedDate.dayOfMonth == 25 -> "$standardFormat 🎄 (Holiday Release)"
                parsedDate.monthValue == 2 && parsedDate.dayOfMonth == 29 -> "$standardFormat ✨ (Leap Year Magic)"

                // 3. The "Friday the 13th" Horror Trope Check!
                parsedDate.dayOfWeek == DayOfWeek.FRIDAY && parsedDate.dayOfMonth == 13 -> "$standardFormat 🔪 (Friday the 13th!)"

                // 4. Eras & Decades
                parsedDate.year < 1980 -> "$standardFormat 🎞️ (Golden Era)"
                parsedDate.year in 1980..1989 -> "$standardFormat 📼 (VHS Era)"
                parsedDate.year in 1990..1999 -> "$standardFormat 💿 (DVD Era)"

                // 5. The Random Loot Table (Using a d100 roll for percentages)
                else -> {
                    val dropRate = Random.nextInt(100) // Rolls a number from 0 to 99
                    when {
                        dropRate < 5 -> "$standardFormat 🍿"   // 5% chance
                        dropRate < 10 -> "$standardFormat 🎬"  // 5% chance
                        dropRate < 12 -> "$standardFormat 🎟️"  // 2% chance (Admit One)
                        dropRate < 14 -> "$standardFormat 🕶️"  // 2% chance (3D Glasses)
                        dropRate == 99 -> "$standardFormat 👽 (Out of this world)" // 1% Ultra-Rare drop!

                        // 85% of the time, act completely professional
                        else -> standardFormat
                    }
                }
            }

        } catch (e: DateTimeParseException) {
            // The cheeky error handler
            if (dateString.isBlank() || dateString.equals("N/A", ignoreCase = true)) {
                "Lost in the Archives 🕵️‍♂️"
            } else {
                dateString
            }
        } catch (e: Exception) {
            // Absolute fallback just in case
            dateString
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////



    /**
     * Preloads movie images into the Glide disk cache in the background.
     */
     fun preloadMovieImages(context: Context, primaryUrl: String, secondaryUrl: String) {

         return
        /*
        val appContext = context.applicationContext

        if (primaryUrl.isNotBlank()) {
            Glide.with(appContext)
                .load(primaryUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .preload()
        }

        if (secondaryUrl.isNotBlank() && secondaryUrl != primaryUrl) {
            Glide.with(appContext)
                .load(secondaryUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .preload()
        }

         */
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    fun incrementMoviesWatched(context: Context) {
        val prefs = getSharedPreferences(context)
        val currentCount = prefs.getInt(KEY_MOVIES_WATCHED, 0)
        prefs.edit().putInt(KEY_MOVIES_WATCHED, currentCount + 1).apply()
        Log.d("GlobalUtils", "Movies watched incremented to: ${currentCount + 1}")
    }

    fun incrementSeriesWatched(context: Context) {
        val prefs = getSharedPreferences(context)
        val currentCount = prefs.getInt(KEY_SERIES_WATCHED, 0)
        prefs.edit().putInt(KEY_SERIES_WATCHED, currentCount + 1).apply()
        Log.d("GlobalUtils", "Series watched incremented to: ${currentCount + 1}")
    }

    fun getMoviesWatched(context: Context): Int {
        return getSharedPreferences(context).getInt(KEY_MOVIES_WATCHED, 0)
    }

    fun getSeriesWatched(context: Context): Int {
        return getSharedPreferences(context).getInt(KEY_SERIES_WATCHED, 0)
    }
}

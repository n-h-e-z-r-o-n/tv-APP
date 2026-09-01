package com.example.onyx

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.onyx.OnyxObjects.GlobalUtils
import com.example.onyx.OnyxObjects.StreamingLinks.performCenterClick
import java.util.concurrent.atomic.AtomicBoolean

class AnimeStreamResolver : AppCompatActivity() {

    private val isVideoResolved = AtomicBoolean(false)

    private var episodeId: String = ""
    private var serverName: String = ""
    private var category: String = ""
    private var embedUrl: String = ""
    private var initialReferer: String = ""
    private var initialOrigin: String = ""
    private var initialUserAgent: String = ""

    private var webView: WebView? = null

    private val timeoutHandler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

    private var cursorX = 0f
    private var cursorY = 0f
    private var cursorView: View? = null

    private val moveHandler = Handler(Looper.getMainLooper())
    private var moveRunnable: Runnable? = null
    private var currentStep = BASE_STEP

    override fun onCreate(savedInstanceState: Bundle?) {
        GlobalUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_play)

        episodeId = intent.getStringExtra(EXTRA_EPISODE_ID).orEmpty()
        serverName = intent.getStringExtra(EXTRA_SERVER_NAME).orEmpty()
        category = intent.getStringExtra(EXTRA_CATEGORY).orEmpty()
        embedUrl = intent.getStringExtra(EXTRA_EMBED_URL).orEmpty()
        initialReferer = intent.getStringExtra(EXTRA_REFERER).orEmpty()
        initialOrigin = intent.getStringExtra(EXTRA_ORIGIN).orEmpty()
        initialUserAgent = intent.getStringExtra(EXTRA_USER_AGENT).orEmpty()

        if (embedUrl.isBlank()) {
            finishWithError("Missing embed URL")
            return
        }

        val wv = findViewById<WebView>(R.id.webView)
        webView = wv

        val typedValue = TypedValue()
        theme.resolveAttribute(R.attr.BG_color, typedValue, true)
        wv.setBackgroundColor(typedValue.data)

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true)

        wv.webChromeClient = WebChromeClient()
        wv.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                wv.postDelayed({ wv.evaluateJavascript(AD_CLEANUP_JS, null) }, 5_000L)
                wv.postDelayed({ wv.performCenterClick(repeat = 4, interval = 2_500L) }, 8_000L)
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val reqUri = request?.url ?: return super.shouldInterceptRequest(view, request)
                val rawUrl = reqUri.toString()

                if (AD_DOMAINS.any { rawUrl.contains(it, ignoreCase = true) }) {
                    return WebResourceResponse("text/plain", "utf-8", null)
                }

                val path = reqUri.path?.lowercase().orEmpty()
                val scheme = reqUri.scheme.orEmpty()
                val lowerUrl = rawUrl.lowercase()

                val isVideo = scheme.startsWith("http") && (
                    VIDEO_EXTENSIONS.any { path.endsWith(it) } ||
                        STREAMING_INDICATORS.any { lowerUrl.contains(it) }
                    )

                if (BuildConfig.DEBUG && isVideo) {
                    Log.d("ANIME_RESOLVER", "Video candidate path=$path full=$rawUrl")
                }

                if (isVideo && isVideoResolved.compareAndSet(false, true)) {
                    val requestHeaders = request.requestHeaders ?: emptyMap()
                    val resolvedHeaders = buildResolvedHeaders(rawUrl, requestHeaders)
                    cancelTimeout()

                    runOnUiThread {
                        if (BuildConfig.DEBUG) {
                            Log.d("ANIME_RESOLVER", "Resolved stream $rawUrl headers=$resolvedHeaders")
                        }
                        finishWithSuccess(rawUrl, resolvedHeaders)
                    }
                }

                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url.toString()
                return !(url.startsWith("http://") || url.startsWith("https://") || url == "about:blank")
            }
        }

        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
            loadsImagesAutomatically = false
            blockNetworkImage = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString = initialUserAgent.ifBlank {
                WebSettings.getDefaultUserAgent(this@AnimeStreamResolver)
            }
        }

        if (BuildConfig.DEBUG) {
            Log.d(
                "ANIME_RESOLVER",
                "Loading embed url=$embedUrl referer=$initialReferer origin=$initialOrigin"
            )
        }

        wv.loadUrl(embedUrl, buildInitialLoadHeaders())

        setupMouseOverlay()
        setupBackPressedCallback()
        startTimeout()
    }

    private fun buildInitialLoadHeaders(): Map<String, String> {
        return buildMap {
            if (initialReferer.isNotBlank()) {
                put("Referer", initialReferer)
            }
            if (initialOrigin.isNotBlank()) {
                put("Origin", initialOrigin)
            }
        }
    }

    private fun buildResolvedHeaders(
        resolvedUrl: String,
        requestHeaders: Map<String, String>
    ): Map<String, String> {
        val normalizedHeaders = requestHeaders
            .filterKeys { it.isNotBlank() }
            .mapValues { it.value.trim() }
            .filterValues { it.isNotBlank() }
            .toMutableMap()

        val referer = normalizedHeaders.entries.firstOrNull {
            it.key.equals("Referer", ignoreCase = true)
        }?.value ?: deriveRefererFromUrl(resolvedUrl) ?: initialReferer

        val origin = normalizedHeaders.entries.firstOrNull {
            it.key.equals("Origin", ignoreCase = true)
        }?.value ?: deriveOrigin(referer) ?: deriveOrigin(resolvedUrl) ?: initialOrigin

        val userAgent = normalizedHeaders.entries.firstOrNull {
            it.key.equals("User-Agent", ignoreCase = true)
        }?.value ?: initialUserAgent.ifBlank {
            WebSettings.getDefaultUserAgent(this)
        }

        val passthroughHeaders = linkedMapOf<String, String>()
        normalizedHeaders.forEach { (key, value) ->
            val lowered = key.lowercase()
            if (lowered != "referer" && lowered != "origin" && lowered != "user-agent") {
                passthroughHeaders[key] = value
            }
        }

        return buildMap {
            putAll(passthroughHeaders)
            if (referer.isNotBlank()) {
                put("Referer", referer)
            }
            if (origin.isNotBlank()) {
                put("Origin", origin)
            }
            if (userAgent.isNotBlank()) {
                put("User-Agent", userAgent)
            }
        }
    }

    private fun deriveRefererFromUrl(url: String): String? {
        return try {
            val parsed = Uri.parse(url)
            val scheme = parsed.scheme
            val authority = parsed.encodedAuthority
            val path = parsed.encodedPath ?: "/"
            if (scheme.isNullOrBlank() || authority.isNullOrBlank()) return null

            val lastSlash = path.lastIndexOf('/')
            val basePath = if (lastSlash >= 0) path.substring(0, lastSlash + 1) else "/"
            "$scheme://$authority$basePath"
        } catch (_: Exception) {
            null
        }
    }

    private fun deriveOrigin(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return try {
            val parsed = Uri.parse(url)
            val scheme = parsed.scheme
            val authority = parsed.encodedAuthority
            if (scheme.isNullOrBlank() || authority.isNullOrBlank()) null else "$scheme://$authority"
        } catch (_: Exception) {
            null
        }
    }

    private fun finishWithSuccess(resolvedUrl: String, headers: Map<String, String>) {
        clearWebViewData()
        setResult(
            Activity.RESULT_OK,
            Intent().apply {
                putExtra(EXTRA_EPISODE_ID, episodeId)
                putExtra(EXTRA_SERVER_NAME, serverName)
                putExtra(EXTRA_CATEGORY, category)
                putExtra(EXTRA_RESOLVED_URL, resolvedUrl)
                putExtra(EXTRA_REFERER, headers["Referer"])
                putExtra(EXTRA_ORIGIN, headers["Origin"])
                putExtra(EXTRA_USER_AGENT, headers["User-Agent"])
            }
        )
        finish()
    }

    private fun finishWithError(message: String) {
        cancelTimeout()
        stopRepeating()
        clearWebViewData()
        setResult(
            Activity.RESULT_CANCELED,
            Intent().putExtra(EXTRA_ERROR_MESSAGE, message)
        )
        finish()
    }

    private fun startTimeout() {
        timeoutRunnable = Runnable {
            if (!isVideoResolved.get()) {
                Toast.makeText(
                    this,
                    "Stream not detected. Use the D-pad and press OK on the player.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        timeoutHandler.postDelayed(timeoutRunnable!!, 45_000L)
    }

    private fun cancelTimeout() {
        timeoutRunnable?.let { timeoutHandler.removeCallbacks(it) }
        timeoutRunnable = null
    }

    private fun clearWebViewData() {
        try {
            webView?.let { wv ->
                wv.handler?.removeCallbacksAndMessages(null)
                wv.stopLoading()
                wv.clearCache(true)
                wv.clearHistory()
                wv.clearFormData()
                CookieManager.getInstance().apply {
                    removeAllCookies(null)
                    flush()
                }
                WebStorage.getInstance().deleteAllData()
                wv.loadUrl("about:blank")
                wv.destroy()
                webView = null
            }
        } catch (e: Exception) {
            Log.e("ANIME_RESOLVER", "Failed to clear WebView", e)
        }
    }

    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishWithError("Resolver canceled")
            }
        })
    }

    private fun setupMouseOverlay() {
        val root = window.decorView.findViewById<ViewGroup>(android.R.id.content)
        val size = dp(30)
        val cursor = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(size, size)
            elevation = 300f
        }
        cursor.addView(View(this).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH, MATCH)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(170, 60, 140, 255))
                setStroke(dp(2), Color.WHITE)
            }
        })
        cursor.addView(View(this).apply {
            val dotSize = dp(8)
            layoutParams = FrameLayout.LayoutParams(dotSize, dotSize).apply {
                gravity = android.view.Gravity.CENTER
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.WHITE)
            }
        })

        cursorView = cursor
        root.addView(cursor)

        root.post {
            cursorX = root.width / 2f
            cursorY = root.height / 2f
            updateCursorPos()
        }
    }

    private fun moveCursor(dx: Float, dy: Float) {
        val root = window.decorView.findViewById<ViewGroup>(android.R.id.content)
        cursorX = (cursorX + dx * currentStep).coerceIn(0f, root.width.toFloat())
        cursorY = (cursorY + dy * currentStep).coerceIn(0f, root.height.toFloat())
        updateCursorPos()
    }

    private fun updateCursorPos() {
        val half = (cursorView?.width ?: dp(30)) / 2f
        cursorView?.x = cursorX - half
        cursorView?.y = cursorY - half
    }

    private fun startRepeating(action: () -> Unit) {
        stopRepeating()
        currentStep = BASE_STEP
        moveRunnable = object : Runnable {
            override fun run() {
                action()
                currentStep = (currentStep * 1.12f).coerceAtMost(MAX_STEP)
                moveHandler.postDelayed(this, 60L)
            }
        }
        moveHandler.post(moveRunnable!!)
    }

    private fun stopRepeating() {
        moveRunnable?.let { moveHandler.removeCallbacks(it) }
        moveRunnable = null
        currentStep = BASE_STEP
    }

    private fun dispatchCursorClick() {
        val wv = webView ?: return
        val location = IntArray(2)
        wv.getLocationOnScreen(location)
        val webX = cursorX - location[0]
        val webY = cursorY - location[1]

        if (webX < 0 || webY < 0 || webX > wv.width || webY > wv.height) return

        val time = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, webX, webY, 0)
        val up = MotionEvent.obtain(time, time + 100L, MotionEvent.ACTION_UP, webX, webY, 0)
        wv.dispatchTouchEvent(down)
        wv.dispatchTouchEvent(up)
        down.recycle()
        up.recycle()

        cursorView?.animate()
            ?.scaleX(1.8f)
            ?.scaleY(1.8f)
            ?.setDuration(80)
            ?.withEndAction {
                cursorView?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(100)?.start()
            }
            ?.start()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> {
                    startRepeating { moveCursor(0f, -1f) }
                    return true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    startRepeating { moveCursor(0f, 1f) }
                    return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    startRepeating { moveCursor(-1f, 0f) }
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    startRepeating { moveCursor(1f, 0f) }
                    return true
                }
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER -> {
                    dispatchCursorClick()
                    return true
                }
            }

            KeyEvent.ACTION_UP -> when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    stopRepeating()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }

    override fun onDestroy() {
        cancelTimeout()
        stopRepeating()
        isVideoResolved.set(false)
        clearWebViewData()
        super.onDestroy()
    }

    companion object {
        private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        private const val BASE_STEP = 10f
        private const val MAX_STEP = 55f

        const val EXTRA_EPISODE_ID = "episodeId"
        const val EXTRA_SERVER_NAME = "serverName"
        const val EXTRA_CATEGORY = "category"
        const val EXTRA_EMBED_URL = "embedUrl"
        const val EXTRA_RESOLVED_URL = "resolvedUrl"
        const val EXTRA_REFERER = "referer"
        const val EXTRA_ORIGIN = "origin"
        const val EXTRA_USER_AGENT = "userAgent"
        const val EXTRA_ERROR_MESSAGE = "errorMessage"

        private val VIDEO_EXTENSIONS = setOf(
            ".m3u8", ".mp4", ".webm", ".mov", ".mkv",
            ".ts", ".m4v", ".f4v", ".flv", ".wmv",
            ".avi", ".3gp", ".ogv", ".mpeg", ".mpg"
        )

        private val STREAMING_INDICATORS = listOf(
            "video=", "stream=", "media=", "playback", "videoplayback",
            "master.m3u8", "playlist.m3u8"
        )

        private val AD_DOMAINS = listOf(
            "doubleclick.net", "googlesyndication.com", "adservice.google.com",
            "popads.net", "adexchangeclear.com", "propellerads", "adsterra"
        )

        private val AD_CLEANUP_JS = """
            (function(){
                ['iframe[src*="doubleclick"]','iframe[src*="adservice"]','iframe[src*="/ads"]',
                 'div[id^="ad_"]','div[id*="_ad_"]','.adsbox','#ads','.ad-banner',
                 '.advertisement','.sponsor','div[class="ad-container"]'].forEach(sel=>{
                    document.querySelectorAll(sel).forEach(el=>el.remove());
                });
                const cx=window.innerWidth/2,cy=window.innerHeight/2;
                document.querySelectorAll('*').forEach(el=>{
                    const s=window.getComputedStyle(el);
                    if(s.position==='fixed'){
                        const r=el.getBoundingClientRect();
                        if(r.left<=cx&&r.right>=cx&&r.top<=cy&&r.bottom>=cy&&(r.height>80||r.width>80))
                            el.remove();
                    }
                });
                if(!window.__adObs){
                    new MutationObserver(ms=>ms.forEach(m=>m.addedNodes.forEach(n=>{
                        if(n.nodeType===1&&n.matches&&n.matches(
                            '.adsbox,.ad-banner,iframe[src*="doubleclick"],iframe[src*="adservice"]'))
                            n.remove();
                    }))).observe(document.body,{childList:true,subtree:true});
                    window.__adObs=true;
                }
            })();
        """.trimIndent()

        fun createIntent(
            context: Context,
            episodeId: String,
            serverName: String,
            category: String,
            embedUrl: String,
            referer: String?,
            origin: String?,
            userAgent: String?
        ): Intent {
            return Intent(context, AnimeStreamResolver::class.java).apply {
                putExtra(EXTRA_EPISODE_ID, episodeId)
                putExtra(EXTRA_SERVER_NAME, serverName)
                putExtra(EXTRA_CATEGORY, category)
                putExtra(EXTRA_EMBED_URL, embedUrl)
                putExtra(EXTRA_REFERER, referer)
                putExtra(EXTRA_ORIGIN, origin)
                putExtra(EXTRA_USER_AGENT, userAgent)
            }
        }
    }
}

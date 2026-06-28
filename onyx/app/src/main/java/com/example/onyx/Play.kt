package com.example.onyx

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
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
import android.webkit.*
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi
import com.example.onyx.OnyxObjects.GlobalUtils
import com.example.onyx.OnyxObjects.StreamingLinks
import com.example.onyx.OnyxObjects.StreamingLinks.performCenterClick
import java.util.concurrent.atomic.AtomicBoolean

class Play : AppCompatActivity() {

    /**
     * AtomicBoolean ensures only the first matching stream request launches the player,
     * even when shouldInterceptRequest fires concurrently from multiple threads (HLS segments).
     */
    private val isVideoLaunching = AtomicBoolean(false)

    private var showId: String = ""
    private var showType: String = ""
    private var showTitle: String = ""
    private var showPoster: String = ""
    private var showBackdrop: String = ""
    private var showSNo: String = ""
    private var showENo: String = ""

    // Field-level WebView reference so we can cancel callbacks and destroy safely
    private var webView: WebView? = null

    // Timeout: bail out gracefully if no stream URL is intercepted in time
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

    // ── Virtual Mouse Cursor (always on) ─────────────────────────────────────
    private var cursorX = 0f
    private var cursorY = 0f
    private var cursorView: View? = null

    private val moveHandler = Handler(Looper.getMainLooper())
    private var moveRunnable: Runnable? = null
    private var currentStep = BASE_STEP

    // ─────────────────────────────────────────────────────────────────────────

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        GlobalUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_play)

        showId       = intent.getStringExtra("imdb_code")   ?: ""
        showType     = intent.getStringExtra("type")         ?: ""
        showTitle    = intent.getStringExtra("title")        ?: ""
        showPoster   = intent.getStringExtra("poster")       ?: ""
        showBackdrop = intent.getStringExtra("showBackdrop") ?: ""
        showSNo      = intent.getStringExtra("seasonNo")     ?: ""
        showENo      = intent.getStringExtra("episodeNo")    ?: ""

        if (BuildConfig.DEBUG) {
            Log.d("DEBUG_WEBVIEW", "id=$showId type=$showType title=$showTitle S=$showSNo E=$showENo")
            Log.d("DEBUG_WEBVIEW", "poster=$showPoster backdrop=$showBackdrop")
        }

        if (showType == "movie") GlobalUtils.incrementMoviesWatched(this)
        else GlobalUtils.incrementSeriesWatched(this)

        //----------------------------------------------------------------------

        val wv = findViewById<WebView>(R.id.webView)
        webView = wv

        // Avoid white flash while content loads
        val typedValue = TypedValue()
        theme.resolveAttribute(R.attr.BG_color, typedValue, true)
        wv.setBackgroundColor(typedValue.data)

        wv.webChromeClient = WebChromeClient()
        wv.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                // Step 1 – ad cleanup at 3 s
                wv.postDelayed({ wv.evaluateJavascript(AD_CLEANUP_JS, null) }, 5000L)

                // Step 2 – centre-click at 7 s (after ads have been removed)
                //wv.postDelayed({ wv.performCenterClick() }, 70000L)
            }

            @OptIn(UnstableApi::class)
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val reqUri = request?.url ?: return super.shouldInterceptRequest(view, request)
                val rawUrl = reqUri.toString()

                // ── Ad blocker (runs on every request) ────────────────────────
                if (AD_DOMAINS.any { rawUrl.contains(it) }) {
                    return WebResourceResponse("text/plain", "utf-8", null)
                }

                // ── Video URL detection ───────────────────────────────────────
                // Use the path component (no query string) so we test only the
                // file extension at the END of the path, preventing false-positive
                // matches like "?format=mp4" or "/api/mp4-proxy/check".
                val path = reqUri.path?.lowercase() ?: ""
                val scheme = reqUri.scheme ?: ""

                val isVideo = scheme.startsWith("http") &&       // must be real HTTP(S)
                              path.isNotEmpty() &&                // must have a path
                              VIDEO_EXTENSIONS.any { ext -> path.endsWith(ext) }

                if (BuildConfig.DEBUG && isVideo) {
                    Log.d("DEBUG_WEBVIEW", "Video candidate → path=$path  full=$rawUrl")
                }

                // compareAndSet: only the FIRST matching request launches the player.
                // All subsequent concurrent calls (HLS segments, etc.) are skipped.
                if (isVideo && isVideoLaunching.compareAndSet(false, true)) {
                    val headers = request.requestHeaders
                    val referer = headers["Referer"]
                    val ua      = headers["User-Agent"]
                    cancelTimeout()
                    runOnUiThread {
                        if (BuildConfig.DEBUG) Log.d("DEBUG_WEBVIEW", "Launching player → $rawUrl")
                        clearWebViewData()
                        Video_payer.playVideoExternally(
                            this@Play, rawUrl, referer, ua,
                            showId, showType, showTitle,
                            showPoster, showBackdrop, showSNo, showENo
                        )
                        finish()
                    }
                }

                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url.toString()
                val allowed = ALLOWED_URL_PREFIXES.any { url.startsWith(it) }
                if (!allowed && BuildConfig.DEBUG) Log.d("DEBUG_WEBVIEW", "Navigation blocked: $url")
                return !allowed
            }
        }

        // Apply all settings once — no duplicate assignments
        wv.settings.apply {
            javaScriptEnabled                     = true
            domStorageEnabled                     = true
            mediaPlaybackRequiresUserGesture      = false
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
            userAgentString                       = WebSettings.getDefaultUserAgent(this@Play)
        }

        val url = StreamingLinks.getServerUrl(this, showType, showId.trim(), showSNo.trim(), showENo.trim())
        if (BuildConfig.DEBUG) Log.d("DEBUG_WEBVIEW", "Loading: $url")
        wv.loadUrl(url)

        setupMouseOverlay()
        startTimeout()
        setupBackPressedCallback()
    }

    // ── Virtual Mouse Cursor ──────────────────────────────────────────────────

    /**
     * Adds a cursor dot on top of the WebView. Always visible — D-pad keys
     * move it and DPAD_CENTER / ENTER dispatches a click at its position.
     */
    private fun setupMouseOverlay() {
        val root = window.decorView.findViewById<ViewGroup>(android.R.id.content)

        val cSize = dp(30)
        val cursor = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(cSize, cSize)
            elevation = 300f
        }
        // Outer ring
        cursor.addView(View(this).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH, MATCH)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(170, 60, 140, 255))
                setStroke(dp(2), Color.WHITE)
            }
        })
        // Inner precision dot
        cursor.addView(View(this).apply {
            val s = dp(8)
            layoutParams = FrameLayout.LayoutParams(s, s).apply {
                gravity = android.view.Gravity.CENTER
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.WHITE)
            }
        })
        cursorView = cursor
        root.addView(cursor)

        // Initialise cursor to screen centre after layout is measured
        root.post {
            cursorX = root.width / 2f
            cursorY = root.height / 2f
            updateCursorPos()
        }
    }


    /**
     * Move the cursor by (dx, dy) direction units.
     * Speed is controlled by [currentStep] which accelerates while a button is held.
     */
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

    /**
     * Begin a repeating movement action with progressive acceleration.
     * Speed starts at [BASE_STEP] and ramps up to [MAX_STEP] the longer a button is held.
     */
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

    /**
     * Dispatch a real [MotionEvent] touch at the cursor's current screen position
     * into the underlying WebView, then play a pulse animation on the cursor.
     */
    private fun dispatchCursorClick() {
        val wv = webView ?: return

        // Convert screen-space cursor coords → WebView-local coords
        val loc = IntArray(2)
        wv.getLocationOnScreen(loc)
        val wx = cursorX - loc[0]
        val wy = cursorY - loc[1]

        // Ignore if cursor is outside WebView bounds
        if (wx < 0 || wy < 0 || wx > wv.width || wy > wv.height) return

        val t    = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(t, t,       MotionEvent.ACTION_DOWN, wx, wy, 0)
        val up   = MotionEvent.obtain(t, t + 100L, MotionEvent.ACTION_UP, wx, wy, 0)
        wv.dispatchTouchEvent(down)
        wv.dispatchTouchEvent(up)
        down.recycle()
        up.recycle()

        // Pulse animation to confirm the click visually
        cursorView?.animate()
            ?.scaleX(1.8f)?.scaleY(1.8f)?.setDuration(80)
            ?.withEndAction {
                cursorView?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(100)?.start()
            }?.start()
    }


    /**
     * D-pad always drives the cursor — no toggle needed.
     * Intercepts at the Activity level so the WebView never steals these keys.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP    -> { startRepeating { moveCursor(0f,  -1f) }; return true }
                KeyEvent.KEYCODE_DPAD_DOWN  -> { startRepeating { moveCursor(0f,  +1f) }; return true }
                KeyEvent.KEYCODE_DPAD_LEFT  -> { startRepeating { moveCursor(-1f,  0f) }; return true }
                KeyEvent.KEYCODE_DPAD_RIGHT -> { startRepeating { moveCursor(+1f,  0f) }; return true }
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER      -> { dispatchCursorClick();                   return true }
            }
            KeyEvent.ACTION_UP -> when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT -> { stopRepeating(); return true }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()

    // ── Constants ─────────────────────────────────────────────────────────────

    companion object {
        private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        private const val BASE_STEP = 10f
        private const val MAX_STEP  = 55f

        /**
         * Extensions checked against the **path component only** (no query string).
         * Using a Set gives O(1) lookup instead of linear scan.
         */
        private val VIDEO_EXTENSIONS = setOf(
            ".m3u8", ".mp4", ".webm", ".mov", ".mkv",
            ".ts",   ".m4v", ".f4v", ".flv", ".wmv",
            ".avi",  ".3gp", ".ogv", ".mpeg", ".mpg"
        )
        private val AD_DOMAINS = listOf(
            "doubleclick.net", "googlesyndication.com", "adservice.google.com",
            "popads.net", "adexchangeclear.com", "propellerads", "adsterra"
        )
        private val ALLOWED_URL_PREFIXES = listOf(
            "https://vidsrc.to/",
            "https://player.embed-api.stream/",
            "https://2embed",
            "https://embedmaster",
            "https://embdmstrplayer",
            "https://embed.su/",
            "https://primewire.si/",
            "https://vidking.net/"
        )

        /**
         * Minified JS injected 3 s after page load to strip ad elements
         * before the automatic centre-click fires at 7 s.
         */
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
    }

    // ── Timeout ───────────────────────────────────────────────────────────────

    private fun startTimeout() {
        timeoutRunnable = Runnable {
            if (!isVideoLaunching.get()) {
                android.widget.Toast.makeText(
                    this,
                    "Stream not detected — use the D-pad to navigate and press OK to click play.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
        timeoutHandler.postDelayed(timeoutRunnable!!, 45_000L)
    }

    private fun cancelTimeout() {
        timeoutRunnable?.let { timeoutHandler.removeCallbacks(it) }
        timeoutRunnable = null
    }

    // ── WebView cleanup ───────────────────────────────────────────────────────

    private fun clearWebViewData() {
        try {
            webView?.let { wv ->
                // Cancel ALL pending postDelayed callbacks before destroy
                // (centre-click loop, JS injection, etc.) — prevents use-after-destroy crashes.
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
                if (BuildConfig.DEBUG) Log.d("DEBUG_TAG_PlayActivity", "WebView cleared")
            }
        } catch (e: Exception) {
            Log.e("DEBUG_TAG_PlayActivity", "Failed to clear WebView", e)
        }
    }

    // ── Back press & lifecycle ────────────────────────────────────────────────

    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                cancelTimeout()
                stopRepeating()
                clearWebViewData()
                finish()
            }
        })
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        cancelTimeout()
        stopRepeating()
        isVideoLaunching.set(false)
        // Note: do NOT call finish() here — activity is already being destroyed.
        super.onDestroy()
    }
}

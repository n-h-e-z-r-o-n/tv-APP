package com.example.onyx

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.os.Handler
import android.os.Looper
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

class Home_Page : AppCompatActivity() {
    
    private var autoScrollHandler: Handler? = null
    private var autoScrollRunnable: Runnable? = null
    private var currentPosition = 0
    private var sliderAdapter: CardSwiper? = null
    private var sliderRecyclerView: RecyclerView? = null
    private var isUserInteracting = false
    private var interactionPauseTime = 20000L // Pause for 10 seconds after user interaction
    private var autoScrollInterval = 15000L // 5 seconds between slides
    private var initialDelay = 12000L // 2 seconds before starting auto-scroll
    private var lastInteractionTime = 0L
    private var userScrollDirection = 0 // -1 for left, 1 for right, 0 for none
    private var interactionCount = 0
    private var isUserScrolling = false
    private var lastFocusTime = 0L
    private var focusChangeCount = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        GlobalUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_page)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        LoadingAnimation.setup(this@Home_Page)
        NavAction.setupSidebar(this)

        SliderData()
    }
    
    override fun onResume() {
        super.onResume()
        resetInteractionCounters()
        startAutoScroll()
    }
    
    override fun onPause() {
        super.onPause()
        stopAutoScroll()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopAutoScroll()
    }


    private fun SliderData() {
        LoadingAnimation.show(this@Home_Page)
        CoroutineScope(Dispatchers.IO).launch {

            repeat(5) { attempt ->

                try {
                    val url = "https://api.themoviedb.org/3/discover/movie?include_adult=true"
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("accept", "application/json")
                    connection.setRequestProperty(
                        "Authorization",
                        "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJhZjliMmUyN2MxYTZiYzMyMzNhZjE4MzJmNGFjYzg1MCIsIm5iZiI6MTcxOTY3NDUxNy4xOTYsInN1YiI6IjY2ODAyNjk1ZWZhYTI1ZjBhOGE4NGE3MyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.RTms-g8dzOl3WwCeJ7WNLq3i2kXxl3T7gOTa8POcxcw"
                    )

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = org.json.JSONObject(response)
                    val moviesArray = jsonObject.getJSONArray("results")


                    val url2 = "https://api.themoviedb.org/3/discover/tv?include_adult=true"
                    val connection2 = URL(url2).openConnection() as HttpURLConnection
                    connection2.requestMethod = "GET"
                    connection2.setRequestProperty("accept", "application/json")
                    connection2.setRequestProperty(
                        "Authorization",
                        "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJhZjliMmUyN2MxYTZiYzMyMzNhZjE4MzJmNGFjYzg1MCIsIm5iZiI6MTcxOTY3NDUxNy4xOTYsInN1YiI6IjY2ODAyNjk1ZWZhYTI1ZjBhOGE4NGE3MyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.RTms-g8dzOl3WwCeJ7WNLq3i2kXxl3T7gOTa8POcxcw"
                    )

                    val response2 = connection2.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject2 = org.json.JSONObject(response2)
                    val moviesArray2 = jsonObject2.getJSONArray("results")


                    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                    val url3 =
                        "https://api.themoviedb.org/3/trending/all/day?primary_release_year=$currentYear"
                    val connection3 = URL(url3).openConnection() as HttpURLConnection
                    connection3.requestMethod = "GET"
                    connection3.setRequestProperty("accept", "application/json")
                    connection3.setRequestProperty(
                        "Authorization",
                        "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJhZjliMmUyN2MxYTZiYzMyMzNhZjE4MzJmNGFjYzg1MCIsIm5iZiI6MTcxOTY3NDUxNy4xOTYsInN1YiI6IjY2ODAyNjk1ZWZhYTI1ZjBhOGE4NGE3MyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.RTms-g8dzOl3WwCeJ7WNLq3i2kXxl3T7gOTa8POcxcw"
                    )

                    val response3 = connection3.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject3 = org.json.JSONObject(response3)
                    val moviesArray3 = jsonObject3.getJSONArray("results")




                    var movies = mutableListOf<SliderItem>()
                    for (i in 0 until moviesArray.length()) {

                        val item = moviesArray.getJSONObject(i)

                        val title = item.getString("title")
                        //val backdrop_path = "https://image.tmdb.org/t/p/w1280" + item.getString("backdrop_path")

                        val backdrop_path = if (item.has("backdrop_path") && !item.isNull("backdrop_path")) {
                            "https://image.tmdb.org/t/p/w1280${item.getString("backdrop_path")}"
                        } else if (item.has("poster_path") && !item.isNull("poster_path")) {
                            "https://image.tmdb.org/t/p/w780${item.getString("poster_path")}"
                        } else { "" }

                        val PG = if (item.optString("adult") == "true") "PG-18 +" else "PG-13"
                        if(PG =="PG-18 +"){continue}

                        val id = item.getString("id")
                        val type = "movie"
                        val overview = item.getString("overview")
                        val release_date = item.getString("release_date").substring(0, 4)
                        val vote_average = item.getString("vote_average").substring(0, 3)
                        val poster_path = item.getString("poster_path")
                        val genreIdsJson = item.getJSONArray("genre_ids")
                        val genreIds: List<Int> = List(genreIdsJson.length()) { idx ->
                            genreIdsJson.getInt(idx)
                        }

                        movies.add(
                            SliderItem(
                                title,
                                backdrop_path,
                                id,
                                type,
                                overview,
                                release_date,
                                vote_average,
                                poster_path,
                                genreIds,
                                PG
                            )

                        )
                        Log.e("DEBUG_MAIN_Slider 1", movies.toString())
                    }


                    for (i in 0 until moviesArray2.length()) {

                        val item = moviesArray2.getJSONObject(i)
                        val title = item.getString("original_name")

                        val backdrop_path = if (item.has("backdrop_path") && !item.isNull("backdrop_path")) {
                            "https://image.tmdb.org/t/p/w1280${item.getString("backdrop_path")}"
                        } else if (item.has("poster_path") && !item.isNull("poster_path")) {
                            "https://image.tmdb.org/t/p/w780${item.getString("poster_path")}"
                        } else { "" }

                        val PG = if (item.optString("adult") == "true") "PG-18 +" else "PG-13"

                        val id = item.getString("id")
                        val type = "tv"
                        val overview = item.getString("overview")
                        val release_date = item.getString("first_air_date").substring(0, 4)
                        val vote_average = item.getString("vote_average").substring(0, 3)
                        val poster_path = item.getString("poster_path")
                        val genreIdsJson = item.getJSONArray("genre_ids")
                        val genreIds: List<Int> = List(genreIdsJson.length()) { idx ->
                            genreIdsJson.getInt(idx)
                        }

                        movies.add(
                            SliderItem(
                                title,
                                backdrop_path,
                                id,
                                type,
                                overview,
                                release_date,
                                vote_average,
                                poster_path,
                                genreIds,
                                PG
                            )
                        )
                    }

                    for (i in 0 until moviesArray3.length()) {
                        val item = moviesArray3.getJSONObject(i)
                        val title = when {
                            item.has("original_name") && !item.isNull("original_name") -> item.getString(
                                "original_name"
                            )

                            item.has("original_title") && !item.isNull("original_title") -> item.getString(
                                "original_title"
                            )

                            item.has("title") && !item.isNull("title") -> item.getString("title")
                            else -> "Untitled"
                        }

                        val type = item.getString("media_type")
                        if (type != "movie" && type != "tv") {
                            continue   // skip this loop iteration
                        }
                        val backdrop_path = if (item.has("backdrop_path") && !item.isNull("backdrop_path")) {
                            "https://image.tmdb.org/t/p/w1280${item.getString("backdrop_path")}"
                        } else if (item.has("poster_path") && !item.isNull("poster_path")) {
                            "https://image.tmdb.org/t/p/w780${item.getString("poster_path")}"
                        } else { "" }

                        val PG = if (item.optString("adult") == "true") "PG-18 +" else "PG-13"
                        val id = item.getString("id")
                        val overview = item.getString("overview")
                        val release_date = try {
                                    item.getString("release_date").substring(0, 4)
                                } catch (e: Exception) {
                                    item.getString("first_air_date").substring(0, 4)
                                }
                        val vote_average = item.getString("vote_average").substring(0, 3)
                        val poster_path = item.getString("poster_path")
                        val genreIdsJson = item.getJSONArray("genre_ids")
                        val genreIds: List<Int> = List(genreIdsJson.length()) { idx ->
                            genreIdsJson.getInt(idx)
                        }

                        movies.add(
                            SliderItem(
                                title,
                                backdrop_path,
                                id,
                                type,
                                overview,
                                release_date,
                                vote_average,
                                poster_path,
                                genreIds,
                                PG
                            )
                        )

                    }



                    //movies.shuffle()
                    movies = movies.distinctBy { it.imdbCode }.toMutableList()

                    withContext(Dispatchers.Main) {
                        LoadingAnimation.hide(this@Home_Page)
                        val recyclerView = findViewById<RecyclerView>(R.id.Slider_widget)
                        val adapter = CardSwiper(movies, R.layout.card_layout)

                        // Store references for auto-scroll
                        sliderRecyclerView = recyclerView
                        sliderAdapter = adapter

                        val layoutManager = LinearLayoutManager(
                            this@Home_Page,
                            LinearLayoutManager.HORIZONTAL, // 👈 makes it horizontal
                            false
                        )
                        recyclerView.layoutManager = layoutManager
                        
                        // Add snap behavior for better auto-scroll experience
                        recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
                            override fun getItemOffsets(
                                outRect: android.graphics.Rect,
                                view: android.view.View,
                                parent: RecyclerView,
                                state: RecyclerView.State
                            ) {
                                val position = parent.getChildAdapterPosition(view)
                                if (position == 0) {
                                    outRect.left = 0
                                } else {
                                    outRect.left = 0
                                }
                                outRect.right = 0
                            }
                        })
                        
                        // Add snap helper for smooth snapping to items
                        val snapHelper = LinearSnapHelper()
                        snapHelper.attachToRecyclerView(recyclerView)
                        
                        // Enhanced scroll listener for better user interaction detection
                        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                            private var lastScrollX = 0
                            private var scrollStartTime = 0L
                            
                            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                                super.onScrollStateChanged(recyclerView, newState)
                                when (newState) {
                                    RecyclerView.SCROLL_STATE_DRAGGING -> {
                                        // User started manually scrolling
                                        handleUserInteractionStart()
                                        scrollStartTime = System.currentTimeMillis()
                                        Log.d("AutoScroll", "User started dragging - pausing auto-scroll")
                                    }
                                    RecyclerView.SCROLL_STATE_SETTLING -> {
                                        // User released but still scrolling
                                        isUserScrolling = true
                                        Log.d("AutoScroll", "User released - settling")
                                    }
                                    RecyclerView.SCROLL_STATE_IDLE -> {
                                        // Scrolling completely stopped
                                        handleUserInteractionEnd()
                                        Log.d("AutoScroll", "Scrolling stopped - will resume auto-scroll")
                                    }
                                }
                            }
                            
                            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                                super.onScrolled(recyclerView, dx, dy)
                                
                                if (isUserInteracting || isUserScrolling) {
                                    // Detect scroll direction
                                    userScrollDirection = when {
                                        dx > 0 -> 1  // Scrolling right
                                        dx < 0 -> -1 // Scrolling left
                                        else -> 0
                                    }
                                    
                                    // Update current position based on visible item
                                    val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
                                    layoutManager?.let { lm ->
                                        val firstVisible = lm.findFirstCompletelyVisibleItemPosition()
                                        if (firstVisible != RecyclerView.NO_POSITION) {
                                            currentPosition = firstVisible
                                        }
                                    }
                                    
                                    // Update last interaction time
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                            }
                        })
                        
                        // Setup touch listener for better interaction detection
                        setupTouchListener(recyclerView)
                        
                        // Setup focus change listener for TV remote interactions
                        setupFocusListener(recyclerView)
                        
                        recyclerView.adapter = adapter
                        
                        // Start auto-scrolling after a short delay
                        Handler(Looper.getMainLooper()).postDelayed({
                            startAutoScroll()
                        }, initialDelay)

                    }

                    return@launch
                } catch (e: Exception) {
                    delay(10_000)
                    Log.e("DEBUG_MAINSliderPage", "Error fetching data", e)
                }
            }
        }
    }
    
    private fun startAutoScroll() {
        if (isUserInteracting) {
            Log.d("AutoScroll", "Skipping auto-scroll start - user is interacting")
            return
        }
        
        stopAutoScroll() // Stop any existing auto-scroll
        
        autoScrollHandler = Handler(Looper.getMainLooper())
        autoScrollRunnable = object : Runnable {
            override fun run() {
                if (isUserInteracting) {
                    Log.d("AutoScroll", "Skipping auto-scroll - user is interacting")
                    return
                }
                
                sliderRecyclerView?.let { recyclerView ->
                    sliderAdapter?.let { adapter ->
                        if (adapter.itemCount > 1) {
                            currentPosition = (currentPosition + 1) % adapter.itemCount
                            
                            // Use custom smooth scroller for better animation
                            val smoothScroller = SmoothScroller(this@Home_Page)
                            smoothScroller.targetPosition = currentPosition
                            recyclerView.layoutManager?.startSmoothScroll(smoothScroller)
                            
                            Log.d("AutoScroll", "Scrolling to position: $currentPosition")
                        }
                    }
                }
                
                // Schedule next auto-scroll
                autoScrollHandler?.postDelayed(this, autoScrollInterval)
            }
        }
        
        // Start the auto-scroll
        autoScrollRunnable?.let { runnable ->
            autoScrollHandler?.postDelayed(runnable, autoScrollInterval)
        }
    }
    
    private fun stopAutoScroll() {
        autoScrollRunnable?.let { runnable ->
            autoScrollHandler?.removeCallbacks(runnable)
        }
        autoScrollHandler = null
        autoScrollRunnable = null
    }
    
    // Public methods for external control
    fun setAutoScrollInterval(interval: Long) {
        autoScrollInterval = interval
    }
    
    fun setInitialDelay(delay: Long) {
        initialDelay = delay
    }
    
    fun setInteractionPauseTime(pauseTime: Long) {
        interactionPauseTime = pauseTime
    }
    
    fun isAutoScrolling(): Boolean {
        return autoScrollHandler != null && !isUserInteracting
    }
    
    fun getUserInteractionStatus(): String {
        return when {
            isUserActivelyBrowsing() -> "Actively Browsing"
            isUserInteracting -> "Interacting"
            isUserScrolling -> "Scrolling"
            else -> "Watching"
        }
    }
    
    private fun handleUserInteractionStart() {
        isUserInteracting = true
        isUserScrolling = false
        interactionCount++
        lastInteractionTime = System.currentTimeMillis()
        stopAutoScroll()
        
        Log.d("AutoScroll", "User interaction #$interactionCount detected")
    }
    
    private fun handleUserInteractionEnd() {
        isUserScrolling = false
        
        // Calculate dynamic pause time based on user behavior
        val dynamicPauseTime = calculateDynamicPauseTime()
        
        Handler(Looper.getMainLooper()).postDelayed({
            if (isUserInteracting) {
                isUserInteracting = false
                startAutoScroll()
                Log.d("AutoScroll", "Resuming auto-scroll after ${dynamicPauseTime}ms pause")
            }
        }, dynamicPauseTime)
    }
    
    private fun calculateDynamicPauseTime(): Long {
        // Base pause time
        var pauseTime = interactionPauseTime
        
        // If user has interacted multiple times recently, increase pause time
        if (interactionCount > 3) {
            pauseTime = (pauseTime * 1.5).toLong()
        }
        
        // If user scrolled in opposite direction, give them more time
        if (userScrollDirection != 0) {
            pauseTime = (pauseTime * 1.2).toLong()
        }
        
        // If user is actively browsing (multiple focus changes), give more time
        if (focusChangeCount > 2) {
            pauseTime = (pauseTime * 1.3).toLong()
        }
        
        // If user is rapidly interacting, extend pause significantly
        if (isUserActivelyBrowsing()) {
            pauseTime = (pauseTime * 2.0).toLong()
        }
        
        // Cap the maximum pause time
        return pauseTime.coerceAtMost(30000L) // Max 30 seconds
    }
    
    private fun isUserActivelyBrowsing(): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastInteraction = currentTime - lastInteractionTime
        
        // User is actively browsing if they've had multiple interactions in the last 30 seconds
        return interactionCount > 2 && timeSinceLastInteraction < 30000L
    }
    
    // Add touch listener for better interaction detection
    private fun setupTouchListener(recyclerView: RecyclerView) {
        recyclerView.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    handleUserInteractionStart()
                    Log.d("AutoScroll", "Touch detected - pausing auto-scroll")
                }
                android.view.MotionEvent.ACTION_UP, 
                android.view.MotionEvent.ACTION_CANCEL -> {
                    // Don't immediately resume, let the scroll listener handle it
                    Log.d("AutoScroll", "Touch released")
                }
            }
            false // Don't consume the event
        }
    }
    
    // Add focus change listener for TV remote interactions
    private fun setupFocusListener(recyclerView: RecyclerView) {
        recyclerView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                lastFocusTime = System.currentTimeMillis()
                focusChangeCount++
                
                // If user is actively using the remote, pause auto-scroll
                if (focusChangeCount > 1) {
                    handleUserInteractionStart()
                    Log.d("AutoScroll", "Focus change detected - pausing auto-scroll")
                }
            }
        }
        
        // Add child focus change listener
        recyclerView.addOnChildAttachStateChangeListener(object : RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: android.view.View) {
                view.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        lastFocusTime = System.currentTimeMillis()
                        focusChangeCount++
                        
                        if (focusChangeCount > 1) {
                            handleUserInteractionStart()
                            Log.d("AutoScroll", "Child focus change detected - pausing auto-scroll")
                        }
                    }
                }
            }
            
            override fun onChildViewDetachedFromWindow(view: android.view.View) {
                // Clean up if needed
            }
        })
    }
    
    // Reset interaction counters periodically
    private fun resetInteractionCounters() {
        val currentTime = System.currentTimeMillis()
        
        // Reset counters if it's been more than 5 minutes since last interaction
        if (currentTime - lastInteractionTime > 300000L) { // 5 minutes
            interactionCount = 0
            focusChangeCount = 0
            userScrollDirection = 0
            Log.d("AutoScroll", "Interaction counters reset")
        }
    }

}


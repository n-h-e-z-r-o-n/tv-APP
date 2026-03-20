package com.example.onyx

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import com.bumptech.glide.request.target.Target
import com.example.onyx.Database.AppDatabase
import com.example.onyx.Database.SessionManger
import com.example.onyx.FetchData.TMDBapi
import com.example.onyx.OnyxClasses.CategoryAdapter
import com.example.onyx.OnyxClasses.CustomKeyboardManager
import com.example.onyx.OnyxClasses.EqualSpaceItemDecoration
import com.example.onyx.OnyxClasses.FavAdapter
import com.example.onyx.OnyxClasses.FavItem
import com.example.onyx.OnyxClasses.FilterAdapter
import com.example.onyx.OnyxClasses.GridAdapter
import com.example.onyx.OnyxClasses.GridAdapter2
import com.example.onyx.OnyxClasses.MovieItem
import com.example.onyx.OnyxClasses.MovieItemOne
import com.example.onyx.OnyxClasses.NotificationAdapter
import com.example.onyx.OnyxClasses.NotificationItem
import com.example.onyx.OnyxClasses.OnSearchListener
import com.example.onyx.OnyxClasses.cWatchingAdapter
import com.example.onyx.OnyxClasses.categoryItem
import com.example.onyx.OnyxClasses.filterItemOne
import com.example.onyx.OnyxObjects.GlobalUtils
import com.example.onyx.OnyxObjects.LoadingAnimation
import com.example.onyx.OnyxObjects.NavAction
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import java.io.IOException
import java.util.Calendar

class Shows_Page : AppCompatActivity() {
    private var currentMoviePage = 1
    private var isLoadingMoreMovies = false

    private var lastFocusedView: View? = null
    private var currentTvPage = 1
    private var isLoadingMoreTv = false



    private lateinit var moviesBtn: LinearLayout
    private lateinit var tvBtn: LinearLayout
    private lateinit var SearchBtn: LinearLayout
    private lateinit var filterBtn: LinearLayout


    private lateinit var mvBtnText: TextView
    private lateinit var tvBtnText: TextView


    private lateinit var searchContainerImg: ImageView
    private lateinit var filterContainerImg: ImageView

    private lateinit var searchContainer: LinearLayout
    private lateinit var fliterContainer: LinearLayout


    //Adapters
    private lateinit var movieAdapter: GridAdapter
    private lateinit var tvAdapter: GridAdapter
    private lateinit var searchAdapter: GridAdapter2
    private lateinit var filterAdapter: FilterAdapter
    private lateinit var thrillAdapter: FilterAdapter



    //RecyclerViews
    private lateinit var tvRecyclerView : RecyclerView
    private lateinit var movieRecyclerView : RecyclerView
    private lateinit var searchRecyclerView : RecyclerView
    private lateinit var fliterRecyclerView : RecyclerView
    private lateinit var  thrillRecyclerView : RecyclerView


    private lateinit var faveRecyclerView: RecyclerView
    private lateinit var faveAdapter: FavAdapter

    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var  notificationRecyclerView: RecyclerView

    private lateinit var watchRecyclerView: RecyclerView
    private lateinit var watchAdapter: cWatchingAdapter


    private lateinit var db: AppDatabase
    private lateinit var  sm: SessionManger

    private lateinit var fetchTMDB: TMDBapi
    private var userId: Int = -1


    override fun onCreate(savedInstanceState: Bundle?) {
        GlobalUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_shows_page)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        LoadingAnimation.setup(this, R.raw.line_loading)
        //LoadingAnimation.show(this)
        NavAction.setupSidebar(this)
        fetchTMDB = TMDBapi(this)
        db = AppDatabase(this)         // Initialize database
        sm = SessionManger(this)
        userId = sm.getUserId()


        //setupBackPressedCallback()
        trackFocus()
        ////////////////////////////////////////////////////////////////////////////////////////////
        val navBar = findViewById<LinearLayout>(R.id.NavBar)

        val HomeBtn = findViewById<LinearLayout>(R.id.HomeBtn)
        val MoviesBtn = findViewById<LinearLayout>(R.id.MoviesBtn)
        val seriesBtn = findViewById<LinearLayout>(R.id.seriesBtn)
        val SearchBtn = findViewById<LinearLayout>(R.id.SearchBtn)
        val FilterBtn = findViewById<LinearLayout>(R.id.FilterBtn)
        val FavBtn = findViewById<LinearLayout>(R.id.FavBtn)
        val cWatchBtn = findViewById<LinearLayout>(R.id.cWatchBtn)
        val cNotificationBtn = findViewById<LinearLayout>(R.id.cNotificationBtn)


        val HomePage = findViewById<LinearLayout>(R.id.HomePage)
        val moviePage = findViewById<LinearLayout>(R.id.MoviePage)
        val seriesPage = findViewById<LinearLayout>(R.id.SeriesPage)
        val searchPage = findViewById<LinearLayout>(R.id.searchPage)
        val filterPage = findViewById<LinearLayout>(R.id.FilterPage)
        val FavePage = findViewById<LinearLayout>(R.id.FavePage)
        val cWatchPage = findViewById<LinearLayout>(R.id.cWatchPage)
        val cNotificationPage = findViewById<LinearLayout>(R.id.cNotificationPage)


        HomeBtn.setOnClickListener {
            HomePage.visibility = View.VISIBLE
            moviePage.visibility = View.GONE
            seriesPage.visibility = View.GONE
            searchPage.visibility = View.GONE
            filterPage.visibility = View.GONE
            FavePage.visibility = View.GONE
            cWatchPage.visibility = View.GONE
            cNotificationPage.visibility = View.GONE


            HomeBtn.isSelected = true
            MoviesBtn.isSelected = false
            seriesBtn.isSelected = false
            SearchBtn.isSelected = false
            FilterBtn.isSelected = false
            FavBtn.isSelected = false
            cWatchBtn.isSelected = false
            cNotificationBtn.isSelected = false
        }
        HomeBtn.setOnFocusChangeListener { v, hasFocus ->
            Log.e("AUTO CLICK", "HomeBtn : " + HomeBtn )
            if (hasFocus) {
                v.performClick()
            }
        }
        HomeBtn.performClick()
        HomeBtn.requestFocus()


        MoviesBtn.setOnClickListener {
            HomePage.visibility = View.GONE
            moviePage.visibility = View.VISIBLE
            seriesPage.visibility = View.GONE
            searchPage.visibility = View.GONE
            filterPage.visibility = View.GONE
            FavePage.visibility = View.GONE
            cWatchPage.visibility = View.GONE
            cNotificationPage.visibility = View.GONE


            HomeBtn.isSelected = false
            MoviesBtn.isSelected = true
            seriesBtn.isSelected = false
            SearchBtn.isSelected = false
            FilterBtn.isSelected = false
            FavBtn.isSelected = false
            cWatchBtn.isSelected = false
            cNotificationBtn.isSelected = false
        }
        MoviesBtn.setOnFocusChangeListener { v, hasFocus ->
            Log.e("AUTO CLICK", "MoviesBtn : " + hasFocus)
            if (hasFocus) {
                // Use post to delay the click until after focus is fully set
                v.post {
                    v.performClick()
                }
            }
        }




        seriesBtn.setOnClickListener {
            HomePage.visibility = View.GONE
            moviePage.visibility = View.GONE
            seriesPage.visibility = View.VISIBLE
            searchPage.visibility = View.GONE
            filterPage.visibility = View.GONE
            FavePage.visibility = View.GONE
            cWatchPage.visibility = View.GONE
            cNotificationPage.visibility = View.GONE

            HomeBtn.isSelected = false
            MoviesBtn.isSelected = false
            seriesBtn.isSelected = true
            SearchBtn.isSelected = false
            FilterBtn.isSelected = false
            FavBtn.isSelected = false
            cWatchBtn.isSelected = false
            cNotificationBtn.isSelected = false
        }

        SearchBtn.setOnClickListener {
            HomePage.visibility = View.GONE
            moviePage.visibility = View.GONE
            seriesPage.visibility = View.GONE
            searchPage.visibility = View.VISIBLE
            filterPage.visibility = View.GONE
            FavePage.visibility = View.GONE
            cWatchPage.visibility = View.GONE
            cNotificationPage.visibility = View.GONE

            HomeBtn.isSelected = false
            MoviesBtn.isSelected = false
            seriesBtn.isSelected = false
            SearchBtn.isSelected = true
            FilterBtn.isSelected = false
            FavBtn.isSelected = false
            cWatchBtn.isSelected = false
            cNotificationBtn.isSelected = false
        }


        FilterBtn.setOnClickListener {
            HomePage.visibility = View.GONE
            moviePage.visibility = View.GONE
            seriesPage.visibility = View.GONE
            searchPage.visibility = View.GONE
            filterPage.visibility = View.VISIBLE
            FavePage.visibility = View.GONE
            cWatchPage.visibility = View.GONE
            cNotificationPage.visibility = View.GONE

            HomeBtn.isSelected = false
            MoviesBtn.isSelected = false
            seriesBtn.isSelected = false
            SearchBtn.isSelected = false
            FilterBtn.isSelected = true
            FavBtn.isSelected = false
            cWatchBtn.isSelected = false
            cNotificationBtn.isSelected = false
        }


        FavBtn.setOnClickListener {
            HomePage.visibility = View.GONE
            moviePage.visibility = View.GONE
            seriesPage.visibility = View.GONE
            searchPage.visibility = View.GONE
            filterPage.visibility = View.GONE
            FavePage.visibility = View.VISIBLE
            cWatchPage.visibility = View.GONE
            cNotificationPage.visibility = View.GONE

            HomeBtn.isSelected = false
            MoviesBtn.isSelected = false
            seriesBtn.isSelected = false
            SearchBtn.isSelected = false
            FilterBtn.isSelected = false
            FavBtn.isSelected = true
            cWatchBtn.isSelected = false
            cNotificationBtn.isSelected = false
        }

        cWatchBtn.setOnClickListener {
            HomePage.visibility = View.GONE
            moviePage.visibility = View.GONE
            seriesPage.visibility = View.GONE
            searchPage.visibility = View.GONE
            filterPage.visibility = View.GONE
            FavePage.visibility = View.GONE
            cWatchPage.visibility = View.VISIBLE
            cNotificationPage.visibility = View.GONE

            HomeBtn.isSelected = false
            MoviesBtn.isSelected = false
            seriesBtn.isSelected = false
            SearchBtn.isSelected = false
            FilterBtn.isSelected = false
            FavBtn.isSelected = false
            cWatchBtn.isSelected = true
            cNotificationBtn.isSelected = false
        }

        cNotificationBtn.setOnClickListener {
            HomePage.visibility = View.GONE
            moviePage.visibility = View.GONE
            seriesPage.visibility = View.GONE
            searchPage.visibility = View.GONE
            filterPage.visibility = View.GONE
            FavePage.visibility = View.GONE
            cWatchPage.visibility = View.GONE
            cNotificationPage.visibility = View.VISIBLE

            HomeBtn.isSelected = false
            MoviesBtn.isSelected = false
            seriesBtn.isSelected = false
            SearchBtn.isSelected = false
            FilterBtn.isSelected = false
            FavBtn.isSelected = false
            cWatchBtn.isSelected = false
            cNotificationBtn.isSelected = true
        }

        GlobalUtils.expandParentOnChildFocus(
            parent = navBar,
            expandedWidthDp = 155f,
            collapsedWidthDp = 70f
        )

        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val screenWidth = displayMetrics.widthPixels


        val dp70 = (70 * displayMetrics.density).toInt()
        val container = findViewById<LinearLayout>(R.id.contentContainer)

        container.minimumWidth = screenWidth - dp70
        val params = container.layoutParams
        params.width = screenWidth - dp70
        container.layoutParams = params



        ////////////////////////////////////////////////////////////////////////////////////////////

        setupRecyclerViews()




        lifecycleScope.launch {
            HomeData()
            categoryShow()
            dark()

            fetchMovies()
            fetchTvShows()
            filter()
            tvFavoritesList()
            notificationS()
            watchedList()

        }

    }

    override fun onResume() {
        super.onResume()


        if (this::watchAdapter.isInitialized) {
            watchAdapter.clearItems()
        }


        /*
        if(this::notificationAdapter.isInitialized){
            notificationAdapter.clearItems()
        }
        notificationS()
        */


        if (this::faveAdapter.isInitialized) {
            faveAdapter.clearItems()
        }

        tvFavoritesList()
        watchedList()


        // Only request focus if nothing has focus
        window.decorView.post {
            if (currentFocus == null) {
                if (lastFocusedView != null && lastFocusedView!!.isShown && lastFocusedView!!.isFocusable) {
                    lastFocusedView!!.requestFocus()
                } else {
                    findViewById<LinearLayout>(R.id.HomeBtn).requestFocus()
                }
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()

        // Clear all adapters
        movieAdapter.clearItems()
        tvAdapter.clearItems()
        searchAdapter.clearItems()
        filterAdapter.clearItems()

        // Cancel any running coroutines
        lifecycleScope.coroutineContext.cancelChildren()

        // Remove all handler callbacks
        Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null)

        finish()
    }


    private fun trackFocus() {
        window.decorView.viewTreeObserver.addOnGlobalFocusChangeListener { _, newFocus ->
            if (newFocus != null) {
                lastFocusedView = newFocus
            }
        }
    }

    private fun setupRecyclerViews() {

        val Spacing = (10 * resources.displayMetrics.density).toInt()
        val item_grid2_width = 280
        val gapUsed = 70


        //  Movies ---------------------------------------------------------------------------------
        movieRecyclerView = findViewById(R.id.MoviesRecyclerView)
        movieRecyclerView.layoutManager  = object :GridLayoutManager(this@Shows_Page,  GlobalUtils.calculateSpanCountV2(this, item_grid2_width, gapUsed )){
            override fun onInterceptFocusSearch(focused: View, direction: Int): View? {
                val currentPosition = getPosition(focused)
                if (currentPosition == RecyclerView.NO_POSITION) return null

                if (direction == View.FOCUS_RIGHT) {
                    val span = spanCount
                    val isLastColumn = (currentPosition + 1) % span == 0
                    val nextRowFirstPos = currentPosition + 1

                    if (isLastColumn) {
                        if (nextRowFirstPos >= itemCount) {
                            // Block focus at end of grid
                            return focused
                        }

                        // Ensure view exists (scroll if needed)
                        val nextView = findViewByPosition(nextRowFirstPos)
                        return nextView ?: run {
                            movieRecyclerView.scrollToPosition(nextRowFirstPos)
                            focused
                        }
                    }
                }
                return super.onInterceptFocusSearch(focused, direction)
            }
        }
        movieRecyclerView.addItemDecoration(EqualSpaceItemDecoration(Spacing))
        movieAdapter = GridAdapter(mutableListOf(), R.layout.item_grid2,)
        movieRecyclerView.adapter = movieAdapter
        movieAdapter.onAddMoreClicked = { loadMoreMovies() }
        movieAdapter.onItemFocused = { view, item ->
            //showPopupBeside(view, item.posterUlr, 165)
        }
        movieAdapter.onItemFocusLost = {
            hidePopup()
        }


        //  TV Shows -------------------------------------------------------------------------------
        tvRecyclerView = findViewById(R.id.TVsRecyclerView)
        tvRecyclerView.layoutManager  = object : GridLayoutManager(this@Shows_Page, GlobalUtils.calculateSpanCountV2(this, item_grid2_width, gapUsed ) ){
            override fun onInterceptFocusSearch(focused: View, direction: Int): View? {
                val currentPosition = getPosition(focused)
                if (currentPosition == RecyclerView.NO_POSITION) return null

                if (direction == View.FOCUS_RIGHT) {
                    val span = spanCount
                    val isLastColumn = (currentPosition + 1) % span == 0
                    val nextRowFirstPos = currentPosition + 1

                    if (isLastColumn) {
                        if (nextRowFirstPos >= itemCount) {
                            // Block focus at end of grid
                            return focused
                        }

                        // Ensure view exists (scroll if needed)
                        val nextView = findViewByPosition(nextRowFirstPos)
                        return nextView ?: run {
                            tvRecyclerView.scrollToPosition(nextRowFirstPos)
                            focused
                        }
                    }
                }

                return super.onInterceptFocusSearch(focused, direction)
            }

        }
        tvRecyclerView.addItemDecoration(EqualSpaceItemDecoration(Spacing))
        tvAdapter = GridAdapter(mutableListOf(), R.layout.item_grid2)
        tvRecyclerView.adapter = tvAdapter
        tvAdapter.onAddMoreClicked = { loadMoreTv() }
        tvAdapter.onItemFocused = { view, item ->
           // showPopupBeside(view, item.posterUlr, 165)
        }
        tvAdapter.onItemFocusLost = {
            hidePopup()
        }

        //  Search  --------------------------------------------------------------------------------
        searchAdapter = GridAdapter2(mutableListOf(), R.layout.item_grid)
        searchRecyclerView = findViewById(R.id.SearchResults)
        searchRecyclerView.layoutManager = GridLayoutManager(this@Shows_Page, 4)

        searchRecyclerView.adapter = searchAdapter
        searchRecyclerView.addItemDecoration(EqualSpaceItemDecoration(Spacing))
        setupSearchUi()

        // Filter  ---------------------------------------------------------------------------------
        filterAdapter = FilterAdapter(mutableListOf(), R.layout.item_filter)
        filterAdapter.onItemFocused = { view, item ->
            //showPopupBeside(view, item.posterUlr, 240)
        }
        filterAdapter.onItemFocusLost = {
            hidePopup()
        }
        fliterRecyclerView = findViewById<RecyclerView>(R.id.filterResults)
        fliterRecyclerView.layoutManager = GridLayoutManager(this@Shows_Page, GlobalUtils.calculateSpanCountV2(this, 160, gapUsed))
        fliterRecyclerView.adapter = filterAdapter
        fliterRecyclerView.addItemDecoration(EqualSpaceItemDecoration(Spacing))

        //------------------------------------------------------------------------------------------

        thrillAdapter = FilterAdapter(mutableListOf(), R.layout.item_list)
        thrillAdapter.onItemFocused = { view, item ->
            //showPopupBeside(view, item.posterUlr, 240)
        }
        thrillAdapter.onItemFocusLost = {
            hidePopup()
        }
        thrillRecyclerView = findViewById<RecyclerView>(R.id.ThrillsRecyclerView)
        //thrillRecyclerView.layoutManager = GridLayoutManager(this@Shows_Page, GlobalUtils.calculateSpanCountV2(this, 160, gapUsed))
        thrillRecyclerView.layoutManager = LinearLayoutManager(
            this@Shows_Page,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        thrillRecyclerView.adapter = thrillAdapter
        //------------------------------------------------------------------------------------------

        faveRecyclerView = findViewById(R.id.faveRecycler)
        faveRecyclerView.layoutManager = GridLayoutManager(this@Shows_Page, 1)
        faveRecyclerView.addItemDecoration(EqualSpaceItemDecoration(Spacing))

        //------------------------------------------------------------------------------------------

        notificationRecyclerView = findViewById<RecyclerView>(R.id.notificationRecycler)
        notificationRecyclerView.layoutManager = LinearLayoutManager(this)
        val clearBtn = findViewById<TextView>(R.id.clearNotBtn)

        clearBtn.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                db.clearAllTvNotifications(userId)
            }
            notificationAdapter.clearItems()
        }

        //------------------------------------------------------------------------------------------

        watchRecyclerView = findViewById(R.id.watchingRecycler)
        watchRecyclerView.layoutManager = GridLayoutManager(this@Shows_Page, GlobalUtils.calculateSpanCountV2(this@Shows_Page,160,gapUsed))
        watchRecyclerView.addItemDecoration(EqualSpaceItemDecoration(Spacing))

    }


    private fun showPopupBeside(targetView: View, imageUrl: String, popupHeightDp: Int = 0) {
        val popup = findViewById<CardView>(R.id.floatingPopup)

        val popupHeightPx = (popupHeightDp * targetView.context.resources.displayMetrics.density).toInt()
        popup.layoutParams.height = popupHeightPx
        popup.requestLayout()

        //findViewById<TextView>(R.id.popupTitle).text = item.posterUlr
        val imageC = findViewById<ImageView>(R.id.floatingPopupImg)

        Glide.with(targetView.context)
            .load(imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .override(Target.SIZE_ORIGINAL, popup.height) // scale height to container
            .into(imageC)

        val margin = 0.dp(targetView.context)

        // Get item location
        val location = IntArray(2)
        targetView.getLocationOnScreen(location)
        val x = location[0]
        val y = location[1]

        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        val popupWidth = popup.width.takeIf { it > 0 } ?: 300  // fallback estimate

        var popupX = x + targetView.width + margin   // default → right side

        //  If it goes off-screen, move popup to LEFT
        if (popupX + popupWidth > screenWidth) {
            popupX = x - popupWidth - margin
            if (popupX < 0) popupX = margin // safety
        }

        //val popupY = y + (targetView.height / 3)
        val popupY = y


        // Apply position
        popup.x = popupX.toFloat()
        popup.y = popupY.toFloat()

        popup.visibility = View.VISIBLE
        popup.alpha = 0f
        popup.animate().alpha(1f).setDuration(150).start()
    }
    private fun hidePopup() {
        val popup = findViewById<CardView>(R.id.floatingPopup)
        if (popup.visibility == View.VISIBLE) {
            popup.animate()
                .alpha(0f)
                .setDuration(120)
                .withEndAction { popup.visibility = View.GONE }
                .start()
        }
    }


    private fun Int.dp(context: Context): Int =
        (this * context.resources.displayMetrics.density).toInt()
    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun HomeData() {

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels     // in pixels
        val screenHeight = displayMetrics.heightPixels    // in pixels

        val inflater = LayoutInflater.from(this)
        val container = findViewById<FrameLayout>(R.id.spotlightShows)


        val params = container.layoutParams
        //params.height = (screenHeight * 0.70).toInt()
        val otherItemHeightPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            124f,
            container.resources.displayMetrics
        ).toInt()

        params.height = screenHeight - otherItemHeightPx
        container.layoutParams = params

        lifecycleScope.launch {

            val currentYear = Calendar.getInstance().get(Calendar.YEAR)


            val jsonObject =
                withContext(Dispatchers.IO) { fetchTMDB.fetchTrendingData(currentYear.toString()) }
            Log.e("DEBUG_Watch", jsonObject.toString())
            if (jsonObject != null) {

                val moviesArray3 = jsonObject.getJSONArray("results") ?: return@launch

                Log.e("DEBUG_MAIN_Slider raw", moviesArray3.toString())

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

                    val type = item.optString("media_type", "")
                    if (type != "movie" && type != "tv") {
                        continue   // skip this loop iteration
                    }

                    val backdrop_path =
                        if (item.has("backdrop_path") && !item.isNull("backdrop_path")) {
                            "https://image.tmdb.org/t/p/original${item.getString("backdrop_path")}"
                        } else if (item.has("poster_path") && !item.isNull("poster_path")) {
                            "https://image.tmdb.org/t/p/original${item.getString("poster_path")}"
                        } else {
                            ""
                        }

                    val pg = if (item.optString("adult") == "true") "PG-18 +" else "PG-13"
                    val id = item.optString("id", "")
                    val overview = item.optString("overview", "")

                    val release_date = if (type == "movie") {
                        item.optString("release_date", "")
                    } else {
                        item.optString("first_air_date", "")
                    }
                    val year =
                        if (release_date.length >= 4) release_date.substring(0, 4) else release_date

                    //val vote_average = item.optString("vote_average", "").substring(0, 3)
                    val voteAverageRaw = item.optString("vote_average", "")
                    val vote_average = if (voteAverageRaw.length >= 3) {
                        voteAverageRaw.substring(0, 3)
                    } else {
                        voteAverageRaw
                    }

                    val poster_path = item.optString("poster_path", "")
                    val genreIdsJson = item.getJSONArray("genre_ids") ?: JSONArray()


                    val genreNames = mutableListOf<String>()
                    if (type == "movie") {
                        for (j in 0 until genreIdsJson.length()) {
                            val genreId = genreIdsJson.getInt(j)
                            GlobalUtils.movieGenreMap[genreId]?.let { genreNames.add(it) }
                        }
                    } else {
                        for (j in 0 until genreIdsJson.length()) {
                            val genreId = genreIdsJson.getInt(j)
                            GlobalUtils.tvGenreMap[genreId]?.let { genreNames.add(it) }
                        }
                    }

                    val card = inflater.inflate(
                        R.layout.card_layout,
                        container,
                        false
                    ) as CardView

                    card.findViewById<TextView>(R.id.cardGenre).text =
                        genreNames.joinToString(" • ")



                    card.findViewById<TextView>(R.id.cardTitle).text = title
                    //card.findViewById<TextView>(R.id.cardGenre).text = genreNames.toString().trim('[', ']')
                    card.findViewById<TextView>(R.id.cardQuality).text = "HD"
                    card.findViewById<TextView>(R.id.cardPg).text = pg
                    card.findViewById<TextView>(R.id.cardType).text = type
                    card.findViewById<TextView>(R.id.cardRating).text = vote_average
                    card.findViewById<TextView>(R.id.cardYear).text = year
                    card.findViewById<TextView>(R.id.cardOverview).text = overview


                    val SliderBackdrop = card.findViewById<ImageView>(R.id.cardBackdrop)

                    Glide.with(card.context)
                        .load(backdrop_path)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerInside()
                        .thumbnail(0.25f) // Load low-res first
                        .into(SliderBackdrop)


                    card.setOnClickListener {
                        val context = card.context
                        val intent = Intent(context, Watch_Page::class.java)
                        intent.putExtra("imdb_code", id)
                        intent.putExtra("type", type)
                        context.startActivity(intent)
                    }


                    container.addView(card)

                }

                GlobalUtils.setupCardStackFromContainer(container)
                LoadingAnimation.hide(this@Shows_Page)
            } else {
                return@launch
                //LoadingAnimation.setup(this@Shows_Page, R.raw.error)
                //LoadingAnimation.show(this@Shows_Page)

            }
        }

    }





    private fun categoryShow() {
        val company_show = mapOf(
            "Marvel Studios" to Pair(420, "https://image.tmdb.org/t/p/original/hUzeosd33nzE5MCNsZxCGEKTXaQ.png"),
            "Marvel Animation" to Pair(13252, "https://image.tmdb.org/t/p/original/1gKwYyTDNhumwBKUlKqoxXRUdpC.png"),
            "DC Films" to Pair(128064, "https://image.tmdb.org/t/p/original/13F3Jf7EFAcREU0xzZqJnVnyGXu.png"),
            "Walt Disney Pictures" to Pair(2, "https://image.tmdb.org/t/p/original/wdrCwmRnLFJhEoH8GSfymY85KHT.png"),
            "Walt Disney Television" to Pair(670, "https://image.tmdb.org/t/p/original/rRGi5UkwvdOPSfr5Xf42RZUsYgd.png"),
            "Warner Bros. Pictures" to Pair(174, "https://image.tmdb.org/t/p/original/zhD3hhtKB5qyv7ZeL4uLpNxgMVU.png"),
            "Universal Pictures" to Pair(33, "https://image.tmdb.org/t/p/original/3wwjVpkZtnog6lSKzWDjvw2Yi00.png"),
            "Paramount Pictures" to Pair(4, "https://image.tmdb.org/t/p/original/gz66EfNoYPqHTYI4q9UEN4CbHRc.png"),
            "Sony Pictures Entertainment" to Pair(34, "https://image.tmdb.org/t/p/original/mtp1fvZbe4H991Ka1HOORl572VH.png"),
            "Lionsgate " to Pair(1632, "https://image.tmdb.org/t/p/original/cisLn1YAUuptXVBa0xjq7ST9cH0.png"),
            "DreamWorks Animation " to Pair(521, "https://image.tmdb.org/t/p/original/3BPX5VGBov8SDqTV7wC1L1xShAS.png"),
            "Netflix Animation " to Pair(171251, "https://image.tmdb.org/t/p/original/AqUAfMC270bGGK09Nh3mycwT1hY.png"),
            "Netflix" to Pair(178464, "https://image.tmdb.org/t/p/original/tyHnxjQJLH6h4iDQKhN5iqebWmX.png"),
            "Pixar" to Pair(3, "https://image.tmdb.org/t/p/original/1TjvGVDMYsj6JBxOAkUHpPEwLf7.png"),
            "Illumination" to Pair(6704, "https://image.tmdb.org/t/p/original/fOG2oY4m1YuYTQh4bMqqZkmgOAI.png"),
            "Blue Sky Studios" to Pair(9383, "https://image.tmdb.org/t/p/original/ppeMh4iZJQUMm1nAjRALeNhWDfU.png"),
            "Laika" to Pair(11537, "https://image.tmdb.org/t/p/original/AgCkAk8EpUG9fTmK6mWcaJA2Zwh.png"),
            "Amazon Studios" to Pair(20580, "https://image.tmdb.org/t/p/original/oRR9EXVoKP9szDkVKlze5HVJS7g.png"),
            "HBO" to Pair(3268, "https://image.tmdb.org/t/p/original/tuomPhY2UtuPTqqFnKMVHvSb724.png"),
            "Apple" to Pair(14801, "https://image.tmdb.org/t/p/original/bnlD5KJ5oSzBYbEpDkwi6w8SoBO.png")
        )

        // Convert map to list of categoryItem objects
        val categoryItems = mutableListOf<categoryItem>()
        company_show.forEach { (name, pair) ->
            categoryItems.add(
                categoryItem(
                    cCode = pair.first.toString(),
                    cImg = pair.second,
                    cName = name
                )
            )
        }

        // Setup RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.CategoryRecyclerView)
        val adapter = CategoryAdapter(categoryItems, R.layout.item_category)

        val layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

    }


    private fun dark(){
        lifecycleScope.launch {
        //ovieUrl = "https://api.themoviedb.org/3/discover/movie?"
        //tvUrl = "https://api.themoviedb.org/3/discover/tv?"
            val jsonObject = withContext(Dispatchers.IO) { fetchTMDB.fetchDiscoverMovie ("&with_genres=27") }
            if (jsonObject == null) return@launch
            val mvData = jsonObject.getJSONArray("results")

            for (i in 0 until mvData.length()) {


                val current = mvData.getJSONObject(i)

                val backdrop_path = current.optString("backdrop_path", "")
                val poster_path = current.optString("poster_path", "")



                val vote_average = current.optString("vote_average", "")
                val title = current.optString("title", "")
                //val overview = current.optString("overview", "")
                val id = current.optString("id", "")
                val original_title = current.optString("original_title", "")
                //val original_name = current.optString("original_name", "")

                val type: String
                var date: String
                var showD: String = ""

                val imgPost =  "https://image.tmdb.org/t/p/original$poster_path"
                val imgback  = "https://image.tmdb.org/t/p/original$backdrop_path"

                if (original_title == "") {
                    type = "tv";
                    date = if (current.getString("first_air_date").length >= 4) {
                        current.getString("first_air_date").substring(0, 4)
                    } else{
                        current.getString("first_air_date")}
                } else {
                    type = "movie"
                    date = if (current.getString("release_date").length >= 4) {
                        current.getString("release_date").substring(0, 4)
                    } else{
                        current.getString("release_date")}
                }

                val Item = filterItemOne(
                    title = title,
                    backdropUrl = imgPost,
                    posterUlr = imgback,
                    imdbCode = id,
                    type = type,
                    year = date,
                    rating = vote_average,
                    runtime = showD
                )

                withContext(Dispatchers.Main) {
                    thrillAdapter.addItem(Item)
                    //isLoadingFliter = false
                    thrillAdapter.isLoadingMore = false

                }
            }




        }


    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////


    private fun fetchMovies() {
        if (isLoadingMoreMovies) return
        isLoadingMoreMovies = true
        movieAdapter.isLoadingMore = true

        CoroutineScope(Dispatchers.IO).launch {
            repeat(3) { attempt ->
                try {
                    val urlM = "https://api.themoviedb.org/3/discover/movie?include_video=true&language=en-US&page=$currentMoviePage&sort_by=popularity.desc"


                    val connection2 = URL(urlM).openConnection() as HttpURLConnection
                    connection2.requestMethod = "GET"
                    connection2.setRequestProperty("accept", "application/json")
                    connection2.setRequestProperty(
                    "Authorization",
                    "Bearer ${BuildConfig.TM_K}"
                    )

                    val response2 = connection2.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject2 = org.json.JSONObject(response2)
                    val dataFetched = jsonObject2.getJSONArray("results")



                    val movies = mutableListOf<MovieItemOne>()

                    for (i in 0 until dataFetched.length()) {
                        val detailsJson = dataFetched.getJSONObject(i)

                        val id = detailsJson.optString("id", "Unknown")
                        val title = detailsJson.optString("title", "Unknown")
                        val year = detailsJson.optString("release_date", "")
                        val runtime = detailsJson.optString("runtime", "0")
                        val rating = detailsJson.optDouble("vote_average", 0.0)
                        //val imgUrl = "https://image.tmdb.org/t/p/w780" + detailsJson.optString("poster_path", "")
                        val imgUrl =
                            if (detailsJson.has("backdrop_path") && !detailsJson.isNull("backdrop_path")) {
                                "https://image.tmdb.org/t/p/original${detailsJson.getString("backdrop_path")}"
                            } else if (detailsJson.has("poster_path") && !detailsJson.isNull("poster_path")) {
                                "https://image.tmdb.org/t/p/original${detailsJson.getString("poster_path")}"
                            } else {
                                ""
                            }
                        val imgUrl2 =
                            if (detailsJson.has("poster_path") && !detailsJson.isNull("poster_path")) {
                                "https://image.tmdb.org/t/p/original${detailsJson.getString("poster_path")}"
                            } else if (detailsJson.has("backdrop_path") && !detailsJson.isNull("backdrop_path")) {
                                "https://image.tmdb.org/t/p/original${detailsJson.getString("backdrop_path")}"
                            } else {
                                ""
                            }

                        movies.add(
                            MovieItemOne(
                                title = title,
                                backdropUrl = imgUrl,
                                posterUlr = imgUrl2,
                                imdbCode = id,
                                type = "movie",
                                year = year,
                                rating = "${String.format("%.1f", rating)}imdb",
                                runtime = "⏱$runtime min"
                            )
                        )
                    }

                    // ✅ Update UI once per batch
                    withContext(Dispatchers.Main) {
                        movieAdapter.addItems(movies)
                        isLoadingMoreMovies = false
                        movieAdapter.isLoadingMore = false
                        LoadingAnimation.hide(this@Shows_Page)
                    }

                    return@launch // success → stop repeating
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) {
                        Log.e("DEBUG_SHOWS PAGE", "Network error ", e)
                        LoadingAnimation.setup(this@Shows_Page, R.raw.error)
                        LoadingAnimation.show(this@Shows_Page)
                    }
                    delay(30_000)
                } catch (e: Exception) {
                    Log.e("DEBUG_MOVIES_ERROR", "Attempt ${attempt + 1} failed: ${e.message}", e)
                    delay(5000)
                }
            }

            withContext(Dispatchers.Main) {
                isLoadingMoreMovies = false
                movieAdapter.isLoadingMore = false
            }
        }
    }

    private fun loadMoreMovies() {
        if (isLoadingMoreMovies) return // Prevent multiple rapid clicks
        currentMoviePage++
        fetchMovies()
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private fun fetchTvShows() {
        isLoadingMoreTv = true
        tvAdapter.isLoadingMore = true
        CoroutineScope(Dispatchers.IO).launch {

            repeat(5) { attempt ->
                try {
                    val urlM = "https://api.themoviedb.org/3/discover/tv?include_video=true&language=en-US&page=$currentTvPage&sort_by=popularity.desc"

                    val connection2 = URL(urlM).openConnection() as HttpURLConnection
                    connection2.requestMethod = "GET"
                    connection2.setRequestProperty("accept", "application/json")
                    connection2.setRequestProperty(
                        "Authorization",
                        "Bearer ${BuildConfig.TM_K}"
                    )

                    val response2 = connection2.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject2 = org.json.JSONObject(response2)
                    val dataFetched = jsonObject2.getJSONArray("results")

                    val movies = mutableListOf<MovieItemOne>()

                    for (i in 0 until dataFetched.length()) {
                        val jsonObject = dataFetched.getJSONObject(i)


                        val title = jsonObject.getString("name")
                        val numberOfSeasons = try {
                            jsonObject.getJSONObject("last_episode_to_air")
                                .getString("season_number")
                        } catch (e: Exception) {
                            ""
                        }
                        val episodeNumber = try {
                            jsonObject.getJSONObject("last_episode_to_air")
                                .getString("episode_number")
                        } catch (e: Exception) {
                            ""
                        }
                        val showD = "SS$numberOfSeasons EPS$episodeNumber"
                        val firstAirDate = if (jsonObject.getString("first_air_date").length >= 4) {
                            jsonObject.getString("first_air_date").substring(0, 4)
                        } else {
                            jsonObject.getString("first_air_date")
                        }

                        val voteAverage = "☆" + jsonObject.getString("vote_average").substring(0, 3)

                        //val imgUrl = "https://image.tmdb.org/t/p/w500" + jsonObject.getString("poster_path")
                        val imgUrl =
                            if (jsonObject.has("backdrop_path") && !jsonObject.isNull("backdrop_path")) {
                                "https://image.tmdb.org/t/p/original${jsonObject.getString("backdrop_path")}"
                            } else if (jsonObject.has("poster_path") && !jsonObject.isNull("poster_path")) {
                                "https://image.tmdb.org/t/p/original${jsonObject.getString("poster_path")}"
                            } else {
                                ""
                            }

                        val imgUrl2 =
                            if (jsonObject.has("poster_path") && !jsonObject.isNull("poster_path")) {
                                "https://image.tmdb.org/t/p/original${jsonObject.getString("poster_path")}"
                            } else if (jsonObject.has("backdrop_path") && !jsonObject.isNull("backdrop_path")) {
                                "https://image.tmdb.org/t/p/original${jsonObject.getString("backdrop_path")}"

                            } else {
                                ""
                            }

                        val id = jsonObject.getString("id")
                        val type = "tv"
                        //movies.add(MovieItemOne(title=title, backdropUrl = imgUrl, posterUlr = imgUrl2, imdbCode=id, type=type, year="", rating="", runtime=""))

                        val MovieItemOne = MovieItemOne(
                            title = title,
                            backdropUrl = imgUrl,
                            posterUlr = imgUrl2,
                            imdbCode = id,
                            type = type,
                            year = firstAirDate,
                            rating = voteAverage,
                            runtime = showD
                        )

                        withContext(Dispatchers.Main) {
                            tvAdapter.addItem(MovieItemOne)
                            isLoadingMoreTv = false
                            tvAdapter.isLoadingMore = false
                            LoadingAnimation.hide(this@Shows_Page)
                        }


                    }
                    Log.e("DEBUG_TAG_TvShows 4", movies.toString())

                    return@launch
                } catch (e: IOException) {
                        withContext(Dispatchers.Main) {
                            Log.e("DEBUG_SHOWS PAGE", "Network error ", e)
                            LoadingAnimation.setup(this@Shows_Page, R.raw.error)
                            LoadingAnimation.show(this@Shows_Page)
                        }
                        delay(30_000)

                } catch (e: Exception) {
                    Log.e("DEBUG_TAG_TvShows", "Attempt ${attempt+1} failed", e)
                    delay(10_000)
                    currentTvPage--
                }
                withContext(Dispatchers.Main) {
                    isLoadingMoreTv = false
                    tvAdapter.isLoadingMore = false
                }
            }
        }
    }

    private fun loadMoreTv() {

        if (isLoadingMoreTv) return // Prevent multiple rapid clicks
        currentTvPage++
        fetchTvShows()
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun setupSearchUi() {

        val searchInput = findViewById<EditText>(R.id.HomeSearchInput)
        val keyboardLayout = findViewById<LinearLayout>(R.id.keyboard_layout)
        val keyboardManager = CustomKeyboardManager(
            this,
            searchInput,
            keyboardLayout,
            object : OnSearchListener {
                override fun EnterActionTrigger(query: String) {
                    val searchTerm = query.trim()
                    if (searchTerm.isNotEmpty()) {
                        performSearch(searchTerm)
                    }
                }
            }
        )
        keyboardManager.showKeyboard()
        //keyboardManager.hideKeyboard()
        keyboardManager.isKeyboardVisible()

    }

    private fun performSearch(searchTerm:String) {
        CoroutineScope(Dispatchers.IO).launch {
            repeat(3) { attempt ->
                try {

                    Log.e("SEARCH RESULTS", searchTerm)

                    // --- Background work (network request) ---
                    val url =
                        "https://api.themoviedb.org/3/search/multi?include_adult=false&query=$searchTerm"
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("accept", "application/json")
                    connection.setRequestProperty(
                        "Authorization",
                        "Bearer ${BuildConfig.TM_K}"
                    )

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)

                    Log.e("SEARCH RESULTS", jsonObject.toString())
                    val moviesArray = jsonObject.getJSONArray("results")

                    //Log.e("SEARCH RESULTS", moviesArray.toString())


                    withContext(Dispatchers.Main)  {
                        val find = findViewById<TextView>(R.id.searchResultsDisplay)
                        find.text = "Search Results for: $searchTerm (${moviesArray.length()})"
                        searchAdapter.clearItems()
                    }

                    for (i in 0 until moviesArray.length()) {
                        val current = moviesArray.getJSONObject(i)
                        current.remove("overview")
                        current.remove("genre_ids")
                        current.remove("popularity")
                        current.remove("video")

                        val mediaType = current.optString("media_type", "movie")

                        var title = "Unknown"
                        var info = ""
                        var date = ""
                        var voteAverage = ""
                        var imgUrl = ""
                        var poster = ""

                        Log.e("SEARCH RESULTS $i", current.toString())
                        when (mediaType) {
                            "person" -> {
                                title = current.optString(
                                    "name",
                                    current.optString("original_name", "Unknown")
                                )
                                imgUrl = "https://image.tmdb.org/t/p/w500" + current.optString(
                                    "profile_path",
                                    ""
                                )
                                poster = current.optString("profile_path", "null")
                                info = current.optString("known_for_department", "")
                                voteAverage = ""
                            }

                            "tv" -> {
                                title = current.optString(
                                    "name",
                                    current.optString("original_name", "Unknown")
                                )
                                date =
                                    current.optString("first_air_date")
                                        .takeIf { it.isNotEmpty() }
                                        ?.substring(0, 4) ?: ""
                                imgUrl = "https://image.tmdb.org/t/p/w500" + current.optString(
                                    "poster_path",
                                    ""
                                )
                                poster = current.optString("poster_path", "null")
                                info = ""
                                voteAverage =
                                    current.optDouble("vote_average", 0.0).toInt()
                                        .toString() + " ★"
                            }

                            "movie" -> {
                                title = current.optString("original_title", "Unknown")
                                date =
                                    current.optString("release_date").takeIf { it.isNotEmpty() }
                                        ?.substring(0, 4) ?: ""
                                imgUrl = "https://image.tmdb.org/t/p/w500" + current.optString(
                                    "poster_path",
                                    ""
                                )
                                poster = current.optString("poster_path", "null")
                                info = "" // TODO: runtime if available
                                voteAverage = current.optDouble("vote_average", 0.0).toInt()
                                    .toString() + " ★"
                            }
                        }

                        if (poster.isBlank() || poster.endsWith("null")) continue

                        val id = current.getString("id")


                        //movies.add(MovieItem(title, imgUrl, id, type))

                        val movieItem = MovieItem(
                            title = title,
                            imageUrl = imgUrl,
                            imdbCode = id,
                            type = mediaType,
                            year = date,
                            rating = voteAverage,
                            runtime = info
                        )

                        withContext(Dispatchers.Main) {
                            searchAdapter.addItem(movieItem)
                        }




                    }


                    return@launch
                } catch (e: Exception) {
                    Log.e("SEARCH RESULTS ERROR", "S ERROR", e)
                    delay(10_000)
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun filter(){

        //filter options buttons
        val filterTypeBtn = findViewById<TextView>(R.id.FilterTypeBtn)
        val filterCountryBtn = findViewById<TextView>(R.id.FilterCountryBtn)
        val filterSortBtn = findViewById<TextView>(R.id.FilterSortBtn)
        val filterGenreBtn = findViewById<TextView>(R.id.FilterGenreBtn)
        val filterYearBtn = findViewById<TextView>(R.id.FilterYearBtn)


        // Filter data
        val typeOptions = listOf("Movie", "TV", "All")
        val countryOptions = listOf(
            FilterChoice("AR", "Argentina"),
            FilterChoice("AT", "Austria"),
            FilterChoice("BR", "Brazil"),
            FilterChoice("FI", "Finland"),
            FilterChoice("AR", "Argentina"),
            FilterChoice("AR", "Argentina")
        )

        val sortOptions = listOf(
            FilterChoice("original_title.asc", "Original Title ↑ Ascending"),
            FilterChoice("original_title.desc", "Original Title ↓ Descending"),
            FilterChoice("popularity.asc", "Popularity ↑ Ascending"),
            FilterChoice("popularity.desc", "Popularity ↓ Descending"),
            FilterChoice("revenue.asc", "Revenue ↑ Ascending"),
            FilterChoice("revenue.desc", "Revenue ↓ Descending")
        )

        val genreOptions = listOf(
            FilterChoice("28", "Action"),
            FilterChoice("12", "Adventure"),
            FilterChoice("12", "Series Action & Adventure"),
            FilterChoice("16", "Animation"),
            FilterChoice("35", "Comedy"),
            FilterChoice("80", "Crime"),
            FilterChoice("99", "Documentary"),
            FilterChoice("18", "Drama"),
            FilterChoice("10751", "Family"),
            FilterChoice("9648", "Mystery"),
            FilterChoice("10749", "Romance"),
            FilterChoice("878", "Sci-Fi"),
            FilterChoice("14", "Fantasy"),
            FilterChoice("10402", "Music"),
            FilterChoice("10770", "TV Movie"),
            FilterChoice("53", "Thriller"),
            FilterChoice("37", "Western"),
            FilterChoice("10752", "War"),
            FilterChoice("27", "Horror"),
            FilterChoice("10764", "Series Reality"),
            FilterChoice("10766", "Series Soap"),
            FilterChoice("10767", "Series Talk"),
            FilterChoice("10762", "Series Kids"),
        )

        val yearOptions = listOf(
            FilterChoice("2025", "2026"),
            FilterChoice("2025", "2025"),
            FilterChoice("2024", "2024"),
            FilterChoice("2023", "2023"),
            FilterChoice("2022", "2022"),
            FilterChoice("2021", "2021")
        )

        // Filter state
        var selectedType: String? = null
        val selectedGenres = mutableSetOf<String>()
        val selectedCountries = mutableSetOf<String>()
        val selectedsortOptions = mutableSetOf<String>()
        val selectedyearOptions   = mutableSetOf<String>()

        val filterDisplay = findViewById<TextView>(R.id.FilterDisplay)
        var movieUrl: String
        var tvUrl: String
        var filterPage = 1
        var isLoadingFliter = false

        fun updateFilterDisplay() {
            isLoadingFliter = true
            filterAdapter.isLoadingMore = true
            //filterAdapter.clearItems()

            // Reset attributes
            movieUrl = "https://api.themoviedb.org/3/discover/movie?"
            tvUrl = "https://api.themoviedb.org/3/discover/tv?"


            /*
            val parts = mutableListOf<String>()
            //selectedType?.let { parts.add("Type: ${it.label}") }
            if (selectedGenres.isNotEmpty()) parts.add("Genres: ${selectedGenres.joinToString()}")
            if (selectedCountries.isNotEmpty()) parts.add("Country: ${selectedCountries.joinToString()}")
            if (selectedsortOptions.isNotEmpty()) parts.add("Sort: ${selectedsortOptions.joinToString()}")
            if (selectedyearOptions.isNotEmpty()) parts.add("Year: ${selectedyearOptions.joinToString()}")

            filterDisplay.text = if (parts.isEmpty()) "Filter Results ($default)" else parts.joinToString(" | ")

             */


            if (selectedGenres.isNotEmpty()) {
                val genres = selectedGenres.joinToString(",")
                movieUrl += "&with_genres=$genres"
                tvUrl += "&with_genres=$genres"
            }

            if (selectedyearOptions.isNotEmpty()) {
                val year = selectedyearOptions.first()
                movieUrl += "&year=$year"
                tvUrl += "&year=$year"
            }

            if (selectedCountries.isNotEmpty()) {
                val country = selectedCountries.first()
                movieUrl += "&with_origin_country=$country"
                tvUrl += "&with_origin_country=$country"
            }

            if (selectedsortOptions.isNotEmpty()) {
                val sort = selectedsortOptions.first()
                movieUrl += "&sort_by=$sort"
                tvUrl += "&sort_by=$sort"
            }

            if(selectedType == "Movie"){
                tvUrl = ""
            } else if(selectedType == "TV"){
                movieUrl = ""
            } else {}

            CoroutineScope(Dispatchers.IO).launch {

                val urlM = "$movieUrl&page=$filterPage"
                val urlT = "$tvUrl&page=$filterPage"

                val movieList = mutableListOf<JSONObject>()
                val tvList = mutableListOf<JSONObject>()

                Log.e("Filter Results urlM", urlM.toString())
                Log.e("Filter Results urlT", urlT.toString())

                // ------------------ MOVIES ------------------
                try {
                    val connection2 = URL(urlM).openConnection() as HttpURLConnection
                    connection2.requestMethod = "GET"
                    connection2.setRequestProperty("accept", "application/json")
                    connection2.setRequestProperty(
                        "Authorization",
                        "Bearer ${BuildConfig.TM_K}"
                    )

                    val response2 = connection2.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject2 = org.json.JSONObject(response2)
                    val mvData = jsonObject2.getJSONArray("results")

                    for (i in 0 until mvData.length()) {
                        movieList.add(mvData.getJSONObject(i))
                    }

                    Log.e("Filter Results mvData", mvData.toString())
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // ------------------ TV SHOWS ------------------
                try {
                    val connection2 = URL(urlT).openConnection() as HttpURLConnection
                    connection2.requestMethod = "GET"
                    connection2.setRequestProperty("accept", "application/json")
                    connection2.setRequestProperty(
                        "Authorization",
                        "Bearer ${BuildConfig.TM_K}"
                    )

                    val response2 = connection2.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject2 = org.json.JSONObject(response2)
                    val tvData = jsonObject2.getJSONArray("results")

                    for (i in 0 until tvData.length()) {
                        tvList.add(tvData.getJSONObject(i))
                    }

                    Log.e("Filter Results tvData", tvData.toString())
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Merge movie + tv
                val combined = (movieList + tvList).shuffled()
                for (i in 0 until combined.size) {
                    val current = combined[i]

                    val backdrop_path = current.optString("backdrop_path", "")
                    val poster_path = current.optString("poster_path", "")



                    val vote_average = current.optString("vote_average", "")
                    val title = current.optString("title", "")
                    //val overview = current.optString("overview", "")
                    val id = current.optString("id", "")
                    val original_title = current.optString("original_title", "")
                    //val original_name = current.optString("original_name", "")

                    val type: String
                    var date: String
                    var showD: String = ""

                    val imgPost =  "https://image.tmdb.org/t/p/original$poster_path"
                    val imgback  = "https://image.tmdb.org/t/p/original$backdrop_path"

                    if (original_title == "") {
                        type = "tv";
                        date = if (current.getString("first_air_date").length >= 4) {
                            current.getString("first_air_date").substring(0, 4)
                        } else{
                            current.getString("first_air_date")}
                    } else {
                        type = "movie"
                        date = if (current.getString("release_date").length >= 4) {
                            current.getString("release_date").substring(0, 4)
                        } else{
                            current.getString("release_date")}
                    }

                    val Item = filterItemOne(
                        title = title,
                        backdropUrl = imgPost,
                        posterUlr = imgback,
                        imdbCode = id,
                        type = type,
                        year = date,
                        rating = vote_average,
                        runtime = showD
                    )

                    withContext(Dispatchers.Main) {
                        filterAdapter.addItem(Item)
                        isLoadingFliter = false
                        filterAdapter.isLoadingMore = false


                    }
                }
            }
        }
        fun loadMoreFilter() {
            if (isLoadingFliter) return // Prevent multiple rapid clicks
            filterPage++
            updateFilterDisplay()
        }

        filterAdapter.onAddMoreClicked = { loadMoreFilter() }
        updateFilterDisplay() //Default

        filterTypeBtn.setOnClickListener {
            showSingleChoiceDialog(
                title = "Select Type",
                options = typeOptions,
                currentSelection = typeOptions.indexOf(selectedType ?: "")
            ) { selected ->
                selectedType = selected   // <— this is just a STRING
                filterPage = 1
                filterAdapter.clearItems()
                updateFilterDisplay()
            }
        }

        filterGenreBtn.setOnClickListener {
            showMultiChoiceDialog(
                title = "Select Genres",
                options = genreOptions,
                selectedKeys = selectedGenres
            ) { selected ->
                filterPage = 1
                filterAdapter.clearItems()
                updateFilterDisplay()
            }
        }

        filterCountryBtn.setOnClickListener {
            showMultiChoiceDialog(
                title = "Select Genres",
                options = countryOptions,
                selectedKeys = selectedCountries
            ) { selected ->
                filterPage = 1
                filterAdapter.clearItems()
                updateFilterDisplay()
            }
        }

        filterSortBtn.setOnClickListener {
            showMultiChoiceDialog(
                title = "Select Genres",
                options = sortOptions,
                selectedKeys = selectedsortOptions
            ) { selected ->
                filterPage = 1
                filterAdapter.clearItems()
                updateFilterDisplay()
            }
        }

        filterYearBtn.setOnClickListener {
            showMultiChoiceDialog(
                title = "Select Genres",
                options = yearOptions,
                selectedKeys = selectedyearOptions
            ) { selected ->
                filterPage = 1
                filterAdapter.clearItems()
                updateFilterDisplay()
            }
        }

    }
    data  class FilterChoice(
        val value: String,
        val label: String
    )

    private fun showSingleChoiceDialog(
        title: String,
        options: List<String>,
        currentSelection: Int,
        onDone: (String) -> Unit
    ) {
        val builder = AlertDialog.Builder(this, R.style.CustomDialogTheme)
        builder.setTitle(title)

        builder.setSingleChoiceItems(options.toTypedArray(), currentSelection) { dialog, which ->
            onDone(options[which])
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun showMultiChoiceDialog(
        title: String,
        options: List<FilterChoice>,
        selectedKeys: MutableSet<String>,
        onDone: (Set<String>) -> Unit
    ) {
        val labels = options.map { it.label }.toTypedArray()
        val checkedItems = options.map { selectedKeys.contains(it.value) }.toBooleanArray()

        val builder = AlertDialog.Builder(this, R.style.CustomDialogTheme)
        builder.setTitle(title)

        builder.setMultiChoiceItems(labels, checkedItems) { _, index, isChecked ->
            // Ensure you reference the options list inside the lambda
            val selectedKey = options[index].value
            if (isChecked) selectedKeys.add(selectedKey)
            else selectedKeys.remove(selectedKey)
        }

        builder.setPositiveButton("Apply") { _, _ ->
            onDone(selectedKeys)
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun tvFavoritesList(){
        lifecycleScope.launch(Dispatchers.Main) {

                val FavBackdrop: ImageView = findViewById(R.id.FavBackdrop)
                val FavTitle: TextView = findViewById(R.id.FavTitle)
                val FavGenre: TextView = findViewById(R.id.FavGenre)
                val FavType: TextView = findViewById(R.id.FavType)
                val FavRating: TextView = findViewById(R.id.FavRating)
                val FavYear: TextView = findViewById(R.id.FavYear)
                val FavOverview: TextView = findViewById(R.id.FavOverview)
                val RemoveFaveItem: LinearLayout = findViewById(R.id.RemoveFaveItem)
                val emptyState: TextView = findViewById(R.id.favEmptyState)


                val showFavData =  withContext(Dispatchers.IO) { db.getFavoriteShows(userId)}


                emptyState.visibility = if (showFavData.isEmpty()) View.VISIBLE else View.GONE
                if (showFavData.isEmpty()) return@launch

                //val items = mutableListOf<FavItem>()

                val items = showFavData.map { anime ->

                //for (anime in showFavData) {
                    Log.d("Fav_anime", "show_id: ${anime["show_id"]}")
                    Log.d("Fav_anime", "title: ${anime["title"]}")
                    Log.d("Fav_anime", "poster: ${anime["poster"]}")
                    Log.d("Fav_anime", "type: ${anime["type"]}")
                    Log.d("Fav_anime", "noOfSeason: ${anime["noOfSeason"]}")
                    Log.d("Fav_anime", "lastSeason: ${anime["lastSeason"]}")
                    Log.d("Fav_anime", "lastEpisode: ${anime["lastEpisode"]}")


                        FavItem(
                            title = anime["title"] ?: "",
                            posterUrl = anime["poster"] ?: "",
                            backdropUrl = anime["backdrop"] ?: "",
                            releaseDate = anime["year"] ?: "",
                            runtime = anime["runtime"] ?: "",
                            overview = anime["overview"] ?: "",
                            voteAverage = anime["rating"] ?: "",
                            genres = anime["genres"] ?: "",
                            production = "",
                            parentalGuide = anime["pg"] ?: "",
                            imdbCode = anime["show_id"] ?: "",
                            showType = anime["type"] ?: ""
                        )

                }

                if (!::faveAdapter.isInitialized) {

                        faveAdapter = FavAdapter(
                            items.toMutableList(),
                            R.layout.square_card,
                            FavBackdrop,
                            FavTitle,
                            FavGenre,
                            FavType,
                            FavRating,
                            FavYear,
                            FavOverview,
                            RemoveFaveItem
                        )

                        faveRecyclerView.adapter = faveAdapter
                }else{
                     faveAdapter.updateItems(items)
                }

        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun o_notificationS() {
        lifecycleScope.launch(Dispatchers.Main) {

            val dnot = withContext(Dispatchers.IO) { db.getAllTvNotifications(userId) }
            val notifications = mutableListOf<NotificationItem>()

            findViewById<TextView>(R.id.notificationHeadline).text = "notifications"
            if (dnot.size > 0) findViewById<CardView>(R.id.cNotificationAnimeIcon).visibility = View.VISIBLE

            for (item in dnot) {
                Log.d("Not_tv", "notificationId: ${item["id"]}")
                Log.d("Not_tv", "anime_id: ${item["anime_id"]}")
                Log.d("Not_tv", "title: ${item["title"]}")
                Log.d("Not_tv", "poster: ${item["poster"]}")
                Log.d("Not_tv", "noOfSeason: ${item["noOfSeason"]}")
                Log.d("Not_tv", "lastSeason: ${item["lastSeason"]}")
                Log.d("Not_tv", "lastEpisode: ${item["lastEpisode"]}")
                Log.d("Not_tv", "notify_at: ${item["notify_at"]}\n\n\n")


                val itemData = NotificationItem(
                    notificationId = item["id"].toString(),      // ✅ PASS ID
                    imdbCode = item["tv_id"].toString(),
                    title = item["title"].toString(),
                    imageUrl = item["poster"],
                    info = "Season ${item["lastSeason"]} - Episode ${item["lastEpisode"]}",
                    type = "tv",
                    newSeason = item["lastSeason"].toString(),
                    newEpisode = item["lastEpisode"].toString(),
                    time = item["notify_at"].toString()
                )
                notifications.add(itemData)
            }


            notificationAdapter = NotificationAdapter(
                items = notifications.toMutableList(),
                layoutResId = R.layout.item_notification,
                db,
                userId
            )
            notificationRecyclerView.adapter = notificationAdapter

        }
    }
    private fun notificationS() {
        lifecycleScope.launch {
            // Fetch from DB on IO thread
            val dbNotifications = withContext(Dispatchers.IO) {
                db.getAllTvNotifications(userId)
            }

            if (dbNotifications.size > 0) findViewById<CardView>(R.id.cNotificationAnimeIcon).visibility = View.VISIBLE

            // Update UI on Main thread
            val notificationItems = dbNotifications.map { item ->

                Log.d(
                    "Not_tv",
                    """
                notificationId: ${item["id"]}
                anime_id: ${item["anime_id"]}
                title: ${item["title"]}
                poster: ${item["poster"]}
                noOfSeason: ${item["noOfSeason"]}
                lastSeason: ${item["lastSeason"]}
                lastEpisode: ${item["lastEpisode"]}
                notify_at: ${item["notify_at"]}
                """.trimIndent()
                )

                NotificationItem(
                    notificationId = item["id"]?.toString().orEmpty(),
                    imdbCode = item["tv_id"]?.toString().orEmpty(),
                    title = item["title"]?.toString().orEmpty(),
                    imageUrl = item["poster"],
                    info = "Season ${item["lastSeason"]} - Episode ${item["lastEpisode"]}",
                    type = "tv",
                    newSeason = item["lastSeason"]?.toString().orEmpty(),
                    newEpisode = item["lastEpisode"]?.toString().orEmpty(),
                    time = item["notify_at"]?.toString().orEmpty()
                )
            }

            // UI updates
            findViewById<TextView>(R.id.notificationHeadline).text =
                "notifications (${notificationItems.size})"

            if (!::notificationAdapter.isInitialized) {
                notificationAdapter = NotificationAdapter(
                    items = notificationItems.toMutableList(),
                    layoutResId = R.layout.item_notification,
                    db,
                    userId
                )
                notificationRecyclerView.adapter = notificationAdapter
            } else {
                notificationAdapter.updateItems(notificationItems)
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////


    private fun o_watchedList() {
        lifecycleScope.launch(Dispatchers.Main) {
                val userId = sm.getUserId()
                val cWatchingMv = withContext(Dispatchers.IO) { db.getContinueWatchingAll(userId, "movie")}
                val cWatchingTv = withContext(Dispatchers.IO) { db.getContinueWatchingAll(userId, "tv")}

                // Combine and sort in one operation
                val combinedList = ArrayList<HashMap<String, String>>().apply {
                    addAll(cWatchingMv)
                    addAll(cWatchingTv)
                    // Sort by updated_at in descending order
                    sortByDescending { it["updated_at"]?.toLongOrNull() ?: 0L }
                }

                watchAdapter = cWatchingAdapter(
                    combinedList,
                    R.layout.item_watched
                )
                watchRecyclerView.adapter = watchAdapter
        }
    }

    private fun watchedList() {
        lifecycleScope.launch {
            val userId = sm.getUserId()

            // Fetch both lists in parallel on IO
            val (movies, tvShows) = withContext(Dispatchers.IO) {
                coroutineScope {
                    val mv = async { db.getContinueWatchingAll(userId, "movie") }
                    val tv = async { db.getContinueWatchingAll(userId, "tv") }
                    mv.await() to tv.await()
                }
            }

            // Combine + sort
            val combinedList = (movies + tvShows)
                .sortedByDescending { it["updated_at"]?.toLongOrNull() ?: 0L }

            // Adapter setup / update
            if (!::watchAdapter.isInitialized) {
                watchAdapter = cWatchingAdapter(
                    combinedList.toMutableList(),
                    R.layout.item_watched
                )
                watchRecyclerView.adapter = watchAdapter
            } else {
                watchAdapter.updateItems(combinedList)
            }
        }
    }



    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {

            }
        })
    }

}

package com.example.onyx

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Rect
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
import android.widget.ScrollView
import android.widget.TextView
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.doOnLayout
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
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import org.json.JSONArray
import java.io.IOException
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt

import androidx.fragment.app.Fragment
import com.example.onyx.OnyxClasses.AnimeGridItem
import com.example.onyx.OnyxClasses.FocusOverlay


import android.graphics.drawable.Drawable
import android.view.animation.LinearInterpolator
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener

class ShowsFragment : Fragment(R.layout.fragment_shows) {
    private var currentMoviePage = 1
    private var isLoadingMoreMovies = false

    private var lastFocusedView: View? = null
    private var currentTvPage = 1
    private var isLoadingMoreTv = false
    private var updateContentJob: kotlinx.coroutines.Job? = null


    private lateinit var backgroundContainer: View
    private lateinit var SpotlightSection: FrameLayout
    private lateinit var HomeContentSection: FrameLayout
    private lateinit var currentContent: CardView




    private lateinit var tvBtnText: TextView
    private lateinit var filterContainerImg: ImageView
    private lateinit var fliterContainer: LinearLayout


    //Adapters
    private lateinit var movieAdapter: GridAdapter
    private lateinit var tvAdapter: GridAdapter
    private lateinit var filterAdapter: FilterAdapter


    private lateinit var thrillAdapter: FilterAdapter

    private lateinit var realityAdapter: FilterAdapter
    private lateinit var genreAdapter: FilterAdapter





    //RecyclerViews
    private lateinit var tvRecyclerView : RecyclerView
    private lateinit var tvFixedFocusOverlay: MaterialCardView

    private lateinit var movieRecyclerView : RecyclerView
    private lateinit var movieFixedFocusOverlay: MaterialCardView
    private lateinit var fliterRecyclerView : RecyclerView





    private lateinit var  realityRecyclerView : RecyclerView
    //private lateinit var realityFixedFocusOverlay: View
    private lateinit var  thrillRecyclerView : RecyclerView
    private lateinit var  genreRecyclerView : RecyclerView





    private lateinit var db: AppDatabase
    private lateinit var  sm: SessionManger

    private lateinit var fetchTMDB: TMDBapi
    private var userId: Int = -1


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        GlobalUtils.applyTheme(requireActivity())
        super.onViewCreated(view, savedInstanceState)

        view.viewTreeObserver.addOnGlobalFocusChangeListener { _, newFocus ->
            if (newFocus != null && view.findViewById<View>(newFocus.id) != null) {
                lastFocusedView = newFocus
            }
        }

        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        LoadingAnimation.setup(requireContext(), view, R.raw.line_loading)
        LoadingAnimation.show(view)

        fetchTMDB = TMDBapi(requireActivity())
        db = AppDatabase(requireActivity())
        sm = SessionManger(requireActivity())
        userId = sm.getUserId()


        //setupBackPressedCallback()
        //trackFocus()

        val activityScrollVIEW = requireView().findViewById<ScrollView>(R.id.activityScrollVIEW)


        backgroundContainer = requireView().findViewById(R.id.BackgroundContainer)
        currentContent = requireView().findViewById(R.id.currentContent)

        SpotlightSection = requireView().findViewById(R.id.SpotlightSection)
        HomeContentSection = requireView().findViewById(R.id.HomeContentSection)
        val movieSection = requireView().findViewById<LinearLayout>(R.id.MovieSection)
        val filterSection = requireView().findViewById<LinearLayout>(R.id.filterSection)
        val tvSection = requireView().findViewById<LinearLayout>(R.id.tvSection)

        SpotlightSection.visibility = View.GONE
        HomeContentSection.visibility = View.GONE
        movieSection.visibility = View.GONE
        filterSection.visibility = View.GONE
        tvSection.visibility = View.GONE
        tvSection.visibility = View.GONE


        GlobalUtils.setHeightToMatchScreen(SpotlightSection)
        GlobalUtils.setHeightToMatchScreen(HomeContentSection)



        GlobalUtils.snapRowToTopOnFocus(activityScrollVIEW, SpotlightSection)
        GlobalUtils.centerParentOnFocus(activityScrollVIEW, HomeContentSection)
        GlobalUtils.centerParentOnFocus(activityScrollVIEW, movieSection)
        GlobalUtils.centerParentOnFocus(activityScrollVIEW, filterSection)
        GlobalUtils.centerParentOnFocus(activityScrollVIEW, tvSection)
        GlobalUtils.centerParentOnFocus(activityScrollVIEW, tvSection)

        ////////////////////////////////////////////////////////////////////////////////////////////


        val homeScrollView = requireView().findViewById<ScrollView>(R.id.HomeInnerScrollView)
        val realityRow = requireView().findViewById<LinearLayout>(R.id.realityRow)
        val thrillsRow = requireView().findViewById<LinearLayout>(R.id.thrillsRow)


        GlobalUtils.snapRowToTopOnFocus(homeScrollView, realityRow)
        GlobalUtils.snapRowToTopOnFocus(homeScrollView, thrillsRow)


        ////////////////////////////////////////////////////////////////////////////////////////////

        setupRecyclerViews()

        lifecycleScope.launch {
            HomeData()
            categoryShow()
            loadFilterContent(realityAdapter, "&with_genres=10764", false)
            loadFilterContent(thrillAdapter, "&with_genres=27", true)
            genreFilter()
            fetchMovies()
            fetchTvShows()
            //filter()
        }

    }

    override fun onResume() {
        super.onResume()



        // Only request focus if nothing has focus
        requireView().post {
            if (lastFocusedView != null && lastFocusedView!!.isShown && lastFocusedView!!.isFocusable) {
                lastFocusedView!!.requestFocus()
            } else {
                movieRecyclerView.requestFocus()
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()

        // Clear all adapters
        movieAdapter.clearItems()
        tvAdapter.clearItems()
        filterAdapter.clearItems()

        // Cancel any running coroutines
        lifecycleScope.coroutineContext.cancelChildren()



        // Remove all handler callbacks

        // requireActivity().finish()
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun projectMovieItemIntoHero(item: MovieItemOne) {
        try {
            val view = requireView()
            val overlayPoster = view.findViewById<ImageView>(R.id.movieOverlayPoster)
            val titleText = view.findViewById<TextView>(R.id.movieoOverlayTitle)
            val yearText = view.findViewById<TextView>(R.id.movieOverlayYear)
            val ratingText = view.findViewById<TextView>(R.id.movieOverlayRating)
            val overlayCard = view.findViewById<MaterialCardView>(R.id.movieFixedFocusOverlay)

            val heroImage = if (item.backdropUrl.isNotBlank() && item.backdropUrl != "null") {
                item.backdropUrl
            } else {
                item.posterUlr
            }

            // 1. Cross-fade the text content
            val fadeDuration = 250L
            titleText.animate().alpha(0f).setDuration(fadeDuration).withEndAction {
                // Update text after it fades out
                titleText.text = item.title
                yearText.text = item.year
                ratingText.text = item.rating
                overlayCard.alpha = 1f

                // Fade text back in
                titleText.animate().alpha(1f).setDuration(fadeDuration).start()
                yearText.animate().alpha(1f).setDuration(fadeDuration).start()
                ratingText.animate().alpha(1f).setDuration(fadeDuration).start()
            }.start()

            yearText.animate().alpha(0f).setDuration(fadeDuration).start()
            ratingText.animate().alpha(0f).setDuration(fadeDuration).start()

            // 2. Load Image with Cross-fade and Panning Animation
            Glide.with(requireContext())
                .load(heroImage)
                .transition(DrawableTransitionOptions.withCrossFade(150)) // Glide cross-fade
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        // Cancel any previous panning animation to prevent jank
                        overlayPoster.animate().cancel()

                        // Start position: Shifted slightly to the right
                        overlayPoster.translationX = 60f

                        // Pan slowly to the left (Ken Burns effect)
                        overlayPoster.animate()
                            .translationX(0f)
                            .setDuration(200)
                            .setInterpolator(LinearInterpolator())
                            .start()

                        return false
                    }
                })
                .into(overlayPoster)

        } catch (e: Exception) {
            Log.e("projectMovieItemIntoHero", e.toString())
        }
    }

    private fun projectTvItemIntoHero(item: MovieItemOne) {
        try {
           val overlayPoster = requireView().findViewById<ImageView>(R.id.tvOverlayPoster)

            requireView().findViewById<TextView>(R.id.tvOverlayTitle).text = item.title
            requireView().findViewById<TextView>(R.id.tvOverlayYear).text = item.year
            requireView().findViewById<TextView>(R.id.tvOverlayRating).text = item.rating
            requireView().findViewById<MaterialCardView>(R.id.tvFixedFocusOverlay).alpha = 1f


            val heroImage = if (item.backdropUrl.isNotBlank() && item.backdropUrl != "null") {
                item.backdropUrl
            } else {
                item.posterUlr
            }

            Glide.with(requireContext())
                .load(heroImage)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(overlayPoster)

        }catch (e: Exception){
            Log.e("projectTvItemIntoHero", e.toString())
        }
    }


    private fun centerChildUnderFixedFocus(
        recyclerView: RecyclerView,
        overlay: View,
        child: View,
        smooth: Boolean = true
    ) {
        if (recyclerView.width == 0) return

        // Calculate the overlay anchor X directly within the function
        val viewportCenterX = if (overlay.width == 0) {
            val fallbackWidth = (110f * overlay.resources.displayMetrics.density).roundToInt()
            val lp = overlay.layoutParams as? FrameLayout.LayoutParams
            val startMargin = lp?.marginStart ?: 0
            (startMargin + fallbackWidth / 2).coerceIn(0, recyclerView.width)
        } else {
            val recyclerLocation = IntArray(2)
            val overlayLocation = IntArray(2)
            recyclerView.getLocationInWindow(recyclerLocation)
            overlay.getLocationInWindow(overlayLocation)

            val overlayCenterX = overlayLocation[0] - recyclerLocation[0] + (overlay.width / 2)
            overlayCenterX.coerceIn(0, recyclerView.width)
        }

        val childCenterX = child.left + (child.width / 2)
        val distanceToCenter = childCenterX - viewportCenterX
        if (distanceToCenter == 0) return

        if (smooth) {
            val duration = (110 + abs(distanceToCenter) * 0.20f)
                .roundToInt()
                .coerceIn(110, 260)
            recyclerView.smoothScrollBy(
                distanceToCenter,
                0,
                AccelerateDecelerateInterpolator(),
                duration
            )
        } else {
            recyclerView.scrollBy(distanceToCenter, 0)
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////



    private fun setupRecyclerViews() {

        val Spacing = (10 * resources.displayMetrics.density).toInt()
        val gapUsed = 70


        //  Movies ---------------------------------------------------------------------------------
        movieRecyclerView = requireView().findViewById(R.id.MoviesRecyclerView)
        movieFixedFocusOverlay = requireView().findViewById(R.id.movieFixedFocusOverlay)
        movieRecyclerView.layoutManager  =  LinearLayoutManager(
            requireActivity(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        movieRecyclerView.addItemDecoration(EqualSpaceItemDecoration(Spacing))
        movieAdapter = GridAdapter(mutableListOf(), R.layout.item_grid)
        movieRecyclerView.adapter = movieAdapter

        movieAdapter.onAddMoreClicked = { loadMoreMovies() }

        FocusOverlay<MovieItemOne>(
            overlay = movieFixedFocusOverlay,
            recyclerView = movieRecyclerView,
            adapter = movieAdapter
        ) { item ->
            projectMovieItemIntoHero(item)
        }


        //  TV Shows -------------------------------------------------------------------------------

        val tvFixedFocusOverlay = requireView().findViewById<MaterialCardView>(R.id.tvFixedFocusOverlay)
        val tvRecyclerView = requireView().findViewById<RecyclerView>(R.id.TVsRecyclerView)
        tvRecyclerView.layoutManager  =  LinearLayoutManager(
            requireActivity(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        tvRecyclerView.addItemDecoration(EqualSpaceItemDecoration(Spacing))
        tvAdapter = GridAdapter(mutableListOf(), R.layout.item_grid)
        tvRecyclerView.adapter = tvAdapter

        FocusOverlay<MovieItemOne>(
            overlay = tvFixedFocusOverlay,
            recyclerView = tvRecyclerView,
            adapter = tvAdapter
        ) { item ->
            projectTvItemIntoHero(item)
        }
        tvAdapter.onAddMoreClicked = { loadMoreTv() }

        // Filter  ---------------------------------------------------------------------------------
        filterAdapter = FilterAdapter(mutableListOf(), R.layout.item_filter)
        filterAdapter.onItemFocused = { view, item ->
            //showPopupBeside(view, item.posterUlr, 240)
        }
        filterAdapter.onItemFocusLost = {
        }
        fliterRecyclerView = requireView().findViewById<RecyclerView>(R.id.filterResults)
        fliterRecyclerView.layoutManager = GridLayoutManager(requireActivity(), GlobalUtils.calculateSpanCountV2(requireActivity(), 160, gapUsed))
        fliterRecyclerView.adapter = filterAdapter
        fliterRecyclerView.addItemDecoration(EqualSpaceItemDecoration(Spacing))

        //------------------------------------------------------------------------------------------

         fun realitySetFixedFocusOverlayVisible(overlay: View, visible: Boolean) {
            val targetAlpha = if (visible) 1f else 0f
            if (overlay.alpha == targetAlpha) return

            overlay.animate()
                .alpha(targetAlpha)
                .setDuration(if (visible) 90L else 130L)
                .start()
        }

        val realityFixedFocusOverlay = requireView().findViewById<View>(R.id.realityFixedFocusOverlay)
        realityRecyclerView = requireView().findViewById(R.id.realityRecyclerView)
        realityAdapter = FilterAdapter(mutableListOf(), R.layout.item_list2)
        realityAdapter.onItemFocused = { view, item ->
            currentContent.visibility = View.VISIBLE
            updateContentJob?.cancel()
            updateContentJob = lifecycleScope.launch {
                delay(300)
                updateCurrentContent(item)
            }
            realitySetFixedFocusOverlayVisible(realityFixedFocusOverlay, true)
            centerChildUnderFixedFocus(realityRecyclerView, realityFixedFocusOverlay, view)
        }
        realityAdapter.onItemFocusLost = {
            currentContent.visibility = View.GONE
            realityRecyclerView.post {
                realitySetFixedFocusOverlayVisible( realityFixedFocusOverlay,realityRecyclerView.hasFocus()                )
            }
        }


        realityRecyclerView.layoutManager = LinearLayoutManager(
            requireActivity(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        realityRecyclerView.adapter = realityAdapter
        realityRecyclerView.layoutManager?.scrollToPosition(0)



        realityRecyclerView.addOnChildAttachStateChangeListener(object : RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                view.foreground = null // Dynamically strip the focus highlight ONLY for this specific RecyclerView
            }
            override fun onChildViewDetachedFromWindow(view: View) {
            }
        })




        //------------------------------------------------------------------------------------------
        fun thrillSetFixedFocusOverlayVisible(overlay: View, visible: Boolean) {
            val targetAlpha = if (visible) 1f else 0f
            if (overlay.alpha == targetAlpha) return

            overlay.animate()
                .alpha(targetAlpha)
                .setDuration(if (visible) 90L else 130L)
                .start()
        }

        val thrillFixedFocusOverlay = requireView().findViewById<View>(R.id.thrillFixedFocusOverlay)
        thrillRecyclerView = requireView().findViewById<RecyclerView>(R.id.ThrillsRecyclerView)
        thrillAdapter = FilterAdapter(mutableListOf(), R.layout.item_list2)
        thrillAdapter.onItemFocused = { view, item ->
            currentContent.visibility = View.VISIBLE
            updateContentJob?.cancel()
            updateContentJob = lifecycleScope.launch {
                delay(300)
                updateCurrentContent(item)
            }
            thrillSetFixedFocusOverlayVisible(thrillFixedFocusOverlay, true)
            centerChildUnderFixedFocus(thrillRecyclerView, thrillFixedFocusOverlay, view)
        }
        thrillAdapter.onItemFocusLost = {
            currentContent.visibility = View.GONE
            realitySetFixedFocusOverlayVisible( thrillFixedFocusOverlay,thrillRecyclerView.hasFocus()                )

        }
        thrillRecyclerView.layoutManager = LinearLayoutManager(
            requireActivity(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        thrillRecyclerView.adapter = thrillAdapter
        thrillRecyclerView.layoutManager?.scrollToPosition(0)

        thrillRecyclerView.addOnChildAttachStateChangeListener(object : RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                view.foreground = null // Dynamically strip the focus highlight ONLY for this specific RecyclerView
            }
            override fun onChildViewDetachedFromWindow(view: View) {
            }
        })

        //------------------------------------------------------------------------------------------

        genreAdapter = FilterAdapter(mutableListOf(), R.layout.item_list)
        genreAdapter.onItemFocused = { view, item ->
            //backgroundContainer.visibility = View.VISIBLE
        }
        genreAdapter.onItemFocusLost = {
        }
        genreRecyclerView = requireView().findViewById(R.id.genresRecyclerView)
        genreRecyclerView.layoutManager = LinearLayoutManager(
            requireActivity(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        genreRecyclerView.adapter = genreAdapter
        genreRecyclerView.layoutManager?.scrollToPosition(0)
        //------------------------------------------------------------------------------------------




    }




    private fun Int.dp(context: Context): Int =
        (this * context.resources.displayMetrics.density).toInt()
    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun updateCurrentContent(item: filterItemOne) {

        if (currentContent.visibility != View.VISIBLE) {
            currentContent.visibility = View.VISIBLE
        }

        val backdrop = currentContent.findViewById<ImageView>(R.id.cardBackdrop)
        val title = currentContent.findViewById<TextView>(R.id.cardTitle)
        val type = currentContent.findViewById<TextView>(R.id.cardType)
        val rating = currentContent.findViewById<TextView>(R.id.cardRating)
        val year = currentContent.findViewById<TextView>(R.id.cardYear)
        val genre = currentContent.findViewById<TextView>(R.id.cardGenre)
        val pg = currentContent.findViewById<TextView>(R.id.cardPg)
        val overview = currentContent.findViewById<TextView>(R.id.cardOverview)

        Log.d("updateCurrentContent", item.toString())

        genre.visibility = if (item.genres.isNotEmpty()) View.VISIBLE else View.GONE
        pg.visibility = View.VISIBLE
        overview.visibility = if (item.overview.isNotEmpty()) View.VISIBLE else View.GONE

        genre.text = item.genres
        pg.text = if (item.isAdult) "18+" else "PG-13"
        overview.text = item.overview

        title.text = item.title
        type.text = item.type
        rating.text = item.rating
        year.text = item.year

        val imageUrl = if (!item.backdropUrl.isNullOrEmpty() &&
            item.backdropUrl != "null"
        ) {
            item.backdropUrl
        } else {
            item.posterUlr
        }

        Glide.with(currentContent)
            .load(imageUrl)
            .into(backdrop)
    }

    private fun HomeData() {

        val inflater = LayoutInflater.from(requireActivity())
        val container = requireView().findViewById<FrameLayout>(R.id.spotlightShows)

        lifecycleScope.launch {

            val currentYear = Calendar.getInstance().get(Calendar.YEAR)


            val jsonObject =
                withContext(Dispatchers.IO) { fetchTMDB.fetchTrendingData(currentYear.toString()) }
            Log.e("DEBUG_Watch", jsonObject.toString())
            if (jsonObject != null) {

                val moviesArray3 = jsonObject.getJSONArray("results") ?: return@launch

                Log.e("DEBUG_MAIN_Slider raw", moviesArray3.toString())

                val parsedItems = withContext(Dispatchers.IO) {
                    val list = mutableListOf<Map<String, String>>()
                    for (i in 0 until moviesArray3.length()) {
                        val item = moviesArray3.getJSONObject(i)
                        val title = when {
                            item.has("original_name") && !item.isNull("original_name") -> item.getString("original_name")
                            item.has("original_title") && !item.isNull("original_title") -> item.getString("original_title")
                            item.has("title") && !item.isNull("title") -> item.getString("title")
                            else -> "Untitled"
                        }

                        val type = item.optString("media_type", "")
                        if (type != "movie" && type != "tv") {
                            continue
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
                        val year = GlobalUtils.formatDateString(release_date)

                        val voteAverageRaw = item.optString("vote_average", "")
                        val vote_average = if (voteAverageRaw.length >= 3) {
                            voteAverageRaw.substring(0, 3)
                        } else {
                            voteAverageRaw
                        }

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
                        
                        list.add(mapOf(
                            "title" to title,
                            "type" to type,
                            "backdrop_path" to backdrop_path,
                            "pg" to pg,
                            "id" to id,
                            "overview" to overview,
                            "year" to year,
                            "vote_average" to vote_average,
                            "genreNames" to genreNames.joinToString(" • ")
                        ))
                    }
                    list
                }

                for (itemMap in parsedItems) {
                    val card = inflater.inflate(
                        R.layout.card_layout,
                        container,
                        false
                    ) as CardView

                    card.findViewById<TextView>(R.id.cardGenre).text = itemMap["genreNames"]
                    card.findViewById<TextView>(R.id.cardTitle).text = itemMap["title"]
                    card.findViewById<TextView>(R.id.cardQuality).text = "HD"
                    card.findViewById<TextView>(R.id.cardPg).text = itemMap["pg"]
                    card.findViewById<TextView>(R.id.cardType).text = itemMap["type"]
                    card.findViewById<TextView>(R.id.cardRating).text = itemMap["vote_average"]
                    card.findViewById<TextView>(R.id.cardYear).text = itemMap["year"]
                    card.findViewById<TextView>(R.id.cardOverview).text = itemMap["overview"]

                    val SliderBackdrop = card.findViewById<ImageView>(R.id.cardBackdrop)

                    val currentHeight = card.height
                    val currentWidth = card.width
                    val sizeH = (currentHeight  * 2f).toInt()
                    val sizeW = (currentWidth  * 2f).toInt()

                    Glide.with(card.context)
                        .load(itemMap["backdrop_path"])
                        .override(sizeW, sizeH)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerInside()
                        .into(SliderBackdrop)

                    card.setOnClickListener {
                        val context = card.context
                        val intent = Intent(context, Watch_Page::class.java)
                        intent.putExtra("imdb_code", itemMap["id"])
                        intent.putExtra("type", itemMap["type"])
                        context.startActivity(intent)
                    }

                    container.addView(card)
                }

                if (moviesArray3.length() > 0){
                    //GlobalUtils.setupCardStackFromContainer(container)
                    com.example.onyx.OnyxClasses.CardStack().setupCardStackFromContainer(container)
                    requireView().findViewById<FrameLayout>(R.id.SpotlightSection).visibility = View.VISIBLE
                    //LoadingAnimation.hide(requireActivity())
                    LoadingAnimation.hide(requireView())
                }


            } else {
                return@launch
                //LoadingAnimation.setup(requireActivity(), R.raw.error)
                //LoadingAnimation.show(requireActivity())

                LoadingAnimation.setup(requireContext(), requireView(), R.raw.error)
                LoadingAnimation.show(requireView())

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
                    cName = name,
                    parentView = SpotlightSection
                )
            )
        }

        // Setup RecyclerView
        val recyclerView = requireView().findViewById<RecyclerView>(R.id.CategoryRecyclerView)
        val adapter = CategoryAdapter(categoryItems, R.layout.item_category)

        val layoutManager = LinearLayoutManager(
            requireActivity(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        adapter.onItemFocused = { _, _ ->
            backgroundContainer.visibility = View.GONE
            //currentContent.visibility = View.GONE
        }

    }




    private fun loadFilterContent(
        adapter: FilterAdapter,
        query: String,
        isMovie: Boolean
    ){
        var page  = 1
        var isLoading  = false

        fun fetchContent() {
            isLoading  = true
            lifecycleScope.launch {

                val jsonObject =
                    withContext(Dispatchers.IO) {
                        if (isMovie) {
                            fetchTMDB.fetchDiscoverMovie("$query&page=$page")
                        } else {
                            fetchTMDB.fetchDiscoverTv("$query&page=$page")
                        }
                    }
                if (jsonObject == null){
                    isLoading  = false
                    return@launch
                }


                val mvData = jsonObject.getJSONArray("results")

                for (i in 0 until mvData.length()) {

                    val current = mvData.getJSONObject(i)

                    Log.d("DEBUG_MAIN_Filter", current.toString())

                    val backdrop_path = current.optString("backdrop_path", "")
                    val poster_path = current.optString("poster_path", "")

                    val vote_average = current.optString("vote_average", "")
                    val title = current.optString("title").takeIf { it.isNotEmpty() } ?: current.optString("name", "")
                    val overview = current.optString("overview", "")
                    val adult = current.optBoolean("adult", false)
                    val id = current.optString("id", "")
                    val original_title = current.optString("original_title", "")

                    val genreIdsArr = current.optJSONArray("genre_ids")
                    val genresList = mutableListOf<String>()
                    if (genreIdsArr != null) {
                        for (j in 0 until genreIdsArr.length()) {
                            val gName = when(genreIdsArr.optInt(j)) {
                                28 -> "Action"
                                12 -> "Adventure"
                                16 -> "Animation"
                                35 -> "Comedy"
                                80 -> "Crime"
                                99 -> "Documentary"
                                18 -> "Drama"
                                10751 -> "Family"
                                14 -> "Fantasy"
                                36 -> "History"
                                27 -> "Horror"
                                10402 -> "Music"
                                9648 -> "Mystery"
                                10749 -> "Romance"
                                878 -> "Sci-Fi"
                                10770 -> "TV Movie"
                                53 -> "Thriller"
                                10752 -> "War"
                                37 -> "Western"
                                10759 -> "Action & Adventure"
                                10762 -> "Kids"
                                10763 -> "News"
                                10764 -> "Reality"
                                10765 -> "Sci-Fi & Fantasy"
                                10766 -> "Soap"
                                10767 -> "Talk"
                                10768 -> "War & Politics"
                                else -> ""
                            }
                            if (gName.isNotEmpty()) genresList.add(gName)
                        }
                    }
                    val genreString = genresList.joinToString(", ")

                    val type: String
                    var date: String
                    var showD: String = ""

                    val imgPost = "https://image.tmdb.org/t/p/original$poster_path"
                    val imgback = "https://image.tmdb.org/t/p/original$backdrop_path"

                    if (original_title == "") {
                        type = "tv";
                        date = if (current.getString("first_air_date").length >= 4) {
                            current.getString("first_air_date").substring(0, 4)
                        } else {
                            current.getString("first_air_date")
                        }
                    } else {
                        type = "movie"
                        date = if (current.getString("release_date").length >= 4) {
                            current.getString("release_date").substring(0, 4)
                        } else {
                            current.getString("release_date")
                        }
                    }

                    val Item = filterItemOne(
                        title = title,
                        backdropUrl = imgback,
                        posterUlr = imgPost ,
                        imdbCode = id,
                        type = type,
                        year = date,
                        rating = vote_average,
                        runtime = showD,
                        overview = overview,
                        isAdult = adult,
                        genres = genreString
                    )

                    withContext(Dispatchers.Main) {
                    if (!isAdded || view == null) return@withContext
                        adapter.addItem(Item)
                        isLoading  = false


                    }
                }
                withContext(Dispatchers.Main) {
                    if (!isAdded || view == null) return@withContext
                    adapter.isLoadingMore = false
                    if (adapter.itemCount > 0) {
                        requireView().findViewById<FrameLayout>(R.id.HomeContentSection).visibility =
                            View.VISIBLE
                    }
                }
            }
        }

        fun loadMore() {
            if (isLoading) return // Prevent multiple rapid clicks
            page++
            fetchContent()
        }

        adapter.onAddMoreClicked = { loadMore() }

        loadMore()
    }


    private fun genreFilter() {
        var isloadingGenre = false
        var genrePage = 1
        var genresMv = ""
        var genresTv = ""

        // Helper function to map genre IDs to names
        fun getGenresString(genreIdsArr: JSONArray?): String {
            if (genreIdsArr == null) return ""
            val genresList = mutableListOf<String>()
            for (j in 0 until genreIdsArr.length()) {
                val gName = when (genreIdsArr.optInt(j)) {
                    28 -> "Action"
                    12 -> "Adventure"
                    16 -> "Animation"
                    35 -> "Comedy"
                    80 -> "Crime"
                    99 -> "Documentary"
                    18 -> "Drama"
                    10751 -> "Family"
                    14 -> "Fantasy"
                    36 -> "History"
                    27 -> "Horror"
                    10402 -> "Music"
                    9648 -> "Mystery"
                    10749 -> "Romance"
                    878 -> "Sci-Fi"
                    10770 -> "TV Movie"
                    53 -> "Thriller"
                    10752 -> "War"
                    37 -> "Western"
                    10759 -> "Action & Adventure"
                    10762 -> "Kids"
                    10763 -> "News"
                    10764 -> "Reality"
                    10765 -> "Sci-Fi & Fantasy"
                    10766 -> "Soap"
                    10767 -> "Talk"
                    10768 -> "War & Politics"
                    else -> ""
                }
                if (gName.isNotEmpty()) genresList.add(gName)
            }
            return genresList.joinToString(", ")
        }

        // Helper to parse JSON arrays into your data objects to prevent code duplication
        fun parseJsonArray(jsonArray: JSONArray?, isTvShow: Boolean): List<filterItemOne> {
            val parsedList = mutableListOf<filterItemOne>()
            if (jsonArray == null) return parsedList

            for (i in 0 until jsonArray.length()) {
                val current = jsonArray.optJSONObject(i) ?: continue

                val backdropPath = current.optString("backdrop_path", "")
                val posterPath = current.optString("poster_path", "")
                val voteAverage = current.optString("vote_average", "")
                val title = current.optString("title").takeIf { it.isNotEmpty() } ?: current.optString("name", "")
                val overview = current.optString("overview", "")
                val adult = current.optBoolean("adult", false)
                val id = current.optString("id", "")

                val genreString = getGenresString(current.optJSONArray("genre_ids"))

                val type = if (isTvShow) "tv" else "movie"
                val dateKey = if (isTvShow) "first_air_date" else "release_date"
                val rawDate = current.optString(dateKey, "")
                val date = if (rawDate.length >= 4) rawDate.substring(0, 4) else rawDate

                val imgPost = "https://image.tmdb.org/t/p/original$posterPath"
                val imgBack = "https://image.tmdb.org/t/p/original$backdropPath"

                parsedList.add(
                    filterItemOne(
                        title = title,
                        backdropUrl = imgBack,
                        posterUlr = imgPost,
                        imdbCode = id,
                        type = type,
                        year = date,
                        rating = voteAverage,
                        runtime = "",
                        overview = overview,
                        isAdult = adult,
                        genres = genreString
                    )
                )
            }
            return parsedList
        }

        fun fetchGenre(first: Boolean = true) {
            if (isloadingGenre) return
            isloadingGenre = true

            lifecycleScope.launch {

                // 1. Fetch AND parse completely in the background (IO Thread)
                val (newMovies, newTvShows) = withContext(Dispatchers.IO) {

                    // Use coroutineScope to safely launch parallel async tasks
                    coroutineScope {
                        val mvDeferred = async { fetchTMDB.fetchDiscoverMovie("$genresMv&page=$genrePage") }
                        val tvDeferred = async { fetchTMDB.fetchDiscoverTv("$genresTv&page=$genrePage") }

                        val jsonObjectMv = mvDeferred.await()
                        val jsonObjectTV = tvDeferred.await()

                        // Parse the JSON while still on the background thread
                        val movies = parseJsonArray(jsonObjectMv?.optJSONArray("results"), isTvShow = false)
                        val tvShows = parseJsonArray(jsonObjectTV?.optJSONArray("results"), isTvShow = true)

                        // Return the paired lists
                        movies to tvShows
                    }
                }

                // 2. Combine the lists into one
                val combinedList = newMovies + newTvShows

                // 3. Switch back to the Main UI thread to update the screen
                if (combinedList.isNotEmpty()) {
                    var wasEmpty = genreAdapter.itemCount <= 1

                    genreAdapter.addItems(combinedList)
                    if (wasEmpty) {
                        genreRecyclerView.scrollToPosition(0)
                        wasEmpty =  false

                        requireView().findViewById<LinearLayout>(R.id.filterSection).visibility = View.VISIBLE

                    }




                    // Increment your page here so the next call gets the next page!
                    genrePage++
                }

                genreAdapter.isLoadingMore = false
                isloadingGenre = false


            }
        }

        fun loadMoreGenre() {
            if (isloadingGenre) return
            genrePage++
            fetchGenre(false) // Assuming load more isn't the 'first' load
        }

        genreAdapter.onAddMoreClicked = { loadMoreGenre() }

        // --- UI Setup ---
        val comedyBtn = requireView().findViewById<TextView>(R.id.genreComedy)
        val actionBtn = requireView().findViewById<TextView>(R.id.genreAction)
        val sciFiBtn = requireView().findViewById<TextView>(R.id.genreSCIFI)
        val animationBtn = requireView().findViewById<TextView>(R.id.genreAnimation)
        val familyBtn = requireView().findViewById<TextView>(R.id.genreFamily)
        val romanceBtn = requireView().findViewById<TextView>(R.id.genreRomance)
        val dramaBtn = requireView().findViewById<TextView>(R.id.genreDrama)

        val allGenreButtons = listOf(comedyBtn, actionBtn, sciFiBtn, animationBtn, familyBtn, romanceBtn, dramaBtn)

        // Helper to handle button clicks cleanly
        fun setupGenreButton(button: TextView, mvGenreId: Int, tvGenreId: Int) {
            button.setOnClickListener {
                if (isloadingGenre) return@setOnClickListener

                // Update UI State for all buttons dynamically
                allGenreButtons.forEach { it.isSelected = (it == button) }

                // Reset pagination and adapter
                genrePage = 1
                genreAdapter.clearItems()

                // Update genre query parameters
                genresMv = "&with_genres=$mvGenreId"
                genresTv = "&with_genres=$tvGenreId"

                fetchGenre(true)
            }
        }

        // Initialize all buttons
        setupGenreButton(comedyBtn, 35, 35)
        setupGenreButton(actionBtn, 28, 10759)
        setupGenreButton(sciFiBtn, 878, 10765)
        setupGenreButton(animationBtn, 12, 16)
        setupGenreButton(familyBtn, 10751, 10751)
        setupGenreButton(romanceBtn, 10749, 10766)
        setupGenreButton(dramaBtn, 18, 18)

        // Trigger initial load
        comedyBtn.performClick()
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////


    private fun fetchMovies() {
        if (isLoadingMoreMovies) return
        isLoadingMoreMovies = true
        movieAdapter.isLoadingMore = true

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
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

                        GlobalUtils.preloadMovieImages(requireContext(), imgUrl2, imgUrl)

                        movies.add(
                            MovieItemOne(
                                title = title,
                                backdropUrl = imgUrl,
                                posterUlr = imgUrl2,
                                imdbCode = id,
                                type = "movie",
                                year = GlobalUtils.formatDateString(year),
                                rating = "${String.format("%.1f", rating)}imdb",
                                runtime = "⏱$runtime min"
                            )
                        )
                    }

                    // ✅ Update UI once per batch
                    withContext(Dispatchers.Main) {
                        if (!isAdded || view == null) return@withContext
                        movieAdapter.addItems(movies)
                        isLoadingMoreMovies = false
                        movieAdapter.isLoadingMore = false

                        if (movieAdapter.itemCount > 0) {
                            requireView().findViewById<LinearLayout>(R.id.MovieSection).visibility = View.VISIBLE
                        }

                        LoadingAnimation.hide(requireView())
                    }

                    return@launch // success → stop repeating
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) {
                    if (!isAdded || view == null) return@withContext
                        Log.e("DEBUG_SHOWS PAGE", "Network error ", e)
                        //LoadingAnimation.setup(requireActivity(), R.raw.error)
                        //LoadingAnimation.show(requireActivity())
                    }
                    delay(30_000)
                } catch (e: Exception) {
                    Log.e("DEBUG_MOVIES_ERROR", "Attempt ${attempt + 1} failed: ${e.message}", e)
                    delay(5000)
                }
            }

            withContext(Dispatchers.Main) {
                    if (!isAdded || view == null) return@withContext
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
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

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
                        val firstAirDate = jsonObject.getString("first_air_date")

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
                        movies.add(
                            MovieItemOne(
                                title = title,
                                backdropUrl = imgUrl,
                                posterUlr = imgUrl2,
                                imdbCode = id,
                                type = type,
                                year =   GlobalUtils.formatDateString(firstAirDate),
                                rating = voteAverage,
                                runtime = showD
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {
                        if (!isAdded || view == null) return@withContext
                        tvAdapter.addItems(movies)
                        isLoadingMoreTv = false
                        tvAdapter.isLoadingMore = false

                        if (tvAdapter.itemCount > 0) {
                            requireView().findViewById<LinearLayout>(R.id.tvSection).visibility = View.VISIBLE
                        }
                        LoadingAnimation.hide(requireView())
                    }

                    Log.e("DEBUG_TAG_TvShows 4", movies.toString())

                    return@launch
                } catch (e: IOException) {
                        withContext(Dispatchers.Main) {
                    if (!isAdded || view == null) return@withContext
                            Log.e("DEBUG_SHOWS PAGE", "Network error ", e)
                            //LoadingAnimation.setup(requireActivity(), R.raw.error)
                            //LoadingAnimation.show(requireActivity())
                        }
                        delay(30_000)

                } catch (e: Exception) {
                    Log.e("DEBUG_TAG_TvShows", "Attempt ${attempt+1} failed", e)
                    delay(10_000)
                    currentTvPage--
                }
                withContext(Dispatchers.Main) {
                    if (!isAdded || view == null) return@withContext
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
private fun filter(){

        //filter options buttons
        val filterTypeBtn = requireView().findViewById<TextView>(R.id.FilterTypeBtn)
        val filterCountryBtn = requireView().findViewById<TextView>(R.id.FilterCountryBtn)
        val filterSortBtn = requireView().findViewById<TextView>(R.id.FilterSortBtn)
        val filterGenreBtn = requireView().findViewById<TextView>(R.id.FilterGenreBtn)
        val filterYearBtn = requireView().findViewById<TextView>(R.id.FilterYearBtn)


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

        val filterDisplay = requireView().findViewById<TextView>(R.id.FilterDisplay)
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

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

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
                    if (!isAdded || view == null) return@withContext
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
        val builder = AlertDialog.Builder(requireActivity(), R.style.CustomDialogTheme)
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

        val builder = AlertDialog.Builder(requireActivity(), R.style.CustomDialogTheme)
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




    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////




}

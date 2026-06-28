package com.example.onyx

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.onyx.OnyxClasses.EqualSpaceItemDecoration
import com.example.onyx.OnyxClasses.GridAdapter
import com.example.onyx.OnyxClasses.MovieItemOne
import com.example.onyx.OnyxObjects.GlobalUtils
import com.example.onyx.OnyxObjects.LoadingAnimation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.onyx.FetchData.TMDBapi
import com.example.onyx.OnyxClasses.FocusOverlay
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.cancelChildren

class CategoryFragment : Fragment(R.layout.fragment_category) {
    private lateinit var fragmentBackCallback: OnBackPressedCallback
    private lateinit var fetchTMDB: TMDBapi

    private lateinit var moviesAdapter: GridAdapter
    private lateinit var tvAdapter: GridAdapter
    private lateinit var categoryTitle: TextView

    private var companyId: String = ""
    private var companyName: String = "Collection"

    private var currentMoviePage = 1
    private var totalMoviePages = 1
    private var isLoadingMovies = false
    private var shouldFetchMovies = true

    private var currentTvPage = 1
    private var totalTvPages = 1
    private var isLoadingTv = false
    private var shouldFetchTv = true

    private val movieCache = mutableListOf<MovieItemOne>()
    private val tvCache = mutableListOf<MovieItemOne>()
    private val cachePrefs by lazy { requireActivity().getSharedPreferences("CategoryCache", Context.MODE_PRIVATE) }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        GlobalUtils.applyTheme(requireActivity())
        super.onViewCreated(view, savedInstanceState)

        setupBackPressedCallback()

        fetchTMDB = TMDBapi(requireActivity())

        //------------------------------------------------------------------------------------------

        val fragmentScrollVIEW = requireView().findViewById<ScrollView>(R.id.fragmentScrollVIEW)
        val movieSection = requireView().findViewById<LinearLayout>(R.id.MovieSection)
        val tvSection = requireView().findViewById<LinearLayout>(R.id.tvSection)

        movieSection.visibility = View.GONE
        tvSection.visibility = View.GONE


        GlobalUtils.centerParentOnFocus(fragmentScrollVIEW, movieSection)
        GlobalUtils.centerParentOnFocus(fragmentScrollVIEW, tvSection)

        //------------------------------------------------------------------------------------------

        categoryTitle = requireView().findViewById(R.id.CategoryTitle)
        categoryTitle.requestFocus()

        companyId = arguments?.getString("company_id")?: ""
        companyName = arguments?.getString("company_name")?: ""



        if (companyId.isBlank()) {
            Log.e("Category_Page", "Missing company_id, closing activity. $companyId, $companyName")
            return
        }
        Log.e("Category_Page", "\n company_id: $companyId,\n company_name: $companyName, \n Bearer ${BuildConfig.TM_K}")

        categoryTitle.text = companyName

        setupRecyclerViews()


        shouldFetchMovies = !loadCachedList(getMoviesCacheKey(companyId), movieCache, moviesAdapter)
        shouldFetchTv = !loadCachedList(getTvCacheKey(companyId), tvCache, tvAdapter)

        if (shouldFetchMovies) fetchMovies()
        if (shouldFetchTv) fetchTv()
    }

    private fun setupRecyclerViews() {

        //------------------------------------------------------------------------------------------

        val moviesRecyclerView = requireView().findViewById<RecyclerView>(R.id.MoviesRecyclerView)
        val movieFixedFocusOverlay = requireView().findViewById<MaterialCardView>(R.id.movieFixedFocusOverlay)
        moviesRecyclerView.layoutManager  =  LinearLayoutManager(
            requireActivity(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        moviesRecyclerView.addItemDecoration(EqualSpaceItemDecoration(20))
        moviesAdapter = GridAdapter(mutableListOf(), R.layout.item_grid)
        moviesRecyclerView.adapter = moviesAdapter

        moviesAdapter.onAddMoreClicked = { loadMoreMovies() }

        FocusOverlay<MovieItemOne>(
            overlay = movieFixedFocusOverlay,
            recyclerView = moviesRecyclerView,
            adapter = moviesAdapter
        ) { item ->
            projectMovieItemIntoHero(item)
        }

        //------------------------------------------------------------------------------------------

        val tvFixedFocusOverlay = requireView().findViewById<MaterialCardView>(R.id.tvFixedFocusOverlay)
        val tvRecyclerView = requireView().findViewById<RecyclerView>(R.id.TVsRecyclerView)
        tvRecyclerView.layoutManager  =  LinearLayoutManager(
            requireActivity(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        tvRecyclerView.addItemDecoration(EqualSpaceItemDecoration(20))
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
                        target: com.bumptech.glide.request.target.Target<Drawable>,
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
    //////////////////////////////////////////////////////////////////////////////////////////////



    private fun loadCachedList(
        key: String,
        targetList: MutableList<MovieItemOne>,
        adapter: GridAdapter
    ): Boolean {
        val raw = cachePrefs.getString(key, null) ?: return false
        return try {
            val array = JSONArray(raw)
            if (array.length() == 0) return false

            val cached = mutableListOf<MovieItemOne>()
            for (i in 0 until array.length()) {
                val entry = array.getJSONObject(i)
                cached.add(
                    MovieItemOne(
                        title = entry.optString("title"),
                        backdropUrl = entry.optString("backdropUrl"),
                        posterUlr = entry.optString("posterUlr"),
                        imdbCode = entry.optString("imdbCode"),
                        type = entry.optString("type").ifEmpty { "movie" },
                        year = entry.optString("year"),
                        rating = entry.optString("rating"),
                        runtime = entry.optString("runtime")
                    )
                )
            }

            targetList.clear()
            targetList.addAll(cached)

            adapter.clearItems()
            adapter.addItems(cached)
            true
        } catch (e: Exception) {
            Log.e("Category_Page", "Failed to parse cache for $key", e)
            false
        }
    }

    private fun saveCache(key: String, data: List<MovieItemOne>) {
        try {
            val array = JSONArray()
            data.forEach { item ->
                val entry = JSONObject().apply {
                    put("title", item.title)
                    put("backdropUrl", item.backdropUrl)
                    put("posterUlr", item.posterUlr)
                    put("imdbCode", item.imdbCode)
                    put("type", item.type)
                    put("year", item.year)
                    put("rating", item.rating)
                    put("runtime", item.runtime)
                }
                array.put(entry)
            }

            cachePrefs.edit().putString(key, array.toString()).apply()
        } catch (e: Exception) {
            Log.e("Category_Page", "Failed to write cache for $key", e)
        }
    }

    private fun getMoviesCacheKey(companyId: String) = "Category_Movies_$companyId"
    private fun getTvCacheKey(companyId: String) = "Category_TV_$companyId"
    private fun fetchMovies() {
        if (isLoadingMovies || companyId.isBlank() || !shouldFetchMovies) return
        if (currentMoviePage > totalMoviePages) return

        isLoadingMovies = true
        moviesAdapter.isLoadingMore = true

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            repeat(3) { attempt ->
                try {
                    val url =
                        "https://api.themoviedb.org/3/discover/movie?include_adult=true&with_companies=$companyId&page=$currentMoviePage"

                    val payload = fetchTMDB.fetchDiscoverMovie("with_companies=$companyId&page=$currentMoviePage")
                    if (payload == null) {
                        return@launch
                    }

                    totalMoviePages = payload.optInt("total_pages", totalMoviePages)

                    val seenIds = movieCache.map { it.imdbCode }.toMutableSet()
                    val list = mutableListOf<MovieItemOne>()
                    val results = payload.getJSONArray("results")

                    for (i in 0 until results.length()) {
                        val item = results.getJSONObject(i)
                        val id = item.optString("id")
                        if (id.isBlank() || seenIds.contains(id)) continue
                        seenIds.add(id)

                        val releaseDate = item.optString("release_date")
                        val year = releaseDate.takeIf { it.length >= 4 }?.substring(0, 4) ?: ""
                        val vote = item.optDouble("vote_average", 0.0)
                        val rating =
                            if (vote > 0) "☆${String.format(Locale.US, "%.1f", vote)}" else ""

                        val posterPath = item.optString("poster_path")
                        val backdropPath = item.optString("backdrop_path")

                        val backdropUrl = when {
                            backdropPath.isNotBlank() -> "https://image.tmdb.org/t/p/w1280$backdropPath"
                            posterPath.isNotBlank() -> "https://image.tmdb.org/t/p/w780$posterPath"
                            else -> ""
                        }

                        val posterUrl = when {
                            posterPath.isNotBlank() -> "https://image.tmdb.org/t/p/w780$posterPath"
                            backdropPath.isNotBlank() -> "https://image.tmdb.org/t/p/w1280$backdropPath"
                            else -> ""
                        }

                        list.add(
                            MovieItemOne(
                                title =
                                    item.optString(
                                        "title",
                                        item.optString("original_title", "Untitled")
                                    ),
                                backdropUrl = backdropUrl,
                                posterUlr = posterUrl,
                                imdbCode = id,
                                type = "movie",
                                year = year,
                                rating = rating,
                                runtime = releaseDate
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {
                    if (!isAdded || view == null) return@withContext
                        val wasEmpty = movieCache.isEmpty()

                        if (list.isNotEmpty()) {
                            movieCache.addAll(list)
                            moviesAdapter.addItems(list)
                            saveCache(getMoviesCacheKey(companyId), movieCache)
                            requireView().findViewById<LinearLayout>(R.id.MovieSection).visibility = View.VISIBLE
                            requireView().findViewById<ProgressBar>(R.id.progress_bar).visibility = View.GONE


                        }



                        currentMoviePage++
                        isLoadingMovies = false
                        moviesAdapter.isLoadingMore = false
                    }
                    return@launch
                } catch (e: Exception) {
                    Log.e(
                        "Category_Page",
                        "Failed to fetch movies page $currentMoviePage (attempt ${attempt + 1})",
                        e
                    )
                    delay(4000)
                }

            }

            withContext(Dispatchers.Main) {
                    if (!isAdded || view == null) return@withContext
                isLoadingMovies = false
                moviesAdapter.isLoadingMore = false
            }
        }
    }

    private fun fetchTv() {
        if (isLoadingTv || companyId.isBlank() || !shouldFetchTv) return
        if (currentTvPage > totalTvPages) return

        isLoadingTv = true
        tvAdapter.isLoadingMore = true

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            repeat(3) { attempt ->
                try {

                    val payload = fetchTMDB.fetchDiscoverTv("with_companies=$companyId&page=$currentTvPage")
                    if (payload == null) {
                        return@launch
                    }

                    totalTvPages = payload.optInt("total_pages", totalTvPages)

                    val seenIds = tvCache.map { it.imdbCode }.toMutableSet()
                    val list = mutableListOf<MovieItemOne>()
                    val results = payload.getJSONArray("results")

                    for (i in 0 until results.length()) {
                        val item = results.getJSONObject(i)
                        val id = item.optString("id")
                        if (id.isBlank() || seenIds.contains(id)) continue
                        seenIds.add(id)

                        val firstAirDate = item.optString("first_air_date")
                        val year =
                            firstAirDate.takeIf { it.length >= 4 }?.substring(0, 4) ?: ""
                        val vote = item.optDouble("vote_average", 0.0)
                        val rating =
                            if (vote > 0) "☆${String.format(Locale.US, "%.1f", vote)}" else ""

                        val posterPath = item.optString("poster_path")
                        val backdropPath = item.optString("backdrop_path")

                        val backdropUrl = when {
                            backdropPath.isNotBlank() -> "https://image.tmdb.org/t/p/w1280$backdropPath"
                            posterPath.isNotBlank() -> "https://image.tmdb.org/t/p/w780$posterPath"
                            else -> ""
                        }

                        val posterUrl = when {
                            posterPath.isNotBlank() -> "https://image.tmdb.org/t/p/w780$posterPath"
                            backdropPath.isNotBlank() -> "https://image.tmdb.org/t/p/w1280$backdropPath"
                            else -> ""
                        }

                        list.add(
                            MovieItemOne(
                                title = item.optString(
                                    "name",
                                    item.optString("original_name", "Untitled")
                                ),
                                backdropUrl = backdropUrl,
                                posterUlr = posterUrl,
                                imdbCode = id,
                                type = "tv",
                                year = year,
                                rating = rating,
                                runtime = firstAirDate
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {
                    if (!isAdded || view == null) return@withContext
                        val wasEmpty = tvCache.isEmpty()

                        if (list.isNotEmpty()) {
                            tvCache.addAll(list)
                            tvAdapter.addItems(list)
                            saveCache(getTvCacheKey(companyId), tvCache)
                            requireView().findViewById<LinearLayout>(R.id.tvSection).visibility = View.VISIBLE
                            requireView().findViewById<ProgressBar>(R.id.progress_bar).visibility = View.GONE
                        }



                        currentTvPage++
                        isLoadingTv = false
                        tvAdapter.isLoadingMore = false
                    }
                    return@launch
                } catch (e: Exception) {
                    Log.e(
                        "Category_Page",
                        "Failed to fetch tv page $currentTvPage (attempt ${attempt + 1})",
                        e
                    )
                    delay(4000)
                }
            }

            withContext(Dispatchers.Main) {
                    if (!isAdded || view == null) return@withContext
                isLoadingTv = false
                tvAdapter.isLoadingMore = false
            }
        }
    }

    private fun loadMoreMovies() {
        if (!shouldFetchMovies) return
        if (isLoadingMovies) return
        if (currentMoviePage > totalMoviePages) return
        fetchMovies()
    }

    private fun loadMoreTv() {
        if (!shouldFetchTv) return
        if (isLoadingTv) return
        if (currentTvPage > totalTvPages) return
        fetchTv()
    }


    private fun setupBackPressedCallback() {
        // 1. Assign it to the variable we created
        fragmentBackCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {

                // Cancel any running coroutines
                lifecycleScope.coroutineContext.cancelChildren()

                val homeActivity = requireActivity() as HomeActivity
                homeActivity.showsFragment?.let { existingShowsAnimeTab ->
                    homeActivity.navigateToExistingAndDestroyCurrent(existingShowsAnimeTab, this@CategoryFragment)
                } ?: run {
                    homeActivity.navigateToFragment(ShowsFragment())
                }
            }
        }

        // 2. Add it to the dispatcher
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, fragmentBackCallback)
    }

    override fun onResume() {
        super.onResume()
        if (::fragmentBackCallback.isInitialized) {
            fragmentBackCallback.remove()
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, fragmentBackCallback)
        }
    }
}
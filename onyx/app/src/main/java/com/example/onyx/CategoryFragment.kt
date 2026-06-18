package com.example.onyx

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
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
class CategoryFragment : Fragment(R.layout.fragment_category) {

    private lateinit var moviesRecyclerView: RecyclerView
    private lateinit var tvRecyclerView: RecyclerView
    private lateinit var moviesAdapter: GridAdapter
    private lateinit var tvAdapter: GridAdapter
    private lateinit var moviesButton: LinearLayout
    private lateinit var tvButton: LinearLayout
    private lateinit var moviesButtonText: TextView
    private lateinit var tvButtonText: TextView
    private lateinit var categoryTitle: TextView
    private lateinit var categorySubtitle: TextView

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
        
        
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        //LoadingAnimation.setup(requireActivity(), R.raw.line_loading)
        //LoadingAnimation.show(requireActivity())

        categoryTitle = requireView().findViewById(R.id.CategoryTitle)
        categoryTitle.requestFocus()

        companyId = requireActivity().intent.getStringExtra("company_id").orEmpty()
        companyName = requireActivity().intent.getStringExtra("company_name") ?: "Collection"

        if (companyId.isBlank()) {
            Log.e("Category_Page", "Missing company_id, closing activity.")
            // requireActivity().finish()
            return
        }

        categoryTitle.text = companyName

        setupRecyclerViews()


        shouldFetchMovies = !loadCachedList(getMoviesCacheKey(companyId), movieCache, moviesAdapter)
        shouldFetchTv = !loadCachedList(getTvCacheKey(companyId), tvCache, tvAdapter)

        if (shouldFetchMovies) fetchMovies()
        if (shouldFetchTv) fetchTv()
    }

    private fun setupRecyclerViews() {
        val itemWidthDp = 289

        moviesRecyclerView = requireView().findViewById(R.id.CategoryMoviesRecyclerView)
        //moviesRecyclerView.layoutManager = GridLayoutManager(requireActivity(), GlobalUtils.calculateSpanCount(requireActivity(), itemWidthDp))
        moviesRecyclerView.layoutManager = LinearLayoutManager(
            requireActivity(),
            LinearLayoutManager.HORIZONTAL,
            false
        )

        val movieSpacing = (15 * resources.displayMetrics.density).toInt()
        //moviesRecyclerView.addItemDecoration(EqualSpaceItemDecoration(movieSpacing))

        moviesAdapter = GridAdapter(mutableListOf(), R.layout.item_grid2)
        moviesRecyclerView.adapter = moviesAdapter
        moviesAdapter.onAddMoreClicked = { loadMoreMovies() }

        tvRecyclerView = requireView().findViewById(R.id.CategoryTvRecyclerView)
        //tvRecyclerView.layoutManager = GridLayoutManager(requireActivity(), GlobalUtils.calculateSpanCount(requireActivity(), itemWidthDp))
        tvRecyclerView.layoutManager =  LinearLayoutManager(
            requireActivity(),
            LinearLayoutManager.HORIZONTAL,
            false
        )

        val tvSpacing = (15 * resources.displayMetrics.density).toInt()
        //tvRecyclerView.addItemDecoration(EqualSpaceItemDecoration(tvSpacing))

        tvAdapter = GridAdapter(mutableListOf(), R.layout.item_grid2)
        tvRecyclerView.adapter = tvAdapter
        tvAdapter.onAddMoreClicked = { loadMoreTv() }
    }



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
                    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        setRequestProperty("accept", "application/json")
                        setRequestProperty("Authorization", "Bearer ${BuildConfig.TM_K}")
                    }

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val payload = JSONObject(response)
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
                        }

                        if (wasEmpty) {
                            moviesRecyclerView.post {
                                (moviesRecyclerView.layoutManager as? LinearLayoutManager)
                                    ?.scrollToPositionWithOffset(0, 0)
                            }
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
                LoadingAnimation.hide(requireActivity())
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
                    val url =
                        "https://api.themoviedb.org/3/discover/tv?include_adult=true&with_companies=$companyId&page=$currentTvPage"
                    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        setRequestProperty("accept", "application/json")
                        setRequestProperty("Authorization", "Bearer ${BuildConfig.TM_K}")
                    }

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val payload = JSONObject(response)
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
                        }

                        if (wasEmpty) {
                            tvRecyclerView.post {
                                (tvRecyclerView.layoutManager as? LinearLayoutManager)
                                    ?.scrollToPositionWithOffset(0, 0)
                            }
                        }

                        currentTvPage++
                        isLoadingTv = false
                        tvAdapter.isLoadingMore = false
                        LoadingAnimation.hide(requireActivity())
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
                LoadingAnimation.hide(requireActivity())
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

}
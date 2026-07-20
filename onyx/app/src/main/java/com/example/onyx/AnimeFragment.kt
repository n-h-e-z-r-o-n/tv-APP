package com.example.onyx

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.onyx.Database.AppDatabase
import com.example.onyx.Database.SessionManger
import com.example.onyx.FetchData.AnimeApi
import com.example.onyx.OnyxClasses.AiringAnimeItem
import com.example.onyx.OnyxClasses.AnimeAiringAdapter
import com.example.onyx.OnyxClasses.AnimeGridAdapter
import com.example.onyx.OnyxClasses.AnimeGridItem
import com.example.onyx.OnyxClasses.AnimeTrendingAdapter
import com.example.onyx.OnyxClasses.EqualSpaceItemDecoration
import com.example.onyx.OnyxClasses.FavAdapter
import com.example.onyx.OnyxClasses.FavItem
import com.example.onyx.OnyxClasses.FocusOverlay
import com.example.onyx.OnyxClasses.TrendingAnimeItem
import com.example.onyx.OnyxObjects.GlobalUtils
import com.example.onyx.OnyxObjects.LoadingAnimation
import com.example.onyx.databinding.FragmentAnimeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.content.Context
import android.graphics.Color
import android.widget.FrameLayout
import android.widget.Toast
import com.google.android.material.card.MaterialCardView

class AnimeFragment : Fragment() {

    // ViewBinding variables
    private var _binding: FragmentAnimeBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: AppDatabase
    private lateinit var sm: SessionManger
    private var lastFocusedView: View? = null
    private lateinit var fetchAnimeAPI: AnimeApi
    private var userId: Int = -1
    private var urlHome = BuildConfig.A_K

    private lateinit var dubbedAdapter: AnimeGridAdapter
    private var lowestLoadedDubbedPage = 0
    private var highestLoadedDubbedPage = 0
    private val loadedDubbedPages = mutableSetOf<Int>()
    private var isLoadingNextDubbed = false
    private var isLoadingPrevDubbed = false

    private lateinit var trendingAdapter: AnimeTrendingAdapter
    private var lowestLoadedPage = 0
    private var highestLoadedPage = 0
    private val loadedPages = mutableSetOf<Int>()
    private var isLoadingNextTrending = false
    private var isLoadingPrevTrending = false

    private var isDataLoaded = false
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnimeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        GlobalUtils.applyTheme(requireActivity())

        view.viewTreeObserver.addOnGlobalFocusChangeListener { _, newFocus ->
            if (newFocus != null && view.findViewById<View>(newFocus.id) != null) {
                lastFocusedView = newFocus
            }
        }

        val prefs = requireActivity().getSharedPreferences("AnimePrefs", Context.MODE_PRIVATE)
        val savedDubbedPage = prefs.getInt("currentDubbedAnimePage", 1)
        highestLoadedDubbedPage = savedDubbedPage
        lowestLoadedDubbedPage = savedDubbedPage
        
        val savedTrendingPage = prefs.getInt("currentTrendingAnimePage", 1)
        highestLoadedPage = savedTrendingPage
        lowestLoadedPage = savedTrendingPage

        LoadingAnimation.setup(requireContext(), view, R.raw.line_loading)
        LoadingAnimation.show(view)

        db = AppDatabase(requireActivity())
        sm = SessionManger(requireActivity())
        fetchAnimeAPI = AnimeApi(requireActivity())

        userId = sm.getUserId()

        // Using Binding to access layout elements instantly
        binding.animeSpotlightSection.visibility = View.GONE
        binding.animeTrendingSection.visibility = View.GONE
        binding.animeAiringSection.visibility = View.GONE
        binding.dubbSection.visibility = View.GONE

        GlobalUtils.centerParentOnFocus(binding.activityScrollVIEW, binding.animeSpotlightSection)
        GlobalUtils.centerParentOnFocus(binding.activityScrollVIEW, binding.animeTrendingSection)
        GlobalUtils.centerParentOnFocus(binding.activityScrollVIEW, binding.animeAiringSection)
        GlobalUtils.centerParentOnFocus(binding.activityScrollVIEW, binding.dubbSection)



        val tvSpacing = (10 * resources.displayMetrics.density).toInt()

        // Dubbed Recycler Setup
        binding.dubbedRecycler.layoutManager = LinearLayoutManager(
            requireActivity(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.dubbedRecycler.addItemDecoration(EqualSpaceItemDecoration(tvSpacing))
        dubbedAdapter = AnimeGridAdapter(mutableListOf(), R.layout.anime_dubbed_item)
        binding.dubbedRecycler.adapter = dubbedAdapter

        dubbedAdapter.onAddMoreClicked = { loadNextDubbedAnime() }
        dubbedAdapter.onPositionFocused = { position ->
            val layoutManager = binding.dubbedRecycler.layoutManager as LinearLayoutManager
            val visibleItemCount = layoutManager.childCount
            val threshold = kotlin.math.max(5, visibleItemCount * 2)

            if (position >= dubbedAdapter.itemCount - threshold - 1) { // 1 for Add More
                loadNextDubbedAnime()
            } else if (position <= threshold) {
                loadPrevDubbedAnime()
            }
        }

        trendingAdapter = AnimeTrendingAdapter(mutableListOf(), R.layout.anime_trending_item)
        binding.AnimeTrendingWidget.layoutManager = LinearLayoutManager(
            requireActivity(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.AnimeTrendingWidget.adapter = trendingAdapter
        trendingAdapter.onAddMoreClicked = { loadNextTrendingAnime() }
        trendingAdapter.onItemFocused = { position ->
            val layoutManager = binding.AnimeTrendingWidget.layoutManager as LinearLayoutManager
            val visibleItemCount = layoutManager.childCount
            val threshold = kotlin.math.max(5, visibleItemCount * 2)

            if (position >= trendingAdapter.itemCount - threshold - 1) { // 1 for Add More
                loadNextTrendingAnime()
            } else if (position <= threshold) {
                loadPrevTrendingAnime()
            }
        }

        FocusOverlay<AnimeGridItem>(
            overlay = binding.dubbFixedFocusOverlay,
            recyclerView = binding.dubbedRecycler,
            adapter = dubbedAdapter
        ) { item ->
            projectDubbItemIntoHero(item)
        }

        animeHomeData()
        fetchDubbedAnime(highestLoadedDubbedPage, isPrepending = true)
        fetchTrendingAnime(highestLoadedPage, isPrepending = false)


        setupNetworkListener()
    }

    override fun onResume() {
        super.onResume()

        binding.root.post {
            if (lastFocusedView != null && lastFocusedView!!.isShown && lastFocusedView!!.isFocusable) {
                lastFocusedView!!.requestFocus()
            } else {
                binding.animeSpotlightSection.requestFocus()
            }
        }
    }

    private fun setupNetworkListener() {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                if (!isDataLoaded) {
                    // Back online and data isn't loaded! Run it again on the Main thread
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                        try { LoadingAnimation.show(requireView()) } catch (e: Exception) {}
                        animeHomeData()
                        if (dubbedAdapter.itemCount <= 1) {
                            loadNextDubbedAnime()
                        }
                        if (trendingAdapter.itemCount <= 1) {
                            loadNextTrendingAnime()
                        }
                    }
                }
            }
        }
        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
        } catch (e: Exception) {
            Log.e("AnimeFragment", "Failed to register network callback", e)
        }
    }

    private fun projectDubbItemIntoHero(item: AnimeGridItem) {
        try {

            // 1. Cross-fade the text content
            val fadeDuration = 250L
            binding.dubbOverlayTitle.animate().alpha(0f).setDuration(fadeDuration).withEndAction {
                binding.dubbOverlayTitle.text = item.title
                binding.dubbOverlayYear.text = GlobalUtils.formatDateString(item.releaseDate)
                binding.dubbOverlayRating.text = item.rating
                binding.dubbFixedFocusOverlay.alpha = 1f

                binding.dubbOverlayTitle.animate().alpha(1f).setDuration(fadeDuration).start()
                binding.dubbOverlayYear.animate().alpha(1f).setDuration(fadeDuration).start()
                binding.dubbOverlayRating.animate().alpha(1f).setDuration(fadeDuration).start()
            }.start()
            binding.dubbOverlayYear.animate().alpha(0f).setDuration(fadeDuration).start()
            binding.dubbOverlayRating.animate().alpha(0f).setDuration(fadeDuration).start()

            // 2. Load Image with Cross-fade Factory (avoids recycled bitmap crash)
            val factory = com.bumptech.glide.request.transition.DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
            Glide.with(this)
                .load(item.backdropUrl)
                .transition(DrawableTransitionOptions.withCrossFade(factory))
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding.dubbOverlayPoster)

        } catch (e: Exception) {
            Log.e("AnimeFragment", "Failed to project dub item: ${e.message}")
        }
    }

    private fun animeHomeData() {
        // Tied to viewLifecycleOwner to prevent memory leaks if fragment is closed early
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            val displayMetrics = resources.displayMetrics
            val screenHeight = displayMetrics.heightPixels

            val inflater = LayoutInflater.from(requireActivity())

            val params = binding.spotlightAnimes.layoutParams
            params.height = (screenHeight * 0.85).toInt()
            binding.spotlightAnimes.layoutParams = params

            val jsonObject = withContext(Dispatchers.IO) { fetchAnimeAPI.animeHome() }

            if (jsonObject == null) {
                LoadingAnimation.hide(requireView())
                Toast.makeText(requireContext(), "No Internet Connection", Toast.LENGTH_LONG).show()
                return@launch
            }
            
            isDataLoaded = true

            val (spotlightList, trendingList, airingList) = withContext(Dispatchers.IO) {
                val showHomeData = jsonObject.getJSONObject("data")
                val spotlightAnimes = showHomeData.getJSONArray("spotlightAnimes")
                val trendingAnimes = showHomeData.getJSONArray("trendingAnimes")
                val topAiringAnimes = showHomeData.getJSONArray("topAiringAnimes")

                val sList = mutableListOf<Map<String, String>>()
                for (i in 0 until spotlightAnimes.length()) {
                    val item = spotlightAnimes.getJSONObject(i)
                    sList.add(
                        mapOf(
                            "title" to item.getString("name"),
                            "overview" to item.getString("description"),
                            "imageUrl" to item.getString("backdrop"),
                            "id" to item.getString("id"),
                            "type" to item.getString("type"),
                            "runtime" to item.getString("duration"),
                            "releaseDate" to item.getString("releaseDate"),
                            "quality" to item.getString("quality"),
                            "sub" to item.getJSONObject("episodes").optInt("sub", 0).toString(),
                            "dub" to item.getJSONObject("episodes").optInt("dub", 0).toString()
                        )
                    )
                }
                val tList = mutableListOf<TrendingAnimeItem>()

                /*

                val tList = mutableListOf<TrendingAnimeItem>()
                for (i in 0 until trendingAnimes.length()) {
                    val item = trendingAnimes.getJSONObject(i)
                    tList.add(
                        TrendingAnimeItem(
                            id = item.getString("id"),
                            title = item.getString("name"),
                            imageUrl = item.getString("poster"),
                            rank = "0$i"
                        )

                    )
                }

                 */


                val aList = mutableListOf<AiringAnimeItem>()
                for (i in 0 until topAiringAnimes.length()) {
                    val item = topAiringAnimes.getJSONObject(i)
                    aList.add(
                        AiringAnimeItem(
                            id = item.getString("id"),
                            title = item.getString("name"),
                            imageUrl = item.getString("poster"),
                            type = item.getString("type"),
                            sub = item.getJSONObject("episodes").optString("sub", ""),
                            dub = item.getJSONObject("episodes").optString("dub", "")
                        )
                    )
                }
                
                Triple(sList, tList, aList)
            }

            if (spotlightList.isNotEmpty()) {
                binding.animeSpotlightSection.visibility = View.VISIBLE
            }

            for (item in spotlightList) {
                val card = inflater.inflate(
                    R.layout.anime_card_spotlight,
                    binding.spotlightAnimes,
                    false
                ) as MaterialCardView

                card.findViewById<TextView>(R.id.cardTitle).text = item["title"]
                card.findViewById<TextView>(R.id.cardPg).text = "PG-13"
                card.findViewById<TextView>(R.id.cardType).text = item["type"]
                card.findViewById<TextView>(R.id.cardRuntime).text = item["runtime"]
                card.findViewById<TextView>(R.id.cardYear).text = item["releaseDate"]
                card.findViewById<TextView>(R.id.cardQuality).text = item["quality"]
                card.findViewById<TextView>(R.id.cardSub).text = item["sub"]
                card.findViewById<TextView>(R.id.cardDub).text = item["dub"]
                card.findViewById<TextView>(R.id.cardOverview).text = item["overview"]

                val sliderBackdrop = card.findViewById<ImageView>(R.id.SliderBackdrop)
                val sliderImage = item["imageUrl"].toString()

                GlobalUtils.extractDynamicColor(requireContext(), sliderImage) { color ->

                    val blurContainerLeft = card.findViewById<LinearLayout>(R.id.blurContainerLeft)
                    val blurContainerBottom = card.findViewById<LinearLayout>(R.id.blurContainerBottom)

                    card.findViewById<FrameLayout>(R.id.CardBackground).setBackgroundColor(color)

                    val baseColor = (color and 0x00FFFFFF) or -0x1000000
                    val gradientDrawableLeft = android.graphics.drawable.GradientDrawable(
                        android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                        intArrayOf(baseColor, Color.TRANSPARENT, Color.TRANSPARENT)
                    )

                    val gradientDrawableBottom = android.graphics.drawable.GradientDrawable(
                        android.graphics.drawable.GradientDrawable.Orientation.BOTTOM_TOP,
                        intArrayOf(baseColor, Color.TRANSPARENT, Color.TRANSPARENT)
                    )

                    blurContainerLeft.background = gradientDrawableLeft
                    blurContainerBottom.background = gradientDrawableBottom
                }

                Glide.with(this@AnimeFragment)
                    .load(sliderImage)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerInside()
                    .into(sliderBackdrop)

                card.setOnClickListener {
                    val context = card.context
                    val args = Bundle().apply {
                        putString("anime_code", item["id"])
                        putString("anime_poster", item["imageUrl"])
                    }
                    (context as HomeActivity).navigateToFragment(WatchAnimeFragment(), args)
                }
                binding.spotlightAnimes.addView(card)
            }

            if (airingList.isNotEmpty()) {
                binding.animeAiringSection.visibility = View.VISIBLE
            }

            showAiring(airingList)

            //GlobalUtils.setupCardStackFromContainer(binding.spotlightAnimes)
            com.example.onyx.OnyxClasses.CardStack().setupCardStackFromContainer(binding.spotlightAnimes)
            LoadingAnimation.hide(requireView())

        }
    }

    private fun showAiring(airingItems: List<AiringAnimeItem>) {
        binding.AnimeAiringWidget.layoutManager = LinearLayoutManager(
            requireActivity(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.AnimeAiringWidget.adapter = AnimeAiringAdapter(airingItems.toMutableList(), R.layout.anime_airing_item)
    }

    private fun loadNextDubbedAnime() {
        if (isLoadingNextDubbed) return
        isLoadingNextDubbed = true
        dubbedAdapter.isLoadingNext = true
        
        val isInitialLoad = dubbedAdapter.itemCount <= 1
        val nextPage = if (isInitialLoad && highestLoadedDubbedPage > 0) highestLoadedDubbedPage else highestLoadedDubbedPage + 1
        
        Log.d("AnimeFragment", "Loading next dubbed anime page: $nextPage")
        fetchDubbedAnime(nextPage, isPrepending = false)
    }

    private fun loadPrevDubbedAnime() {
        if (isLoadingPrevDubbed || lowestLoadedDubbedPage <= 1) return
        isLoadingPrevDubbed = true
        val prevPage = lowestLoadedDubbedPage - 1
        Log.d("AnimeFragment", "Loading prev dubbed anime page: $prevPage")
        fetchDubbedAnime(prevPage, isPrepending = true)
    }

    private fun fetchDubbedAnime(pageToLoad: Int, isPrepending: Boolean) {
        if (loadedDubbedPages.contains(pageToLoad)) {
            if (isPrepending) isLoadingPrevDubbed = false
            else {
                isLoadingNextDubbed = false
                dubbedAdapter.isLoadingNext = false
            }
            return
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            repeat(3) { attempt ->
                try {
                    val url = "$urlHome/api/v2/anime/dubbed?page=$pageToLoad"
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 10_000
                    connection.readTimeout = 10_000

                    if (connection.responseCode !in 200..299) {
                        throw Exception("HTTP ${connection.responseCode}")
                    }

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    connection.disconnect()

                    val dubbedAnime = org.json.JSONObject(response).getJSONObject("data").getJSONArray("animes")
                    
                    if (dubbedAnime.length() == 0) {
                        if (!isPrepending && pageToLoad == 1) {
                            highestLoadedDubbedPage = 0
                            lowestLoadedDubbedPage = 0
                            val prefs = requireActivity().getSharedPreferences("AnimePrefs", Context.MODE_PRIVATE)
                            prefs.edit().putInt("currentDubbedAnimePage", highestLoadedDubbedPage).apply()
                        }
                        
                        withContext(Dispatchers.Main) {
                            if (isPrepending) isLoadingPrevDubbed = false
                            else {
                                isLoadingNextDubbed = false
                                dubbedAdapter.isLoadingNext = false
                            }
                        }
                        return@launch
                    }

                    val animeGridItems = mutableListOf<AnimeGridItem>()

                    for (i in 0 until dubbedAnime.length()) {
                        val item = dubbedAnime.getJSONObject(i)
                        animeGridItems.add(
                            AnimeGridItem(
                                id = item.optString("id", ""),
                                anilistId = item.optString("anilistId"),
                                malId = item.optString("malId"),
                                title = item.optString("name"),
                                japaneseTitle = item.optString("jname"),
                                poster = item.optString("poster"),
                                backdropUrl = item.optString("backdrop"),
                                description = item.optString("description"),
                                releaseDate = item.optString("releaseDate"),
                                type = item.optString("type"),
                                quality = item.optString("quality"),
                                status = item.optString("status"),
                                genres = emptyList(), // Omitted generic population for brevity
                                duration = item.optString("duration"),
                                sub = item.optJSONObject("episodes")?.optString("sub", "") ?: "",
                                dub = item.optJSONObject("episodes")?.optString("dub", "") ?: "",
                                rating = item.optString("rating", "")
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {
                        if (!isAdded || view == null) return@withContext
                        
                        loadedDubbedPages.add(pageToLoad)
                        
                        if (isPrepending) {
                            lowestLoadedDubbedPage = pageToLoad
                            
                            val layoutManager = binding.dubbedRecycler.layoutManager as LinearLayoutManager
                            val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
                            val firstVisibleView = layoutManager.findViewByPosition(firstVisiblePosition)
                            val offset = firstVisibleView?.left ?: 0

                            val focusedView = requireView().findFocus()
                            val focusedId = if (focusedView != null) {
                                val holder = binding.dubbedRecycler.findContainingViewHolder(focusedView)
                                holder?.itemId
                            } else null

                            dubbedAdapter.prependItems(animeGridItems.filter { it.id.isNotEmpty() })
                            
                            layoutManager.scrollToPositionWithOffset(firstVisiblePosition + animeGridItems.size, offset)

                            if (focusedId != null) {
                                requireView().post {
                                    for (i in 0 until binding.dubbedRecycler.childCount) {
                                        val child = binding.dubbedRecycler.getChildAt(i)
                                        if (binding.dubbedRecycler.getChildViewHolder(child).itemId == focusedId) {
                                            child.requestFocus()
                                            break
                                        }
                                    }
                                }
                            }
                            
                            isLoadingPrevDubbed = false
                        } else {
                            highestLoadedDubbedPage = pageToLoad
                            val prefs = requireActivity().getSharedPreferences("AnimePrefs", Context.MODE_PRIVATE)
                            prefs.edit().putInt("currentDubbedAnimePage", highestLoadedDubbedPage).apply()
                            
                            val isInitialLoad = dubbedAdapter.itemCount <= 1
                            dubbedAdapter.appendItems(animeGridItems.filter { it.id.isNotEmpty() })
                            isLoadingNextDubbed = false
                            dubbedAdapter.isLoadingNext = false

                            if (isInitialLoad) {
                                binding.dubbSection.visibility = View.VISIBLE
                                binding.dubbedRecycler.scrollToPosition(0)
                            }
                        }
                    }
                    return@launch
                } catch (e: Exception) {
                    Log.e("AnimeFragment", "Failed to fetch dubbed anime (attempt ${attempt + 1})", e)
                    if (attempt == 2) {
                        withContext(Dispatchers.Main) {
                            if (isPrepending) isLoadingPrevDubbed = false
                            else {
                                isLoadingNextDubbed = false
                                dubbedAdapter.isLoadingNext = false
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadNextTrendingAnime() {
        if (isLoadingNextTrending) return
        isLoadingNextTrending = true
        trendingAdapter.isLoadingNext = true
        
        val isInitialLoad = trendingAdapter.itemCount <= 1
        val nextPage = if (isInitialLoad && highestLoadedPage > 0) highestLoadedPage else highestLoadedPage + 1
        
        Log.d("AnimeFragment", "Loading next trending anime page: $nextPage")
        fetchTrendingAnime(nextPage, isPrepending = false)
    }

    private fun loadPrevTrendingAnime() {
        if (isLoadingPrevTrending || lowestLoadedPage <= 1) return
        isLoadingPrevTrending = true
        val prevPage = lowestLoadedPage - 1
        Log.d("AnimeFragment", "Loading prev trending anime page: $prevPage")
        fetchTrendingAnime(prevPage, isPrepending = true)
    }

    private fun fetchTrendingAnime(pageToLoad: Int, isPrepending: Boolean) {
        if (loadedPages.contains(pageToLoad)) {
            if (isPrepending) isLoadingPrevTrending = false
            else {
                isLoadingNextTrending = false
                trendingAdapter.isLoadingNext = false
            }
            return
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            repeat(3) { attempt ->
                try {
                    val url = "https://echo-anime.vercel.app/api/v2/anime/category/trending?page=$pageToLoad"
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 10_000
                    connection.readTimeout = 10_000

                    if (connection.responseCode !in 200..299) {
                        throw Exception("HTTP ${connection.responseCode}")
                    }

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    connection.disconnect()

                    val trendingAnime = org.json.JSONObject(response).getJSONObject("data").getJSONArray("animes")
                    
                    if (trendingAnime.length() == 0) {
                        if (!isPrepending && pageToLoad == 1) {
                            highestLoadedPage = 0
                            lowestLoadedPage = 0
                            val prefs = requireActivity().getSharedPreferences("AnimePrefs", Context.MODE_PRIVATE)
                            prefs.edit().putInt("currentTrendingAnimePage", highestLoadedPage).apply()
                        }
                        
                        withContext(Dispatchers.Main) {
                            if (isPrepending) isLoadingPrevTrending = false
                            else {
                                isLoadingNextTrending = false
                                trendingAdapter.isLoadingNext = false
                            }
                        }
                        return@launch
                    }

                    val trendingGridItems = mutableListOf<TrendingAnimeItem>()

                    for (i in 0 until trendingAnime.length()) {
                        val item = trendingAnime.getJSONObject(i)
                        val itemsPerPage = 20
                        val rankStr = ((pageToLoad - 1) * itemsPerPage + i + 1).toString().padStart(2, '0')
                        
                        trendingGridItems.add(
                            TrendingAnimeItem(
                                id = item.optString("id", ""),
                                title = item.optString("name"),
                                imageUrl = item.optString("poster"),
                                rank = rankStr,
                                sub = item.optJSONObject("episodes")?.optString("sub", "") ?: "",
                                dub = item.optJSONObject("episodes")?.optString("dub", "") ?: ""
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {
                        if (!isAdded || view == null) return@withContext
                        
                        loadedPages.add(pageToLoad)
                        
                        if (isPrepending) {
                            lowestLoadedPage = pageToLoad
                            
                            val layoutManager = binding.AnimeTrendingWidget.layoutManager as LinearLayoutManager
                            val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
                            val firstVisibleView = layoutManager.findViewByPosition(firstVisiblePosition)
                            val offset = firstVisibleView?.left ?: 0

                            val focusedView = requireView().findFocus()
                            val focusedId = if (focusedView != null) {
                                val holder = binding.AnimeTrendingWidget.findContainingViewHolder(focusedView)
                                holder?.itemId
                            } else null

                            trendingAdapter.prependItems(trendingGridItems.filter { it.id.isNotEmpty() })
                            
                            layoutManager.scrollToPositionWithOffset(firstVisiblePosition + trendingGridItems.size, offset)

                            if (focusedId != null) {
                                requireView().post {
                                    for (i in 0 until binding.AnimeTrendingWidget.childCount) {
                                        val child = binding.AnimeTrendingWidget.getChildAt(i)
                                        if (binding.AnimeTrendingWidget.getChildViewHolder(child).itemId == focusedId) {
                                            child.requestFocus()
                                            break
                                        }
                                    }
                                }
                            }
                            
                            isLoadingPrevTrending = false
                        } else {
                            highestLoadedPage = pageToLoad
                            val prefs = requireActivity().getSharedPreferences("AnimePrefs", Context.MODE_PRIVATE)
                            prefs.edit().putInt("currentTrendingAnimePage", highestLoadedPage).apply()
                            
                            val isInitialLoad = trendingAdapter.itemCount <= 1
                            trendingAdapter.appendItems(trendingGridItems.filter { it.id.isNotEmpty() })
                            isLoadingNextTrending = false
                            trendingAdapter.isLoadingNext = false

                            if (isInitialLoad) {
                                binding.animeTrendingSection.visibility = View.VISIBLE
                                binding.AnimeTrendingWidget.scrollToPosition(0)
                            }
                        }
                    }
                    return@launch
                } catch (e: Exception) {
                    Log.e("AnimeFragment", "Failed to fetch trending anime (attempt ${attempt + 1})", e)
                    if (attempt == 2) {
                        withContext(Dispatchers.Main) {
                            if (isPrepending) isLoadingPrevTrending = false
                            else {
                                isLoadingNextTrending = false
                                trendingAdapter.isLoadingNext = false
                            }
                        }
                    }
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        // Unregister network callback to prevent leaks
        networkCallback?.let {
            val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.unregisterNetworkCallback(it)
        }
        networkCallback = null
        
        // Remove closures to prevent memory leaks!
        dubbedAdapter.onPositionFocused = null
        dubbedAdapter.onAddMoreClicked = null
        dubbedAdapter.onItemFocused = null
        trendingAdapter.onItemFocused = null
        trendingAdapter.onAddMoreClicked = null
        
        dubbedAdapter.clearItems()
        trendingAdapter.clearItems()
        
        // Critical: Nullify binding to prevent memory leaks when fragment goes to backstack
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.coroutineContext.cancelChildren()
    }
}
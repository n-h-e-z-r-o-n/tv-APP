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
    private var currentDubbedAnimePage = 0
    private var isLoadingMoreDubbed = false

    private lateinit var faveAdapter: FavAdapter

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
        binding.favoriteSection.visibility = View.GONE

        GlobalUtils.centerParentOnFocus(binding.activityScrollVIEW, binding.animeSpotlightSection)
        GlobalUtils.centerParentOnFocus(binding.activityScrollVIEW, binding.animeTrendingSection)
        GlobalUtils.centerParentOnFocus(binding.activityScrollVIEW, binding.animeAiringSection)
        GlobalUtils.centerParentOnFocus(binding.activityScrollVIEW, binding.dubbSection)
        GlobalUtils.centerParentOnFocus(binding.activityScrollVIEW, binding.favoriteSection)

        // Activity views still require findViewById
        val homeAnimeBtn = requireActivity().findViewById<ImageView>(R.id.sidebarBtnAnime)
        val searchAnimeBtn = requireActivity().findViewById<ImageView>(R.id.sidebarSearchBtn)
        val cWatchAnimeBtn = requireActivity().findViewById<ImageView>(R.id.sidebarWatchListBtn)
        val cNotificationAnimeBtn = requireActivity().findViewById<ImageView>(R.id.sidebarNotificationBtn)

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

        FocusOverlay<AnimeGridItem>(
            overlay = binding.dubbFixedFocusOverlay,
            recyclerView = binding.dubbedRecycler,
            adapter = dubbedAdapter
        ) { item ->
            projectDubbItemIntoHero(item)
        }

        dubbedAdapter.onAddMoreClicked = { loadDubbedAnime() }

        // Fave Recycler Setup
        binding.faveRecycler.layoutManager = LinearLayoutManager(
            requireActivity(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.faveRecycler.addItemDecoration(EqualSpaceItemDecoration(tvSpacing))

        animeHomeData()
        loadDubbedAnime()
    }

    override fun onResume() {
        super.onResume()

        if (this::faveAdapter.isInitialized) {
            faveAdapter.clearItems()
        }

        animeFavoritesList()

        requireActivity().window.decorView.post {
            if (requireActivity().currentFocus == null) {
                if (lastFocusedView != null && lastFocusedView!!.isShown && lastFocusedView!!.isFocusable) {
                    lastFocusedView!!.requestFocus()
                } else {
                    // Assuming HomeBtn is in the parent activity or handled elsewhere
                    requireActivity().findViewById<LinearLayout>(R.id.HomeBtn)?.requestFocus()
                }
            }
        }
    }

    private fun trackFocus() {
        requireActivity().window.decorView.viewTreeObserver.addOnGlobalFocusChangeListener { _, newFocus ->
            if (newFocus != null) {
                lastFocusedView = newFocus
            }
        }
    }

    private fun projectDubbItemIntoHero(item: AnimeGridItem) {
        try {
            binding.dubbOverlayTitle.text = item.title
            binding.dubbOverlayYear.text = GlobalUtils.formatDateString(item.releaseDate)
            binding.dubbOverlayRating.text = item.rating
            binding.dubbFixedFocusOverlay.alpha = 1f

            Glide.with(this)
                .load(item.backdropUrl)
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

            if (jsonObject == null) return@launch

            val showHomeData = jsonObject.getJSONObject("data")
            val spotlightAnimes = showHomeData.getJSONArray("spotlightAnimes")
            val trendingAnimes = showHomeData.getJSONArray("trendingAnimes")
            val topAiringAnimes = showHomeData.getJSONArray("topAiringAnimes")

            if (spotlightAnimes.length() > 0) {
                binding.animeSpotlightSection.visibility = View.VISIBLE
            }

            for (i in 0 until spotlightAnimes.length()) {
                val card = inflater.inflate(
                    R.layout.anime_card_spotlight,
                    binding.spotlightAnimes,
                    false
                ) as CardView

                val item = spotlightAnimes.getJSONObject(i)
                val title = item.getString("name")
                val overview = item.getString("description")
                val imageUrl = item.getString("backdrop")
                val id = item.getString("id")
                val type = item.getString("type")
                val runtime = item.getString("duration")
                val releaseDate = item.getString("releaseDate")
                val quality = item.getString("quality")
                val sub = item.getJSONObject("episodes").optInt("sub", 0)
                val dub = item.getJSONObject("episodes").optInt("dub", 0)

                // Note: Still using findViewById here because this is a dynamically inflated child view,
                // not the main Fragment layout. (Consider making a binding for anime_card_spotlight.xml too)
                card.findViewById<TextView>(R.id.cardTitle).text = title
                card.findViewById<TextView>(R.id.cardPg).text = "PG-13"
                card.findViewById<TextView>(R.id.cardType).text = type
                card.findViewById<TextView>(R.id.cardRuntime).text = runtime
                card.findViewById<TextView>(R.id.cardYear).text = releaseDate
                card.findViewById<TextView>(R.id.cardQuality).text = quality
                card.findViewById<TextView>(R.id.cardSub).text = sub.toString()
                card.findViewById<TextView>(R.id.cardDub).text = dub.toString()
                card.findViewById<TextView>(R.id.cardOverview).text = overview

                val sliderBackdrop = card.findViewById<ImageView>(R.id.SliderBackdrop)

                Glide.with(this@AnimeFragment)
                    .load(imageUrl)
                    .centerInside()
                    .into(sliderBackdrop)

                card.setOnClickListener {
                    val context = card.context
                    val args = Bundle().apply {
                        putString("anime_code", id)
                        putString("anime_poster", imageUrl)
                    }
                    (context as HomeActivity).navigateToFragment(WatchAnimeFragment(), args)
                }
                binding.spotlightAnimes.addView(card)
            }

            if (trendingAnimes.length() > 0) {
                binding.animeTrendingSection.visibility = View.VISIBLE
            }
            if (topAiringAnimes.length() > 0) {
                binding.animeAiringSection.visibility = View.VISIBLE
            }

            showTrending(trendingAnimes)
            showAiring(topAiringAnimes)

            GlobalUtils.setupCardStackFromContainer(binding.spotlightAnimes)

            LoadingAnimation.hide(requireView())

        }
    }

    private fun showTrending(trending: JSONArray) {
        val trendingItems = mutableListOf<TrendingAnimeItem>()

        for (i in 0 until trending.length()) {

            val item = trending.getJSONObject(i)
            val title = item.getString("name")
            val imageUrl = item.getString("poster")
            val id = item.getString("id")
            val ranking = "0" + i

            trendingItems.add(
                TrendingAnimeItem(
                    id,
                    title,
                    imageUrl,
                    ranking
                )
            )

        }

        binding.AnimeTrendingWidget.layoutManager = LinearLayoutManager(
            requireActivity(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.AnimeTrendingWidget.adapter = AnimeTrendingAdapter(trendingItems, R.layout.anime_trending_item)
    }

    private fun showAiring(airing: JSONArray) {
        val airingItems = mutableListOf<AiringAnimeItem>()
        for (i in 0 until airing.length()) {
            val item = airing.getJSONObject(i)
            airingItems.add(
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

        binding.AnimeAiringWidget.layoutManager = LinearLayoutManager(
            requireActivity(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.AnimeAiringWidget.adapter = AnimeAiringAdapter(airingItems, R.layout.anime_airing_item)
    }

    private fun loadDubbedAnime() {
        if (isLoadingMoreDubbed) return
        isLoadingMoreDubbed = true
        dubbedAdapter.isLoadingMore = true
        currentDubbedAnimePage++
        fetchDubbedAnime()
    }

    private fun fetchDubbedAnime() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            repeat(3) { attempt ->
                try {
                    val url = "$urlHome/api/v2/anime/dubbed?page=$currentDubbedAnimePage"
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
                        val isInitialLoad = dubbedAdapter.itemCount <= 1
                        dubbedAdapter.addItems(animeGridItems.filter { it.id.isNotEmpty() })

                        if (isInitialLoad) {
                            binding.dubbSection.visibility = View.VISIBLE
                            binding.dubbedRecycler.scrollToPosition(0)
                        }
                        delay(3000)
                        dubbedAdapter.isLoadingMore = false
                        isLoadingMoreDubbed = false
                    }
                    return@launch
                } catch (e: Exception) {
                    if (attempt < 2) delay(2000)
                }
            }

            withContext(Dispatchers.Main) {
                if (!isAdded || view == null) return@withContext
                isLoadingMoreDubbed = false
                dubbedAdapter.isLoadingMore = false
            }
        }
    }

    private fun animeFavoritesList() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            val animeFavData = withContext(Dispatchers.IO) { db.getFavoriteAnime(userId) }
            val items = animeFavData.map { anime ->
                FavItem(
                    title = anime["name"] ?: "",
                    posterUrl = anime["poster"] ?: "",
                    backdropUrl = anime["poster"] ?: "",
                    releaseDate = anime["aired"] ?: "",
                    runtime = anime["duration"] ?: "",
                    overview = anime["description"] ?: "",
                    voteAverage = anime["rating"] ?: "",
                    genres = anime["genre"] ?: "",
                    production = "",
                    parentalGuide = anime["rating"] ?: "",
                    imdbCode = anime["anime_id"] ?: "",
                    showType = "anime"
                )
            }

            if (items.isNotEmpty()) {
                binding.favoriteSection.visibility = View.VISIBLE
                if (!::faveAdapter.isInitialized) {
                    faveAdapter = FavAdapter(items.toMutableList(), R.layout.square_card)
                    binding.faveRecycler.adapter = faveAdapter
                } else {
                    faveAdapter.updateItems(items)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Critical: Nullify binding to prevent memory leaks when fragment goes to backstack
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        dubbedAdapter.clearItems()
        if (this::faveAdapter.isInitialized) faveAdapter.clearItems()
        lifecycleScope.coroutineContext.cancelChildren()
    }
}
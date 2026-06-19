package com.example.onyx


import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.FocusFinder
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.EditText
import android.widget.LinearLayout
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import com.example.onyx.Database.AppDatabase
import com.example.onyx.Database.SessionManger
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.onyx.FetchData.AnimeApi
import com.example.onyx.OnyxClasses.AiringAnimeItem
import com.example.onyx.OnyxClasses.AnimeAiringAdapter
import com.example.onyx.OnyxClasses.AnimeGridAdapter
import com.example.onyx.OnyxClasses.AnimeGridItem
import com.example.onyx.OnyxClasses.AnimeSearchAdapter
import com.example.onyx.OnyxClasses.AnimeSearchItem
import com.example.onyx.OnyxClasses.AnimeTrendingAdapter
import com.example.onyx.OnyxClasses.CustomKeyboardManager
import com.example.onyx.OnyxClasses.EqualSpaceItemDecoration
import com.example.onyx.OnyxClasses.FavAdapter
import com.example.onyx.OnyxClasses.FavItem
import com.example.onyx.OnyxClasses.FocusOverlay
import com.example.onyx.OnyxClasses.NotificationAdapter
import com.example.onyx.OnyxClasses.NotificationItem
import com.example.onyx.OnyxClasses.OnSearchListener
import com.example.onyx.OnyxClasses.TrendingAnimeItem
import com.example.onyx.OnyxClasses.cWatchingAdapter
import com.example.onyx.OnyxObjects.GlobalUtils
import com.example.onyx.OnyxObjects.LoadingAnimation
import com.example.onyx.OnyxObjects.NavAction
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlin.String
import kotlin.math.abs
import kotlin.math.roundToInt


import androidx.fragment.app.Fragment
class AnimeFragment : Fragment(R.layout.fragment_anime) {

     private lateinit var db: AppDatabase
     private lateinit var  sm: SessionManger
    private var lastFocusedView: View? = null
    private lateinit var  fetchAnimeAPI: AnimeApi
     private var userId: Int = -1
     private var urlHome = BuildConfig.A_K      //private var urlHome = "http://192.168.100.22:4000"

    //----------------------------------------------------------------------------------------------
    private lateinit var dubbFixedFocusOverlay: MaterialCardView
    private lateinit var dubbedAdapter: AnimeGridAdapter
     private lateinit var dubbedRecyclerView: RecyclerView
     private var currentDubbedAnimePage = 0
     private var isLoadingMoreDubbed = false
     //----------------------------------------------------------------------------------------------


     private lateinit var popularAdapter: AnimeGridAdapter
     private lateinit var popularRecyclerView: RecyclerView
     private var currentPopularAnimePage = 0
     private var isLoadingMorePopular = false

     //----------------------------------------------------------------------------------------------

     private lateinit var RecentlyAdapter: AnimeGridAdapter
     private lateinit var RecentlyRecyclerView: RecyclerView
     private var currentRecentlyAnimePage = 0
     private var isLoadingMoreRecently = false

     //---------------------------------------------------------------------------------------------
    private lateinit var faveRecyclerView: RecyclerView
    private lateinit var faveAdapter: FavAdapter

   //----------------------------------------------------------------------------------------------

     override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
         super.onViewCreated(view, savedInstanceState)
         GlobalUtils.applyTheme(requireActivity())


         LoadingAnimation.setup(requireContext(), view, R.raw.line_loading)
         LoadingAnimation.show(view)


         db = AppDatabase(requireActivity())         // Initialize database==
         sm = SessionManger(requireActivity())
         fetchAnimeAPI = AnimeApi(requireActivity())

         userId = sm.getUserId()


         val activityScrollVIEW = requireView().findViewById<ScrollView>(R.id.activityScrollVIEW)


         val animeSpotlightSection = requireView().findViewById<LinearLayout>(R.id.animeSpotlightSection)
         val animeTrendingSection = requireView().findViewById<LinearLayout>(R.id.animeTrendingSection)
         val animeAiringSection = requireView().findViewById<LinearLayout>(R.id.animeAiringSection)
         val dubbSection = requireView().findViewById<LinearLayout>(R.id.dubbSection)
         val favoriteSection = requireView().findViewById<LinearLayout>(R.id.favoriteSection)

         animeSpotlightSection.visibility = View.GONE
         animeTrendingSection.visibility = View.GONE
         animeAiringSection.visibility = View.GONE
         dubbSection.visibility = View.GONE
         favoriteSection.visibility = View.GONE



         GlobalUtils.centerParentOnFocus(activityScrollVIEW, animeSpotlightSection)
         GlobalUtils.centerParentOnFocus(activityScrollVIEW, animeTrendingSection)
         GlobalUtils.centerParentOnFocus(activityScrollVIEW, animeAiringSection)
         GlobalUtils.centerParentOnFocus(activityScrollVIEW, dubbSection)
         GlobalUtils.centerParentOnFocus(activityScrollVIEW, favoriteSection)

         ///////////////////////////////////////////////////////////////////////////////////////////
         val homeAnimeBtn = requireActivity().findViewById<ImageView>(R.id.sidebarBtnAnime)
         val searchAnimeBtn = requireActivity().findViewById<ImageView>(R.id.sidebarSearchBtn)
         val cWatchAnimeBtn = requireActivity().findViewById<ImageView>(R.id.sidebarWatchListBtn)
         val cNotificationAnimeBtn = requireActivity().findViewById<ImageView>(R.id.sidebarNotificationBtn)



         val tvSpacing = (10 * resources.displayMetrics.density).toInt()

         ////////////////////////////////////////////////////////////////////////////////////////
         ////////////////////////////////////////////////////////////////////////////////////////



         dubbedRecyclerView = requireView().findViewById(R.id.dubbedRecycler)
         dubbFixedFocusOverlay = requireView().findViewById(R.id.dubbFixedFocusOverlay)

         dubbedRecyclerView.layoutManager =   LinearLayoutManager(
             requireActivity(),
             LinearLayoutManager.HORIZONTAL,
             false
         )
         dubbedRecyclerView.addItemDecoration(EqualSpaceItemDecoration(tvSpacing))
         dubbedAdapter = AnimeGridAdapter(mutableListOf(), R.layout.anime_dubbed_item)
         dubbedRecyclerView.adapter = dubbedAdapter


         FocusOverlay<AnimeGridItem>(
             overlay = dubbFixedFocusOverlay,
             recyclerView = dubbedRecyclerView,
             adapter = dubbedAdapter
         ) { item ->
             projectDubbItemIntoHero(item)
         }

         dubbedAdapter.onAddMoreClicked = { loadDubbedAnime() }




         //------------------------------------------------------------------------------------------

         faveRecyclerView = requireView().findViewById(R.id.faveRecycler)
         faveRecyclerView.layoutManager = LinearLayoutManager(
             requireActivity(),
             LinearLayoutManager.HORIZONTAL,
             false
         )

         faveRecyclerView.addItemDecoration(EqualSpaceItemDecoration(tvSpacing))


         ////////////////////////////////////////////////////////////////////////////////////////////
         ////////////////////////////////////////////////////////////////////////////////////////////



         animeHomeData()
         loadDubbedAnime()
         ////setupSearchUi()
         //loadPopularAnime()

     }






    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun projectDubbItemIntoHero(item: AnimeGridItem) {

        try {
            val overlayPoster = requireView().findViewById<ImageView>(R.id.dubbOverlayPoster)
            requireView().findViewById<TextView>(R.id.dubbOverlayTitle).text = item.title
            requireView().findViewById<TextView>(R.id.dubbOverlayYear).text = GlobalUtils.formatDateString(item.releaseDate)
            requireView().findViewById<TextView>(R.id.dubbOverlayRating).text = item.rating

            requireView().findViewById<MaterialCardView>(R.id.dubbFixedFocusOverlay).alpha = 1f


            val heroImage = item.backdropUrl

            Glide.with(requireActivity())
                .load(heroImage)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(overlayPoster)

        }catch (e: Exception){}

    }






    override fun onResume() {
        super.onResume()

        val rootView = requireActivity().window.decorView.rootView
        if (rootView.findFocus() == null) {
           // requireView().findViewById<LinearLayout>(R.id.HomeAnimeBtn).requestFocus()
        }


        if (this::faveAdapter.isInitialized) {
            faveAdapter.clearItems()
        }

        /*
            if(this::notificationAdapter.isInitialized){
                notificationAdapter.clearItems()
            }
            notificationS()
         */

        animeFavoritesList()


        // Only request focus if nothing has focus
        requireActivity().window.decorView.post {
            if (requireActivity().currentFocus == null) {
                if (lastFocusedView != null && lastFocusedView!!.isShown && lastFocusedView!!.isFocusable) {
                    lastFocusedView!!.requestFocus()
                } else {
                    requireView().findViewById<LinearLayout>(R.id.HomeBtn).requestFocus()
                }
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()

        // Clear all adapters
        dubbedAdapter.clearItems()
        faveAdapter.clearItems()

        // Cancel any running coroutines
        lifecycleScope.coroutineContext.cancelChildren()

        // Remove all handler callbacks
        Handler(Looper.getMainLooper()).removeCallbacksAndMessages(null)

        // requireActivity().finish()
    }

    private fun trackFocus() {
        requireActivity().window.decorView.viewTreeObserver.addOnGlobalFocusChangeListener { _, newFocus ->
            if (newFocus != null) {
                lastFocusedView = newFocus
            }
        }
    }

     private fun animeHomeData() {
         lifecycleScope.launch(Dispatchers.Main){
             val displayMetrics = resources.displayMetrics
             val screenWidth = displayMetrics.widthPixels     // in pixels
             val screenHeight = displayMetrics.heightPixels    // in pixels

             val inflater = LayoutInflater.from(requireActivity())

             val SpotlightContaner = requireView().findViewById<FrameLayout>(R.id.spotlightAnimes)

             val params = SpotlightContaner.layoutParams
             params.height = (screenHeight * 0.85).toInt()
             SpotlightContaner.layoutParams = params


             val jsonObject = withContext(Dispatchers.IO) {fetchAnimeAPI.animeHome()}

             if (jsonObject == null){
                 return@launch
             }

             Log.e("ANIME_STATUS HOME 2", jsonObject.toString())

             val ShowHomeData = jsonObject.getJSONObject("data")
             Log.e("ANIME_STATUS HOME 3", ShowHomeData.toString())


             val spotlightAnimes = ShowHomeData.getJSONArray("spotlightAnimes")
             val trendingAnimes = ShowHomeData.getJSONArray("trendingAnimes")
             val latestEpisodeAnimes = ShowHomeData.getJSONArray("spotlightAnimes")
             val top10Animes = ShowHomeData.getJSONArray("spotlightAnimes")
             val topAiringAnimes = ShowHomeData.getJSONArray("topAiringAnimes")
             val latestCompletedAnimes = ShowHomeData.getJSONArray("spotlightAnimes")

             if (spotlightAnimes.length() > 0) {
                 requireView().findViewById<LinearLayout>(R.id.animeSpotlightSection).visibility = View.VISIBLE
                 LoadingAnimation.hide(requireView())
             }

             for (i in 0 until spotlightAnimes.length()) {

                 val card = inflater.inflate(
                     R.layout.anime_card_spotlight,
                     SpotlightContaner,
                     false
                 ) as CardView


                 val item = spotlightAnimes.getJSONObject(i)
                 val title = item.getString("name")
                 val titleJ = item.getString("jname")
                 val overview = item.getString("description")
                 val imageUrl = item.getString("backdrop")
                 val id = item.getString("id")
                 val type = item.getString("type")
                 val runtime = item.getString("duration")
                 val releaseDate = item.getString("releaseDate")
                 val quality = item.getString("quality")
                 val sub = item.getJSONObject("episodes").optInt("sub", 0)
                 val dub = item.getJSONObject("episodes").optInt("dub", 0)

                 val genres = item.getJSONArray("genres")


                 card.findViewById<TextView>(R.id.cardTitle).text = title
                 card.findViewById<TextView>(R.id.cardPg).text = "PG-13"
                 card.findViewById<TextView>(R.id.cardType).text = type
                 card.findViewById<TextView>(R.id.cardRuntime).text = runtime
                 card.findViewById<TextView>(R.id.cardYear).text = releaseDate
                 card.findViewById<TextView>(R.id.cardQuality).text = quality
                 card.findViewById<TextView>(R.id.cardSub).text = sub.toString()
                 card.findViewById<TextView>(R.id.cardDub).text = dub.toString()
                 card.findViewById<TextView>(R.id.cardOverview).text = overview

                 val SliderBackdrop = card.findViewById<ImageView>(R.id.SliderBackdrop)

                 Glide.with(card.context)
                     .load(imageUrl)
                     .centerInside()
                     .into(SliderBackdrop)

                 card.setOnClickListener {
                     val context = card.context
                     val intent = android.content.Intent(context, Watch_Anime_Page::class.java)
                     intent.putExtra("anime_code", id)
                     intent.putExtra("anime_poster", imageUrl)
                     context.startActivity(intent)
                 }
                 SpotlightContaner.addView(card)
             }

             if (trendingAnimes.length() > 0) {
                 requireView().findViewById<LinearLayout>(R.id.animeTrendingSection).visibility = View.VISIBLE
                 LoadingAnimation.hide(requireView())
             }
             if (topAiringAnimes.length() > 0) {
                 requireView().findViewById<LinearLayout>(R.id.animeAiringSection).visibility = View.VISIBLE
                 LoadingAnimation.hide(requireView())

             }
             showTrending(trendingAnimes)
             showAiring(topAiringAnimes)

             GlobalUtils.setupCardStackFromContainer(SpotlightContaner)
         }
     }











    /////////////////////////////////////////////////////////////////////////////////////////////////

    private fun showTrending(trending: JSONArray) {

        var trendingItems = mutableListOf<TrendingAnimeItem>()
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

        Log.e("DEBUG_MAIN_Slider 1", trendingItems.toString())


        val recyclerView = requireView().findViewById<RecyclerView>(R.id.Anime_Trending_widget)
        recyclerView.layoutManager = LinearLayoutManager(
            requireActivity(),
            LinearLayoutManager.HORIZONTAL,
            false
        )
        recyclerView.adapter = AnimeTrendingAdapter(trendingItems, R.layout.anime_trending_item)
    }

     private fun showAiring(Airing: JSONArray) {

         var airingItems = mutableListOf<AiringAnimeItem>()

         for (i in 0 until Airing.length()) {


             val item = Airing.getJSONObject(i)
             val title = item.getString("name")
             val imageUrl = item.getString("poster")
             val id = item.getString("id")
             val type = item.getString("type")
             val sub = item.getJSONObject("episodes").optString("sub", "")
             val dub = item.getJSONObject("episodes").optString("dub", "")

             airingItems.add(
                 AiringAnimeItem(
                     id,
                     title,
                     imageUrl,
                     type,
                     sub,
                     dub
                 )
             )
         }

         Log.e("DEBUG_MAIN_Slider 1", airingItems.toString())

         val recyclerView = requireView().findViewById<RecyclerView>(R.id.Anime_Airing_widget)
         recyclerView.layoutManager = LinearLayoutManager(
             requireActivity(),
             LinearLayoutManager.HORIZONTAL,
             false
         )
         recyclerView.adapter = AnimeAiringAdapter(airingItems, R.layout.anime_airing_item)

     }

     ////////////////////////////////////////////////////////////////////////////////////////////////
     ////////////////////////////////////////////////////////////////////////////////////////////////
     private fun loadDubbedAnime() {
         Log.e("DEBUG_DubbedAnime1", "isLoadingMoreDubbed: $isLoadingMoreDubbed \n currentDubbedAnimePage: $currentDubbedAnimePage \n isLoadingMoreDubbedA: ${dubbedAdapter.isLoadingMore}")
         if (isLoadingMoreDubbed) return // Prevent multiple rapid clicks
         isLoadingMoreDubbed = true
         dubbedAdapter.isLoadingMore = true
         currentDubbedAnimePage++
         fetchDubbedAnime()
     }

    private fun fetchDubbedAnime() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            var lastError: String? = null

            repeat(3) { attempt ->
                try {
                    val url = "$urlHome/api/v2/anime/dubbed?page=$currentDubbedAnimePage"
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 10_000
                    connection.readTimeout = 10_000

                    val responseCode = connection.responseCode
                    if (responseCode !in 200..299) {
                        val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
                        throw Exception("HTTP $responseCode: $errorBody")
                    }

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    connection.disconnect()

                    val jsonObject = org.json.JSONObject(response)
                    val fData = jsonObject.getJSONObject("data")
                    val dubbedAnime = fData.getJSONArray("animes")

                    val animeGridItems = mutableListOf<AnimeGridItem>()

                    for (i in 0 until dubbedAnime.length()) {
                        val item = dubbedAnime.getJSONObject(i)

                        val animeGridItem = AnimeGridItem(
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
                            genres = buildList {
                                val genresArray = item.optJSONArray("genres")
                                if (genresArray != null) {
                                    for (j in 0 until genresArray.length()) {
                                        add(genresArray.getString(j))
                                    }
                                }
                            },
                            duration = item.optString("duration"),
                            sub = item.optJSONObject("episodes")?.optString("sub", "") ?: "",
                            dub = item.optJSONObject("episodes")?.optString("dub", "") ?: "",
                            rating = item.optString("rating", "")
                        )

                        if (animeGridItem.id.isNotEmpty()) {
                            animeGridItems.add(animeGridItem)
                        } else {
                            Log.e("DEBUG_DubbedAnime1", "Skipped item with missing id: $item")
                        }
                    }

                    withContext(Dispatchers.Main) {
                    if (!isAdded || view == null) return@withContext
                        //Log.e("DEBUG_DubbedAnime1", "Adding ${animeGridItems.size} items for page $currentDubbedAnimePage")

                        val isInitialLoad = dubbedAdapter.itemCount <= 1

                        dubbedAdapter.addItems(animeGridItems)

                        if (isInitialLoad) {
                            requireView().findViewById<LinearLayout>(R.id.dubbSection).visibility = View.VISIBLE
                            dubbedRecyclerView.scrollToPosition(0)
                            LoadingAnimation.hide(requireView())
                        }


                        delay(3000)
                        dubbedAdapter.isLoadingMore = false
                        isLoadingMoreDubbed = false
                    }

                    return@launch
                } catch (e: Exception) {
                    lastError = e.message
                    Log.e("DEBUG_TAG_ANIME", "Attempt $attempt failed: ${e.message}", e)
                    if (attempt < 2) delay(2000)
                }
            }

            Log.e("DEBUG_DubbedAnime1", "All attempts failed for page $currentDubbedAnimePage: $lastError")
            withContext(Dispatchers.Main) {
                    if (!isAdded || view == null) return@withContext
                isLoadingMoreDubbed = false
                dubbedAdapter.isLoadingMore = false
            }
        }
    }


    private fun animeFavoritesList() {
        lifecycleScope.launch(Dispatchers.Main) {

            val animeFavData = withContext(Dispatchers.IO) { db.getFavoriteAnime(userId)}

            //val items = mutableListOf<FavItem>()

            //for (anime in animeFavData) {

            val items = animeFavData.map { anime ->

                Log.d("Fav_anime", "anime_id: ${anime["anime_id"]}")
                Log.d("Fav_anime", "title: ${anime["name"]}")
                Log.d("Fav_anime", "poster: ${anime["poster"]}")
                Log.d("Fav_anime", "type: ${anime["type"]}")
                Log.d("Fav_anime", "seasons: ${anime["seasons"]}")
                Log.d("Fav_anime", "sub: ${anime["sub"]}")
                Log.d("Fav_anime", "dub: ${anime["dub"]}")

                val genres = anime["genre"] ?: ""

                    FavItem(
                        title = anime["name"] ?: "",
                        posterUrl = anime["poster"] ?: "",
                        backdropUrl = anime["poster"] ?: "",
                        releaseDate = anime["aired"] ?: "",
                        runtime = anime["duration"] ?: "",
                        overview = anime["description"] ?: "",
                        voteAverage = anime["rating"] ?: "",
                        genres = genres,
                        production = "",
                        parentalGuide = anime["rating"] ?: "",
                        imdbCode = anime["anime_id"] ?: "",
                        showType = "anime"
                    )

            }

            if(!items.isEmpty()){
                requireView().findViewById<LinearLayout>(R.id.favoriteSection).visibility = View.VISIBLE

                if (!::faveAdapter.isInitialized) {
                    faveAdapter = FavAdapter(
                        items.toMutableList(),
                        R.layout.square_card
                    )

                    faveRecyclerView.adapter = faveAdapter
                }else {
                    faveAdapter.updateItems(items)
                }

            }



        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////






    private fun setupBackPressedCallback() {
        requireActivity().onBackPressedDispatcher.addCallback(requireActivity(), object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {

            }
        })
    }

}
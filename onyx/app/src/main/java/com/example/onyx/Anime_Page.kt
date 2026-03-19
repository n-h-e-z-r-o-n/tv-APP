package com.example.onyx


import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.EditText
import android.widget.LinearLayout
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
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
import kotlin.String
import com.example.onyx.Database.AppDatabase
import com.example.onyx.Database.SessionManger
import com.bumptech.glide.Glide
import com.example.onyx.FetchData.AnimeApi
import com.example.onyx.OnyxClasses.AiringAnimeItem
import com.example.onyx.OnyxClasses.AnimeAiringAdapter
import com.example.onyx.OnyxClasses.AnimeGridAdapter
import com.example.onyx.OnyxClasses.AnimeSearchAdapter
import com.example.onyx.OnyxClasses.AnimeSearchItem
import com.example.onyx.OnyxClasses.AnimeTrendingAdapter
import com.example.onyx.OnyxClasses.CustomKeyboardManager
import com.example.onyx.OnyxClasses.EqualSpaceItemDecoration
import com.example.onyx.OnyxClasses.FavAdapter
import com.example.onyx.OnyxClasses.FavItem
import com.example.onyx.OnyxClasses.NotificationAdapter
import com.example.onyx.OnyxClasses.NotificationItem
import com.example.onyx.OnyxClasses.OnSearchListener
import com.example.onyx.OnyxClasses.TrendingAnimeItem
import com.example.onyx.OnyxClasses.cWatchingAdapter
import com.example.onyx.OnyxObjects.GlobalUtils
import com.example.onyx.OnyxObjects.LoadingAnimation
import com.example.onyx.OnyxObjects.NavAction
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren


class Anime_Page : AppCompatActivity() {

     private lateinit var db: AppDatabase
     private lateinit var  sm: SessionManger
    private var lastFocusedView: View? = null
    private lateinit var  fetchAnimeAPI: AnimeApi
     private var userId: Int = -1
     private var urlHome = BuildConfig.A_K      //private var urlHome = "http://192.168.100.22:4000"

    //----------------------------------------------------------------------------------------------
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
     private lateinit var searchAdapter: AnimeSearchAdapter
     private lateinit var searchRecyclerView: RecyclerView

     //----------------------------------------------------------------------------------------------

     private lateinit var watchRecyclerView: RecyclerView
     private lateinit var watchAdapter: cWatchingAdapter


    private lateinit var faveRecyclerView: RecyclerView
    private lateinit var faveAdapter: FavAdapter

   //-----------------------------------------------------------------------------------------------

    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var  notificationRecyclerView: RecyclerView

   //---------------------------------------------------------------------------------------------

     override fun onCreate(savedInstanceState: Bundle?) {
         super.onCreate(savedInstanceState)
         GlobalUtils.applyTheme(this)
         enableEdgeToEdge()
         setContentView(R.layout.activity_anime_page)
         window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

         NavAction.setupSidebar(this@Anime_Page)
         LoadingAnimation.setup(this@Anime_Page, R.raw.line_loading)
         LoadingAnimation.show(this@Anime_Page)


         db = AppDatabase(this)         // Initialize database==
         sm = SessionManger(this)
         fetchAnimeAPI = AnimeApi(this)

         userId = sm.getUserId()



         ////////////////////////////////////////////////////////////////////////////////////////

         val navBar = findViewById<LinearLayout>(R.id.NavBar)
         val homeAnimeBtn = findViewById<LinearLayout>(R.id.HomeAnimeBtn)
         val favAnimeBtn = findViewById<LinearLayout>(R.id.FavAnimeBtn)
         val searchAnimeBtn = findViewById<LinearLayout>(R.id.SearchAnimeBtn)
         val popularAnimeBtn = findViewById<LinearLayout>(R.id.PopularAnimeBtn)
         val dubbedAnimeBtn = findViewById<LinearLayout>(R.id.DubbedAnimeBtn)
         val cWatchAnimeBtn = findViewById<LinearLayout>(R.id.cWatchAnimeBtn)
         val cNotificationAnimeBtn = findViewById<LinearLayout>(R.id.cNotificationAnimeBtn)

         val homePageAnime = findViewById<LinearLayout>(R.id.HomePageAnime)
         val dubbedPageAnime = findViewById<LinearLayout>(R.id.DubbedPageAnime)
         val popularPageAnime = findViewById<LinearLayout>(R.id.PopularPageAnime)
         val searchPageAnime = findViewById<LinearLayout>(R.id.SearchPageAnime)
         val favPageAnime = findViewById<LinearLayout>(R.id.FavPageAnime)
         val cWatchedPageAnime = findViewById<LinearLayout>(R.id.WatchedListPageAnime)
         val notificationPageAnime = findViewById<LinearLayout>(R.id.notificationPageAnime)





         homeAnimeBtn.setOnClickListener {
             homePageAnime.visibility = View.VISIBLE
             dubbedPageAnime.visibility = View.GONE
             popularPageAnime.visibility = View.GONE
             searchPageAnime.visibility = View.GONE
             cWatchedPageAnime.visibility = View.GONE
             favPageAnime.visibility = View.GONE
             notificationPageAnime.visibility = View.GONE


             homeAnimeBtn.isSelected = true
             dubbedAnimeBtn.isSelected = false
             popularAnimeBtn.isSelected = false
             searchAnimeBtn.isSelected = false
             favAnimeBtn.isSelected = false
             cWatchAnimeBtn.isSelected = false
             cNotificationAnimeBtn.isSelected = false
         }

         homeAnimeBtn.performClick()
         homeAnimeBtn.requestFocus()


         favAnimeBtn.setOnClickListener {
             homePageAnime.visibility = View.GONE
             dubbedPageAnime.visibility = View.GONE
             popularPageAnime.visibility = View.GONE
             searchPageAnime.visibility = View.GONE
             cWatchedPageAnime.visibility = View.GONE
             favPageAnime.visibility = View.VISIBLE
             notificationPageAnime.visibility = View.GONE


             homeAnimeBtn.isSelected = false
             dubbedAnimeBtn.isSelected = false
             popularAnimeBtn.isSelected = false
             searchAnimeBtn.isSelected = false
             favAnimeBtn.isSelected = true
             cWatchAnimeBtn.isSelected = false
             cNotificationAnimeBtn.isSelected = false
         }

         searchAnimeBtn.setOnClickListener {
             homePageAnime.visibility = View.GONE
             dubbedPageAnime.visibility = View.GONE
             popularPageAnime.visibility = View.GONE
             searchPageAnime.visibility = View.VISIBLE
             cWatchedPageAnime.visibility = View.GONE
             favPageAnime.visibility = View.GONE
             notificationPageAnime.visibility = View.GONE


             homeAnimeBtn.isSelected = false
             dubbedAnimeBtn.isSelected = false
             popularAnimeBtn.isSelected = false
             searchAnimeBtn.isSelected = true
             favAnimeBtn.isSelected = false
             cWatchAnimeBtn.isSelected = false

         }

         cWatchAnimeBtn.setOnClickListener {
             homePageAnime.visibility = View.GONE
             dubbedPageAnime.visibility = View.GONE
             popularPageAnime.visibility = View.GONE
             searchPageAnime.visibility = View.GONE
             cWatchedPageAnime.visibility = View.VISIBLE
             favPageAnime.visibility = View.GONE
             notificationPageAnime.visibility = View.GONE


             homeAnimeBtn.isSelected = false
             dubbedAnimeBtn.isSelected = false
             popularAnimeBtn.isSelected = false
             searchAnimeBtn.isSelected = false
             favAnimeBtn.isSelected = false
             cWatchAnimeBtn.isSelected = true
             cNotificationAnimeBtn.isSelected = false
         }

         popularAnimeBtn.setOnClickListener {
             homePageAnime.visibility = View.GONE
             dubbedPageAnime.visibility = View.GONE
             popularPageAnime.visibility = View.VISIBLE
             searchPageAnime.visibility = View.GONE
             cWatchedPageAnime.visibility = View.GONE
             favPageAnime.visibility = View.GONE
             notificationPageAnime.visibility = View.GONE


             homeAnimeBtn.isSelected = false
             dubbedAnimeBtn.isSelected = false
             popularAnimeBtn.isSelected = true
             searchAnimeBtn.isSelected = false
             favAnimeBtn.isSelected = false
             cWatchAnimeBtn.isSelected = false
             cNotificationAnimeBtn.isSelected = false
         }

         dubbedAnimeBtn.setOnClickListener {
             homePageAnime.visibility = View.GONE
             dubbedPageAnime.visibility = View.VISIBLE
             popularPageAnime.visibility = View.GONE
             searchPageAnime.visibility = View.GONE
             cWatchedPageAnime.visibility = View.GONE
             favPageAnime.visibility = View.GONE
             notificationPageAnime.visibility = View.GONE


             homeAnimeBtn.isSelected = false
             dubbedAnimeBtn.isSelected = true
             popularAnimeBtn.isSelected = false
             searchAnimeBtn.isSelected = false
             favAnimeBtn.isSelected = false
             cWatchAnimeBtn.isSelected = false
             cNotificationAnimeBtn.isSelected = false

         }

         cNotificationAnimeBtn.setOnClickListener {
             homePageAnime.visibility = View.GONE
             dubbedPageAnime.visibility = View.GONE
             popularPageAnime.visibility = View.GONE
             searchPageAnime.visibility = View.GONE
             cWatchedPageAnime.visibility = View.GONE
             favPageAnime.visibility = View.GONE
             notificationPageAnime.visibility = View.VISIBLE


             homeAnimeBtn.isSelected = false
             dubbedAnimeBtn.isSelected = false
             popularAnimeBtn.isSelected = false
             searchAnimeBtn.isSelected = false
             favAnimeBtn.isSelected = false
             cWatchAnimeBtn.isSelected = false
             cNotificationAnimeBtn.isSelected = true

             findViewById<CardView>(R.id.cNotificationAnimeIcon).visibility = View.GONE
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



         ////////////////////////////////////////////////////////////////////////////////////////
         ////////////////////////////////////////////////////////////////////////////////////////


         val tvSpacing = (10 * resources.displayMetrics.density).toInt()
         //------------------------------------------------------------------------------------------
         val anime_airing_item_width = 160
         val gapUsed = 70

         dubbedRecyclerView = findViewById(R.id.dubbedRecycler)
         dubbedRecyclerView.layoutManager = object : GridLayoutManager(
             this@Anime_Page,
             GlobalUtils.calculateSpanCountV2(this@Anime_Page, anime_airing_item_width, gapUsed)
         ){
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
                             dubbedRecyclerView.scrollToPosition(nextRowFirstPos)
                             focused
                         }
                     }
                 }

                 return super.onInterceptFocusSearch(focused, direction)
             }
         }



         dubbedRecyclerView.addItemDecoration(EqualSpaceItemDecoration(tvSpacing))
         dubbedAdapter = AnimeGridAdapter(mutableListOf(), R.layout.anime_airing_item)
         dubbedRecyclerView.adapter = dubbedAdapter
         dubbedAdapter.onAddMoreClicked = { loadDubbedAnime() }


         popularRecyclerView = findViewById(R.id.popularRecycler)
         popularRecyclerView.layoutManager = object :GridLayoutManager(
             this@Anime_Page,
             GlobalUtils.calculateSpanCountV2(this@Anime_Page, anime_airing_item_width, gapUsed)
         ){
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
                             popularRecyclerView.scrollToPosition(nextRowFirstPos)
                             focused
                         }
                     }
                 }
                 return super.onInterceptFocusSearch(focused, direction)
             }
         }
         popularRecyclerView.addItemDecoration(EqualSpaceItemDecoration(tvSpacing))
         popularAdapter = AnimeGridAdapter(mutableListOf(), R.layout.anime_airing_item)
         popularRecyclerView.adapter = popularAdapter
         popularAdapter.onAddMoreClicked = { loadPopularAnime() }



         //-----------------------------------------------------------------------------------------

         searchRecyclerView = findViewById(R.id.SearchRecycler)
         searchRecyclerView.layoutManager =  object : GridLayoutManager(this@Anime_Page, 3){

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
                             searchRecyclerView.scrollToPosition(nextRowFirstPos)
                             focused
                         }
                     }
                 }

                 return super.onInterceptFocusSearch(focused, direction)
             }

         }
         searchAdapter  = AnimeSearchAdapter(mutableListOf(), R.layout.anime_airing_item)
         searchRecyclerView.adapter = searchAdapter
         searchRecyclerView.addItemDecoration(EqualSpaceItemDecoration(tvSpacing))

         //------------------------------------------------------------------------------------------


         watchRecyclerView = findViewById(R.id.watchingRecycler)
         watchRecyclerView.layoutManager = GridLayoutManager(this@Anime_Page, GlobalUtils.calculateSpanCountV2(this@Anime_Page,160,gapUsed))
         watchRecyclerView.addItemDecoration(EqualSpaceItemDecoration(tvSpacing))


         //------------------------------------------------------------------------------------------

         faveRecyclerView = findViewById(R.id.faveRecycler)
         /*
         faveRecyclerView.layoutManager = LinearLayoutManager(
             this,
             LinearLayoutManager.HORIZONTAL,
             false
         )
          */
         faveRecyclerView.layoutManager = GridLayoutManager(this@Anime_Page, GlobalUtils.calculateSpanCountV2(this@Anime_Page,120,gapUsed))
         faveRecyclerView.addItemDecoration(EqualSpaceItemDecoration(tvSpacing))

         //------------------------------------------------------------------------------------------

         notificationRecyclerView = findViewById<RecyclerView>(R.id.notificationRecycler)
         notificationRecyclerView.layoutManager = LinearLayoutManager(this)
         val clearBtn = findViewById<TextView>(R.id.clearNotBtn)

         clearBtn.setOnClickListener {
             lifecycleScope.launch(Dispatchers.IO) {
                 db.clearAllAnimeNotifications(userId)
             }
             notificationAdapter.clearItems()
         }


         ////////////////////////////////////////////////////////////////////////////////////////////
         ////////////////////////////////////////////////////////////////////////////////////////////

        setupSearchUi()
        animeHomeData()
        loadDubbedAnime()
        loadPopularAnime()
        animeWatchedList()
        notificationS()

     }


    override fun onResume() {
        super.onResume()

        val rootView = window.decorView.rootView
        if (rootView.findFocus() == null) {
            findViewById<LinearLayout>(R.id.HomeAnimeBtn).requestFocus()
        }

        if (this::watchAdapter.isInitialized) {
            watchAdapter.clearItems()
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

        animeWatchedList()
        animeFavoritesList()


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
        dubbedAdapter.clearItems()
        popularAdapter.clearItems()
        searchAdapter.clearItems()
        watchAdapter.clearItems()
        faveAdapter.clearItems()

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

     private fun animeHomeData() {
         lifecycleScope.launch(Dispatchers.Main){
             val displayMetrics = resources.displayMetrics
             val screenWidth = displayMetrics.widthPixels     // in pixels
             val screenHeight = displayMetrics.heightPixels    // in pixels

             val inflater = LayoutInflater.from(this@Anime_Page)

             val SpotlightContaner = findViewById<FrameLayout>(R.id.spotlightAnimes)

             val params = SpotlightContaner.layoutParams
             params.height = (screenHeight * 0.75).toInt()
             SpotlightContaner.layoutParams = params


             val jsonObject = withContext(Dispatchers.IO) {fetchAnimeAPI.animeHome()}

             if (jsonObject == null){
                 LoadingAnimation.setup(this@Anime_Page, R.raw.error)
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



             for (i in 0 until spotlightAnimes.length()) {

                 val card = inflater.inflate(
                     R.layout.anime_card_spotlight,
                     SpotlightContaner,
                     false
                 ) as CardView


                 val item = spotlightAnimes.getJSONObject(i)
                 val title = item.getString("name")
                 val overview = item.getString("description")
                 val imageUrl = item.getString("poster")
                 val id = item.getString("id")
                 val type = item.getString("type")
                 val runtime = item.optJSONArray("otherInfo").optString(1, "")
                 val release_date = item.optJSONArray("otherInfo").optString(2, "")
                 val quality = item.optJSONArray("otherInfo").optString(3, "")
                 val sub = item.getJSONObject("episodes").optInt("sub", 0)
                 val dub = item.getJSONObject("episodes").optInt("dub", 0)

                 card.findViewById<TextView>(R.id.cardTitle).text = title
                 card.findViewById<TextView>(R.id.cardPg).text = "PG-13"
                 card.findViewById<TextView>(R.id.cardType).text = type
                 card.findViewById<TextView>(R.id.cardRuntime).text = runtime
                 card.findViewById<TextView>(R.id.cardYear).text = release_date
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

             showTrending(trendingAnimes)
             showAiring(topAiringAnimes)

             GlobalUtils.setupCardStackFromContainer(SpotlightContaner)
             LoadingAnimation.hide(this@Anime_Page)
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
             val ranking = "0" + item.getString("rank")


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


         val recyclerView = findViewById<RecyclerView>(R.id.Anime_Trending_widget)
         recyclerView.layoutManager = LinearLayoutManager(
             this@Anime_Page,
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

         val recyclerView = findViewById<RecyclerView>(R.id.Anime_Airing_widget)
         recyclerView.layoutManager = LinearLayoutManager(
             this@Anime_Page,
             LinearLayoutManager.HORIZONTAL,
             false
         )
         recyclerView.adapter = AnimeAiringAdapter(airingItems, R.layout.anime_airing_item)

     }

     ////////////////////////////////////////////////////////////////////////////////////////////////
     ////////////////////////////////////////////////////////////////////////////////////////////////

     private fun loadDubbedAnime() {
         if (isLoadingMoreDubbed) return // Prevent multiple rapid clicks
         currentDubbedAnimePage++
         fetchDubbedAnime()
     }

     private fun fetchDubbedAnime() {
         isLoadingMoreDubbed = true
         CoroutineScope(Dispatchers.IO).launch {

             repeat(5) { attempt ->
                 try {
                     val url =
                         "$urlHome/api/v2/hianime/category/dubbed-anime?page=$currentDubbedAnimePage"
                     val connection = URL(url).openConnection() as HttpURLConnection
                     connection.requestMethod = "GET"
                     val response = connection.inputStream.bufferedReader().use { it.readText() }
                     val jsonObject = org.json.JSONObject(response)
                     val fData = jsonObject.getJSONObject("data")

                     Log.e("DEBUG_DubbedAnime1", "${fData}")

                     val dubbedAnime = fData.getJSONArray("animes")
                     Log.e("DEBUG_DubbedAnime2", "${fData}")

                     val airingItems = mutableListOf<AiringAnimeItem>()

                     for (i in 0 until dubbedAnime.length()) {

                         val item = dubbedAnime.getJSONObject(i)
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

                         val movieItem = AiringAnimeItem(
                             id,
                             title,
                             imageUrl,
                             type,
                             sub,
                             dub
                         )

                         withContext(Dispatchers.Main) {
                             dubbedAdapter.addItem(movieItem)
                             isLoadingMoreDubbed = false
                         }
                     }

                     return@launch
                 } catch (e: java.net.ConnectException) {

                     Log.e("DEBUG_TAG_ANIME dubbed", "Connect Error", e)
                     currentRecentlyAnimePage--
                     return@repeat
                 } catch (e: Exception) {
                     Log.e("DEBUG_TAG_ANIME dubbed", "Attempt ${attempt + 1} failed", e)
                     delay(10_000)
                     currentDubbedAnimePage--
                 }
                 withContext(Dispatchers.Main) {
                     isLoadingMoreDubbed = false
                 }
             }
         }
     }

     ////////////////////////////////////////////////////////////////////////////////////////////////

     private fun loadPopularAnime() {
         if (isLoadingMorePopular) return // Prevent multiple rapid clicks
         currentPopularAnimePage++
         fetchPopularAnime()
     }

     private fun fetchPopularAnime() {
         isLoadingMorePopular = true
         CoroutineScope(Dispatchers.IO).launch {

             repeat(5) { attempt ->
                 try {
                     val url =
                         "$urlHome/api/v2/hianime/category/most-popular?page=$currentPopularAnimePage"
                     val connection = URL(url).openConnection() as HttpURLConnection
                     connection.requestMethod = "GET"
                     val response = connection.inputStream.bufferedReader().use { it.readText() }
                     val jsonObject = org.json.JSONObject(response)
                     val fData = jsonObject.getJSONObject("data")

                     val popularAnime = fData.getJSONArray("animes")
                     //Log.e("DEBUG_TAG_ANIME Popular", "Data${fData}")

                     val airingItems = mutableListOf<AiringAnimeItem>()

                     for (i in 0 until popularAnime.length()) {

                         val item = popularAnime.getJSONObject(i)
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

                         val movieItem = AiringAnimeItem(
                             id,
                             title,
                             imageUrl,
                             type,
                             sub,
                             dub
                         )

                         withContext(Dispatchers.Main) {
                             popularAdapter.addItem(movieItem)
                             isLoadingMorePopular = false
                         }
                     }

                     return@launch
                 } catch (e: java.net.ConnectException) {

                     Log.e("DEBUG_TAG_ANIME Popular", "Connect Error", e)
                     currentPopularAnimePage--
                     return@repeat
                 } catch (e: Exception) {
                     Log.e("DEBUG_TAG_ANIME Popular", "Attempt ${attempt + 1} failed", e)
                     delay(10_000)
                     currentPopularAnimePage--
                 }
                 withContext(Dispatchers.Main) {
                     isLoadingMorePopular = false
                 }
             }
         }
     }
     ////////////////////////////////////////////////////////////////////////////////////////////////
     ////////////////////////////////////////////////////////////////////////////////////////////////

     private fun loadRecentlyAnime() {
         if (isLoadingMoreRecently) return // Prevent multiple rapid clicks
         currentRecentlyAnimePage++
         fetchRecentlyAnime()
     }


     private fun fetchRecentlyAnime() {
         isLoadingMoreDubbed = true
         CoroutineScope(Dispatchers.IO).launch {

             repeat(5) { attempt ->
                 try {
                     val url =
                         "$urlHome/api/v2/hianime/category/recently-updated?page=$currentRecentlyAnimePage"
                     val connection = URL(url).openConnection() as HttpURLConnection
                     connection.requestMethod = "GET"
                     val response = connection.inputStream.bufferedReader().use { it.readText() }
                     val jsonObject = org.json.JSONObject(response)
                     val fData = jsonObject.getJSONObject("data")

                     Log.e("DEBUG_DubbedAnime1", "${fData}")
                     val dubbedAnime = fData.getJSONArray("animes")
                     Log.e("DEBUG_DubbedAnime2", "${fData}")
                     val airingItems = mutableListOf<AiringAnimeItem>()
                     for (i in 0 until dubbedAnime.length()) {
                         val item = dubbedAnime.getJSONObject(i)
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


                         val movieItem = AiringAnimeItem(
                             id,
                             title,
                             imageUrl,
                             type,
                             sub,
                             dub
                         )

                         withContext(Dispatchers.Main) {
                             RecentlyAdapter.addItem(movieItem)
                             isLoadingMoreRecently = false
                         }
                     }

                     return@launch
                 } catch (e: java.net.ConnectException) {

                     Log.e("DEBUG_TAG_ANIME recent", "Connect Error", e)
                     currentRecentlyAnimePage--
                     return@repeat

                 } catch (e: java.io.FileNotFoundException) {

                     Log.e(
                         "DEBUG_TAG_ANIME recent",
                         "URL Not Found (404/403) at page $currentRecentlyAnimePage",
                         e
                     )
                     currentRecentlyAnimePage--
                     return@repeat
                 } catch (e: Exception) {
                     Log.e("DEBUG_TAG_ANIME recent", "Attempt ${attempt + 1} failed", e)
                     delay(10_000)
                     currentRecentlyAnimePage--
                 }
                 withContext(Dispatchers.Main) {
                 }
             }
         }

     }
////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////

         private  fun searchAnimeFetch(searchTerm:String){
             val searchTextDisplay = findViewById<TextView>(R.id.searchTextDisplay)

             searchAdapter.clearItems()

             CoroutineScope(Dispatchers.IO).launch {
                 repeat(1) { attempt ->
                     try {

                         val url = "$urlHome/api/v2/hianime/search?q=$searchTerm&page=1"
                         val connection = URL(url).openConnection() as HttpURLConnection
                         connection.requestMethod = "GET"
                         connection.setRequestProperty("accept", "application/json")

                         val response = connection.inputStream.bufferedReader().use { it.readText() }
                         Log.e("ANIME_STATUS search", response.toString())

                         val jsonObject = org.json.JSONObject(response)
                         Log.e("ANIME_STATUS search", jsonObject.toString())
                         val dataFetch = jsonObject.getJSONObject("data")
                         val  searchData = dataFetch.getJSONArray("animes")
                         Log.e("ANIME_STATUS SEARCH-R", dataFetch.toString())
                         var searchDataItmes = mutableListOf<AnimeSearchItem>()


                         withContext(Dispatchers.Main) {
                             if(searchData.length() == 0) {
                                 searchTextDisplay.text = "No Results Found"
                             }else{
                                 searchTextDisplay.text = "Search results for: $searchTerm"
                             }
                         }



                         for (i in 0 until searchData.length()) {
                             val item = searchData.getJSONObject(i)
                             val title = item.getString("name")
                             val imageUrl = item.getString("poster")
                             val id = item.getString("id")
                             val type = item.getString("type")
                             val sub = item.getJSONObject("episodes").optString("sub", "")
                             val dub = item.getJSONObject("episodes").optString("dub", "")



                             val searchItem = AnimeSearchItem(
                                 id,
                                 title,
                                 imageUrl,
                                 type,
                                 sub,
                                 dub,
                             )

                             withContext(Dispatchers.Main) {
                                 searchAdapter.addItem(searchItem)
                             }

                         }


                         return@launch
                     } catch (e: Exception) {
                         delay(20_000)
                         Log.e("ANIME_STATUS S-Error", "Error fetching data", e)
                         return@launch
                     }

                 }
             }
             isLoadingMoreRecently = false
         }


    private fun setupSearchUi() {

        val searchInput = findViewById<EditText>(R.id.AnimeSearchInput)
        val keyboardLayout = findViewById<LinearLayout>(R.id.keyboard_layout)
        val keyboardManager = CustomKeyboardManager(
            this,
            searchInput,
            keyboardLayout,
            object : OnSearchListener {
                override fun EnterActionTrigger(query: String) {
                    val searchTerm = query.trim()
                    if (searchTerm.isNotEmpty()) {
                        searchAnimeFetch(searchTerm)
                    }
                }
            }
        )
        keyboardManager.showKeyboard()
        //keyboardManager.hideKeyboard()
        keyboardManager.isKeyboardVisible()

    }



     ///////////////////////////////////////////////////////////////////////////////////////////////
     ///////////////////////////////////////////////////////////////////////////////////////////////

        private fun animeWatchedList(){
            lifecycleScope.launch(Dispatchers.Main) {
                val userId = sm.getUserId()
                val cWatching = withContext(Dispatchers.IO) {
                    val mv = async {db.getContinueWatchingAll(userId, "anime")}
                    mv.await()
                }

                if (!::watchAdapter.isInitialized) {
                    watchAdapter = cWatchingAdapter(
                        cWatching,
                        R.layout.item_watched
                    )
                    watchRecyclerView.adapter = watchAdapter
                }else {
                    watchAdapter.updateItems(cWatching)
                }
            }

        }

    private fun animeFavoritesList() {
        lifecycleScope.launch(Dispatchers.Main) {

            val FavBackdrop: ImageView = findViewById(R.id.FavBackdrop)
            val FavTitle: TextView = findViewById(R.id.FavTitle)
            val FavGenre: TextView = findViewById(R.id.FavGenre)
            val FavType: TextView = findViewById(R.id.FavType)
            val FavRating: TextView = findViewById(R.id.FavRating)
            val FavYear: TextView = findViewById(R.id.FavYear)
            val FavOverview: TextView = findViewById(R.id.FavOverview)
            val RemoveFaveItem: LinearLayout = findViewById(R.id.RemoveFaveItem)


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
            }else {
                faveAdapter.updateItems(items)
            }

        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////


    private fun notificationS() {
        lifecycleScope.launch {

            // DB call on IO
            val notificationsFromDb = withContext(Dispatchers.IO) {
                db.getAllAnimeNotifications(userId)
            }

            // Headline
            findViewById<TextView>(R.id.notificationHeadline).text =  "notifications"

            if (notificationsFromDb.size > 0) findViewById<CardView>(R.id.cNotificationAnimeIcon).visibility = View.VISIBLE

            // Map DB → UI model
            val notifications = notificationsFromDb.map { item ->
                //Log.e("anime_Not_fetched", "xyz:" + item["subStored"]?.let { it::class } + item["seasons"] )

                val subcount = (item["subStored"]?.toString()?.toIntOrNull()) ?: 0
                val dubcount = (item["dubStored"]?.toString()?.toIntOrNull()) ?: 0

                Log.e("anime_Not_fetched", "xyz:" + "subcount: " +subcount + " | " + item["subStored"] + " dubcount: " + dubcount+ " | " + item["dubStored"] )

                var info = ""

                if(subcount> 0){
                    info = info + "Episode $subcount [SUB] available NOW \n\n"
                }
                if(dubcount> 0){
                    info = info + "Episode $dubcount [DUB] available NOW"
                }

                NotificationItem(
                    notificationId = item["id"]?.toString().orEmpty(),
                    imdbCode = item["anime_id"]?.toString().orEmpty(),
                    title = item["title"]?.toString().orEmpty(),
                    imageUrl = item["poster"],
                    info =  info, //"sub: ${item["subStored"]} dub: ${item["dubStored"]}",
                    type = "anime",
                    newSeason = item["subStored"]?.toString().orEmpty(),
                    newEpisode = item["dubStored"]?.toString().orEmpty(),
                    time = item["notify_at"]?.toString().orEmpty()
                )
            }

            // Adapter setup / update
            if (!::notificationAdapter.isInitialized) {
                notificationAdapter = NotificationAdapter(
                    items = notifications.toMutableList(),
                    layoutResId = R.layout.item_notification,
                    db,
                    userId
                )
                notificationRecyclerView.adapter = notificationAdapter
            } else {
                notificationAdapter.updateItems(notifications)
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
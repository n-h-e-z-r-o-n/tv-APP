package com.example.onyx

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import android.widget.EditText
import android.widget.LinearLayout
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.widget.ImageButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import kotlin.String

class Anime_Page : AppCompatActivity() {
    //private var urlHome = "http://192.168.100.22:4000"
    private var urlHome ="https://corsproxy.io/https://aniwatch-api-r4uo.vercel.app/"
    private var isSearchContainerAnimeVisible = false
    private var searchDebounceHandler: Handler? = null
    private var searchDebounceRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_anime_page)
        NavAction.setupSidebar(this@Anime_Page)


        animeHomeData()
        setupSearchUi()
        setupBackPressedCallback()
    }


    private fun animeHomeData() {
        CoroutineScope(Dispatchers.IO).launch {
            repeat(1) { attempt ->
                try {

                    val url = "$urlHome/api/v2/hianime/home"
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("accept", "application/json")

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.e("ANIME_STATUS HOME 1", response.toString())

                    val jsonObject = org.json.JSONObject(response)

                    Log.e("ANIME_STATUS HOME 2", jsonObject.toString())

                    val ShowHomeData = jsonObject.getJSONObject("data")
                    Log.e("ANIME_STATUS HOME 3", ShowHomeData.toString())


                    val  spotlightAnimes = ShowHomeData.getJSONArray("spotlightAnimes")
                    val  trendingAnimes = ShowHomeData.getJSONArray("trendingAnimes")
                    val  latestEpisodeAnimes = ShowHomeData.getJSONArray("spotlightAnimes")
                    val  top10Animes = ShowHomeData.getJSONArray("spotlightAnimes")
                    val  topAiringAnimes = ShowHomeData.getJSONArray("topAiringAnimes")
                    val  latestCompletedAnimes = ShowHomeData.getJSONArray("spotlightAnimes")



                    var spotlightAnimesitmes = mutableListOf<AnimeSliderItem>()

                    for (i in 0 until spotlightAnimes.length()) {

                        val item = spotlightAnimes.getJSONObject(i)

                        val title = item.getString("name")

                        val overview = item.getString("description")


                        val imageUrl = item.getString("poster")

                        val id = item.getString("id")
                        val type = item.getString("type")

                        val runtime = item.optJSONArray("otherInfo").optString(1, "")
                        val release_date = item.optJSONArray("otherInfo").optString(2, "")
                        val quality = item.optJSONArray("otherInfo").optString(3, "")
                        val sub = item.getJSONObject("episodes").optString("sub", "")
                        val dub = item.getJSONObject("episodes").optString("dub", "")





                        spotlightAnimesitmes.add(
                            AnimeSliderItem(
                                title,
                                imageUrl,
                                id,
                                type,
                                overview,
                                release_date,
                                runtime,
                                quality,
                                sub,
                                dub,
                            )
                        )

                    }

                    Log.e("DEBUG_MAIN_Slider 1", spotlightAnimesitmes.toString())



                    withContext(Dispatchers.Main) {

                        showTrending( trendingAnimes)
                        showAiring(topAiringAnimes)

                        LoadingAnimation.hide(this@Anime_Page)
                        val recyclerView = findViewById<RecyclerView>(R.id.spotlightAnimes)
                        recyclerView.layoutManager = LinearLayoutManager(
                            this@Anime_Page,
                            LinearLayoutManager.HORIZONTAL,
                            false
                        )
                        recyclerView.adapter = AnimeSwiper(spotlightAnimesitmes, R.layout.anime_card_spotlight)
                    }












                    return@launch
                } catch (e: Exception) {
                    delay(20_000)
                    Log.e("ANIME_STATUS HOME 1", "Error fetching data", e)
                    return@launch
                }



            }
        }
    }


    private fun showTrending( trending: JSONArray){

        var trendingItems = mutableListOf<TrendingAnimeItem>()

        for (i in 0 until trending.length()) {


            val item = trending.getJSONObject(i)

            val title = item.getString("name")

            val imageUrl = item.getString("poster")

            val id = item.getString("id")

            val ranking = "0"+item.getString("rank")



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


            LoadingAnimation.hide(this@Anime_Page)
            val recyclerView = findViewById<RecyclerView>(R.id.Anime_Trending_widget)
            recyclerView.layoutManager = LinearLayoutManager(
                this@Anime_Page,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            recyclerView.adapter = AnimeTrendingAdapter(trendingItems, R.layout.anime_trending_item)
            val spacing = (9 * resources.displayMetrics.density).toInt() // 16dp to px
            recyclerView.addItemDecoration(EqualSpaceItemDecoration(spacing))

    }

    private fun showAiring( Airing: JSONArray){

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


        LoadingAnimation.hide(this@Anime_Page)
        val recyclerView = findViewById<RecyclerView>(R.id.Anime_Airing_widget)
        recyclerView.layoutManager = LinearLayoutManager(
            this@Anime_Page,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        recyclerView.adapter = AnimeAiringAdapter(airingItems, R.layout.anime_airing_item)
        val spacing = (9 * resources.displayMetrics.density).toInt() // 16dp to px
        recyclerView.addItemDecoration(EqualSpaceItemDecoration(spacing))

    }

    private fun searchAnimeFetch(searchTerm:String){



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

                    for (i in 0 until searchData.length()) {

                        val item = searchData.getJSONObject(i)

                        val title = item.getString("name")
                        val imageUrl = item.getString("poster")
                        val id = item.getString("id")
                        val type = item.getString("type")
                        val sub = item.getJSONObject("episodes").optString("sub", "")
                        val dub = item.getJSONObject("episodes").optString("dub", "")


                        searchDataItmes.add(
                            AnimeSearchItem(
                                id,
                                title,
                                imageUrl,
                                type,
                                sub,
                                dub,
                            )
                        )

                    }



                    withContext(Dispatchers.Main) {

                        LoadingAnimation.hide(this@Anime_Page)
                        val recyclerView = findViewById<RecyclerView>(R.id.AnimeSearch_widget)

                        // Calculate span count dynamically
                        val widthInPixels = this@Anime_Page.resources.getDimension(R.dimen.grid_item_width)
                        val density = this@Anime_Page.resources.displayMetrics.density
                        val widthInDp = widthInPixels / density
                        val displayMetrics = resources.displayMetrics
                        val screenWidthPx = displayMetrics.widthPixels
                        val itemMinWidthPx = ((widthInDp + 19) * displayMetrics.density).toInt() // 160dp per item
                        val spanCount = maxOf(1, screenWidthPx / itemMinWidthPx)

                        recyclerView.layoutManager = GridLayoutManager(this@Anime_Page, spanCount)
                        recyclerView.adapter = AnimeSearchAdapter(searchDataItmes, R.layout.anime_airing_item)
                    }



                    return@launch
                } catch (e: Exception) {
                    delay(20_000)
                    Log.e("ANIME_STATUS S-Error", "Error fetching data", e)
                    return@launch
                }



            }
        }

    }

    private fun setupSearchUi() {

        val searchInput = findViewById<EditText>(R.id.AnimeSearchInput)
        try{
            searchInput.setOnEditorActionListener { _, actionId, event ->

                if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
                ) {

                    val searchTerm = searchInput.text.toString().trim()
                    if (searchTerm.isNotEmpty()) {
                        findViewById<LinearLayout>(R.id.searchContainerAnime).visibility =
                            View.VISIBLE
                        isSearchContainerAnimeVisible = true
                        searchAnimeFetch(searchTerm)
                    }

                    true
                } else {
                    false
                }
            }

            }catch (e: Exception){
                Log.e("ANIME_STATUS S-Error", "setupSearchUi() ", e)

            }
    }

    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {

                if (isSearchContainerAnimeVisible) {
                    findViewById<LinearLayout>(R.id.searchContainerAnime).visibility = View.GONE
                    isSearchContainerAnimeVisible = false

                }else {
                    findViewById<ImageButton>(R.id.btnAnime).requestFocus()
                }
            }
        })
    }


}
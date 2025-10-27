package com.example.onyx

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_anime_page)
        NavAction.setupSidebar(this@Anime_Page)




        animeHomeData()

    }


    private fun animeHomeData() {
        LoadingAnimation.show(this@Anime_Page)
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


}
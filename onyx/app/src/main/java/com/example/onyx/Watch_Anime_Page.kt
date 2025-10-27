package com.example.onyx

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class Watch_Anime_Page : AppCompatActivity() {

    private var urlHome ="https://corsproxy.io/https://aniwatch-api-r4uo.vercel.app/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_watch_anime_page)

        val animeCode = intent.getStringExtra("anime_code")
        val poster = intent.getStringExtra("anime_poster")

        Log.e("Watch_Anime_Page 1", animeCode.toString())

        //http://192.168.100.22:4000/api/v2/hianime/anime/$animeCode/episodes



        getInfo(animeCode.toString())

    }



    private fun getInfo(id: String){
        CoroutineScope(Dispatchers.IO).launch {
            repeat(1) { attempt ->
                try {

                    val url = "$urlHome/api/v2/hianime/anime/$id"

                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("accept", "application/json")
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = org.json.JSONObject(response)
                    val data = jsonObject.getJSONObject("data")

                    val id = data.getJSONObject("anime").getJSONObject("info").getString("id")
                    val anilistId = data.getJSONObject("anime").getJSONObject("info").getString("anilistId")
                    val malId = data.getJSONObject("anime").getJSONObject("info").getString("malId")
                    val name = data.getJSONObject("anime").getJSONObject("info").getString("name")
                    val poster = data.getJSONObject("anime").getJSONObject("info").getString("poster")
                    val description = data.getJSONObject("anime").getJSONObject("info").getString("description")
                    val rating = data.getJSONObject("anime").getJSONObject("info").getJSONObject("stats").getString("rating")
                    val quality = data.getJSONObject("anime").getJSONObject("info").getJSONObject("stats").getString("quality")
                    val type = data.getJSONObject("anime").getJSONObject("info").getJSONObject("stats").getString("type")
                    val duration = data.getJSONObject("anime").getJSONObject("info").getJSONObject("stats").getString("duration")
                    val sub = data.getJSONObject("anime").getJSONObject("info").getJSONObject("stats").getJSONObject("episodes").optString("sub", "")
                    val dub = data.getJSONObject("anime").getJSONObject("info").getJSONObject("stats").getJSONObject("episodes").optString("dub", "")
                    val aired = data.getJSONObject("anime").getJSONObject("moreInfo").getString("aired")
                    val status = data.getJSONObject("anime").getJSONObject("moreInfo").getString("status")
                    val studios = data.getJSONObject("anime").getJSONObject("moreInfo").getString("studios")
                    val genresArray = data.getJSONObject("anime").getJSONObject("moreInfo").getJSONArray("genres")
                    var genre = ""
                    for (i in 0 until genresArray.length()) {
                        genre = genre +" ~ " +genresArray.getString(i)
                    }

                    val  seasons = data.getJSONArray("seasons")
                    val  relatedAnimes = data.getJSONArray("relatedAnimes")
                    val  recommendedAnime = data.getJSONArray("recommendedAnimes")




                    withContext(Dispatchers.Main) {

                        findViewById<TextView>(R.id.watchTitle).text = name
                        findViewById<TextView>(R.id.watchRating).text = rating
                        findViewById<TextView>(R.id.watchRuntime).text = duration
                        findViewById<TextView>(R.id.watchType).text = type
                        findViewById<TextView>(R.id.watchQuality).text = quality
                        findViewById<TextView>(R.id.watchSub).text = sub
                        findViewById<TextView>(R.id.watchDub).text = dub
                        findViewById<TextView>(R.id.watchYear).text = aired
                        findViewById<TextView>(R.id.watchOverview).text = description
                        findViewById<TextView>(R.id.watchGenres).text = genre

                        val posterWidget = findViewById<ImageView>(R.id.WatchImage)
                        Glide.with(posterWidget.context)
                            .load(poster)
                            .centerInside()
                            .into(posterWidget)


                        if (seasons.length() > 0){
                            createSeasonButtons(seasons.length(), seasons )
                            findViewById<TextView>(R.id.SeasonTitle).visibility = View.VISIBLE
                        }else{
                            findViewById<TextView>(R.id.SeasonTitle).visibility = View.GONE
                            getEpisodes(id)
                        }

                        showRecommendation(relatedAnimes, recommendedAnime)

                    }



                    return@launch
                } catch (e: Exception) {
                    delay(20_000)
                    Log.e("ANIME_STATUS HOME 1", "Error fetching data", e)
                }
            }
        }
    }



    private fun createSeasonButtons(
        noOfSeasons: Int,
        seasonData: JSONArray
    ) {
        val container = findViewById<LinearLayout>(R.id.anime_season_selector_container)
        container.removeAllViews()

        val inflater = LayoutInflater.from(this)

        for (i in 0 until noOfSeasons) {
            val season = seasonData.getJSONObject(i)

            val cardView = inflater.inflate(R.layout.anime_season_item, container, false) as CardView
            val seasonTitle = cardView.findViewById<TextView>(R.id.SeasonTitle)
            val seasonImage = cardView.findViewById<ImageView>(R.id.SeasonImage)

            val title = season.optString("title", "Season ${i + 1}")
            val imageUrl = season.optString("poster", "")
            val season_id = season.optString("id", "")

            seasonTitle.text = title

            if (imageUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(imageUrl)
                    .centerCrop()
                    .into(seasonImage)
            }

            cardView.setOnClickListener {

                val selected_seasonShow = findViewById<TextView>(R.id.selected_seasonShow)
                selected_seasonShow.text = "List of episodes ($title)"

                getEpisodes(season_id)
            }

            container.addView(cardView)
        }
    }
    @OptIn(UnstableApi::class)
    private fun getEpisodes(id: String){
        CoroutineScope(Dispatchers.IO).launch {
            repeat(1) { attempt ->
                try {

                    val url = "$urlHome/api/v2/hianime/anime/$id/episodes"

                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("accept", "application/json")
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = org.json.JSONObject(response)
                    val data = jsonObject.getJSONObject("data")
                    val  episodes = data.getJSONArray("episodes")

                    Log.e("Watch_Anime_Page 1", episodes.toString())

                    withContext(Dispatchers.Main) {

                        val container = findViewById<LinearLayout>(R.id.anime_episodes_selector_container)
                        container.removeAllViews()
                        val inflater = LayoutInflater.from(this@Watch_Anime_Page)

                        for (i in 0 until episodes.length()) {
                            val episode = episodes.getJSONObject(i)

                            val cardView = inflater.inflate(R.layout.anime_item_episode, container, false) as FrameLayout
                            val epTitle = cardView.findViewById<TextView>(R.id.episode_name)
                            val epNumber = cardView.findViewById<TextView>(R.id.episode_Number)

                            val title = episode.optString("title", "${i + 1}")
                            val number = episode.optString("number", "")
                            val episodeId = episode.optString("episodeId", "")


                            epTitle.text = title
                            epNumber.text = number


                            cardView.setOnClickListener {
                                Log.e("ANIME_episodeId ", "episodeId: $episodeId")
                                //streamingLink(episodeId)

                                Anime_Video_Player.playVideoExternally(this@Watch_Anime_Page, episodeId)

                                /*
                                val intent = Intent(this@Watch_Anime_Page, Watch_Page::class.java).apply {
                                    putExtra("episodeId", episodeId)
                                }
                                this@Watch_Anime_Page.startActivity(intent)

                                 */
                            }

                            container.addView(cardView)
                        }

                    }


                    return@launch
                } catch (e: Exception) {
                    delay(20_000)
                    Log.e("ANIME_STATUS HOME 1", "Error fetching data", e)
                }
            }
        }
    }


    private fun showRecommendation(data: JSONArray, data2: JSONArray) {

        // ✅ Merge the two JSONArrays
        val Airing = JSONArray()
        for (i in 0 until data.length()) {
            Airing.put(data.getJSONObject(i))
        }
        for (i in 0 until data2.length()) {
            Airing.put(data2.getJSONObject(i))
        }

        var RecommendationItems = mutableListOf<AiringAnimeItem>()

        for (i in 0 until Airing.length()) {


            val item = Airing.getJSONObject(i)

            val title = item.getString("name")

            val imageUrl = item.getString("poster")

            val id = item.getString("id")

            val type = item.getString("type")

            val sub = item.getJSONObject("episodes").optString("sub", "")
            val dub = item.getJSONObject("episodes").optString("dub", "")





            RecommendationItems.add(
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


        val recyclerView = findViewById<RecyclerView>(R.id.animeWatchRecommendation)
        recyclerView.layoutManager = GridLayoutManager(this@Watch_Anime_Page, 6)
        recyclerView.adapter = AnimeAiringAdapter(RecommendationItems, R.layout.anime_airing_item)

        val spacing = (19 * resources.displayMetrics.density).toInt() // 16dp to px
        recyclerView.addItemDecoration(EqualSpaceItemDecoration(spacing))

    }

    private fun streamingLink(episodeId: String){
        CoroutineScope(Dispatchers.IO).launch {
            repeat(1) { attempt ->
                try {

                    val url_servers = "$urlHome/api/v2/hianime/episode/servers?animeEpisodeId=$episodeId"
                    val connection_servers = URL(url_servers).openConnection() as HttpURLConnection
                    connection_servers.requestMethod = "GET"
                    connection_servers.setRequestProperty("accept", "application/json")
                    val response_server = connection_servers.inputStream.bufferedReader().use { it.readText() }
                    val jsonObjectServerInfo = org.json.JSONObject(response_server)
                    val dataServers = jsonObjectServerInfo.getJSONObject("data")

                    val  subServers = dataServers.getJSONArray("sub")
                    val  dubServers = dataServers.getJSONArray("dub")
                    val  rawServers = dataServers.getJSONArray("raw")




                    val url ="$urlHome/api/v2/hianime/episode/sources?animeEpisodeId=$episodeId?server={server}&category={dub || sub || raw}"


                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("accept", "application/json")
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = org.json.JSONObject(response)
                    val data = jsonObject.getJSONObject("data")
                    val  episodes = data.getJSONArray("episodes")

                    Log.e("Watch_Anime_Page 1", episodes.toString())

                    withContext(Dispatchers.Main) {


                    }


                    return@launch
                } catch (e: Exception) {
                    delay(20_000)

                }
            }
        }
    }



}
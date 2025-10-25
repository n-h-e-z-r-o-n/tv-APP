package com.example.onyx

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class Watch_Anime_Page : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_watch_anime_page)

        val animeCode = intent.getStringExtra("anime_code")
        val poster = intent.getStringExtra("anime_poster")

        Log.e("Watch_Anime_Page 1", animeCode.toString())

        //http://192.168.100.22:4000/api/v2/hianime/anime/$animeCode/episodes

        val posterWidget = findViewById<ImageView>(R.id.WatchImage)
        Glide.with(posterWidget.context)
            .load(poster)
            .centerInside()
            .into(posterWidget)


        getEpisodes(animeCode.toString())

        getInfo(animeCode.toString())



    }

    private fun getEpisodes(id: String){
        CoroutineScope(Dispatchers.IO).launch {
            repeat(1) { attempt ->
                try {

                    val url = "http://192.168.100.22:4000/api/v2/hianime/anime/$id/episodes"

                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("accept", "application/json")
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = org.json.JSONObject(response)
                    val data = jsonObject.getJSONObject("data")
                    val  episodes = data.getJSONArray("episodes")

                    Log.e("Watch_Anime_Page 1", episodes.toString())



                    return@launch
                } catch (e: Exception) {
                    delay(20_000)
                    Log.e("ANIME_STATUS HOME 1", "Error fetching data", e)
                }
            }
       }
    }

    private fun getInfo(id: String){
        CoroutineScope(Dispatchers.IO).launch {
            repeat(1) { attempt ->
                try {

                    val url = "http://192.168.100.22:4000/api/v2/hianime/anime/$id"

                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("accept", "application/json")
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = org.json.JSONObject(response)
                    val data = jsonObject.getJSONObject("data")

                    val name = data.getJSONObject("anime").getJSONObject("info").getString("name")
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


                    }



                    return@launch
                } catch (e: Exception) {
                    delay(20_000)
                    Log.e("ANIME_STATUS HOME 1", "Error fetching data", e)
                }
            }
        }
    }


}
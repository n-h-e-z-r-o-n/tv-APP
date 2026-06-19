package com.example.onyx.OnyxObjects

import android.content.Context
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.example.onyx.Database.AppDatabase
import com.example.onyx.Database.SessionManger
import com.example.onyx.FetchData.AnimeApi
import com.example.onyx.FetchData.TMDBapi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray

object NotificationHelper {

    private lateinit var db: AppDatabase
    private lateinit var  sm: SessionManger


    fun getTvNotifications(context: Context) : Boolean{
        db = AppDatabase(context)         // Initialize database
        sm = SessionManger(context)
        val fetch = TMDBapi(context)

        val userId = sm.getUserId()
        val tvList = db.getFavoriteShowsByType(userId, "tv")

        var results = false


        for (item in tvList) {
            Log.d("Fav_tv", "show_id: ${item["show_id"]}")
            Log.d("Fav_tv", "title: ${item["title"]}")
            Log.d("Fav_tv", "poster: ${item["poster"]}")
            Log.d("Fav_tv", "stored lastSeason: ${item["lastSeason"]}")
            Log.d("Fav_tv", "stored lastEpisode: ${item["lastEpisode"]}")
            Log.d("Fav_tv", "stored noOfSeason: ${item["noOfSeason"]}")

            try {
                val show_id = item["show_id"]
                val name = item["title"]
                val poster = item["poster"]
                val storedLastSeason = item["lastSeason"].toString().toIntOrNull() ?: 0
                val storedLastEpisode =item["lastEpisode"].toString().toIntOrNull() ?: 0
                val storedNoOfSeason = item["noOfSeason"].toString().toIntOrNull() ?: 0

                val data = fetch.fetchShowData(show_id.toString(), type="tv")

                if (data != null) {

                    val ddta = data.optJSONObject("last_episode_to_air")?: continue
                    val newLastSeason = ddta.optInt("season_number")
                    val newLastEpisode = ddta.optInt("episode_number")
                    val newNoOfSeason = data.getJSONArray("seasons").length()

                    Log.d("Fav_tv", "fetched lastSeason: ${newLastSeason}")
                    Log.d("Fav_tv", "fetched lastEpisode: ${newLastEpisode}")
                    Log.d("Fav_tv", "fetched noOfSeason: ${newNoOfSeason}\n\n\n")

                    /*
                    var info = ""
                    var episodeDiff = 0

                    if (newLastSeason > storedLastSeason) {
                        // New season released
                        episodeDiff = newLastEpisode
                        info = "New Season ($newLastSeason) with $episodeDiff Episode${if (episodeDiff > 1) "s" else ""}"

                    } else if (newLastSeason == storedLastSeason && newLastEpisode > storedLastEpisode) {
                        // New episodes in the same season
                        episodeDiff = newLastEpisode - storedLastEpisode
                        info = "$episodeDiff New Episode${if (episodeDiff > 1) "s" else ""}"
                    }

                    Log.e("Fav_tv", "info isNotEmpty: ${info.isNotEmpty()}, title: $name , episodeDiff $episodeDiff \n\n")

                     */

                    if (newLastEpisode > storedLastEpisode || newLastSeason > storedLastSeason) {

                        db.insertTvNotification(
                            userId = userId,
                            tvId = show_id.toString(),
                            title =  name.toString(),
                            poster = poster.toString(),
                            noOfSeason = newNoOfSeason,
                            lastSeason = newLastSeason,
                            lastEpisode = newLastEpisode
                        )

                        db.updateFavoriteShowProgress(userId=userId, showId=show_id.toString(), noOfSeason=newNoOfSeason, lastSeason=newLastSeason, lastEpisode=newLastEpisode)

                        results = true


                    }

                }
            } catch (e: Exception) {
                Log.e("NotificationHelper", "Error for item: ${e.message}")
            }
        }
        return results
    }


    fun getAnimeNotifications(context: Context) : Boolean{
        db = AppDatabase(context)         // Initialize database
        sm = SessionManger(context)

        val userId = sm.getUserId()
        val animeList = db.getFavoriteAnime(userId)

        var results = false


        val fetch = AnimeApi(context)

        Log.e("anime_Notification Fav", "animeList:  ${animeList.toString()}")

        for (item in animeList) {
            try{
                Log.d("Not_anime", "\n\n\n")
                Log.d("Not_anime", "userId: $userId")
                Log.d("Not_anime", "anime_id: ${item["anime_id"]}")
                Log.d("Not_anime", "title: ${item["name"]}")
                Log.d("Not_anime", "poster: ${item["poster"]}")
                Log.d("Not_anime", "type: ${item["type"]}")
                Log.d("Not_anime", "seasons: ${item["seasons"]}")
                Log.d("Not_anime", "sub: ${item["sub"]}")
                Log.d("Not_anime", "dub: ${item["dub"]}")


                val animeId = item["anime_id"]
                val name = item["name"]
                val poster = item["poster"]


                var subStored = item["sub"].toString().toIntOrNull() ?: 0
                var dubStored = item["dub"].toString().toIntOrNull() ?: 0

                Log.e("anime_Not_fetched", "seasonsStored:" + JSONArray(item["seasons"]).length() + item["seasons"] )

                val seasonsStored = try {
                    JSONArray(item["seasons"]).length()
                } catch (e: Exception) {
                    0
                }




                val jsonObject = fetch.animeInfo(item["anime_id"].toString())


                if (jsonObject==null){
                    break
                }

                val data = jsonObject.getJSONObject("data")

                if (data != null) {
                    val subFetched = data.getJSONObject("stats").getJSONObject("episodes").optString("sub", "").toIntOrNull() ?: 0
                    val dubFetched = data.getJSONObject("stats").getJSONObject("episodes").optString("dub", "").toIntOrNull() ?: 0
                    val seasonsFetched = data.getJSONArray("seasons").length()?:0

                    Log.d("Not_anime", "subFetched: ${subFetched}")
                    Log.d("Not_anime", "dubFetched: ${dubFetched}")

                    var notSub = 0
                    var notDub = 0

                    if (subFetched > subStored) {
                        notSub = subFetched
                    }
                    if (dubFetched > dubStored) {
                        notDub = dubFetched
                    }

                    Log.d("Not_anime", "notSub: ${notSub}")
                    Log.d("Not_anime", "notDub: ${notDub}")


                    if (subFetched > subStored || dubFetched > dubStored) {

                        db.insertAnimeNotification(
                            userId = userId,
                            animeId = animeId.toString(),
                            title =  name.toString(),
                            poster = poster.toString(),
                            subStored = notSub,
                            dubStored = notDub,
                            seasonsStored = 0
                        )
                        db.updateAnimeProgress(userId, animeId.toString(), subFetched, dubFetched)

                        Log.d("Not_anime", "anime_Notification added,  animeId: $animeId\n")


                        results = true
                    }
                }



            }catch (e: Exception) {
                Log.e("anime_Notification", "Error ${e.message}")
            }
        }
        return results
    }



}
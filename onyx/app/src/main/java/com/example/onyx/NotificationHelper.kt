// NotificationHelper.kt
package com.example.onyx

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.String

object NotificationHelper {

    private const val PREFS_NAME = "notification_prefs"
    private const val KEY_NOTIFICATIONS = "notifications_json"

    val results = mutableListOf<NotificationItem>()

    data class NotificationState(
        val season: Int,
        val episode: Int
    )

    fun checkNotifications(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            getNotifications(context)
        }
    }

    fun getNotifications(context: Context) : List<NotificationItem>{
        val list = FavoritesManager.getFavorites(context).toMutableList()

        // keep only TV shows
        val tvList = list.filter { obj ->
            obj.has("first_air_date") && !obj.optString("first_air_date").isNullOrEmpty()
        }


        for (item in tvList) {

            try {
                val tvId = item.optString("id", "")
                val name = item.optString("name", "")
                val poster = item.optString("poster_path", "")

                //Log.e("NotificationHelper", "ID:  $tvId")
                if (tvId.isEmpty()) continue



                val tvUrl = "https://api.themoviedb.org/3/tv/$tvId?language=en-US"
                val connection = URL(tvUrl).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("accept", "application/json")
                connection.setRequestProperty(
                    "Authorization",
                    "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJhZjliMmUyN2MxYTZiYzMyMzNhZjE4MzJmNGFjYzg1MCIsIm5iZiI6MTcxOTY3NDUxNy4xOTYsInN1YiI6IjY2ODAyNjk1ZWZhYTI1ZjBhOGE4NGE3MyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.RTms-g8dzOl3WwCeJ7WNLq3i2kXxl3T7gOTa8POcxcw"
                )

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(response)

                //Log.e("NotificationHelper", "jsonObject:  ${response.toString()}")

                val lastEpisode = jsonObject.getJSONObject("last_episode_to_air")
                val newSeason = lastEpisode.optInt("season_number")
                val newEpisode = lastEpisode.optInt("episode_number")

                //Log.e("NotificationHelper", "newSeason:  $newSeason")
                //Log.e("NotificationHelper", "newEpisode:  $newEpisode")

                // Load stored state for this TV
                val savedData = getStoredNotification(context, tvId)
                //Log.e("NotificationHelper", "savedData:  ${savedData.toString()}")

                val lastSeason = savedData?.season ?: 0
                val lastEp = savedData?.episode ?: 0

                val info: String
                val episodeDiff: Int

                if (newSeason > lastSeason) {
                    episodeDiff = newEpisode
                    info = "New Season ($newSeason) with $episodeDiff episodes"
                } else if (newSeason == lastSeason && newEpisode > lastEp) {
                    episodeDiff = newEpisode - lastEp
                    info = "$episodeDiff New Episode${if (episodeDiff > 1) "s" else ""}"


                } else {
                    // No update → skip this show
                    continue
                }
                //Log.e("NotificationHelper", "$name $info")

                val itemData = NotificationItem(
                    imdbCode = tvId,
                    title = name,
                    imageUrl = "https://image.tmdb.org/t/p/w500$poster",
                    info = info,
                    type = "tv",
                    newSeason = newSeason,
                    newEpisode = newEpisode
                )
                results.add(itemData)

                // Update local stored state
                //updateNotification(context, tvId, newSeason, newEpisode)


            } catch (e: Exception) {
                Log.e("NotificationHelper", "Error for item: ${e.message}")
            }
        }
        return results
    }

    private fun updateNotification(context: Context, id: String, newSeason: Int, newEpisode: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_NOTIFICATIONS, "{}") ?: "{}"
        val root = JSONObject(json)

        val obj = JSONObject()
        obj.put("season", newSeason)
        obj.put("episode", newEpisode)

        root.put(id, obj)

        prefs.edit().putString(KEY_NOTIFICATIONS, root.toString()).apply()
    }

    private fun getStoredNotification(context: Context, id: String): NotificationState? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_NOTIFICATIONS, "{}") ?: "{}"
        val root = JSONObject(json)

        if (!root.has(id)) return null

        val obj = root.getJSONObject(id)
        return NotificationState(
            season = obj.optInt("season", 0),
            episode = obj.optInt("episode", 0)
        )
    }
}

/////





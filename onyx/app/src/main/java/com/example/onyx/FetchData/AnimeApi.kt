package com.example.onyx.FetchData

import android.content.Context
import android.util.Log
import com.example.onyx.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

class AnimeApi(private val context: Context) {

    private fun makeRequest(urlString: String): JSONObject? {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("accept", "application/json")
            // Set timeouts for efficiency and robustness
            connection.connectTimeout = 15000 // 15 seconds
            connection.readTimeout = 15000    // 15 seconds

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                return JSONObject(response)
            } else {
                // Try to read error stream if available
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e("AnimeApi", "HTTP error code: $responseCode for URL: $urlString. Error: $errorResponse")
                return null
            }
        } catch (e: SocketTimeoutException) {
            Log.e("AnimeApi", "Connection timed out for URL: $urlString", e)
            return null
        } catch (e: UnknownHostException) {
            Log.e("AnimeApi", "Unknown host: ${e.message} for URL: $urlString", e)
            return null
        } catch (e: IOException) {
            Log.e("AnimeApi", "Network error for URL: $urlString", e)
            return null
        } catch (e: JSONException) {
            Log.e("AnimeApi", "JSON parsing error for URL: $urlString", e)
            return null
        } catch (e: Exception) {
            Log.e("AnimeApi", "Unexpected error for URL: $urlString", e)
            return null
        } finally {
            connection?.disconnect()
        }
    }

    //Anime Home
    fun animeHome(): JSONObject? {
        return runBlocking {
            async(Dispatchers.IO) {
                val url = "${BuildConfig.A_K}/api/v2/anime/home"
                makeRequest(url)
            }.await()
        }
    }

    //Anime About Info
    fun animeInfo(animeId: String): JSONObject? {
        return runBlocking {
            async(Dispatchers.IO) {
                val url = "${BuildConfig.A_K}/api/v2/anime/anime/$animeId"
                makeRequest(url)
            }.await()
        }
    }

    //Anime Episodes
    fun animeEpisodes(season_id: String): JSONObject? {
        return runBlocking {
            async(Dispatchers.IO) {
                val url = "${BuildConfig.A_K}/api/v2/anime/anime/$season_id/episodes"
                makeRequest(url)
            }.await()
        }
    }

    //GET Anime Episode Servers
    fun animeEpisodeServers(animeEpisodeId: String): JSONObject? {
        return runBlocking {
            async(Dispatchers.IO) {
                //val url = "${BuildConfig.A_K}/api/v2/anime/episode/servers?animeEpisodeId=$animeEpisodeId"
                val url = "${BuildConfig.A_K}/api/v2/mirurostream/episode/links/$animeEpisodeId&access_code=12echo12"
                makeRequest(url)
            }.await()
        }
    }

    //GET Anime Episode Streaming Links
    fun animeEpisodeStreamingLinks(episodeId: String, serverName: String, category: String): JSONObject? {
        return runBlocking {
            async(Dispatchers.IO) {
                val url = "${BuildConfig.A_K}/api/v2/anime/episode/sources?animeEpisodeId=$episodeId&server=$serverName&category=$category"
                makeRequest(url)
            }.await()
        }
    }
}
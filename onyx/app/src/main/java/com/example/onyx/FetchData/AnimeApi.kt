package com.example.onyx.FetchData

import android.content.Context
import android.util.Log
import com.example.onyx.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import kotlin.coroutines.cancellation.CancellationException

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
    fun animeHome_old(): JSONObject? {
        return runBlocking {
            async(Dispatchers.IO) {
                val url = "${BuildConfig.A_K}/api/v2/anime/home"
                makeRequest(url)
            }.await()
        }
    }


    fun animeHome(): JSONObject? {
        return runBlocking(Dispatchers.IO) {

            var retryDelay = 2_000L

            while (true) {
                try {
                    val url = "${BuildConfig.A_K}/api/v2/anime/home"

                    val result = makeRequest(url)

                    if (result != null) {
                        return@runBlocking result
                    }

                    Log.w(
                        "ANIME_HOME_API",
                        "Request returned null. Retrying in ${retryDelay}ms"
                    )

                } catch (e: CancellationException) {
                    throw e

                } catch (e: Exception) {
                    Log.e(
                        "ANIME_HOME_API",
                        "Request failed. Retrying in ${retryDelay}ms",
                        e
                    )
                }
                delay(retryDelay)
                retryDelay = (retryDelay * 2)
                    .coerceAtMost(30_000L)
            }

            @Suppress("UNREACHABLE_CODE")
            null
        }
    }

    //Anime About Info
    fun animeInfo_old(animeId: String): JSONObject? {
        return runBlocking {
            async(Dispatchers.IO) {
                val url = "${BuildConfig.A_K}/api/v2/anime/anime/$animeId"
                makeRequest(url)
            }.await()
        }
    }

    fun animeInfo(animeId: String): JSONObject? {
        return runBlocking(Dispatchers.IO) {
            var retryDelay = 2_000L
            while (true) {
                try {
                    val url = "${BuildConfig.A_K}/api/v2/anime/anime/$animeId"
                    val result = makeRequest(url)
                    if (result != null) {
                        Log.w(
                            "ANIME_INFO_API",
                            "Request success $animeId. data ${result}ms"
                        )
                        return@runBlocking result
                    }

                    Log.w(
                        "ANIME_INFO_API",
                        "Request returned null for $animeId. Retrying in ${retryDelay}ms"
                    )

                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(
                        "ANIME_INFO_API",
                        "Request failed for $animeId. Retrying in ${retryDelay}ms",
                        e
                    )
                }

                delay(retryDelay)

                retryDelay = (retryDelay * 2)
                    .coerceAtMost(30_000L)
            }

            @Suppress("UNREACHABLE_CODE")
            null
        }
    }



    //Anime Episodes
    fun animeEpisodes_old(season_id: String): JSONObject? {
        return runBlocking {
            async(Dispatchers.IO) {
                val url = "${BuildConfig.A_K}/api/v2/anime/anime/$season_id/episodes"
                makeRequest(url)
            }.await()
        }
    }

    fun animeEpisodes(seasonId: String): JSONObject? {
        return runBlocking(Dispatchers.IO) {

            var retryDelay = 2_000L

            while (true) {
                try {
                    val url =
                        "${BuildConfig.A_K}/api/v2/anime/anime/$seasonId/episodes"

                    val result = makeRequest(url)

                    if (result != null) {
                        return@runBlocking result
                    }

                    Log.w(
                        "ANIME_EPISODES_API",
                        "Request returned null for $seasonId. Retrying in ${retryDelay}ms"
                    )

                } catch (e: CancellationException) {
                    throw e

                } catch (e: Exception) {
                    Log.e(
                        "ANIME_EPISODES_API",
                        "Request failed for $seasonId. Retrying in ${retryDelay}ms",
                        e
                    )
                }

                delay(retryDelay)
                retryDelay = (retryDelay * 2)
                    .coerceAtMost(30_000L)
            }

            @Suppress("UNREACHABLE_CODE")
            null
        }
    }




    //

    fun animeDubbed(pageToLoad: Int): JSONObject? {
        return runBlocking(Dispatchers.IO) {

            var retryDelay = 2_000L

            while (true) {
                try {
                    val url = "${BuildConfig.A_K}/api/v2/anime/dubbed?page=$pageToLoad"

                    val result = makeRequest(url)

                    if (result != null) {
                        return@runBlocking result
                    }

                    Log.w(
                        "ANIME_DUBBED",
                        "Request returned null for page $pageToLoad. Retrying in ${retryDelay}ms"
                    )

                } catch (e: CancellationException) {
                    throw e

                } catch (e: Exception) {
                    Log.e(
                        "ANIME_DUBBED",
                        "Request failed for page $pageToLoad. Retrying in ${retryDelay}ms",
                        e
                    )
                }

                delay(retryDelay)

                retryDelay = (retryDelay * 2)
                    .coerceAtMost(30_000L)
            }

            @Suppress("UNREACHABLE_CODE")
            null
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

}
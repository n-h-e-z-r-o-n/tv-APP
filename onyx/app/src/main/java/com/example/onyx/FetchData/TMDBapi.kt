package com.example.onyx.FetchData

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.onyx.BuildConfig
import com.example.onyx.R
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import kotlin.text.ifEmpty

import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONException
import java.net.HttpURLConnection
import java.net.URL


class TMDBapi(private val context: Context) {




    fun fetchLogos (type:String, tmdbId:String, widget:ImageView, widget2: View){
        CoroutineScope(Dispatchers.IO).launch {
            val logosUrl = " https://api.themoviedb.org/3/$type/$tmdbId/images"
            val logosConnection = URL(logosUrl).openConnection() as HttpURLConnection
            logosConnection.requestMethod = "GET"
            logosConnection.setRequestProperty("accept", "application/json")
            logosConnection.setRequestProperty(
                "Authorization",
                "Bearer ${BuildConfig.TM_K}"
            )

            val logosResponse = logosConnection.inputStream.bufferedReader().use { it.readText() }
            val jsonObjectImg = JSONObject(logosResponse)

            Log.e("DEBUG_Watch_Images", jsonObjectImg.toString())
            val logos = jsonObjectImg.getJSONArray("logos")
            Log.e("DEBUG_Watch_Images", logos.toString())

            for (i in 0 until logos.length()) {
                val logo = logos.getJSONObject(i)
                val logoUrl = "https://image.tmdb.org/t/p/original/"  + logo.getString("file_path")
                val languageCode = logo.getString("iso_639_1") //en


                val width = logo.getString("width")

                if (languageCode == "en") {

                    withContext(Dispatchers.Main) {

                        Glide.with(context)
                            .load(logoUrl)
                            .centerCrop()
                            .fitCenter()
                            .into(widget)

                        widget2.visibility = View.GONE

                    }
                    break

                }else {
                    try {
                       withContext(Dispatchers.Main) {

                                Glide.with(context)
                                    .load(logoUrl)
                                    .centerCrop()
                                    .fitCenter()
                                    .into(widget)

                                widget2.visibility = View.GONE
                       }
                    }catch (e: IOException) {
                      widget2.visibility = View.VISIBLE
                    }
                }

            }
        }
    }





    fun fetchAnimeData(animeId: String): JSONObject? {
        return runBlocking {
            async(Dispatchers.IO) {
                try {
                    val url = "${BuildConfig.A_K}/api/v2/hianime/anime/$animeId"

                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("accept", "application/json")
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000

                    val responseCode = connection.responseCode
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        return@async null
                    }

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("fetchAnimeData", "Response received: ${response.take(200)}...")

                    val jsonObject = JSONObject(response)

                    // Try to get "data" field, fallback to full response
                    jsonObject.optJSONObject("data") ?: jsonObject

                } catch (e: IOException) {
                    Log.e("fetchAnimeData", "Network error: ${e.message}")
                    null
                } catch (e: JSONException) {
                    Log.e("fetchAnimeData", "JSON error: ${e.message}")
                    null
                } catch (e: Exception) {
                    Log.e("fetchAnimeData", "Unexpected error: ${e.message}")
                    null
                }
            }.await()
        }
    }


    fun fetchTrendingData(currentYear: String): JSONObject? {
        return runBlocking {
            async(Dispatchers.IO) {
                try {
                    val tvUrl =  "https://api.themoviedb.org/3/trending/all/day?primary_release_year=$currentYear"
                    val connection = URL(tvUrl).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("accept", "application/json")
                    connection.setRequestProperty(
                        "Authorization",
                        "Bearer ${BuildConfig.TM_K}"
                    )

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)

                    jsonObject.optJSONObject("data") ?: jsonObject

                } catch (e: IOException) {
                    Log.e("fetchAnimeData", "Network error: ${e.message}")
                    null
                } catch (e: JSONException) {
                    Log.e("fetchAnimeData", "JSON error: ${e.message}")
                    null
                } catch (e: Exception) {
                    Log.e("fetchAnimeData", "Unexpected error: ${e.message}")
                    null
                }
            }.await()
        }
    }

    fun fetchVideoData(sid: String, stype: String): JSONObject? {
        return runBlocking {
            async(Dispatchers.IO) {
                try {
                    var urlString =
                        "https://api.themoviedb.org/3/tv/$sid/videos?language=en-US"

                    if (stype == "movie") {
                        urlString =
                            "https://api.themoviedb.org/3/movie/$sid/videos?language=en-US"
                    }

                    val connection = URL(urlString).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("accept", "application/json")
                    connection.setRequestProperty(
                        "Authorization",
                        "Bearer ${BuildConfig.TM_K}"
                    )

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)

                    jsonObject.optJSONObject("data") ?: jsonObject

                } catch (e: IOException) {
                    Log.e("fetchAnimeData", "Network error: ${e.message}")
                    null
                } catch (e: JSONException) {
                    Log.e("fetchAnimeData", "JSON error: ${e.message}")
                    null
                } catch (e: Exception) {
                    Log.e("fetchAnimeData", "Unexpected error: ${e.message}")
                    null
                }
            }.await()
        }
    }


    fun fetchShowData(showId: String, type:String): JSONObject? {
        return runBlocking {
            async(Dispatchers.IO) {
                try {
                    val tvUrl = "https://api.themoviedb.org/3/$type/$showId?language=en-US"
                    val connection = URL(tvUrl).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("accept", "application/json")
                    connection.setRequestProperty(
                        "Authorization",
                        "Bearer ${BuildConfig.TM_K}"
                    )

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)

                    jsonObject.optJSONObject("data") ?: jsonObject

                } catch (e: IOException) {
                    Log.e("fetchAnimeData", "Network error: ${e.message}")
                    null
                } catch (e: JSONException) {
                    Log.e("fetchAnimeData", "JSON error: ${e.message}")
                    null
                } catch (e: Exception) {
                    Log.e("fetchAnimeData", "Unexpected error: ${e.message}")
                    null
                }
            }.await()
        }
    }



    fun fetchShowCast(showId: String, type:String): JSONObject? {
        return runBlocking {
            async(Dispatchers.IO) {
                try {
                    val Url = "https://api.themoviedb.org/3/$type/$showId/credits?language=en-US"
                    val connection = URL(Url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("accept", "application/json")
                    connection.setRequestProperty(
                        "Authorization",
                        "Bearer ${BuildConfig.TM_K}"
                    )

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)

                    jsonObject.optJSONObject("data") ?: jsonObject

                } catch (e: IOException) {
                    Log.e("fetchAnimeData", "Network error: ${e.message}")
                    null
                } catch (e: JSONException) {
                    Log.e("fetchAnimeData", "JSON error: ${e.message}")
                    null
                } catch (e: Exception) {
                    Log.e("fetchAnimeData", "Unexpected error: ${e.message}")
                    null
                }
            }.await()
        }
    }

    fun fetchShowRecommendation (showId: String, type:String): JSONObject? {
        return runBlocking {
            async(Dispatchers.IO) {
                try {
                    val Url = "https://api.themoviedb.org/3/$type/$showId/recommendations?language=en-US&page=1"
                    val connection = URL(Url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("accept", "application/json")
                    connection.setRequestProperty(
                        "Authorization",
                        "Bearer ${BuildConfig.TM_K}"
                    )

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)

                    jsonObject.optJSONObject("data") ?: jsonObject

                } catch (e: IOException) {
                    Log.e("fetchAnimeData", "Network error: ${e.message}")
                    null
                } catch (e: JSONException) {
                    Log.e("fetchAnimeData", "JSON error: ${e.message}")
                    null
                } catch (e: Exception) {
                    Log.e("fetchAnimeData", "Unexpected error: ${e.message}")
                    null
                }
            }.await()
        }
    }

    fun fetchDiscoverMovie (param:String = ""): JSONObject? {
        return runBlocking {
            async(Dispatchers.IO) {
                try {
                   val  Url = "https://api.themoviedb.org/3/discover/movie?$param"
                    //tvUrl = "https://api.themoviedb.org/3/discover/tv?"
                    //val Url = "https://api.themoviedb.org/3/$type/$showId/recommendations?language=en-US&page=1"
                    val connection = URL(Url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("accept", "application/json")
                    connection.setRequestProperty(
                        "Authorization",
                        "Bearer ${BuildConfig.TM_K}"
                    )

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)

                    jsonObject.optJSONObject("data") ?: jsonObject

                } catch (e: IOException) {
                    Log.e("fetchAnimeData", "Network error: ${e.message}")
                    null
                } catch (e: JSONException) {
                    Log.e("fetchAnimeData", "JSON error: ${e.message}")
                    null
                } catch (e: Exception) {
                    Log.e("fetchAnimeData", "Unexpected error: ${e.message}")
                    null
                }
            }.await()
        }
    }

    //Query the details of a TV season.
    fun fetchSeasonInfo(seriesId: String, seasonNo:String): JSONObject? {
        return runBlocking {
            async(Dispatchers.IO) {
                try {
                    val Url = "https://api.themoviedb.org/3/tv/$seriesId/season/$seasonNo?language=en-US"
                    val connection = URL(Url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("accept", "application/json")
                    connection.setRequestProperty(
                        "Authorization",
                        "Bearer ${BuildConfig.TM_K}"
                    )

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)

                    jsonObject.optJSONObject("data") ?: jsonObject

                } catch (e: IOException) {
                    Log.e("fetchAnimeData", "Network error: ${e.message}")
                    null
                } catch (e: JSONException) {
                    Log.e("fetchAnimeData", "JSON error: ${e.message}")
                    null
                } catch (e: Exception) {
                    Log.e("fetchAnimeData", "Unexpected error: ${e.message}")
                    null
                }
            }.await()
        }
    }

    fun fetchImages(showId: String, type:String): JSONObject? {
        return runBlocking {
            async(Dispatchers.IO) {
                try {
                    val logosUrl = "https://api.themoviedb.org/3/$type/$showId/images"
                    val connection = URL(logosUrl).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("accept", "application/json")
                    connection.setRequestProperty(
                        "Authorization",
                        "Bearer ${BuildConfig.TM_K}"
                    )

                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)

                    jsonObject.optJSONObject("data") ?: jsonObject

                } catch (e: IOException) {
                    Log.e("fetchAnimeData", "Network error: ${e.message}")
                    null
                } catch (e: JSONException) {
                    Log.e("fetchAnimeData", "JSON error: ${e.message}")
                    null
                } catch (e: Exception) {
                    Log.e("fetchAnimeData", "Unexpected error: ${e.message}")
                    null
                }
            }.await()
        }
    }


    /*
         if (id.startsWith("tt")) {

                        val url = "https://api.themoviedb.org/3/movie/$id/external_ids"
                        val connection = URL(url).openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        connection.setRequestProperty("accept", "application/json")
                        connection.setRequestProperty(
                            "Authorization",
                            "Bearer ${BuildConfig.TM_K}"
                        )
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        val jsonObject = JSONObject(response)
                        tmdbId = jsonObject.getString("id")
                    }
     */





}



package com.example.onyx.OnyxObjects

import android.net.Uri
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

object MiruroScraper {

    private const val MIRURO_PIPE_URL = "https://www.miruro.tv/api/secure/pipe"
    private val gson = Gson()

    // WARNING: Standard OkHttpClient is easily flagged by Cloudflare.
    // If you receive endless 429 errors, you must replace this with Cronet or a Cloudflare bypass mechanism.
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val pipeHeaders = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36",
        "Referer" to "https://www.miruro.tv/",
        "Origin" to "https://www.miruro.tv",
        "Accept" to "*/*",
        "Accept-Language" to "en-US,en;q=0.9",
        "sec-fetch-site" to "same-origin",
        "sec-fetch-mode" to "cors",
        "sec-fetch-dest" to "empty"
    )

    private fun encodePipeRequest(payload: Map<String, Any?>): String {
        val jsonString = gson.toJson(payload)
        return Base64.encodeToString(
            jsonString.toByteArray(StandardCharsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        ).trim()
    }

    private fun decodePipeResponse(encodedStr: String): Map<String, Any> {
        return try {
            val trimmed = encodedStr.trim()
            if (trimmed.startsWith("{")) {
                val type = object : TypeToken<Map<String, Any>>() {}.type
                return gson.fromJson(trimmed, type) ?: emptyMap()
            }

            // FIXED: Added Base64.NO_PADDING to prevent IllegalArgumentException
            val compressedBytes = Base64.decode(trimmed, Base64.URL_SAFE or Base64.NO_PADDING)

            GZIPInputStream(ByteArrayInputStream(compressedBytes)).use { gzip ->
                ByteArrayOutputStream().use { out ->
                    val buffer = ByteArray(1024)
                    var len: Int
                    while (gzip.read(buffer).also { len = it } > 0) {
                        out.write(buffer, 0, len)
                    }
                    val decompressedString = out.toString("UTF-8")
                    val type = object : TypeToken<Map<String, Any>>() {}.type
                    gson.fromJson(decompressedString, type) ?: emptyMap()
                }
            }
        } catch (e: Exception) {
            Log.e("MiruroScraper", "Failed to decode pipe response: ${e.message}")
            emptyMap()
        }
    }

    // FIXED: Added translateId logic to match Python's handling of encoded IDs
    private fun translateId(encodedId: String): String {
        return try {
            var normalized = encodedId.replace('-', '+').replace('_', '/')
            val paddingNeeded = 4 - (normalized.length % 4)
            if (paddingNeeded < 4) {
                normalized += "=".repeat(paddingNeeded)
            }
            val decodedBytes = Base64.decode(normalized, Base64.DEFAULT)
            val decoded = String(decodedBytes, StandardCharsets.UTF_8)

            if (decoded.contains(":")) decoded else encodedId
        } catch (e: Exception) {
            encodedId
        }
    }

    private suspend fun fetchFromPipe(payload: Map<String, Any?>, retries: Int = 3): Map<String, Any> {
        val encodedReq = encodePipeRequest(payload)
        val url = "$MIRURO_PIPE_URL?e=$encodedReq"

        var lastException: Exception? = null
        for (attempt in 0 until retries) {
            try {
                val requestBuilder = Request.Builder().url(url)
                pipeHeaders.forEach { (k, v) -> requestBuilder.addHeader(k, v) }

                client.newCall(requestBuilder.build()).execute().use { response ->
                    if (response.code == 429) {
                        val retryAfterStr = response.header("Retry-After")
                        val waitTimeSeconds = retryAfterStr?.toLongOrNull() ?: 5L
                        Log.w("MiruroScraper", "Rate limited (429). Waiting $waitTimeSeconds seconds...")
                        delay(waitTimeSeconds * 1000)
                        throw Exception("HTTP 429 Too Many Requests")
                    }

                    if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                    val bodyString = response.body?.string() ?: ""
                    return decodePipeResponse(bodyString)
                }
            } catch (e: Exception) {
                lastException = e
                if (e.message?.contains("429") == false && attempt < retries - 1) {
                    delay(1500)
                }
            }
        }
        throw lastException ?: Exception("Unknown network error")
    }

    private fun deriveRefererFromStreamUrl(streamUrl: String): String? {
        return try {
            val parsed = Uri.parse(streamUrl)
            val scheme = parsed.scheme
            val authority = parsed.encodedAuthority
            val path = parsed.encodedPath ?: "/"
            if (scheme.isNullOrBlank() || authority.isNullOrBlank()) return null

            val lastSlash = path.lastIndexOf('/')
            val basePath = if (lastSlash >= 0) path.substring(0, lastSlash + 1) else "/"
            "$scheme://$authority$basePath"
        } catch (_: Exception) {
            null
        }
    }

    private fun deriveOrigin(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return try {
            val parsed = Uri.parse(url)
            val scheme = parsed.scheme
            val authority = parsed.encodedAuthority
            if (scheme.isNullOrBlank() || authority.isNullOrBlank()) null else "$scheme://$authority"
        } catch (_: Exception) {
            null
        }
    }

    private fun normalizeHeaders(rawHeaders: Map<*, *>?): Map<String, String> {
        if (rawHeaders == null) return emptyMap()
        val normalized = linkedMapOf<String, String>()
        for ((key, value) in rawHeaders) {
            val headerName = key as? String ?: continue
            val headerValue = value as? String ?: continue
            if (headerName.isBlank() || headerValue.isBlank()) continue
            normalized[headerName] = headerValue.trim()
        }
        return normalized
    }

    private fun buildOutputHeaders(streamUrl: String, sourceHeaders: Map<*, *>?): Map<String, String> {
        val normalized = normalizeHeaders(sourceHeaders)

        var existingReferer: String? = null
        var existingOrigin: String? = null
        var existingUserAgent: String? = null
        val passthroughHeaders = linkedMapOf<String, String>()

        for ((key, value) in normalized) {
            when (key.lowercase()) {
                "referer" -> existingReferer = value
                "origin" -> existingOrigin = value
                "user-agent" -> existingUserAgent = value
                else -> passthroughHeaders[key] = value
            }
        }

        val referer = existingReferer
            ?: deriveRefererFromStreamUrl(streamUrl)
            ?: pipeHeaders["Referer"]
            ?: ""

        val origin = existingOrigin
            ?: deriveOrigin(referer)
            ?: deriveOrigin(streamUrl)

        return buildMap {
            putAll(passthroughHeaders)
            put("Referer", referer)
            if (!origin.isNullOrBlank()) {
                put("Origin", origin)
            }
            put("User-Agent", existingUserAgent ?: (pipeHeaders["User-Agent"] ?: ""))
        }
    }

    private fun buildPlayableProbeHeaders(headers: Map<String, String>): Map<String, String> {
        return buildMap {
            put("User-Agent", headers["User-Agent"] ?: (pipeHeaders["User-Agent"] ?: ""))
            put("Referer", headers["Referer"] ?: (pipeHeaders["Referer"] ?: ""))
            put("Range", "bytes=0-1024")
            headers["Origin"]?.takeIf { it.isNotBlank() }?.let { put("Origin", it) }
        }
    }

    private fun getCandidateStreams(sources: Map<String, Any>): List<Map<*, *>> {
        val streams = sources["streams"] as? List<*> ?: return emptyList()

        return streams
            .filterIsInstance<Map<*, *>>()
            .mapNotNull { stream ->
                val url = (stream["url"] as? String)?.trim().orEmpty()
                if (url.isBlank()) null else stream
            }
            .sortedBy { stream ->
                val type = (stream["type"] as? String)?.lowercase().orEmpty()
                when {
                    "hls" in type -> 0
                    "mp4" in type -> 1
                    "embed" in type -> 3
                    else -> 2
                }
            }
    }

    private fun isStreamPlayable(streamUrl: String, headers: Map<String, String>): Boolean {
        return try {
            val requestBuilder = Request.Builder().url(streamUrl)
            buildPlayableProbeHeaders(headers).forEach { (key, value) ->
                requestBuilder.header(key, value)
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                response.isSuccessful || response.isRedirect
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun Response.isRedirect(): Boolean {
        return code in 300..399
    }

    private fun selectPlayableStream(sources: Map<String, Any>): Pair<Map<*, *>, Map<String, String>>? {
        val candidates = getCandidateStreams(sources)
        if (candidates.isEmpty()) return null

        for (candidate in candidates) {
            val streamUrl = (candidate["url"] as? String)?.trim().orEmpty()
            if (streamUrl.isBlank()) continue

            val candidateHeaders = (candidate["headers"] as? Map<*, *>)
                ?: (sources["headers"] as? Map<*, *>)
            val outputHeaders = buildOutputHeaders(streamUrl, candidateHeaders)

            if (isStreamPlayable(streamUrl, outputHeaders)) {
                return candidate to outputHeaders
            }
        }

        val fallback = candidates.first()
        val fallbackUrl = (fallback["url"] as? String)?.trim().orEmpty()
        val fallbackHeaders = (fallback["headers"] as? Map<*, *>)
            ?: (sources["headers"] as? Map<*, *>)
        return fallback to buildOutputHeaders(fallbackUrl, fallbackHeaders)
    }

    suspend fun fetchMiruroStreamingLinks(animeEpisodeId: String): Map<String, List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        val subResults = mutableListOf<Map<String, Any>>()
        val dubResults = mutableListOf<Map<String, Any>>()

        var cleanEpisodeId = animeEpisodeId.trim()
        var episodeNumber = 1

        val epMatch = "[?&]ep=(\\d+)".toRegex().find(cleanEpisodeId)
        if (epMatch != null) {
            episodeNumber = epMatch.groupValues[1].toInt()
        }

        val queryParamIndex = cleanEpisodeId.indexOfFirst { it == '?' || it == '&' }
        if (queryParamIndex != -1) {
            cleanEpisodeId = cleanEpisodeId.substring(0, queryParamIndex)
        }

        val idMatch = "-(\\d+)$".toRegex().find(cleanEpisodeId)
            ?: "^(\\d+)$".toRegex().find(cleanEpisodeId)

        if (idMatch == null) {
            Log.e("MiruroScraper", "Invalid episodeId format. Parsed slug: '$cleanEpisodeId'.")
            return@withContext mapOf("sub" to emptyList(), "dub" to emptyList())
        }

        val anilistId = idMatch.groupValues[1].toIntOrNull()
        if (anilistId == null || anilistId <= 0 || episodeNumber <= 0) {
            Log.e("MiruroScraper","Failed to parse AniList ID or episode number.")
            return@withContext mapOf("sub" to emptyList(), "dub" to emptyList())
        }

        val episodePayload = mapOf(
            "path" to "episodes",
            "method" to "GET",
            "query" to mapOf("anilistId" to anilistId),
            "body" to null,
            "version" to "0.1.0"
        )

        val episodeData = try {
            fetchFromPipe(episodePayload)
        } catch (e: Exception) {
            Log.e("MiruroScraper", "Failed to fetch episodes data: ${e.message}")
            return@withContext mapOf("sub" to emptyList(), "dub" to emptyList())
        }

        val providers = episodeData["providers"] as? Map<*, *> ?: return@withContext mapOf("sub" to emptyList(), "dub" to emptyList())

        val deferredTasks = mutableListOf<Deferred<Pair<String, Map<String, Any>>?>>()

        for ((providerName, providerInfo) in providers) {
            val nameStr = providerName as? String ?: continue
            val infoMap = providerInfo as? Map<*, *> ?: continue
            val episodes = infoMap["episodes"] as? Map<*, *> ?: continue

            (episodes["sub"] as? List<*>)?.filterIsInstance<Map<*, *>>()?.firstOrNull {
                (it["number"] as? Number)?.toInt() == episodeNumber
            }?.let { subEp ->
                deferredTasks.add(async {
                    processProviderEpisode(nameStr, subEp, anilistId, "sub")?.let { "sub" to it }
                })
            }

            (episodes["dub"] as? List<*>)?.filterIsInstance<Map<*, *>>()?.firstOrNull {
                (it["number"] as? Number)?.toInt() == episodeNumber
            }?.let { dubEp ->
                deferredTasks.add(async {
                    processProviderEpisode(nameStr, dubEp, anilistId, "dub")?.let { "dub" to it }
                })
            }
        }

        deferredTasks.awaitAll().filterNotNull().forEach { (category, data) ->
            if (category == "sub") subResults.add(data) else dubResults.add(data)
        }

        mapOf("sub" to subResults, "dub" to dubResults)
    }

    private suspend fun processProviderEpisode(providerName: String, ep: Map<*, *>, anilistId: Int, category: String): Map<String, Any>? {
        return try {
            val rawId = ep["id"] as? String ?: return null

            // FIXED: Process the ID through translateId before re-encoding
            val plainId = translateId(rawId)
            val encId = Base64.encodeToString(
                plainId.toByteArray(StandardCharsets.UTF_8),
                Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
            ).trim()

            val sourcesPayload = mapOf(
                "path" to "sources",
                "method" to "GET",
                "query" to mapOf(
                    "episodeId" to encId,
                    "provider" to providerName,
                    "category" to category,
                    "anilistId" to anilistId
                ),
                "body" to null,
                "version" to "0.1.0"
            )

            val sources = fetchFromPipe(sourcesPayload)
            val selected = selectPlayableStream(sources) ?: return null
            val selectedStream = selected.first
            val selectedHeaders = selected.second

            mapOf(
                "server" to providerName,
                "link" to ((selectedStream["url"] as? String)?.trim().orEmpty()),
                "type" to (selectedStream["type"] as? String ?: "hls"),
                "quality" to (selectedStream["quality"] as? String ?: "auto"),
                "subtitles" to if (category == "sub") {
                    sources["subtitles"] as? List<*> ?: emptyList<Any>()
                } else {
                    emptyList<Any>()
                },
                "headers" to selectedHeaders
            )
        } catch (e: Exception) {
            Log.e("MiruroScraper", "Error processing $providerName ($category): ${e.message}", e)
            null
        }
    }
}

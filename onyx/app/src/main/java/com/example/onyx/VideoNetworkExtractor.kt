import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException

object VideoNetworkExtractor {
    private val client = OkHttpClient()

    @RequiresApi(Build.VERSION_CODES.N)
    @Throws(IOException::class)
    fun extractFromEmbedPage(embedUrl: String): List<String> {
        val found = mutableListOf<String>()
        Log.d("Extractor", "▶ Extracting from: $embedUrl")

        val req = Request.Builder().url(embedUrl).get().build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string() ?: return emptyList()

            // Direct URL scan
            found += extractUrlsFromText(body)

            val doc = Jsoup.parse(body)
            val scriptsText = doc.select("script").joinToString("\n") { it.html() }

            // Match /api/... patterns
            val apiRegex = """/api/[^\s'"]+""".toRegex()
            apiRegex.findAll(scriptsText).forEach { match ->
                val apiPath = match.value
                val apiUrl = makeAbsolute(embedUrl, apiPath)
                Log.d("Extractor", "🔗 API candidate: $apiUrl")
                try {
                    found += extractFromApi(apiUrl)
                } catch (e: Exception) {
                    Log.w("Extractor", "⚠ Failed API fetch: ${e.message}")
                }
            }

            // Follow iframes recursively
            doc.select("iframe[src]").forEach { iframe ->
                val src = makeAbsolute(embedUrl, iframe.attr("src"))
                try {
                    found += extractFromEmbedPage(src)
                } catch (_: Exception) {}
            }
        }

        val result = found.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        Log.d("Extractor", "✅ Found ${result.size} URLs")
        return result
    }

    private fun makeAbsolute(base: String, relative: String): String {
        return try {
            val baseUrl = base.toHttpUrl()
            val relUrl = relative.toHttpUrlOrNull()
            if (relUrl != null) relUrl.toString()
            else baseUrl.resolve(relative)?.toString() ?: relative
        } catch (e: Exception) {
            if (relative.startsWith("http")) relative else base.trimEnd('/') + "/" + relative.trimStart('/')
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @Throws(IOException::class)
    private fun extractFromApi(apiUrl: String): List<String> {
        val candidate = mutableListOf<String>()
        Log.d("Extractor", "🌐 Fetching API: $apiUrl")

        val req = Request.Builder().url(apiUrl).get().build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string() ?: return emptyList()
            Log.v("Extractor", "📥 API response: $body")

            val json = try {
                JSONObject(body)
            } catch (e: Exception) {
                return extractUrlsFromText(body)
            }

            val keys = listOf("servers", "sources", "data", "file", "url", "link", "src")
            for (k in keys) {
                if (json.has(k)) {
                    val v = json.get(k)
                    candidate += parseJsonValue(v)
                }
            }

            candidate += extractUrlsFromText(body)
        }
        return candidate.distinct()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun parseJsonValue(v: Any): List<String> {
        val urls = mutableListOf<String>()
        when (v) {
            is JSONArray -> {
                for (i in 0 until v.length()) {
                    val item = v.get(i)
                    urls += parseJsonValue(item)
                }
            }
            is JSONObject -> {
                v.keys().forEachRemaining { key ->
                    urls += parseJsonValue(v.get(key))
                }
            }
            is String -> urls += extractUrlsFromText(v)
            else -> urls += extractUrlsFromText(v.toString())
        }
        return urls
    }

    private fun extractUrlsFromText(text: String): List<String> {
        val urls = mutableListOf<String>()
        val urlRegex = """https?://[^\s'"]+""".toRegex()
        urlRegex.findAll(text).forEach { m ->
            val u = m.value
            if (u.contains(".m3u8") || u.endsWith(".mp4") || u.endsWith(".webm") ||
                u.contains("videoplayback") || u.contains("master.m3u8") || u.contains(".mkv")
            ) {
                urls.add(u.split("\"", "'")[0])
            }
        }
        return urls
    }
}

import kotlinx.coroutines.*
import org.jsoup.Jsoup

suspend fun extractVideoUrl(embedUrl: String): String? = withContext(Dispatchers.IO) {
    try {
        val doc = Jsoup.connect(embedUrl).get()

        // Example: look for <video><source src="...">
        val videoTag = doc.select("video source").first()
        videoTag?.attr("src")
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

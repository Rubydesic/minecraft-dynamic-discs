package ga.rubydesic.dmd.download

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.URLEncoder

object YoutubeApi {
    private const val YOUTUBE_API_KEY = "AIzaSyCTSK0xqLCZQBMJLC8eAsuv9v_i0bjkAYU";

    suspend fun search(query: String): String? = withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url =
            "https://youtube.googleapis.com/youtube/v3/search?part=snippet&maxResults=1&q=$encodedQuery&key=$YOUTUBE_API_KEY"
        val reader = URL(url).openStream().bufferedReader()
        val json = JsonParser().parse(reader.readText())
        reader.close()

        val items = json.asJsonObject["items"]
        if (items.asJsonArray.size() == 0) return@withContext null

        val id = items.asJsonArray[0].asJsonObject["id"].asJsonObject["videoId"].asString
        id
    }

}
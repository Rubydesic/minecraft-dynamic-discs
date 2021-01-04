package ga.rubydesic.dmd.download

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import ga.rubydesic.dmd.log
import ga.rubydesic.dmd.runCommand
import ga.rubydesic.dmd.util.SizedInputStream
import ga.rubydesic.dmd.ytdlBinaryFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object YoutubeDl {

    /**
     * Returns the ID of the top result for the search
     */
    suspend fun search(search: String): String? = getYoutubeDlOutput("ytsearch:$search")["id"].asString

    suspend fun getInfo(url: String): DownloadInfo {
        val json = getYoutubeDlOutput(url)

        val headers = json["http_headers"].asJsonObject
        val realUrl = json["url"].asString
        val title = json["title"].asString
        val id = json["id"].asString


        val getStream = suspend {
            withContext(Dispatchers.IO) {
                val connection = URL(realUrl).openConnection()
                headers.entrySet().forEach { (key, value) -> connection.setRequestProperty(key, value.asString) }
                SizedInputStream(connection.getInputStream(), connection.contentLength)
            }
        }

        log.info("Generated download info for $id ($title)")

        return DownloadInfo(getStream, AudioDetails(title, id))
    }

    private suspend fun getYoutubeDlOutput(url: String): JsonObject = withContext(Dispatchers.IO) {
        val binary = ytdlBinaryFuture.await()

        val output = runCommand(
            binary.toAbsolutePath().toString(),
            "--dump-json",
            "--format",
            "bestaudio",
            url
        )
        println(output)

        return@withContext JsonParser().parse(output).asJsonObject
    }
}
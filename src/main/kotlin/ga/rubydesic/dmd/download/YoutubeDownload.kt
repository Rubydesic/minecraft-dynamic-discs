package ga.rubydesic.dmd.download

import com.github.kiulian.downloader.YoutubeDownloader
import com.github.kiulian.downloader.model.Extension
import com.github.kiulian.downloader.model.YoutubeVideo
import com.github.kiulian.downloader.model.quality.AudioQuality
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.gson.JsonParser
import com.zakgof.velvetvideo.ISeekableInput
import ga.rubydesic.dmd.AudioStreamVelvet
import ga.rubydesic.dmd.cacheDir
import net.minecraft.client.sounds.AudioStream
import java.net.URL
import java.net.URLEncoder
import java.nio.file.Files
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.TimeUnit

object YoutubeDownload {

	private val downloader = YoutubeDownloader()
	private val searchCache: Cache<String, CompletableFuture<String?>> =
		CacheBuilder.newBuilder().maximumSize(4096).build()
    private val videoCache: Cache<String, CompletableFuture<YoutubeVideo?>> =
        CacheBuilder.newBuilder().maximumSize(4096).build()

	private const val YOUTUBE_API_KEY = "AIzaSyCTSK0xqLCZQBMJLC8eAsuv9v_i0bjkAYU";
	private val ytCacheDir = cacheDir.resolve("youtube")

	init {
		Files.createDirectories(ytCacheDir)
	}

	fun getYtTitle(videoId: String): CompletableFuture<String> {
		return getYtVideo(videoId).thenApply { it?.details()?.title() }
	}

	fun getTopResultId(query: String): CompletableFuture<String?> {
		return searchCache.get(query) {
			CompletableFuture.supplyAsync({
				val encodedQuery = URLEncoder.encode(query, "UTF-8")
				val url = "https://youtube.googleapis.com/youtube/v3/search?part=snippet&maxResults=1&q=$encodedQuery&key=$YOUTUBE_API_KEY"
				val reader = URL(url).openStream().bufferedReader()
				val json = JsonParser().parse(reader.readText())
				reader.close()

                val items = json.asJsonObject["items"]
                if (items.asJsonArray.size() == 0) return@supplyAsync null

				val id = items.asJsonArray[0].asJsonObject["id"].asJsonObject["videoId"].asString
				id
			}, MusicCache.ioPool)
		}
	}

	fun getYtAudioStream(videoId: String): CompletableFuture<AudioStream?> {
		return getYtSeekable(videoId)
			.thenApply { stream ->
				if (stream == null) {
                    println("YOUTUBE VIDEO NOT FOUND...")
                    null
                } else {
                    AudioStreamVelvet(stream) as AudioStream
                }
			}.exceptionally { it.printStackTrace(); null }
	}

	private fun getYtSeekable(videoId: String): CompletableFuture<ISeekableInput?> {
		val path = ytCacheDir.resolve("$videoId.m4a")

		return MusicCache.getSeekableInput(path) { getYtM4aUrl(videoId) }
    }

    private fun getYtVideoFresh(videoId: String): CompletableFuture<YoutubeVideo?> {
        val future = createYtVideoFuture(videoId)
        videoCache.put(videoId, future)
        return future
    }

	private fun getYtVideo(videoId: String): CompletableFuture<YoutubeVideo?> {
		return videoCache.get(videoId) { createYtVideoFuture(videoId) }
	}

    private fun createYtVideoFuture(videoId: String): CompletableFuture<YoutubeVideo?> {
        return CompletableFuture.supplyAsync({ downloader.getVideo(videoId) }, MusicCache.ioPool)
    }

	private fun getYtM4aUrl(videoId: String): CompletableFuture<String?> {
		return getYtVideoFresh(videoId).thenApply { video ->
			if (video == null) return@thenApply null

			val m4as = video.findAudioWithExtension(Extension.M4A)
			m4as.sortBy {
				when (it.audioQuality()) {
					AudioQuality.high -> 0
					AudioQuality.medium -> 1
					AudioQuality.low -> 2
					AudioQuality.unknown -> 3
					AudioQuality.noAudio -> 4
					else -> 5
				}
			}

			// Get the highest quality M4A file associated with this youtube video
			m4as.firstOrNull()?.url()
		}
	}

}
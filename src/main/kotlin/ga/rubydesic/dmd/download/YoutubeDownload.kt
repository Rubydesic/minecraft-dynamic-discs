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

object YoutubeDownload {

	private val downloader = YoutubeDownloader()
	private val searchCache: Cache<String, CompletableFuture<String>> =
		CacheBuilder.newBuilder().maximumSize(2048).build()


	private const val YOUTUBE_API_KEY = "AIzaSyCTSK0xqLCZQBMJLC8eAsuv9v_i0bjkAYU";
	private val ytCacheDir = cacheDir.resolve("youtube")

	init {
		Files.createDirectories(ytCacheDir)
	}

	fun getYtTitle(videoId: String): CompletableFuture<String> {
		return getYtVideo(videoId).thenApply { it?.details()?.title() }
	}

	fun getTopResultId(query: String): CompletableFuture<String> {
		return searchCache.get(query) {
			CompletableFuture.supplyAsync({
				val encodedQuery = URLEncoder.encode(query, "UTF-8")
				val url = "https://youtube.googleapis.com/youtube/v3/search?part=snippet&maxResults=1&q=$encodedQuery&key=$YOUTUBE_API_KEY"
				val reader = URL(url).openStream().bufferedReader()
				val json = JsonParser().parse(reader.readText())
				reader.close()

				val result = json.asJsonObject["items"].asJsonArray[0].asJsonObject
				val id = result["id"].asJsonObject["videoId"].asString
				id
			}, MusicCache.ioPool)
		}
	}

	fun getYtAudioStream(videoId: String): CompletableFuture<AudioStream> {
		return getYtSeekable(videoId)
			.thenApply { stream ->
				if (stream == null) throw CompletionException(Exception("YOUTUBE VIDEO NOT FOUND"))
				else AudioStreamVelvet(stream)
			}
	}

	private fun getYtSeekable(videoId: String): CompletableFuture<ISeekableInput?> {
		val path = ytCacheDir.resolve("$videoId.m4a")

		return getYtM4aUrl(videoId).thenApply { url ->
			MusicCache.getSeekableInput(path, url ?: return@thenApply null)
		}
	}

	private fun getYtVideo(videoId: String): CompletableFuture<YoutubeVideo?> {
		return CompletableFuture.supplyAsync({ downloader.getVideo(videoId) }, MusicCache.ioPool)
	}

	fun getYtM4aUrl(videoId: String): CompletableFuture<String?> {
		return getYtVideo(videoId).thenApply { video ->
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
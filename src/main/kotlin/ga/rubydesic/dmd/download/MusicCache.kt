package ga.rubydesic.dmd.download

import com.google.common.cache.CacheBuilder
import com.google.gson.Gson
import com.zakgof.velvetvideo.impl.FileSeekableInput
import ga.rubydesic.dmd.analytics.Analytics
import ga.rubydesic.dmd.cacheDir
import ga.rubydesic.dmd.download.MusicSource.YOUTUBE
import ga.rubydesic.dmd.fromJson
import ga.rubydesic.dmd.log
import ga.rubydesic.dmd.util.AudioStreamVelvet
import ga.rubydesic.dmd.util.FileBufferedSeekableInput
import kotlinx.coroutines.*
import kotlinx.coroutines.future.future
import net.minecraft.client.sounds.AudioStream
import org.apache.commons.io.FileUtils
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object MusicCache {
    private val currentlyDownloading = ConcurrentHashMap<Path, Deferred<AudioPlaybackInfo?>>()
    private val searchCache = CacheBuilder.newBuilder()
        .expireAfterWrite(1, TimeUnit.DAYS)
        .build<String, String>()

    val ytCache = cacheDir.resolve("youtube")

    init {
        Files.createDirectories(ytCache)
    }

    suspend fun searchYt(query: String): String? {
        return searchCache.getIfPresent(query) ?: run {
            val result = YoutubeDl.search(query) ?: return null
            searchCache.put(query, result)
            result
        }
    }

    fun getAudioStreamFuture(mid: MusicId): CompletableFuture<AudioStream?> =
        GlobalScope.future { getAudioStream(mid) }

    suspend fun getAudioStream(mid: MusicId): AudioStream? {
        val info = getPlaybackInfo(mid) ?: return null
        Analytics.event("Get Audio Stream", false, mid.toNormalString())

        return AudioStreamVelvet(info.input)
    }

    suspend fun getPlaybackInfo(mid: MusicId): AudioPlaybackInfo? {
        val (source, id) = mid
        return when (source) {
            YOUTUBE -> getPlaybackInfo(ytCache.resolve(id)) {
                YoutubeDl.getInfo("https://www.youtube.com/watch?v=$id")
            }
            else -> {
                log.error("Source $source could not be handled")
                null
            }
        }
    }

    private suspend fun getPlaybackInfoFromDownloadInfoAndCache(path: Path, info: DownloadInfo): AudioPlaybackInfo {
        val stream = info.getStream()

        val seekable = FileBufferedSeekableInput(
            stream.stream,
            path,
            stream.size,
        )
        val playbackInfo = AudioPlaybackInfo(
            info.details,
            seekable.slice()
        )

        log.info("Downloading to $path")

        // Create details file
        GlobalScope.launch(Dispatchers.IO) {
            val detailsPath = path.resolveSibling(path.fileName.toString() + ".details")
            val json = Gson().toJson(info.details)
            FileUtils.writeStringToFile(detailsPath.toFile(), json, StandardCharsets.UTF_8)
        }

        // Begin downloading AudioStream
        GlobalScope.launch(Dispatchers.IO) {
            log.info("Downloading on another thread... $path")
            seekable.consumeInputStream()
            log.info("Finished downloading $path")
            currentlyDownloading.remove(path)
        }

        return playbackInfo
    }

    private fun getPlaybackInfoFromPath(path: Path): AudioPlaybackInfo {
        val detailsPath = path.resolveSibling(path.fileName.toString() + ".details")

        val seekable = FileSeekableInput(path.toFile().inputStream())
        val details = Gson().fromJson<AudioDetails>(detailsPath.toFile().bufferedReader())

        return AudioPlaybackInfo(details, seekable)
    }

    private suspend fun getPlaybackInfo(
        path: Path,
        getDownloadInfo: suspend () -> DownloadInfo?
    ): AudioPlaybackInfo? {
        try {
            log.info("Try using existing file: $path")
            return getPlaybackInfoFromPath(path)
        } catch (ex: Exception) {
            log.info("Failed to use existing file... downloading to $path")
            val cached = currentlyDownloading.computeIfAbsent(path) {
                GlobalScope.async {
                    val info = getDownloadInfo() ?: return@async null
                    getPlaybackInfoFromDownloadInfoAndCache(path, info)
                }
            }

            val info = cached.await() ?: return null
            return AudioPlaybackInfo(
                info.details,
                (info.input as FileBufferedSeekableInput).slice()
            )
        }
    }


}

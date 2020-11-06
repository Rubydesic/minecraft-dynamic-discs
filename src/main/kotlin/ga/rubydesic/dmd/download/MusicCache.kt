package ga.rubydesic.dmd.download

import com.zakgof.velvetvideo.ISeekableInput
import com.zakgof.velvetvideo.impl.FileSeekableInput
import ga.rubydesic.dmd.util.FileBufferedSeekableInput
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object MusicCache {

	private val currentlyDownloading = ConcurrentHashMap<Path, FileBufferedSeekableInput>()
	val ioPool: ExecutorService = Executors.newCachedThreadPool()

	fun getSeekableInput(path: Path, getUrl: () -> CompletableFuture<String?>): CompletableFuture<ISeekableInput?> {
        val input = currentlyDownloading[path]

        return when {
            input != null -> completedFuture(input.slice())
            Files.exists(path) -> {
                println("Using existing file: $path")
                completedFuture(FileSeekableInput(path.toFile().inputStream()))
            }
            else -> {
                getUrl().thenApply { url ->
                    if (url == null) return@thenApply null

                    println("Downloading to $path from $url")
                    val conn = URL(url).openConnection()
                    val seekable = FileBufferedSeekableInput(
                        conn.getInputStream(),
                        path,
                        conn.contentLength
                    )
                    currentlyDownloading[path] = seekable
                    ioPool.execute {
                        println("Downloading on another thread... $path")
                        seekable.consumeInputStream()
                        println("Finished downloading $path")
                        currentlyDownloading.remove(path)
                    }
                    seekable.slice()
                }
            }
        }


	}


}
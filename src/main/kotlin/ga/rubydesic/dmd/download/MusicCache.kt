package ga.rubydesic.dmd.download

import com.zakgof.velvetvideo.ISeekableInput
import com.zakgof.velvetvideo.impl.FileSeekableInput
import ga.rubydesic.dmd.util.FileBufferedSeekableInput
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object MusicCache {

	private val currentlyDownloading = ConcurrentHashMap<Path, FileBufferedSeekableInput>()
	val ioPool: ExecutorService = Executors.newCachedThreadPool()

	fun getSeekableInput(path: Path, url: String): ISeekableInput {
        val future = currentlyDownloading[path]

        return if (future != null) {
            future.slice()
        } else if (Files.exists(path)) {
			println("Using existing file: $path")
			FileSeekableInput(path.toFile().inputStream())
		} else {
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
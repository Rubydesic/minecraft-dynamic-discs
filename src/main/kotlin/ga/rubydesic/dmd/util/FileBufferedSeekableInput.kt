package ga.rubydesic.dmd.util

import com.zakgof.velvetvideo.ISeekableInput
import ga.rubydesic.dmd.log
import java.io.InputStream
import java.lang.Byte.toUnsignedInt
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileChannel.MapMode.READ_WRITE
import java.nio.file.Path
import java.nio.file.StandardOpenOption.*
import kotlin.math.min

private const val READ_BUFFER_SIZE = 4096

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
open class FileBufferedSeekableInput : InputStream, ISeekableInput {

    private val input: InputStream
    private val size: Int
    private val writeBuffer: ByteBuffer
    private val readBuffer: ByteBuffer

    // The position of the read buffer
    private var position
        get() = readBuffer.position()
        set(value) {
            readBuffer.position(value)
        }

    private val lock: Object
    private var beganConsuming: Boolean

    constructor(input: InputStream, file: Path, size: Int) {
        this.input = input
        this.size = size
        this.writeBuffer = FileChannel.open(file, CREATE, WRITE, READ).map(READ_WRITE, 0, size.toLong())
        this.readBuffer = this.writeBuffer.slice()
        this.lock = Object()
        this.beganConsuming = false
    }

    private constructor(input: InputStream, size: Int, writeBuffer: ByteBuffer, readBuffer: ByteBuffer, lock: Object) {
        this.input = input
        this.writeBuffer = writeBuffer
        this.size = size
        this.readBuffer = readBuffer
        this.lock = lock
        this.beganConsuming = true
    }

    fun slice(): FileBufferedSeekableInput =
        FileBufferedSeekableInput(input, size, writeBuffer, readBuffer.slice(), lock)

    // This should be called on a separate producer thread
    open fun consumeInputStream() {
        if (beganConsuming) throw IllegalStateException("Already began consuming")
        beganConsuming = true

        val buf = ByteArray(READ_BUFFER_SIZE)
        var len = input.read(buf)
        var read = 0

        while (len != -1) {
            synchronized(lock) {
                read += len
                writeBuffer.put(buf, 0, len)
                len = input.read(buf)
                lock.notifyAll()
            }
            if (read / READ_BUFFER_SIZE % 100 == 0) {
                log.debug("Read $read (${writeBuffer.position()}) bytes, size = $size")
                log.debug("%.1f%% downloaded".format(writeBuffer.position().toDouble() / size * 100))
            }
        }
    }

    override fun read(bytes: ByteArray, offset: Int, len: Int): Int {
        return when {
            position >= size -> -1
            // There's more than "len" left in the input stream, but it hasn't been produced yet
            (position + len > writeBuffer.position()) && (position + len <= size) -> {
                synchronized(lock) {
                    while ((position + len > writeBuffer.position()) && (position + len <= size)) {
                        log.info("Tried to read too much, need to read to ${position + len} / ${writeBuffer.position()}, waiting")
                        lock.wait()
                    }

                    // Read it
                    readBuffer.get(bytes, offset, len)
                    len
                }
            }
            else -> {
                val remaining = min(size - position, len)
                readBuffer.get(bytes, offset, remaining)
                remaining
            }
        }
    }

    override fun read(): Int {
        return when {
            position + 1 >= size -> -1
            position + 1 > writeBuffer.position() -> {
                synchronized(lock) {
                    log.info("Tried to read too much, ${position + 1} / ${writeBuffer.position()}, waiting")
                    lock.wait()
                }
                read()
            }
            else -> {
                toUnsignedInt(readBuffer.get())
            }
        }
    }

    override fun close() {
        input.close()
    }

    override fun seek(position: Long) {
        this.position = position.toInt()
    }

    override fun size(): Long = size.toLong()
}


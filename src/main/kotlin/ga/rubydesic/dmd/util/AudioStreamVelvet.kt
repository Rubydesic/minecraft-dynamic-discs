package ga.rubydesic.dmd.util

import com.zakgof.velvetvideo.ISeekableInput
import com.zakgof.velvetvideo.impl.VelvetVideoLib
import ga.rubydesic.dmd.getInt
import ga.rubydesic.dmd.getShort
import ga.rubydesic.dmd.setShort
import net.minecraft.client.sounds.AudioStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED


class AudioStreamVelvet(input: ISeekableInput, private val convertToMono: Boolean = true) : AudioStream {

    private val demuxer = VelvetVideoLib.getInstance().demuxer(input)
    private val stream = demuxer.audioStream(0)

    private val realFormat = stream.properties().format()

    private val desiredFormat = AudioFormat(
        PCM_SIGNED,
        realFormat.sampleRate,
        realFormat.sampleSizeInBits,
        if (convertToMono) 1 else 2,
        realFormat.frameSize,
        realFormat.frameRate,
        false
    )

    init {
        require(
            realFormat.isBigEndian == (ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN)
                    && realFormat.encoding == PCM_SIGNED
                    && (realFormat.channels == 2 || realFormat.channels == 1)
        ) { "Don't know how to handle the format: $realFormat" }
    }

    override fun close() {
        demuxer.close()
    }

    override fun getFormat() = desiredFormat

    override fun read(len: Int): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(len + 8192)
        val iter = stream.iterator()

        while (buffer.position() < len && iter.hasNext()) {
            val samples = iter.next().samples()
            val size = convertToMono(samples)
            buffer.put(samples, 0, size)
        }

        buffer.flip()

        return buffer
    }

    private fun convertToMono(samples: ByteArray): Int {
        var finalSize = samples.size

        if (realFormat.sampleSizeInBits == 32) {
            for (i in 0 until finalSize step 4) {
                val sample = samples.getInt(i)
                // Shift the sample 16 bits to the right to convert from
                // 32 bits to 16 bits. Divide by 10 to decrease maximum volume.
                val result = (sample shr 16) / 10
                samples.setShort(i, result.toShort())
            }
            for (i in 0 until finalSize step 4) {
                samples[i / 2] = samples[i]
                samples[i / 2 + 1] = samples[i + 1]
            }
            finalSize /= 2
        }

        if (realFormat.channels == 2 && convertToMono) {
            for (i in 0 until finalSize step 4) {
                val sample1 = samples.getShort(i)
                val sample2 = samples.getShort(i + 2)
                val result = (sample1 + sample2) / 2
                samples.setShort(i, result.toShort())
            }
            for (i in 0 until finalSize step 4) {
                samples[i / 2] = samples[i]
                samples[i / 2 + 1] = samples[i + 1]
            }

            finalSize /= 2
        }

        return finalSize
    }
}
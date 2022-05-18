package ga.rubydesic.dmd.util

import com.zakgof.velvetvideo.ISeekableInput
import com.zakgof.velvetvideo.impl.VelvetVideoLib
import net.minecraft.client.sound.AudioStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED

class AudioStreamVelvet(
    input: ISeekableInput,
    private val convertToMono: Boolean = false
)
    : AudioStream {
    private val demuxer = VelvetVideoLib.getInstance().demuxer(input)
    private val stream = demuxer.audioStream(0)

    private val realFormat = stream.properties().format()

    private val desiredFormat = AudioFormat(
        PCM_SIGNED,
        realFormat.sampleRate,
        16,
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

    override fun getBuffer(len: Int): ByteBuffer {
        VelvetVideoLib.getInstance().audioEncoder("pcm_u16be", desiredFormat)
        val buffer = ByteBuffer.allocateDirect(len + 8192)
        val iter = stream.iterator()

        while (buffer.position() < len && iter.hasNext()) {
            val samples = iter.next().samples()
            val size = decreaseBitDepth(samples)
            buffer.put(samples, 0, size)
        }

        buffer.flip()

        return buffer
    }

    private fun decreaseBitDepth(samples: ByteArray): Int {
        var finalSize = samples.size

        if (realFormat.sampleSizeInBits == 32) {
            for (i in 0 until finalSize step 4) {
                val sample = samples.getInt(i)
                // Shift the sample 16 bits to the right to convert from
                // 32 bits to 16 bits. Divide by 10 to decrease maximum volume.
                val result = sample.shr(16) / 10
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
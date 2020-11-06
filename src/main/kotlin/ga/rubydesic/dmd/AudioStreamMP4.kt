package ga.rubydesic.dmd

import net.minecraft.client.sounds.AudioStream
import net.sourceforge.jaad.aac.Decoder
import net.sourceforge.jaad.aac.SampleBuffer
import net.sourceforge.jaad.mp4.MP4Container
import net.sourceforge.jaad.mp4.api.AudioTrack
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import javax.sound.sampled.AudioFormat

class AudioStreamMP4 : AudioStream {

    private val container: MP4Container
    private val track: AudioTrack
    private val decoder: Decoder
    private val buf: SampleBuffer
    private val format: AudioFormat

    constructor(stream: InputStream) : this(MP4Container(stream))
    constructor(file: RandomAccessFile) : this(MP4Container(file))

    constructor(container: MP4Container) {
        this.container = container
        track = container.movie.getTracks(AudioTrack.AudioCodec.AAC).first() as AudioTrack
        decoder = Decoder(track.decoderSpecificInfo)
        buf = SampleBuffer()
        buf.isBigEndian = false
        // We need to be LITTLE ENDIAN for OpenAL to work
        format = AudioFormat(track.sampleRate.toFloat(), track.sampleSize, track.channelCount, true, false)
    }

    override fun close() {
    }


    override fun getFormat(): AudioFormat {
        return format
    }

    override fun read(i: Int): ByteBuffer {
        // not sure whats up with the magic overflow # but Minecraft uses it so whatever I guess
        val buffer = ByteBuffer.allocateDirect(i + 8192)

        while (buffer.position() < i && track.hasMoreFrames()) {
            decoder.decodeFrame(track.readNextFrame().data, buf)
            // ditto
            buffer.put(buf.data)
        }

        buffer.flip()
        return buffer
    }
}
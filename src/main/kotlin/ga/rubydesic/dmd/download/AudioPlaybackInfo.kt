package ga.rubydesic.dmd.download

import com.zakgof.velvetvideo.ISeekableInput

data class AudioPlaybackInfo(
    val details: AudioDetails,
    val input: ISeekableInput
)
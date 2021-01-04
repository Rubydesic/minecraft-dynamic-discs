package ga.rubydesic.dmd.download

import ga.rubydesic.dmd.util.SizedInputStream

data class DownloadInfo(
    val getStream: suspend () -> SizedInputStream,
    val details: AudioDetails
)
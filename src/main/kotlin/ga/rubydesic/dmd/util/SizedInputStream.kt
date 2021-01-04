package ga.rubydesic.dmd.util

import java.io.InputStream

data class SizedInputStream(
    val stream: InputStream,
    val size: Int
)
package ga.rubydesic.dmd.download

import ga.rubydesic.dmd.toHex

data class MusicId(
    val source: MusicSource,
    val id: String
) {
    override fun toString(): String {
        return "${source.short}-${id.toHex()}"
    }

    fun toNormalString(): String {
        return "${source.short}-${id}"
    }
}
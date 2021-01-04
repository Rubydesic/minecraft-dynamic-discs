package ga.rubydesic.dmd.download

enum class MusicSource(
    val short: String
) {

    YOUTUBE("yt");

    companion object {
        val values = values().toList()
    }
}
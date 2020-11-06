package ga.rubydesic.dmd

import java.nio.ByteOrder
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.Supplier
import java.util.regex.Pattern

fun ByteArray.setShort(index: Int, value: Short) = setShort(index, value, ByteOrder.nativeOrder())

fun ByteArray.setShort(index: Int, value: Short, order: ByteOrder) {
    if (order == ByteOrder.LITTLE_ENDIAN) {
        this[index] = value.toByte()
        this[index + 1] = (value.toInt() shr 8).toByte()
    } else {
        this[index + 1] = value.toByte()
        this[index] = (value.toInt() shr 8).toByte()
    }
}

fun ByteArray.getShort(index: Int) = getShort(index, ByteOrder.nativeOrder())

fun ByteArray.getShort(index: Int, order: ByteOrder): Short {
    return if (order == ByteOrder.LITTLE_ENDIAN) {
        ((this[index + 1].toInt() shl 8) or (this[index].toInt() and 0xFF)).toShort()
    } else {
        ((this[index].toInt() shl 8) or (this[index + 1].toInt() and 0xFF)).toShort()
    }
}

// https://stackoverflow.com/questions/923863/converting-a-string-to-hexadecimal-in-java


fun toHexString(ba: ByteArray): String {
    val str = StringBuilder()
    for (i in ba.indices) str.append(String.format("%x", ba[i]))
    return str.toString()
}

fun toHexString(s: String) = toHexString(s.toByteArray())
fun fromHexString(hex: String): String {
    val str = StringBuilder()
    var i = 0
    while (i < hex.length) {
        str.append(hex.substring(i, i + 2).toInt(16).toChar())
        i += 2
    }
    return str.toString()
}


private val youtubeLocatorRegex = Pattern.compile("(?:sounds/)?yt-(\\w+)(?:\\.ogg)?")

fun getYoutubeVideoId(s: String): String? {
    val matcher = youtubeLocatorRegex.matcher(s)
    return if (matcher.matches())
        fromHexString(matcher.group(1))
    else
        null
}

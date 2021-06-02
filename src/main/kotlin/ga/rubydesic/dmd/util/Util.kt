package ga.rubydesic.dmd

import com.google.gson.Gson
import java.io.Reader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.nio.ByteOrder
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.regex.Pattern
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

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

inline fun <reified T> Gson.fromJson(json: Reader) = fromJson(json, T::class.java)
inline fun <reified T> Gson.fromJson(json: String) = fromJson(json, T::class.java)

fun runCommand(vararg args: String): String {
    val stream = Runtime.getRuntime().exec(args).inputStream
    return stream.reader().readText()
}

// https://stackoverflow.com/questions/923863/converting-a-string-to-hexadecimal-in-java
fun toHexString(ba: ByteArray): String {
    val str = StringBuilder()
    for (i in ba.indices) str.append(String.format("%x", ba[i]))
    return str.toString()
}

fun toHexString(s: String) = toHexString(s.toByteArray())
fun String.toHex() = toHexString(this)
fun ByteArray.toHex() = toHexString(this)

const val USER_AGENT = "Rubydesic Dynamic Discs"

// disable ssl checks because mojang java doesn't have ssl public keys
private val allTrustingSSLSocketFactory = run {
    val trustAllCerts: TrustManager = object : X509TrustManager {
        override fun checkClientTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
        override fun checkServerTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate>? = null
    }

    val sc = SSLContext.getInstance("SSL")
    sc.init(null, arrayOf(trustAllCerts), SecureRandom())

    sc.socketFactory
}

fun openInsecureConnection(url: String): URLConnection {
    val conn = URL(url).openConnection()

    if (conn is HttpsURLConnection) {
        conn.sslSocketFactory = allTrustingSSLSocketFactory
    }

    return conn
}

fun httpPost(url: String, body: ByteArray, fakeUserAgent: Boolean = false): Int {
    val conn = openInsecureConnection(url) as HttpURLConnection

    conn.requestMethod = "POST"
    conn.doOutput = true

    val mozillaAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36"
    val userAgent = if (fakeUserAgent) mozillaAgent else USER_AGENT

    conn.setRequestProperty("User-Agent", userAgent)
    conn.setRequestProperty("Content-Length", body.size.toString())
    conn.outputStream.apply {
        write(body)
        close()
    }

    return conn.responseCode
}


fun fromHexString(hex: String): String {
    val str = StringBuilder()
    var i = 0
    while (i < hex.length) {
        str.append(hex.substring(i, i + 2).toInt(16).toChar())
        i += 2
    }
    return str.toString()
}

// https://stackoverflow.com/questions/2591083/getting-java-version-at-runtime
fun getJavaVersion(): String {
    var version = System.getProperty("java.version")
    if (version.startsWith("1.")) {
        version = version.substring(2, 3)
    } else {
        val dot = version.indexOf(".")
        if (dot != -1) {
            version = version.substring(0, dot)
        }
    }
    return version
}

private val youtubeLocatorRegex = Pattern.compile("(?:sounds/)?yt-(\\w+)(?:\\.ogg)?")

fun getYoutubeVideoId(s: String): String? {
    val matcher = youtubeLocatorRegex.matcher(s)
    return if (matcher.matches())
        fromHexString(matcher.group(1))
    else
        null
}

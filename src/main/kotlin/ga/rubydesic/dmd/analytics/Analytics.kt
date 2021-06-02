package ga.rubydesic.dmd.analytics

import ga.rubydesic.dmd.*
import kotlinx.coroutines.*
import net.minecraft.client.Minecraft
import org.apache.commons.io.FileUtils
import java.net.URLEncoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*

object Analytics {

    val id: Deferred<String>

    private val idPath = dir.resolve(".uid")
    private const val TRACKING_CODE = "UA-156429029-1"

    init {
        id = GlobalScope.async(Dispatchers.IO) {
            val idFile = idPath.toFile()

            try {
                FileUtils.readFileToString(idFile, StandardCharsets.UTF_8)
            } catch (ex: Exception) {
                val generated = UUID.randomUUID().toString()
                launch { FileUtils.writeStringToFile(idFile, generated, StandardCharsets.UTF_8) }
                generated
            }
        }
    }

    private val language get() = if (isDedicatedServer) null else Minecraft.getInstance()?.languageManager?.selected?.code
    private val javaVersion = getJavaVersion()
    private val isAnalyticsDisabled get() =
        System.getProperty("dmd.analytics") == "disable" || !config.analytics

    fun event(
        action: String,
        isLogicalServer: Boolean? = null,
        label: String? = null,
        value: Int = -1,
        session: Boolean? = null
    ): Job {
        val category = when {
            isDedicatedServer -> "Dedicated Server"
            isLogicalServer == null -> "General Client"
            isLogicalServer -> "Integrated Server"
            else -> "Client"
        }

        return event(action, category, label, value, session)
    }

    fun event(
        action: String,
        category: String,
        label: String? = null,
        value: Int = -1,
        newSession: Boolean? = null
    ): Job = GlobalScope.launch {
        try {
            val params = mutableListOf(
                Pair("v", "1"),
                Pair("t", "event"),
                Pair("cid", id.await()),
                Pair("ua", USER_AGENT),
                Pair("tid", TRACKING_CODE),
                Pair("ea", action),
                Pair("ec", category),
                Pair("an", MOD_ID),
                Pair("av", MOD_VERSION),
                Pair("cd1", javaVersion)
            )

            if (newSession == true) params.add(Pair("sc", "start"))
            else if (newSession == false) params.add(Pair("sc", "end"))

            val language = language
            if (language != null) params.add(Pair("ul", language))
            if (label != null) params.add(Pair("el", label))
            if (value > -1) params.add(Pair("ev", value.toString()))

            sendToAnalytics(params)
        } catch (ex: Exception) {
            // just log it, is ok if analytics are broken
            log.debug(ex.stackTraceToString())
        }
    }

    private suspend fun sendToAnalytics(params: Collection<Pair<String, String>>) = withContext(Dispatchers.IO) {
        if (isAnalyticsDisabled) return@withContext

        httpPost("https://www.google-analytics.com/collect", toQueryParams(params).toByteArray())
    }

}

fun toQueryParams(
    params: Collection<Pair<String, String>>,
    charset: Charset = StandardCharsets.UTF_8
): String {
    val params = params.map { (key, value) ->
        Pair(
            URLEncoder.encode(key, charset.name()),
            URLEncoder.encode(value, charset.name())
        )
    }

    if (params.isEmpty()) return ""
    val builder = StringBuilder()

    val iter = params.iterator()
    val first = iter.next()

    val (firstKey, firstValue) = first
    builder.append("$firstKey=$firstValue");

    for ((key, value) in iter) {
        builder.append("&$key=$value")
    }

    return builder.toString()
}

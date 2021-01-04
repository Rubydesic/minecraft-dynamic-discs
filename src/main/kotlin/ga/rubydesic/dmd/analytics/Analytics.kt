package ga.rubydesic.dmd.analytics

import ga.rubydesic.dmd.*
import kotlinx.coroutines.*
import net.fabricmc.api.EnvType
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import org.apache.commons.io.FileUtils
import org.apache.http.NameValuePair
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.message.BasicNameValuePair
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

    val isDedicatedServer = FabricLoader.getInstance().environmentType == EnvType.SERVER
    val language get() = Minecraft.getInstance()?.languageManager?.selected?.code
    val javaVersion = getJavaVersion()

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
        val params = mutableListOf(
            BasicNameValuePair("v", "1"),
            BasicNameValuePair("t", "event"),
            BasicNameValuePair("cid", id.await()),
            BasicNameValuePair("ua", USER_AGENT),
            BasicNameValuePair("tid", TRACKING_CODE),
            BasicNameValuePair("ea", action),
            BasicNameValuePair("ec", category),
            BasicNameValuePair("an", MOD_ID),
            BasicNameValuePair("av", MOD_VERSION),
            BasicNameValuePair("cd1", javaVersion)
        )

        if (newSession == true) params.add(BasicNameValuePair("sc", "start"))
        else if (newSession == false) params.add(BasicNameValuePair("sc", "end"))

        if (language != null) params.add(BasicNameValuePair("ul", language))
        if (label != null) params.add(BasicNameValuePair("el", label))
        if (value > -1) params.add(BasicNameValuePair("ev", value.toString()))

        sendToAnalytics(params)
    }

    private suspend fun sendToAnalytics(params: Collection<NameValuePair>) = withContext(Dispatchers.IO) {
        val body = URLEncodedUtils.format(params, StandardCharsets.UTF_8)

        httpPost("https://www.google-analytics.com/collect", body.toByteArray())
    }

}
package ga.rubydesic.dmd

import ga.rubydesic.dmd.analytics.Analytics
import ga.rubydesic.dmd.game.ClientboundPlayMusicPacket
import ga.rubydesic.dmd.game.DynamicRecordItem
import kotlinx.coroutines.*
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.minecraft.client.Minecraft
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.SystemUtils
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.File
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


// For support join https://discord.gg/v6v4pMv

const val MOD_ID = "dynamic-discs"
const val MOD_VERSION = "2.0.0"

val dir: Path = Paths.get("dynamic-discs")
val cacheDir: Path = dir.resolve("cache")

val dynamicRecordItem = DynamicRecordItem(Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(1))

val log: Logger = LogManager.getLogger("Dynamic Discs")
lateinit var ytdlBinaryFuture: Deferred<Path>

private val mcClientDispatcher = Minecraft.getInstance().asCoroutineDispatcher()
val Dispatchers.McClient get() = mcClientDispatcher


@Suppress("unused")
fun init() {
    log.info("Dynamic Discs Loaded!")
    Analytics.event("Start Game", session = true)
    Registry.register(Registry.ITEM, ResourceLocation(MOD_ID, "dynamic_disc"), dynamicRecordItem)
    ClientboundPlayMusicPacket.register(ClientSidePacketRegistry.INSTANCE)

    deleteOldCache()
    clearCacheIfOutdated()
    downloadYtDlBinary()
}

fun deleteOldCache() {
    FileUtils.deleteDirectory(File("dynamic-discs-cache"))
}

fun clearCacheIfOutdated() {
    try {
        val version = "1"
        val versionFile = dir.resolve("version").toFile()
        val actualVersion = FileUtils.readFileToString(versionFile, StandardCharsets.UTF_8)
        if (!versionFile.exists() || actualVersion != version) {
            log.info("Detected outdated cache version $actualVersion instead of $version, deleting it")
            FileUtils.deleteDirectory(cacheDir.toFile())
            FileUtils.writeStringToFile(versionFile, version, StandardCharsets.UTF_8)
        }
    } catch (ex: Exception) {
        log.error("Could not delete cache", ex)
    }
}

fun downloadYtDlBinary() {
    val path = dir.resolve(if (SystemUtils.IS_OS_WINDOWS) "youtube-dl.exe" else "youtube-dl")
    if (Files.exists(path)) {
        log.info("youtube-dl binary already exists, running --update")
        ytdlBinaryFuture = GlobalScope.async(Dispatchers.IO) {
            runCommand(path.toAbsolutePath().toString(), "--update")
            path
        }
    } else {
        val downloadUrl = if (SystemUtils.IS_OS_WINDOWS) {
            log.info("Detected OS is Windows, downloading Windows youtube-dl binary")
            "https://yt-dl.org/downloads/latest/youtube-dl.exe"
        } else {
            log.info("Detected OS is not Windows, downloading MacOS/Linux youtube-dl binary")
            "https://yt-dl.org/downloads/latest/youtube-dl"
        }

        ytdlBinaryFuture = GlobalScope.async(Dispatchers.IO) {
            FileUtils.copyURLToFile(URL(downloadUrl), path.toFile())
            log.info("Finished downloading youtube-dl binary")
            path
        }
    }
}


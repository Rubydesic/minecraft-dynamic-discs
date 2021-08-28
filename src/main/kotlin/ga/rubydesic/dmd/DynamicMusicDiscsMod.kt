package ga.rubydesic.dmd

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import ga.rubydesic.dmd.analytics.Analytics
import ga.rubydesic.dmd.game.ClientboundPlayMusicPacket
import ga.rubydesic.dmd.game.DynamicRecordItem
import ga.rubydesic.dmd.util.using
import kotlinx.coroutines.*
import net.fabricmc.api.EnvType
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.SystemUtils
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermissions


// For support join https://discord.gg/pPAabdafJU

const val MOD_ID = "dynamic-discs"
const val MOD_VERSION = "2.0.7"

val dir: Path = Paths.get("dynamic-discs")
val cacheDir: Path = dir.resolve("cache")


val dynamicRecordItem = DynamicRecordItem(Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(1))

val log: Logger = LogManager.getLogger("Dynamic Discs")
lateinit var ytdlBinaryFuture: Deferred<Path>
var config = Config()

val isDedicatedServer = FabricLoader.getInstance().environmentType == EnvType.SERVER

@Suppress("unused")
fun init() {
    log.info("Dynamic Discs Loaded!")

    Files.createDirectories(dir)
    readConfig()
    setupVelvet()

    log.info("Natives extracted!")

    Analytics.event("Start Game", session = true)
    Runtime.getRuntime().addShutdownHook(Thread {
        runBlocking {
            Analytics.event("End Game", session = false).join()
        }
    })

    Registry.register(Registry.ITEM, ResourceLocation(MOD_ID, "dynamic_disc"), dynamicRecordItem)

    if (!isDedicatedServer) {
        ClientboundPlayMusicPacket.register(ClientSidePacketRegistry.INSTANCE)
    }

    deleteOldCache()
    clearCacheIfOutdated()
    downloadYtDlBinary()
}

fun readConfig() {
    val configPath = dir.resolve("config.json")
    if (Files.exists(configPath)) {
        try {
            Files.newInputStream(configPath).bufferedReader().use { reader ->
                config = Gson().fromJson(reader)
            }
        } catch (ex: Exception) {
            log.info("Failed to read config from config.json")
            return
        }
    }
    Files.newOutputStream(configPath).bufferedWriter().use { writer ->
        GsonBuilder().setPrettyPrinting().create().toJson(config, writer)
    }
}

fun setupVelvet() {
    val target = Paths.get(System.getProperty("user.home"), ".velvet-video", "natives", "0.2.7.full")
    Files.createDirectories(target)

    if (SystemUtils.IS_OS_WINDOWS) {
        val resource = "velvet-video-natives/windows64"
        extractFile("swscale-5.dll", resource, target)
        extractFile("swresample-3.dll", resource, target)
        extractFile("libopenh264.dll", resource, target)
        extractFile("libgcc_s_seh-1.dll", resource, target)
        extractFile("avutil-56.dll", resource, target)
        extractFile("avformat-58.dll", resource, target)
        extractFile("avfilter-7.dll", resource, target)
        extractFile("avcodec-58.dll", resource, target)
    } else {
        val resource = "velvet-video-natives/linux64"
        extractFile("libswscale.so.5", resource, target)
        extractFile("libswresample.so.3", resource, target)
        extractFile("libopenh264.so.5", resource, target)
        extractFile("libavutil.so.56", resource, target)
        extractFile("libavformat.so.58", resource, target)
        extractFile("libavfilter.so.7", resource, target)
        extractFile("libavcodec.so.58", resource, target)
    }

}

private fun extractFile(name: String, base: String, target: Path) {
    val dest = target.resolve(name)
    if (!Files.exists(dest)) {
        log.info("Extracting /$base/$name...")
        Files.copy(object {}.javaClass.getResourceAsStream("/$base/$name"), dest)
    }
}

fun deleteOldCache() {
    FileUtils.deleteDirectory(File("dynamic-discs-cache"))
}

fun clearCacheIfOutdated() {
    try {
        val version = "1"
        val versionFile = dir.resolve(".version").toFile()
        if (versionFile.exists()) {
            val actualVersion = FileUtils.readFileToString(versionFile, StandardCharsets.UTF_8)
            if (actualVersion != version) {
                log.info("Detected outdated cache version $actualVersion instead of $version, deleting it")
                FileUtils.deleteDirectory(cacheDir.toFile())
                FileUtils.writeStringToFile(versionFile, version, StandardCharsets.UTF_8)
            }
        } else {
            // Create version file if not exists
            FileUtils.writeStringToFile(versionFile, version, StandardCharsets.UTF_8)
        }
    } catch (ex: Exception) {
        log.error("Could not delete cache", ex)
    }
}

fun downloadYtDlBinary() {
    val path = dir.resolve(if (SystemUtils.IS_OS_WINDOWS) "youtube-dl.exe" else "youtube-dl").toAbsolutePath()
    if (Files.exists(path)) {
        log.info("youtube-dl binary already exists")
        setYTDLPermission(path)

        log.info("Running youtube-dl --update")
        ytdlBinaryFuture = GlobalScope.async(Dispatchers.IO) {
            runCommand(path.toString(), "--update")
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
            using(
                openInsecureConnection(downloadUrl).getInputStream(),
                Files.newOutputStream(path)
            ) { input, output ->
                input.copyTo(output)
            }

            log.info("Finished downloading youtube-dl binary")
            setYTDLPermission(path)
            path
        }

    }
}

fun setYTDLPermission(path: Path){
    try {
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxrwxrwx"));
        log.info("Made youtube-dl binary executable")
    } catch (ex: Exception) {
        log.info("Failed to make youtube-dl binary executable...", ex)
    }
}



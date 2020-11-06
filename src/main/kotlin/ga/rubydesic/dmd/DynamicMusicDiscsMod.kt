package ga.rubydesic.dmd

import ga.rubydesic.dmd.download.MusicCache
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.minecraft.commands.Commands.literal
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import java.nio.file.Paths

// For support join https://discord.gg/v6v4pMv

const val MOD_ID = "dmd";
val cacheDir = Paths.get("dynamic-discs-cache")

val dynamicRecordItem = DynamicRecordItem(Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(1))

@Suppress("unused")
fun init() {
    // This code runs as soon as Minecraft is in a mod-load-ready state.
    // However, some things (like resources) may still be uninitialized.
    // Proceed with mild caution.

    println("Hello Fabric world!")
    Registry.register(Registry.ITEM, ResourceLocation(MOD_ID, "dynamic_disc"), dynamicRecordItem)
    ClientboundPlayMusicPacket.register(ClientSidePacketRegistry.INSTANCE)

//    CommandRegistrationCallback.EVENT.register { dispatcher, dedicated ->
//        dispatcher.register(literal("dmd").executes { ctx ->
//            MusicCache.currentlyDownloading.clear()
//            println("cleared")
//            1
//        })
//    }

}


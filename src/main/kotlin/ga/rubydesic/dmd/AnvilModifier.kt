package ga.rubydesic.dmd

import ga.rubydesic.dmd.download.YoutubeDownload
import net.minecraft.nbt.StringTag
import net.minecraft.network.chat.TextComponent
import net.minecraft.server.MinecraftServer
import net.minecraft.world.item.ItemStack
import java.util.*

fun modifyAnvilResult(result: ItemStack, server: MinecraftServer) {
	if (result.item == dynamicRecordItem) {
		val name = result.displayName.string.substring(1, result.displayName.string.length - 1)
		YoutubeDownload.getTopResultId(name)
			.thenAccept { id ->
				YoutubeDownload.getYtTitle(id).thenAcceptAsync({ title ->
					result.hoverName = TextComponent(title)
					result.tag?.put("music_id", StringTag.valueOf("yt-$id"))
					server.playerList.players.forEach {
						it.sendMessage(TextComponent("Sent Message"), UUID.randomUUID())
					}
				}, server)
			}
	}
}
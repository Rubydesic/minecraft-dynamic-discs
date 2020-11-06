package ga.rubydesic.dmd

import ga.rubydesic.dmd.download.YoutubeDownload
import io.netty.buffer.Unpooled
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry
import net.fabricmc.fabric.api.server.PlayerStream
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.JukeboxBlock

class DynamicRecordItem(properties: Properties?) : Item(properties) {

	override fun useOn(ctx: UseOnContext): InteractionResult? {
		val level = ctx.level
		val blockPos = ctx.clickedPos
		val blockState = level.getBlockState(blockPos)
		return if (blockState.`is`(Blocks.JUKEBOX) && !blockState.getValue(JukeboxBlock.HAS_RECORD)) {
			val itemStack = ctx.itemInHand
			if (!level.isClientSide) {
				(Blocks.JUKEBOX as JukeboxBlock).setRecord(level, blockPos, blockState, itemStack)

				playSound(ctx)

				itemStack.shrink(1)
				val player = ctx.player
				player?.awardStat(Stats.PLAY_RECORD)
			}
			InteractionResult.sidedSuccess(level.isClientSide)
		} else {
			InteractionResult.PASS
		}
	}

	fun playSound(ctx: UseOnContext) {
		val item = ctx.itemInHand
		val name = item.displayName.string.substring(1, item.displayName.string.length - 1)

		YoutubeDownload.getTopResultId(name).thenAccept { youtubeId ->
            if (youtubeId == null) return@thenAccept
			PlayerStream.watching(ctx.level, ctx.clickedPos).forEach { player ->
				val data = FriendlyByteBuf(Unpooled.buffer())
				ClientboundPlayMusicPacket(MusicSource.YOUTUBE, ctx.clickedPos, youtubeId).write(data)
				ServerSidePacketRegistry.INSTANCE.sendToPlayer(player, ClientboundPlayMusicPacket.ID, data)
			}
		}

	}

}
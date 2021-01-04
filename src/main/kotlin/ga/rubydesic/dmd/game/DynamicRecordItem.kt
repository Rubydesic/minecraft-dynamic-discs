package ga.rubydesic.dmd.game

import ga.rubydesic.dmd.McClient
import ga.rubydesic.dmd.analytics.Analytics
import ga.rubydesic.dmd.download.MusicCache
import ga.rubydesic.dmd.download.MusicSource
import io.netty.buffer.Unpooled
import kotlinx.coroutines.*
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

    fun playSound(ctx: UseOnContext) {
        val item = ctx.itemInHand
        val name = run {
            val d = item.displayName.string
            d.substring(1, d.length - 1)
        }

        GlobalScope.launch(Dispatchers.McClient) {
            Analytics.event("Search", false, name)
            val id = MusicCache.searchYt(name)

            if (id == null) {
                println("Could not find a result for the search: $name")
                return@launch
            }
            PlayerStream.watching(ctx.level, ctx.clickedPos).forEach { player ->
                val data = FriendlyByteBuf(Unpooled.buffer())
                ClientboundPlayMusicPacket(MusicSource.YOUTUBE, ctx.clickedPos, id).write(data)
                ServerSidePacketRegistry.INSTANCE.sendToPlayer(player, ClientboundPlayMusicPacket.packetId, data)
            }
        }
    }

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
            Analytics.event("Use Disc", level.isClientSide)
            InteractionResult.sidedSuccess(level.isClientSide)
        } else {
            InteractionResult.PASS
        }
    }

}
package ga.rubydesic.dmd.game

import ga.rubydesic.dmd.MOD_ID
import ga.rubydesic.dmd.download.MusicId
import ga.rubydesic.dmd.download.MusicSource
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.fabricmc.fabric.api.network.PacketConsumer
import net.fabricmc.fabric.api.network.PacketContext
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation

data class ClientboundPlayMusicPacket constructor(
    val source: MusicSource,
    val pos: BlockPos,
    val id: String
) {
    companion object {
        val packetId = ResourceLocation(MOD_ID, "play_music")

        fun register(registry: ClientSidePacketRegistry) {
            registry.register(packetId, object : PacketConsumer {
                override fun accept(ctx: PacketContext, data: FriendlyByteBuf) {
                    val (source, pos, id) = ClientboundPlayMusicPacket(data)
                    ctx.taskQueue.execute {
                        Minecraft.getInstance().levelRenderer
                            .playYoutubeMusic(MusicId(source, id), pos)
                    }
                }
            })
        }
    }

    constructor(buf: FriendlyByteBuf) : this(
        MusicSource.values[buf.readByte().toInt()],
        buf.readBlockPos(),
        buf.readCharSequence(buf.readableBytes(), Charsets.UTF_8).toString()
    )

    fun write(buf: FriendlyByteBuf) {
        buf.writeByte(source.ordinal)
        buf.writeBlockPos(pos)
        buf.writeCharSequence(id, Charsets.UTF_8)
    }

}

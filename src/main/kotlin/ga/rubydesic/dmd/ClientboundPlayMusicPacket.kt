package ga.rubydesic.dmd

import ga.rubydesic.dmd.imixin.ILevelRendererMixin
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
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
        val ID = ResourceLocation(MOD_ID, "play_music")

        fun register(registry: ClientSidePacketRegistry) {
            registry.register(ID) { ctx, data ->
                val (source, pos, id) = ClientboundPlayMusicPacket(data)
                ctx.taskQueue.execute {
                    val lr = Minecraft.getInstance().levelRenderer as ILevelRendererMixin
                    if (source == MusicSource.CANCEL) {
                        lr.playYoutubeMusic(null, pos)
                    } else {
                        lr.playYoutubeMusic(id, pos)
                    }
                }
            }
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
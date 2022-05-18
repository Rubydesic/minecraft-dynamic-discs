package ga.rubydesic.dmd.game

import ga.rubydesic.dmd.MOD_ID
import ga.rubydesic.dmd.download.MusicId
import ga.rubydesic.dmd.download.MusicSource
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
import net.fabricmc.fabric.api.network.PacketConsumer
import net.fabricmc.fabric.api.network.PacketContext
import net.minecraft.client.MinecraftClient
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos

data class ClientboundPlayMusicPacket constructor(
    val source: MusicSource,
    val pos: BlockPos,
    val id: String
) {
    companion object {
        val packetId = Identifier(MOD_ID, "play_music")

        fun register() {
            ClientPlayNetworking
                .registerGlobalReceiver(packetId) { client, _, buf, _ ->
                    val (source, pos, id) = ClientboundPlayMusicPacket(buf)
                    client.executeTask {
                        MinecraftClient.getInstance().worldRenderer
                            .playYoutubeMusic(MusicId(source, id), pos)
                    }
            }
        }
    }

    constructor(buf: PacketByteBuf) : this(
        MusicSource.values[buf.readByte().toInt()],
        buf.readBlockPos(),
        buf.readCharSequence(buf.readableBytes(), Charsets.UTF_8).toString()
    )

    fun write(buf: PacketByteBuf) {
        buf.writeByte(source.ordinal)
        buf.writeBlockPos(pos)
        buf.writeCharSequence(id, Charsets.UTF_8)
    }
}

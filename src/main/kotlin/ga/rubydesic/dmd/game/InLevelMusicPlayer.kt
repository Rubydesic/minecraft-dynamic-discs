package ga.rubydesic.dmd

import com.mojang.math.Vector3d
import ga.rubydesic.dmd.download.MusicCache
import ga.rubydesic.dmd.download.MusicId
import ga.rubydesic.dmd.game.VideoSoundInstance
import ga.rubydesic.dmd.mixin.client.LevelRendererAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.TextComponent

fun LevelRenderer.playYoutubeMusic(id: MusicId, blockPos: BlockPos) {
    val minecraft = Minecraft.getInstance()
    val lra = this as LevelRendererAccess

    val currentlyPlaying: SoundInstance? = lra.playingRecords[blockPos]
    if (currentlyPlaying != null) {
        minecraft.soundManager.stop(currentlyPlaying)
        lra.playingRecords.remove(blockPos)
    }

    GlobalScope.launch(Dispatchers.McClient) {
        val info = MusicCache.getPlaybackInfo(id)
        minecraft.gui.setNowPlaying(TextComponent(info?.details?.title))
    }

    val nowPlaying: SoundInstance = VideoSoundInstance(
        id,
        Vector3d(blockPos.x.toDouble(), blockPos.y.toDouble(), blockPos.z.toDouble())
    )
    lra.playingRecords[blockPos] = nowPlaying
    minecraft.soundManager.play(nowPlaying)
    this.callNotifyNearbyEntities(lra.level, blockPos, true)
}
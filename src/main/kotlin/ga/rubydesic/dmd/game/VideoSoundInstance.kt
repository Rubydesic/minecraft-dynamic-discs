package ga.rubydesic.dmd.game

import com.mojang.math.Vector3d
import ga.rubydesic.dmd.MOD_ID
import ga.rubydesic.dmd.config
import ga.rubydesic.dmd.download.MusicId
import net.minecraft.client.resources.sounds.Sound
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation
import net.minecraft.client.sounds.SoundManager
import net.minecraft.client.sounds.WeighedSoundEvents
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundSource

class VideoSoundInstance @JvmOverloads constructor(
    videoId: MusicId,
    private val position: Vector3d,
    private val isRelative: Boolean = false,
    private val attenuation: Attenuation = Attenuation.LINEAR,
    attenuationDistance: Int = config.attenuationDistance,
    private val volume: Float = 1f,
    private val pitch: Float = 1f,
) : SoundInstance {

    private val loc = ResourceLocation(MOD_ID, videoId.toString())
    override fun getLocation() = loc

    private val weighed = WeighedSoundEvents(loc, null)
    private val sound =
        Sound(loc.toString(), volume, pitch, 1, Sound.Type.SOUND_EVENT, true, false, attenuationDistance)

    override fun resolve(soundManager: SoundManager?) = weighed
    override fun getSound() = sound
    override fun getSource() = SoundSource.RECORDS
    override fun isLooping() = false
    override fun isRelative() = isRelative
    override fun getDelay() = 0
    override fun getVolume() = volume
    override fun getPitch() = pitch
    override fun getX() = position.x
    override fun getY() = position.y
    override fun getZ() = position.z
    override fun getAttenuation() = attenuation
}

package ga.rubydesic.dmd.mixin;


import com.mojang.math.Vector3d;
import ga.rubydesic.dmd.VideoSoundInstance;
import ga.rubydesic.dmd.download.YoutubeDownload;
import ga.rubydesic.dmd.imixin.ILevelRendererMixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin implements ILevelRendererMixin {

	@Shadow
	@Final
	private Map<BlockPos, SoundInstance> playingRecords;
	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	private ClientLevel level;

	public void playYoutubeMusic(String videoId, BlockPos blockPos) {
		SoundInstance currentlyPlaying = this.playingRecords.get(blockPos);
		if (currentlyPlaying != null) {
			this.minecraft.getSoundManager().stop(currentlyPlaying);
			this.playingRecords.remove(blockPos);
		}

		if (videoId == null) {
			return;
		}
		YoutubeDownload.INSTANCE.getYtTitle(videoId)
			.thenAcceptAsync(title -> this.minecraft.gui.setNowPlaying(new TextComponent(title)), this.minecraft);

		SoundInstance nowPlaying = new VideoSoundInstance(videoId,
			new Vector3d(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
		this.playingRecords.put(blockPos, nowPlaying);
		this.minecraft.getSoundManager().play(nowPlaying);

		this.notifyNearbyEntities(level, blockPos, true);
	}

	@Shadow
	private void notifyNearbyEntities(Level level, BlockPos blockPos, boolean bl) { }
}

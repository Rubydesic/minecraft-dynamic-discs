package ga.rubydesic.dmd.mixin.client;

import ga.rubydesic.dmd.DynamicMusicDiscsModKt;
import ga.rubydesic.dmd.download.MusicCache;
import ga.rubydesic.dmd.download.MusicId;
import ga.rubydesic.dmd.download.MusicSource;
import net.minecraft.client.sound.AudioStream;
import net.minecraft.client.sound.SoundLoader;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

import static ga.rubydesic.dmd.DynamicMusicDiscsModKt.getLog;
import static ga.rubydesic.dmd.util.UtilKt.getYoutubeVideoId;

@Mixin(SoundLoader.class)
public class SoundBufferLibraryMixin {

    @Inject(
            method = "loadStreamed",
            at = @At("HEAD"),
            cancellable = true
    )
    public void preGetStream(Identifier loc, boolean looping,
                             CallbackInfoReturnable<CompletableFuture<AudioStream>> ci) {

        if (!loc.getNamespace().equals(DynamicMusicDiscsModKt.MOD_ID)) return;

        String youtubeVideoId = getYoutubeVideoId(loc.getPath());
		getLog().debug("Getting audio stream for: " + loc + " (" + youtubeVideoId + ")");
		if (youtubeVideoId == null) return;

        CompletableFuture<AudioStream> stream = MusicCache.INSTANCE.getAudioStreamFuture(
                new MusicId(MusicSource.YOUTUBE, youtubeVideoId));
        ci.setReturnValue(stream);
    }
}

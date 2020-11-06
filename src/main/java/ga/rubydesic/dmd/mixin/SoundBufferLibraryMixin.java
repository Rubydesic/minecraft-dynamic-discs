package ga.rubydesic.dmd.mixin;

import ga.rubydesic.dmd.DynamicMusicDiscsModKt;
import ga.rubydesic.dmd.download.YoutubeDownload;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

import static ga.rubydesic.dmd.UtilKt.getYoutubeVideoId;

@Mixin(SoundBufferLibrary.class)
public class SoundBufferLibraryMixin {

    @Inject(
            method = "getStream",
            at = @At("HEAD"),
            cancellable = true
    )
    public void preGetStream(ResourceLocation loc, boolean looping,
                             CallbackInfoReturnable<CompletableFuture<AudioStream>> ci) {

        if (!loc.getNamespace().equals(DynamicMusicDiscsModKt.MOD_ID)) return;

        String youtubeVideoId = getYoutubeVideoId(loc.getPath());
		System.out.println("Getting audio stream for: " + loc + " (" + youtubeVideoId + ")");
		if (youtubeVideoId == null) return;

        CompletableFuture<AudioStream> stream = YoutubeDownload.INSTANCE.getYtAudioStream(youtubeVideoId);
        ci.setReturnValue(stream);
    }

}

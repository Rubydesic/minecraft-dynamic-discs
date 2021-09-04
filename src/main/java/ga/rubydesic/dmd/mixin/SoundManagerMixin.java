package ga.rubydesic.dmd.mixin;

import ga.rubydesic.dmd.DynamicMusicDiscsModKt;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
public class SoundManagerMixin {

	@Inject(
		at = @At("HEAD"),
		method = "getSoundEvent",
		cancellable = true
	)
	public void preGetSoundEvent(ResourceLocation loc, CallbackInfoReturnable<WeighedSoundEvents> cir) {
		if (!loc.getNamespace().equals(DynamicMusicDiscsModKt.MOD_ID)) return;

		System.out.println("Attempt to get DMD sound event " + loc);

		WeighedSoundEvents events = new WeighedSoundEvents(loc, null);
		events.addSound(new Sound(loc.toString().toLowerCase(), 1, 1, 1, Sound.Type.SOUND_EVENT, true, false, 64));

		cir.setReturnValue(events);
	}

}

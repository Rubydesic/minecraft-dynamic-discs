package ga.rubydesic.dmd.mixin.client;

import ga.rubydesic.dmd.DynamicMusicDiscsModKt;
import net.minecraft.client.sound.Sound;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.WeightedSoundSet;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
public class SoundManagerMixin {
    @Inject(
            at = @At("HEAD"),
            method = "get",
            cancellable = true
    )
    public void preGetSoundEvent(Identifier loc, CallbackInfoReturnable<WeightedSoundSet> cir) {
        if (!loc.getNamespace().equals(DynamicMusicDiscsModKt.MOD_ID)) return;

        System.out.println("Attempt to get DMD sound event " + loc);

        WeightedSoundSet events = new WeightedSoundSet(loc, null);
        events.add(new Sound(loc.toString().toLowerCase(), 1, 1, 1, Sound.RegistrationType.SOUND_EVENT, true, false, 64));

        cir.setReturnValue(events);
    }
}

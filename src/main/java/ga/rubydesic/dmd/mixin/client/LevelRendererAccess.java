package ga.rubydesic.dmd.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(LevelRenderer.class)
public interface LevelRendererAccess {

    @Accessor
    Map<BlockPos, SoundInstance> getPlayingRecords();

    @Accessor
    ClientLevel getLevel();

    @Invoker
    void callNotifyNearbyEntities(Level level, BlockPos blockPos, boolean bl);
}

package ga.rubydesic.dmd.mixin;

import ga.rubydesic.dmd.AnvilModifierKt;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {
	// useless
	public AnvilMenuMixin(@Nullable MenuType<?> menuType, int i, Inventory inventory, ContainerLevelAccess containerLevelAccess) {
		super(menuType, i, inventory, containerLevelAccess);
	}



//	@Inject(
//		method = "onTake",
//		at = @At("HEAD")
//	)
//	public void injectResult(Player player, ItemStack itemStack, CallbackInfoReturnable<ItemStack> ci) {
//		if (player.level instanceof ServerLevel) {
//			itemStack.setHoverName(new TextComponent("XD"));
//			AnvilModifierKt.modifyAnvilResult(itemStack, player.level.getServer());
//		}
//	}

}

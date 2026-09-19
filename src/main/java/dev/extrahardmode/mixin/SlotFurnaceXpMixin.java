package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.FurnaceXp;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Slot.class)
public abstract class SlotFurnaceXpMixin {
    @Shadow
    public net.minecraft.world.Container container;

    @Inject(method = "onTake", at = @At("RETURN"))
    private void ehm$chestCookingXp(Player player, ItemStack stack, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(serverPlayer.level() instanceof ServerLevel level)
                || !WorldGate.isActive(level)) {
            return;
        }
        int taken = stack == null ? 0 : stack.getCount();
        int left = FurnaceXp.countItems(container);
        FurnaceXp.giveToPlayer(serverPlayer, container, taken, left);
    }
}

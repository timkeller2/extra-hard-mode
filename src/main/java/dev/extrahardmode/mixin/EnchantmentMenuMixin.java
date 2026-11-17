package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Enchanting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The button's level cost is {@code costs[slot]}, not the enchantment's own level. */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin {
    @Shadow
    @Final
    public int[] costs;

    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
    private void tougher$limitTableEnchant(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (id < 0 || id >= this.costs.length) {
            return;
        }
        if (Enchanting.deny(serverPlayer, this.costs[id])) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "clickMenuButton", at = @At("RETURN"))
    private void tougher$clearTableEnchant(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        Enchanting.clearPending();
    }
}

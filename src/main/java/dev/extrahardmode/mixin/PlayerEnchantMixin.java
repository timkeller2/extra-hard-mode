package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Enchanting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The int argument is lapis spent, not the requirement level. Training uses the
 * cost saved when the enchantment-table button was clicked.
 */
@Mixin(Player.class)
public abstract class PlayerEnchantMixin {
    @Inject(method = "onEnchantmentPerformed", at = @At("HEAD"))
    private void tougher$trainEnchantSkill(ItemStack stack, int lapisCount, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer serverPlayer) {
            Enchanting.onPerformed(serverPlayer);
        }
    }
}

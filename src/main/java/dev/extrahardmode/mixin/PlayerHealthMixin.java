package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.HealthRules;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * After inventory (and heavy armor attributes) load, put current health back
 * to the saved value instead of leaving it clamped at 20.
 */
@Mixin(Player.class)
public abstract class PlayerHealthMixin {
    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void ehm$restoreHealthAfterEquipment(ValueInput input, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        float loaded = input.getFloatOr("Health", player.getHealth());
        player.setHealth(HealthRules.restoreLoadedHealth(player.getHealth(), loaded, player.getMaxHealth()));
    }
}

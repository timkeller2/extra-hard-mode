package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.WellFed;
import dev.extrahardmode.feature.WellFedRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerFoodMixin {
    @Inject(method = "causeFoodExhaustion", at = @At("HEAD"), cancellable = true)
    private void tougher$wellFedExhaustion(float exhaustion, CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayer player)) {
            return;
        }
        float scale = WellFedRules.saturationMultiplier(WellFed.level(player));
        if (scale >= 0.999F) {
            return;
        }
        ci.cancel();
        if (player.getAbilities().invulnerable) {
            return;
        }
        player.getFoodData().addExhaustion(exhaustion * scale);
    }
}

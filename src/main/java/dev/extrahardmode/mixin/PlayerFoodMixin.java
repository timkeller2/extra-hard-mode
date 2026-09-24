package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Hunger;
import dev.extrahardmode.feature.HungerRules;
import dev.extrahardmode.feature.WellFed;
import dev.extrahardmode.feature.WellFedRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    @Inject(method = "canEat", at = @At("HEAD"), cancellable = true)
    private void tougher$eatUntilWellFedFull(boolean canAlwaysEat, CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        if (self.getAbilities().invulnerable || canAlwaysEat) {
            cir.setReturnValue(true);
            return;
        }
        cir.setReturnValue(HungerRules.canEatMore(self.getFoodData().getFoodLevel(), Hunger.edibleUntil(self)));
    }
}

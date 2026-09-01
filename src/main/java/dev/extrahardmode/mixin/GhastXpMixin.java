package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.monster.Ghasts;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Ghast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** {@code getExperienceReward} is final on {@link LivingEntity}; Ghast cannot host the inject. */
@Mixin(LivingEntity.class)
public abstract class GhastXpMixin {
    @Inject(method = "getExperienceReward", at = @At("RETURN"), cancellable = true)
    private void ehm$scaleGhastExperience(ServerLevel level, Entity attacker, CallbackInfoReturnable<Integer> cir) {
        if (!((Object) this instanceof Ghast ghast)) {
            return;
        }
        if (!WorldGate.isModuleActive(level, Ghasts.ID)) {
            return;
        }
        cir.setReturnValue(Ghasts.scaleIncomingXp(ghast, level, cir.getReturnValueI()));
    }
}

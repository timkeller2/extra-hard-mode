package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.DiamondSkinRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.PoisonMobEffect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Poison uses ordinary magic damage. Mark that one hurt so Diamond Skin can leave it alone. */
@Mixin(PoisonMobEffect.class)
public class PoisonMobEffectMixin {
    @Redirect(
            method = "applyEffectTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean tougher$markPoison(LivingEntity entity, ServerLevel level, DamageSource source, float amount) {
        DiamondSkinRules.beginPoison(entity);
        try {
            return entity.hurtServer(level, source, amount);
        } finally {
            DiamondSkinRules.endPoison(entity);
        }
    }
}

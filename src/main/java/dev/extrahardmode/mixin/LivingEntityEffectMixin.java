package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.WellFed;
import dev.extrahardmode.feature.WellFedRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityEffectMixin {
    @ModifyVariable(
            method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            argsOnly = true)
    private MobEffectInstance tougher$shorterWellFedEffects(MobEffectInstance instance) {
        if (!( (Object) this instanceof ServerPlayer player) || instance == null) {
            return instance;
        }
        if (!instance.is(MobEffects.POISON) && !instance.is(MobEffects.HUNGER)) {
            return instance;
        }
        int duration = WellFedRules.scaleDuration(instance.getDuration(), WellFed.level(player));
        if (duration == instance.getDuration()) {
            return instance;
        }
        return new MobEffectInstance(
                instance.getEffect(),
                duration,
                instance.getAmplifier(),
                instance.isAmbient(),
                instance.isVisible(),
                instance.showIcon());
    }
}

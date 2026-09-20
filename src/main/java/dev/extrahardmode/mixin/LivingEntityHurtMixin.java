package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Players;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityHurtMixin {
    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float tougher$scaleEnvironmentalDamage(float amount, ServerLevel level, DamageSource source) {
        if (!WorldGate.isModuleActive(level, Players.ID)) {
            return amount;
        }
        if (!((Object) this instanceof ServerPlayer player)) {
            return amount;
        }
        return Players.scaleIncomingDamage(player, source, amount);
    }

    @Inject(method = "applyItemBlocking", at = @At("RETURN"), cancellable = true)
    private void tougher$partialShieldAbsorb(
            ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        Float blocked = cir.getReturnValue();
        if (blocked == null || blocked <= 0.0F) {
            return;
        }
        LivingEntity entity = (LivingEntity) (Object) this;
        cir.setReturnValue(Players.scaleBlockedDamage(entity, level, blocked));
    }
}

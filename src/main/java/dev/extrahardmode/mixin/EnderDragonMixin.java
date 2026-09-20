package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Dragon;
import dev.extrahardmode.feature.FeatureBus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderDragon.class)
public abstract class EnderDragonMixin {
    @Inject(
            method = "hurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/boss/enderdragon/EnderDragonPart;Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At("RETURN"))
    private void tougher$extraAttacks(
            ServerLevel level,
            EnderDragonPart part,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir) {
        if (!FeatureBus.guard((Level) level, Dragon.ID)) {
            return;
        }
        if (!Boolean.TRUE.equals(cir.getReturnValue()) || amount <= 0.0F) {
            return;
        }
        Dragon.onDamaged((EnderDragon) (Object) this, source);
    }
}

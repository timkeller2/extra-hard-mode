package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.FeatureBus;
import dev.extrahardmode.feature.monster.Creepers;
import dev.extrahardmode.feature.monster.Zombies;
import dev.extrahardmode.module.EntityHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDropMixin {
    @Inject(method = "dropFromLootTable", at = @At("HEAD"), cancellable = true)
    private void extrahardmode$skipLootless(ServerLevel level, DamageSource source, boolean playerKill, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!FeatureBus.guard((net.minecraft.world.level.Level) level, Zombies.ID)
                && !FeatureBus.guard((net.minecraft.world.level.Level) level, Creepers.ID)) {
            return;
        }
        if (EntityHelper.lootless(self)) {
            ci.cancel();
        }
    }
}

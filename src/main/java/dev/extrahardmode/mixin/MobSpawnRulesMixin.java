package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.AntiGrinder;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobSpawnRulesMixin {
    @Inject(method = "checkSpawnRules", at = @At("HEAD"), cancellable = true)
    private void ehm$antiGrinderFloor(
            LevelAccessor level, EntitySpawnReason reason, CallbackInfoReturnable<Boolean> cir) {
        if (!(level instanceof ServerLevel serverLevel) || !WorldGate.isModuleActive(serverLevel, AntiGrinder.ID)) {
            return;
        }
        Mob mob = (Mob) (Object) this;
        if (!AntiGrinder.onCheckSpawnRules(mob, serverLevel, reason)) {
            cir.setReturnValue(false);
        }
    }
}

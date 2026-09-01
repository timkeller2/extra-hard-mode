package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.AntiGrinder;
import dev.extrahardmode.feature.MoreMonsters;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
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

    @Inject(method = "finalizeSpawn", at = @At("HEAD"))
    private void ehm$stampTrialSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            EntitySpawnReason reason,
            SpawnGroupData data,
            CallbackInfoReturnable<SpawnGroupData> cir) {
        if (reason != EntitySpawnReason.TRIAL_SPAWNER) {
            return;
        }
        AntiGrinder.markTrialSpawned((Mob) (Object) this);
    }

    @Inject(method = "getMaxSpawnClusterSize", at = @At("RETURN"), cancellable = true)
    private void ehm$scaleClusterCap(CallbackInfoReturnable<Integer> cir) {
        Mob mob = (Mob) (Object) this;
        if (!(mob.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, MoreMonsters.ID)) {
            return;
        }
        int vanilla = cir.getReturnValueI();
        int scaled = MoreMonsters.maybeScale(vanilla, mob.getType().getCategory(), level, mob.blockPosition());
        if (scaled != vanilla) {
            cir.setReturnValue(scaled);
        }
    }
}

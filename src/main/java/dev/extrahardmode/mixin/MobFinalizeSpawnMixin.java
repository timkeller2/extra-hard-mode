package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.FeatureBus;
import dev.extrahardmode.feature.monster.Zombies;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobFinalizeSpawnMixin {
    @Inject(method = "finalizeSpawn", at = @At("HEAD"))
    private void extrahardmode$ignoreReinforcements(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            EntitySpawnReason reason,
            SpawnGroupData spawnGroupData,
            CallbackInfoReturnable<SpawnGroupData> cir) {
        if (!FeatureBus.guard((Level) level.getLevel(), Zombies.ID)) {
            return;
        }
        Zombies.stampReinforcement((Mob) (Object) this, reason);
    }

    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void extrahardmode$zombieSpeedVariance(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            EntitySpawnReason reason,
            SpawnGroupData spawnGroupData,
            CallbackInfoReturnable<SpawnGroupData> cir) {
        if (!FeatureBus.guard((Level) level.getLevel(), Zombies.ID)) {
            return;
        }
        Zombies.applySpeedVariance((Mob) (Object) this);
    }
}

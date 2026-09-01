package dev.extrahardmode.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.extrahardmode.module.SpawnReplaceService;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {
    @Inject(
            method =
                    "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V",
                            shift = At.Shift.AFTER),
            require = 0)
    private static void ehm$replaceNaturalSpawn(
            MobCategory category,
            ServerLevel level,
            ChunkAccess chunk,
            BlockPos pos,
            NaturalSpawner.SpawnPredicate predicate,
            NaturalSpawner.AfterSpawnCallback callback,
            CallbackInfo ci,
            @Local Mob mob) {
        if (!WorldGate.isActive(level)) {
            return;
        }
        SpawnReplaceService.replaceIfNeeded(mob, level, EntitySpawnReason.NATURAL);
    }
}

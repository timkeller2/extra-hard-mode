package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.FishStocks;
import dev.extrahardmode.feature.MoreMonsters;
import dev.extrahardmode.module.SpawnReplaceService;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {
    @Redirect(
            method =
                    "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"),
            require = 0)
    private static void ehm$addFreshOrReplace(ServerLevel level, Entity entity) {
        if (!WorldGate.isActive(level)) {
            SpawnReplaceService.markMixinApplied();
            level.addFreshEntityWithPassengers(entity);
            return;
        }
        SpawnReplaceService.markMixinApplied();
        if (entity instanceof Mob mob) {
            SpawnReplaceService.replaceIfNeeded(mob, level, EntitySpawnReason.NATURAL);
            if (mob.isRemoved()) {
                return;
            }
        }
        if (!FishStocks.allowNaturalSpawn(level, entity)) {
            entity.discard();
            return;
        }
        level.addFreshEntityWithPassengers(entity);
    }

    @Redirect(
            method =
                    "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;run(Lnet/minecraft/world/entity/Mob;Lnet/minecraft/world/level/chunk/ChunkAccess;)V"),
            require = 0)
    private static void ehm$skipCallbackIfReplaced(
            NaturalSpawner.AfterSpawnCallback callback, Mob mob, ChunkAccess chunk) {
        if (mob.isRemoved()) {
            return;
        }
        callback.run(mob, chunk);
    }

    @Redirect(
            method =
                    "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/level/biome/MobSpawnSettings$SpawnerData;count()Lnet/minecraft/util/valueproviders/IntProvider;"),
            require = 0)
    private static net.minecraft.util.valueproviders.IntProvider ehm$scalePackCount(
            MobSpawnSettings.SpawnerData data,
            MobCategory category,
            ServerLevel level,
            ChunkAccess chunk,
            BlockPos pos,
            NaturalSpawner.SpawnPredicate predicate,
            NaturalSpawner.AfterSpawnCallback callback) {
        MoreMonsters.markPackCountMixinApplied();
        net.minecraft.util.valueproviders.IntProvider count = data.count();
        int min = count.minInclusive();
        int max = count.maxInclusive();
        if (WorldGate.isModuleActive(level, MoreMonsters.ID)) {
            min = MoreMonsters.maybeScale(min, category, level, pos);
            max = MoreMonsters.maybeScale(max, category, level, pos);
        }
        min = FishStocks.packCount(min, data.type(), level);
        max = FishStocks.packCount(max, data.type(), level);
        if (max < min) {
            max = min;
        }
        if (min == max) {
            return net.minecraft.util.valueproviders.ConstantInt.of(min);
        }
        return net.minecraft.util.valueproviders.UniformInt.of(min, max);
    }

    @Inject(
            method =
                    "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V",
            at = @At("RETURN"),
            require = 0)
    private static void ehm$warnIfAddRedirectMissing(CallbackInfo ci) {
        SpawnReplaceService.warnIfMixinMissing();
        MoreMonsters.warnIfPackMixinMissing();
    }

    @Inject(
            method =
                    "spawnForChunk(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/NaturalSpawner$SpawnState;Ljava/util/List;)V",
            at = @At("RETURN"),
            require = 0)
    private static void ehm$warnIfAddRedirectMissingAfterChunk(CallbackInfo ci) {
        SpawnReplaceService.warnIfMixinMissing();
        MoreMonsters.warnIfPackMixinMissing();
    }
}

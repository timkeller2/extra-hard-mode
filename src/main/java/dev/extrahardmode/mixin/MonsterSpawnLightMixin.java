package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.HostileDepthSpawnRules;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Monster.class)
public abstract class MonsterSpawnLightMixin {
    @Redirect(
            method = "isDarkEnoughToSpawn",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/dimension/DimensionType;monsterSpawnBlockLightLimit()I"))
    private static int tougher$deeperHostileLight(
            DimensionType type, ServerLevelAccessor level, BlockPos pos, RandomSource random) {
        int vanilla = type.monsterSpawnBlockLightLimit();
        ServerLevel server = level.getLevel();
        if (server == null || !WorldGate.isActive(server) || server.dimension() != Level.OVERWORLD) {
            return vanilla;
        }
        return HostileDepthSpawnRules.blockLightLimit(vanilla, pos.getY());
    }
}

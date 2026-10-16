package dev.extrahardmode.mixin;

import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.feature.AntiFarming;
import dev.extrahardmode.feature.GrassSpreadRules;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SpreadingSnowyBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Slows grass spreading onto dirt. Dying back to dirt, and mycelium or podzol spread, stay at the usual rate. */
@Mixin(SpreadingSnowyBlock.class)
public abstract class SpreadingSnowyBlockMixin {
    @Redirect(
            method = "randomTick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
                            ordinal = 1))
    private boolean ehm$slowGrassSpread(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.is(Blocks.GRASS_BLOCK)
                && WorldGate.isModuleActive(level, AntiFarming.ID)
                && !GrassSpreadRules.allowSpread(
                        ConfigManager.world(level).grassGrowthPercent(), level.getRandom().nextInt(100))) {
            return false;
        }
        return level.setBlockAndUpdate(pos, state);
    }
}

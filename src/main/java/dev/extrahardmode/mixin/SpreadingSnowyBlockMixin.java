package dev.extrahardmode.mixin;

import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.feature.AntiFarming;
import dev.extrahardmode.feature.GrassSpreadRules;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SpreadingSnowyBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Slows grass spreading onto dirt. Dying back to dirt, and mycelium or podzol spread, stay at the usual rate. */
@Mixin(SpreadingSnowyBlock.class)
public abstract class SpreadingSnowyBlockMixin {
    /** A fence, gate, door, or other open block does not turn the grass back into dirt. */
    @Inject(method = "canStayAlive", at = @At("HEAD"), cancellable = true)
    private static void ehm$grassSurvivesUnderOpenCover(
            BlockState state, LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!state.is(Blocks.GRASS_BLOCK) || !(level instanceof ServerLevel server)) {
            return;
        }
        if (!WorldGate.isModuleActive(server, AntiFarming.ID)) {
            return;
        }
        BlockPos above = pos.above();
        if (openCover(level.getBlockState(above), level, above)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * The light check reads the block on the grass. A fence can hide a bright sky,
     * so an open cover uses the brightest space next to it.
     */
    @Redirect(
            method = "randomTick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/level/ServerLevel;getMaxLocalRawBrightness(Lnet/minecraft/core/BlockPos;)I"))
    private int ehm$lightAroundOpenCover(ServerLevel level, BlockPos above) {
        int here = level.getMaxLocalRawBrightness(above);
        if (!level.getBlockState(above.below()).is(Blocks.GRASS_BLOCK)
                || !WorldGate.isModuleActive(level, AntiFarming.ID)
                || !openCover(level.getBlockState(above), level, above)) {
            return here;
        }
        return GrassSpreadRules.spreadLight(
                here,
                level.getMaxLocalRawBrightness(above.above()),
                level.getMaxLocalRawBrightness(above.north()),
                level.getMaxLocalRawBrightness(above.east()),
                level.getMaxLocalRawBrightness(above.south()),
                level.getMaxLocalRawBrightness(above.west()));
    }

    private static boolean openCover(BlockState cover, LevelReader level, BlockPos pos) {
        return GrassSpreadRules.openCover(
                cover.isAir(),
                Block.isFaceFull(cover.getCollisionShape(level, pos), Direction.DOWN),
                cover.getFluidState().isFull());
    }

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

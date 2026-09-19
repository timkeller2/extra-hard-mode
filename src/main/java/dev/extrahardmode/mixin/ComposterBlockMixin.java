package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.AntiFarming;
import dev.extrahardmode.feature.CropGrowthRules;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ComposterBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ComposterBlock.class)
public abstract class ComposterBlockMixin {
    @Redirect(
            method = "addItem",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/LevelAccessor;scheduleTick(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;I)V"))
    private static void ehm$slowReadyAfterFill(LevelAccessor level, BlockPos pos, Block block, int delay) {
        level.scheduleTick(pos, block, readyDelay(level, delay));
    }

    @Redirect(
            method = "onPlace",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/Level;scheduleTick(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;I)V"))
    private void ehm$slowReadyOnPlace(Level level, BlockPos pos, Block block, int delay) {
        level.scheduleTick(pos, block, readyDelay(level, delay));
    }

    private static int readyDelay(LevelAccessor level, int delay) {
        if (!(level instanceof ServerLevel serverLevel) || !WorldGate.isModuleActive(serverLevel, AntiFarming.ID)) {
            return delay;
        }
        return CropGrowthRules.composterReadyDelay(delay);
    }
}

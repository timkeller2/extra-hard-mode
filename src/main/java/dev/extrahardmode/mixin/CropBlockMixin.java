package dev.extrahardmode.mixin;

import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.feature.AntiFarming;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CropBlock.class)
public abstract class CropBlockMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void ehm$snowBreaksCrops(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (!WorldGate.isModuleActive(level, AntiFarming.ID)) {
            return;
        }
        if (ConfigManager.world(level).snowBreaksCrops() && AntiFarming.isSnowCovered(level, pos)) {
            AntiFarming.killCrop(level, pos);
            ci.cancel();
        }
    }

    @Inject(method = "randomTick", at = @At("RETURN"))
    private void ehm$weakCropsOnTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (!WorldGate.isModuleActive(level, AntiFarming.ID)) {
            return;
        }
        BlockState now = level.getBlockState(pos);
        if (!(now.getBlock() instanceof CropBlock crop)) {
            return;
        }
        if (AntiFarming.shouldBlockCropGrowth(crop, now) && AntiFarming.plantDies(level, pos, now)) {
            AntiFarming.killCrop(level, pos);
        }
    }

    @Inject(method = "growCrops", at = @At("RETURN"))
    private void ehm$weakCropsOnGrow(Level level, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (!(level instanceof ServerLevel server) || !WorldGate.isModuleActive(server, AntiFarming.ID)) {
            return;
        }
        BlockState now = server.getBlockState(pos);
        if (!(now.getBlock() instanceof CropBlock crop)) {
            return;
        }
        if (AntiFarming.shouldBlockCropGrowth(crop, now) && AntiFarming.plantDies(server, pos, now)) {
            AntiFarming.killCrop(server, pos);
        }
    }
}

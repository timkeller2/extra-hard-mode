package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.AntiFarming;
import dev.extrahardmode.feature.CropGrowthRules;
import dev.extrahardmode.world.WorldGate;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MangrovePropaguleBlock.class)
public abstract class MangrovePropaguleBlockMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void ehm$slowHangingPropagule(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (!WorldGate.isModuleActive(level, AntiFarming.ID)) {
            return;
        }
        if (!state.getValue(MangrovePropaguleBlock.HANGING)) {
            return;
        }
        if (!CropGrowthRules.allowVanillaRandomTick(
                AntiFarming.currentDurationPercent(level, pos, CropGrowthRules.TREE_DURATION_PERCENT),
                new Random(random.nextLong()))) {
            ci.cancel();
        }
    }
}

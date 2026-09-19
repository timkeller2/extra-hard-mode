package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.AntiFarming;
import dev.extrahardmode.feature.CropGrowthRules;
import dev.extrahardmode.world.WorldGate;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SugarCaneBlock.class)
public abstract class SugarCaneBlockMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void ehm$slowSugarCane(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (!WorldGate.isModuleActive(level, AntiFarming.ID)) {
            return;
        }
        if (!CropGrowthRules.allowVanillaRandomTick(
                AntiFarming.currentDurationPercent(level, pos, CropGrowthRules.SUGAR_CANE_DURATION_PERCENT),
                new Random(random.nextLong()))) {
            ci.cancel();
        }
    }

    @Inject(
            method = "randomTick",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"),
            cancellable = true)
    private void ehm$weedOnSecondSegment(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (!WorldGate.isModuleActive(level, AntiFarming.ID)) {
            return;
        }
        if (AntiFarming.tryWeedSugarCaneOnSecondSegment(level, pos, state)) {
            ci.cancel();
        }
    }
}

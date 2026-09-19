package dev.extrahardmode.mixin;

import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.feature.AntiFarming;
import dev.extrahardmode.feature.CropGrowthRules;
import dev.extrahardmode.world.WorldGate;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CropBlock.class)
public abstract class CropBlockMixin {
    @Unique
    private static final ThreadLocal<Boolean> EHM$GROWTH_REENTRY = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void ehm$snowBreaksCrops(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (!WorldGate.isModuleActive(level, AntiFarming.ID)) {
            return;
        }
        if (ConfigManager.world(level).snowBreaksCrops() && AntiFarming.isSnowCovered(level, pos)) {
            AntiFarming.killCrop(level, pos);
            ci.cancel();
            return;
        }
        if (Boolean.TRUE.equals(EHM$GROWTH_REENTRY.get())) {
            return;
        }
        int duration = AntiFarming.currentDurationPercent(
                level, pos, ConfigManager.world(level).cropMatureDurationPercent());
        Random javaRandom = new Random(random.nextLong());
        if (!CropGrowthRules.allowVanillaRandomTick(duration, javaRandom)) {
            ci.cancel();
        }
    }

    @Inject(method = "randomTick", at = @At("RETURN"))
    private void ehm$extraGrowthWhenFaster(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (Boolean.TRUE.equals(EHM$GROWTH_REENTRY.get())) {
            return;
        }
        if (!WorldGate.isModuleActive(level, AntiFarming.ID)) {
            return;
        }
        int extra = CropGrowthRules.extraRandomTicks(
                AntiFarming.currentDurationPercent(
                        level, pos, ConfigManager.world(level).cropMatureDurationPercent()),
                new Random(random.nextLong()));
        if (extra <= 0) {
            return;
        }
        CropBlock crop = (CropBlock) (Object) this;
        EHM$GROWTH_REENTRY.set(Boolean.TRUE);
        try {
            for (int i = 0; i < extra; i++) {
                BlockState now = level.getBlockState(pos);
                if (!(now.getBlock() instanceof CropBlock current) || current.isMaxAge(now)) {
                    break;
                }
                crop.growCrops(level, pos, now);
            }
        } finally {
            EHM$GROWTH_REENTRY.set(Boolean.FALSE);
        }
    }

    @Inject(method = "randomTick", at = @At("RETURN"))
    private void ehm$weakCropsOnTick(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (!WorldGate.isModuleActive(level, AntiFarming.ID)) {
            return;
        }
        AntiFarming.tryKillIfMature(level, pos, level.getBlockState(pos));
    }

    @Inject(method = "growCrops", at = @At("RETURN"))
    private void ehm$weakCropsOnGrow(Level level, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (!(level instanceof ServerLevel server) || !WorldGate.isModuleActive(server, AntiFarming.ID)) {
            return;
        }
        AntiFarming.tryKillIfMature(server, pos, server.getBlockState(pos));
    }
}

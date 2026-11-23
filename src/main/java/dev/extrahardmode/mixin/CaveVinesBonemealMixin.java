package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.AntiFarming;
import dev.extrahardmode.feature.CropGrowthRules;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.CaveVinesBlock;
import net.minecraft.world.level.block.CaveVinesPlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bone meal on a glow berry vine can fail to fruit at the current crop loss percent.
 * Each segment also accepts bone meal only once every three minutes.
 */
@Mixin({CaveVinesBlock.class, CaveVinesPlantBlock.class})
public abstract class CaveVinesBonemealMixin {
    @Inject(method = "isValidBonemealTarget", at = @At("RETURN"), cancellable = true)
    private void tougher$glowBerryMealCooldown(
            LevelReader level,
            BlockPos pos,
            BlockState state,
            BonemealSource source,
            CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue()) || !(level instanceof ServerLevel server)) {
            return;
        }
        if (AntiFarming.glowBerryMealCooling(server, pos)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "performBonemeal", at = @At("HEAD"), cancellable = true)
    private void tougher$glowBerryFruitMayFail(
            ServerLevel level,
            RandomSource random,
            BlockPos pos,
            BlockState state,
            BonemealSource source,
            CallbackInfo ci) {
        if (!WorldGate.isModuleActive(level, AntiFarming.ID) || AntiFarming.boneMealBypasses()) {
            return;
        }
        AntiFarming.markGlowBerryMeal(level, pos);
        int chance = CropGrowthRules.glowBerryFruitFailPercent(AntiFarming.currentSeasonalLossRate(level));
        if (chance <= 0) {
            return;
        }
        if (chance >= 100 || CropGrowthRules.glowBerryBonemealFails(chance, random.nextInt(100))) {
            ci.cancel();
        }
    }
}

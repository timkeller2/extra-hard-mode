package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.AntiFarming;
import dev.extrahardmode.feature.CropGrowthRules;
import dev.extrahardmode.world.WorldGate;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StemBlock.class)
public abstract class StemBlockMixin extends VegetationBlock {
    protected StemBlockMixin(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void ehm$slowStemFruit(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (!WorldGate.isModuleActive(level, AntiFarming.ID)) {
            return;
        }
        if (state.getValue(StemBlock.AGE) < StemBlock.MAX_AGE) {
            return;
        }
        if (!CropGrowthRules.allowVanillaRandomTick(
                AntiFarming.currentDurationPercent(level, pos, CropGrowthRules.STEM_FRUIT_DURATION_PERCENT),
                new Random(random.nextLong()))) {
            ci.cancel();
        }
    }

    @Inject(method = "randomTick", at = @At("RETURN"))
    private void ehm$stemFruitMayWeed(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (!WorldGate.isModuleActive(level, AntiFarming.ID)) {
            return;
        }
        if (level.getBlockState(pos).getBlock() instanceof AttachedStemBlock) {
            AntiFarming.onStemFruitAppeared(level, pos);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (level instanceof ServerLevel server && WorldGate.isModuleActive(server, AntiFarming.ID)) {
            AntiFarming.onStemVinePlaced(server, pos, oldState);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        super.affectNeighborsAfterRemoval(state, level, pos, moved);
        if (WorldGate.isModuleActive(level, AntiFarming.ID)) {
            AntiFarming.onStemVineRemoved(level, pos);
        }
    }
}

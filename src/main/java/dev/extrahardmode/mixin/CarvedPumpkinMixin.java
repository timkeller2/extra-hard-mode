package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.CopperGolems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A player who builds a copper golem is its summoner, and the new copper chest is theirs. */
@Mixin(CarvedPumpkinBlock.class)
public abstract class CarvedPumpkinMixin {
    @Inject(
            method = "spawnGolemInWorld",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private static void tougher$stampCopperGolem(
            Level level, BlockPattern.BlockPatternMatch match, Entity entity, BlockPos pos, CallbackInfo ci) {
        CopperGolems.stamp(entity);
    }

    @Inject(method = "replaceCopperBlockWithChest", at = @At("RETURN"))
    private void tougher$claimSummonChest(Level level, BlockPattern.BlockPatternMatch match, CallbackInfo ci) {
        CopperGolems.claimSummonChest(level, match);
    }
}

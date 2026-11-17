package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.CopperGolems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Copper golems pick a container here. A chest owned by someone else is not a target. */
@Mixin(TransportItemsBetweenContainers.TransportItemTarget.class)
public abstract class TransportItemsMixin {
    @Inject(
            method =
                    "tryCreatePossibleTarget(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/entity/ai/behavior/TransportItemsBetweenContainers$TransportItemTarget;",
            at = @At("HEAD"),
            cancellable = true)
    private static void tougher$skipLockedPos(
            BlockPos pos,
            Level level,
            CallbackInfoReturnable<TransportItemsBetweenContainers.TransportItemTarget> cir) {
        if (CopperGolems.refuses(level, pos)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(
            method =
                    "tryCreatePossibleTarget(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/entity/ai/behavior/TransportItemsBetweenContainers$TransportItemTarget;",
            at = @At("HEAD"),
            cancellable = true)
    private static void tougher$skipLockedEntity(
            BlockEntity entity,
            Level level,
            CallbackInfoReturnable<TransportItemsBetweenContainers.TransportItemTarget> cir) {
        if (entity != null && CopperGolems.refuses(level, entity.getBlockPos())) {
            cir.setReturnValue(null);
        }
    }
}

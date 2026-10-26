package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.CopperLanterns;
import dev.extrahardmode.feature.Torches;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockLightEngine.class)
public abstract class BlockLightEngineMixin {
    @Shadow
    protected abstract void enqueueIncrease(long packedPos, long queueEntry);

    @Shadow
    protected static boolean isEmptyShape(BlockState state) {
        throw new AssertionError();
    }

    @Inject(method = "getEmission", at = @At("RETURN"), cancellable = true)
    private void tougher$dimBurningTorches(
            long packedPos, BlockState state, CallbackInfoReturnable<Integer> cir) {
        BlockGetter getter = ((LightEngineAccess) (Object) this).tougher$chunkSource().getLevel();
        int vanilla = cir.getReturnValueI();
        if (vanilla <= 0) {
            return;
        }
        BlockPos pos = BlockPos.of(packedPos);
        if (getter instanceof ServerLevel level
                && WorldGate.isModuleActive(level, Torches.ID)
                && Torches.isBurnableTorch(state)) {
            int light = Torches.burningLight(level, pos, vanilla);
            if (light != vanilla) {
                cir.setReturnValue(light);
            }
            return;
        }
        int light = CopperLanterns.light(getter, pos, state, vanilla);
        if (light != vanilla) {
            cir.setReturnValue(light);
        }
    }

    /** Chunk lighting reads the block's light directly, skipping {@code getEmission}. */
    @Inject(method = "lambda$propagateLightSources$0", at = @At("HEAD"), cancellable = true)
    private void tougher$dimCopperLanternSources(BlockPos pos, BlockState state, CallbackInfo ci) {
        BlockGetter getter = ((LightEngineAccess) (Object) this).tougher$chunkSource().getLevel();
        int vanilla = state.getLightEmission();
        int light = CopperLanterns.light(getter, pos, state, vanilla);
        if (light == vanilla) {
            return;
        }
        enqueueIncrease(
                pos.asLong(), LightEngine.QueueEntry.increaseLightFromEmission(light, isEmptyShape(state)));
        ci.cancel();
    }
}

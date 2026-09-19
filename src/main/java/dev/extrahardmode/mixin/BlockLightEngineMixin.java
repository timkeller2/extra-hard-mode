package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Torches;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.BlockLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockLightEngine.class)
public abstract class BlockLightEngineMixin {
    @Inject(method = "getEmission", at = @At("RETURN"), cancellable = true)
    private void extrahardmode$dimBurningTorches(
            long packedPos, BlockState state, CallbackInfoReturnable<Integer> cir) {
        BlockGetter getter = ((LightEngineAccess) (Object) this).extrahardmode$chunkSource().getLevel();
        if (!(getter instanceof ServerLevel level) || !WorldGate.isModuleActive(level, Torches.ID)) {
            return;
        }
        int vanilla = cir.getReturnValueI();
        if (vanilla <= 0 || !Torches.isBurnableTorch(state)) {
            return;
        }
        int light = Torches.burningLight(level, BlockPos.of(packedPos), vanilla);
        if (light != vanilla) {
            cir.setReturnValue(light);
        }
    }
}

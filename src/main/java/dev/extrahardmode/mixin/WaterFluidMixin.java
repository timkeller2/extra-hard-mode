package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Water;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.WaterFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlowingFluid.class)
public abstract class WaterFluidMixin {
    @Inject(method = "getNewLiquid", at = @At("RETURN"), cancellable = true)
    private void ehm$noConvertMarked(
            ServerLevel level, BlockPos pos, BlockState state, CallbackInfoReturnable<FluidState> cir) {
        if (!WorldGate.isModuleActive(level, Water.ID)) {
            return;
        }
        if (!((Object) this instanceof WaterFluid)) {
            return;
        }
        if (!Water.enabled(level) || !Water.isMarked(level, pos)) {
            return;
        }
        FluidState result = cir.getReturnValue();
        if (result != null && result.isSource()) {
            cir.setReturnValue(Water.flowingFluid());
        }
    }
}

package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.JobSiteLoss;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class JobSiteLossMixin {
    @Inject(method = "updatePOIOnBlockStateChange", at = @At("HEAD"))
    private void tougher$nitwitLostWork(BlockPos pos, BlockState oldState, BlockState newState, CallbackInfo ci) {
        if (!WorldGate.isActive((ServerLevel) (Object) this)) {
            return;
        }
        JobSiteLoss.onBlockChange((ServerLevel) (Object) this, pos, oldState, newState);
    }
}

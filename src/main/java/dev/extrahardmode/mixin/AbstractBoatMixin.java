package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Players;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractBoat.class)
public abstract class AbstractBoatMixin {
    @Inject(method = "checkFallDamage", at = @At("HEAD"), cancellable = true)
    private void tougher$smashLongFall(
            double dy, boolean onGround, BlockState state, BlockPos pos, CallbackInfo ci) {
        if (Players.smashBoatIfLongFall((AbstractBoat) (Object) this, onGround)) {
            ci.cancel();
        }
    }
}

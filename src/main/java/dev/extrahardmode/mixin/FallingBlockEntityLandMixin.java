package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.CaveIns;
import dev.extrahardmode.feature.Explosions;
import dev.extrahardmode.feature.FallingBlocks;
import dev.extrahardmode.feature.FeatureBus;
import net.minecraft.world.entity.item.FallingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityLandMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void extrahardmode$flyAutoremove(CallbackInfo ci) {
        FallingBlockEntity self = (FallingBlockEntity) (Object) this;
        if (!FeatureBus.guard(self.level(), Explosions.ID)) {
            return;
        }
        if (Explosions.discardFlyingIfFar(self)) {
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void extrahardmode$fallingLand(CallbackInfo ci) {
        FallingBlockEntity self = (FallingBlockEntity) (Object) this;
        if (!FeatureBus.guard(self.level(), FallingBlocks.ID) && !FeatureBus.guard(self.level(), CaveIns.ID)) {
            return;
        }
        if (!self.isRemoved()) {
            return;
        }
        FallingBlocks.onRemoved(self);
    }
}

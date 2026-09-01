package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.monster.Skeletons;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {
    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
    private void ehm$deflectArrow(EntityHitResult hit, CallbackInfo ci) {
        AbstractArrow self = (AbstractArrow) (Object) this;
        if (!(self.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, Skeletons.ID)) {
            return;
        }
        Entity target = hit.getEntity();
        if (!Skeletons.tryDeflect(self, target)) {
            return;
        }
        Vec3 vel = self.getDeltaMovement();
        if (vel.lengthSqr() > 1.0E-7) {
            self.setPos(self.position().add(vel.normalize().scale(2.0)));
        }
        ci.cancel();
    }
}

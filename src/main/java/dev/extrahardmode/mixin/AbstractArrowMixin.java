package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.ManaAbilities;
import dev.extrahardmode.feature.monster.Skeletons;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {
    @Inject(method = "tick()V", at = @At("HEAD"))
    private void tougher$seekMagicArrow(CallbackInfo ci) {
        ManaAbilities.steerSeeker((AbstractArrow) (Object) this);
    }

    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
    private void ehm$deflectArrow(EntityHitResult hit, CallbackInfo ci) {
        AbstractArrow self = (AbstractArrow) (Object) this;
        if (!(self.level() instanceof ServerLevel level)) {
            return;
        }
        Integer damage = self.getAttached(EhmAttachments.EHM_FIREBOLT_POWER);
        if (damage != null) {
            Entity target = hit.getEntity();
            Entity owner = self.getOwner();
            if (target instanceof LivingEntity living && target != owner) {
                if (damage > 0) {
                    living.hurtServer(level, level.damageSources().arrow(self, owner), damage.floatValue());
                }
                Integer burn = self.getAttached(EhmAttachments.EHM_FIREBOLT_BURN);
                if (burn != null && burn > 0) {
                    living.igniteForSeconds(burn.floatValue());
                }
            }
            self.discard();
            ci.cancel();
            return;
        }
        if (!WorldGate.isModuleActive(level, Skeletons.ID)) {
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

    @Inject(method = "onHitBlock", at = @At("HEAD"), cancellable = true)
    private void tougher$magicArrowNoStick(BlockHitResult hit, CallbackInfo ci) {
        AbstractArrow self = (AbstractArrow) (Object) this;
        Integer damage = self.getAttached(EhmAttachments.EHM_FIREBOLT_POWER);
        if (damage == null) {
            return;
        }
        self.discard();
        ci.cancel();
    }
}

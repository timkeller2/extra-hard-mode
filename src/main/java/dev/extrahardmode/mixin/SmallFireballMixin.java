package dev.extrahardmode.mixin;

import dev.extrahardmode.player.EhmAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SmallFireball.class)
public abstract class SmallFireballMixin {
    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
    private void tougher$manaFireBolt(EntityHitResult hit, CallbackInfo ci) {
        SmallFireball self = (SmallFireball) (Object) this;
        Integer power = self.getAttached(EhmAttachments.EHM_FIREBOLT_POWER);
        if (power == null || power <= 0 || !(self.level() instanceof ServerLevel level)) {
            return;
        }
        Entity target = hit.getEntity();
        Entity owner = self.getOwner();
        if (target instanceof LivingEntity living && target != owner) {
            living.hurtServer(level, level.damageSources().fireball(self, owner), power.floatValue());
            living.igniteForSeconds(power.floatValue());
        }
        self.discard();
        ci.cancel();
    }

    @Inject(method = "onHitBlock", at = @At("HEAD"), cancellable = true)
    private void tougher$manaFireBoltNoDrop(BlockHitResult hit, CallbackInfo ci) {
        SmallFireball self = (SmallFireball) (Object) this;
        Integer power = self.getAttached(EhmAttachments.EHM_FIREBOLT_POWER);
        if (power == null || power <= 0) {
            return;
        }
        self.discard();
        ci.cancel();
    }
}

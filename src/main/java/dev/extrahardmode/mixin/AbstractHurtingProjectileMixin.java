package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.ManaAbilities;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractHurtingProjectile.class)
public abstract class AbstractHurtingProjectileMixin {
    @Inject(method = "tick()V", at = @At("HEAD"))
    private void tougher$seekFireBolt(CallbackInfo ci) {
        AbstractHurtingProjectile self = (AbstractHurtingProjectile) (Object) this;
        if (self instanceof SmallFireball bolt) {
            ManaAbilities.steerFireBolt(bolt);
        }
    }
}

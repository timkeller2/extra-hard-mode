package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.monster.Skeletons;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSkeleton.class)
public abstract class AbstractSkeletonMixin {
    @Redirect(
            method = "performRangedAttack",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/projectile/Projectile;spawnProjectileUsingShoot(Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;DDDFF)Lnet/minecraft/world/entity/projectile/Projectile;"),
            require = 0)
    private Projectile ehm$specialProjectiles(
            Projectile projectile,
            ServerLevel level,
            ItemStack stack,
            double x,
            double y,
            double z,
            float velocity,
            float inaccuracy) {
        AbstractSkeleton self = (AbstractSkeleton) (Object) this;
        if (!WorldGate.isModuleActive(level, Skeletons.ID) || !Skeletons.isSpecialShooter(self)) {
            Skeletons.markMixinApplied();
            return Projectile.spawnProjectileUsingShoot(projectile, level, stack, x, y, z, velocity, inaccuracy);
        }
        Skeletons.markMixinApplied();
        return Skeletons.replaceShot(self, projectile, level, stack, x, y, z, velocity, inaccuracy);
    }

    @Inject(method = "performRangedAttack", at = @At("RETURN"), require = 0)
    private void ehm$warnIfShotRedirectMissing(CallbackInfo ci) {
        AbstractSkeleton self = (AbstractSkeleton) (Object) this;
        if (!(self.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, Skeletons.ID)) {
            return;
        }
        Skeletons.warnIfMixinMissing();
    }
}

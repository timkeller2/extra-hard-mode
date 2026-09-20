package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Dragon;
import dev.extrahardmode.feature.FeatureBus;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DragonFireball.class)
public abstract class DragonFireballMixin {
    @Inject(method = "onHit", at = @At("HEAD"))
    private void tougher$explosiveFireball(HitResult hit, CallbackInfo ci) {
        DragonFireball self = (DragonFireball) (Object) this;
        if (!FeatureBus.guard(self.level(), Dragon.ID)) {
            return;
        }
        Dragon.onFireballHit(self, hit);
    }
}

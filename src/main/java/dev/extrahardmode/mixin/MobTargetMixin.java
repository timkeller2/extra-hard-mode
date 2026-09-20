package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Dragon;
import dev.extrahardmode.feature.FeatureBus;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobTargetMixin {
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void tougher$noDragonTarget(LivingEntity target, CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (!FeatureBus.guard(self.level(), Dragon.ID)) {
            return;
        }
        if (Dragon.blocksTarget(target, self.level())) {
            ci.cancel();
        }
    }
}

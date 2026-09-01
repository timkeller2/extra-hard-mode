package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.FeatureBus;
import dev.extrahardmode.feature.monster.Spiders;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobAiStepMixin {
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void extrahardmode$breakWebs(CallbackInfo ci) {
        if (!FeatureBus.guard(((Mob) (Object) this).level(), Spiders.ID)) {
            return;
        }
        Spiders.tryBreakWeb((Mob) (Object) this);
    }
}

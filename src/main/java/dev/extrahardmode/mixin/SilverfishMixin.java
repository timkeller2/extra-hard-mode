package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.monster.Silverfish;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.world.entity.monster.Silverfish.class)
public abstract class SilverfishMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void ehm$visibilityParticles(CallbackInfo ci) {
        net.minecraft.world.entity.monster.Silverfish self = (net.minecraft.world.entity.monster.Silverfish) (Object) this;
        if (!(self.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, Silverfish.ID)) {
            return;
        }
        Silverfish.spawnVisibilityParticles(self, level);
    }
}

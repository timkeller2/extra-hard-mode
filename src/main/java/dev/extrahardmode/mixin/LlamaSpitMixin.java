package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.BiomeBosses;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla spit always deals 1 damage. Brood-mother spit deals half of her bite and blinds. */
@Mixin(LlamaSpit.class)
public abstract class LlamaSpitMixin {
    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
    private void tougher$broodSpit(EntityHitResult hit, CallbackInfo ci) {
        LlamaSpit self = (LlamaSpit) (Object) this;
        if (!(self.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, BiomeBosses.ID)) {
            return;
        }
        if (!BiomeBosses.isBroodBoss(self.getOwner())) {
            return;
        }
        BiomeBosses.onBroodSpitHit(level, self, hit.getEntity());
        self.discard();
        ci.cancel();
    }
}

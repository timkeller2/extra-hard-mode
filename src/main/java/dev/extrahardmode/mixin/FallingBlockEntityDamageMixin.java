package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.CaveIns;
import dev.extrahardmode.feature.FallingBlocks;
import dev.extrahardmode.feature.FeatureBus;
import dev.extrahardmode.feature.RealisticChopping;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityDamageMixin {
    @Shadow
    private boolean hurtEntities;

    @Inject(method = "causeFallDamage", at = @At("HEAD"))
    private void tougher$fallingDamage(
            double fallDistance, float multiplier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        FallingBlockEntity self = (FallingBlockEntity) (Object) this;
        if (!FeatureBus.guard(self.level(), FallingBlocks.ID)
                && !FeatureBus.guard(self.level(), CaveIns.ID)
                && !FeatureBus.guard(self.level(), RealisticChopping.ID)) {
            return;
        }
        if (FallingBlocks.isVanillaFallDamage(self.getBlockState())) {
            return;
        }
        if (this.hurtEntities) {
            return;
        }
        FallingBlocks.applyConfiguredDamage(self, fallDistance);
    }
}

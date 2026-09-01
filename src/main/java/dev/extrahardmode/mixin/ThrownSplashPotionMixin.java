package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.monster.Witches;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownSplashPotion.class)
public abstract class ThrownSplashPotionMixin {
    @Inject(method = "onHitAsPotion", at = @At("HEAD"), cancellable = true)
    private void ehm$extraAttacks(ServerLevel level, ItemStack stack, HitResult hit, CallbackInfo ci) {
        if (!WorldGate.isModuleActive(level, Witches.ID)) {
            return;
        }
        if (Witches.onSplash((ThrownSplashPotion) (Object) this, level, stack, hit)) {
            ci.cancel();
        }
    }
}

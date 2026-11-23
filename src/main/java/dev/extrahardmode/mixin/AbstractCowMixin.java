package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Overgrazing;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bucket milking of cows and mooshrooms. Stew and shearing return before this
 * method, so they are not a milking.
 */
@Mixin(AbstractCow.class)
public abstract class AbstractCowMixin {
    @Unique
    private boolean tougher$milkPending;

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void tougher$denyMilkedCow(
            Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        AbstractCow cow = (AbstractCow) (Object) this;
        if (!(cow.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, Overgrazing.ID)) {
            tougher$milkPending = false;
            return;
        }
        tougher$milkPending = false;
        if (Overgrazing.denyMilk(level, cow, player, hand)) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }
        tougher$milkPending = Overgrazing.milkAttempt(level, cow, player, hand);
    }

    @Inject(method = "mobInteract", at = @At("RETURN"))
    private void tougher$hungerAfterMilk(
            Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!tougher$milkPending) {
            return;
        }
        tougher$milkPending = false;
        if (cir.getReturnValue() != InteractionResult.SUCCESS) {
            return;
        }
        AbstractCow cow = (AbstractCow) (Object) this;
        if (cow.level() instanceof ServerLevel level) {
            Overgrazing.afterMilk(level, cow);
        }
    }
}

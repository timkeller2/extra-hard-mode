package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.ManaAbilities;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A spider eye in the main hand starts the vanilla eat. Releasing it within the click window casts
 * Sense Evil. A finished eat goes through {@code completeUsingItem} and never reaches this method,
 * so the eye is eaten and the ability does not fire. Held ticks are read here, before the use stops.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityReleaseMixin {
    @Inject(method = "releaseUsingItem", at = @At("HEAD"))
    private void tougher$senseEvilClick(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayer player) || !self.isUsingItem()) {
            return;
        }
        if (self.getUsedItemHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        ItemStack using = self.getUseItem();
        if (using == null || using.isEmpty() || !using.is(Items.SPIDER_EYE)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, ManaAbilities.ID)) {
            return;
        }
        ManaAbilities.onSpiderEyeReleased(player, self.getTicksUsingItem());
    }
}

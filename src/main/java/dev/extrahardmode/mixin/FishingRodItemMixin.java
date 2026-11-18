package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.FishingRodRules;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Reeling a line takes three times the durability. Unbreaking still judges the original loss. */
@Mixin(FishingRodItem.class)
public abstract class FishingRodItemMixin {
    @Redirect(
            method = "use",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V"))
    private void tougher$tripleRodDurability(ItemStack stack, int amount, LivingEntity entity, EquipmentSlot slot) {
        if (!(entity.level() instanceof ServerLevel level) || !WorldGate.isActive(level)) {
            stack.hurtAndBreak(amount, entity, slot);
            return;
        }
        if (!(entity instanceof ServerPlayer player)) {
            stack.hurtAndBreak(amount, entity, slot);
            return;
        }
        if (amount <= 0 || !stack.isDamageableItem() || player.hasInfiniteMaterials()) {
            return;
        }
        int taken = EnchantmentHelper.processDurabilityChange(level, stack, amount);
        int loss = FishingRodRules.durabilityLoss(amount, taken);
        if (loss <= 0) {
            return;
        }
        int next = stack.getDamageValue() + loss;
        CriteriaTriggers.ITEM_DURABILITY_CHANGED.trigger(player, stack, next);
        stack.setDamageValue(next);
        if (stack.isBroken()) {
            ItemStack broken = stack.copy();
            stack.shrink(1);
            player.onEquippedItemBroken(broken, slot);
        }
    }
}

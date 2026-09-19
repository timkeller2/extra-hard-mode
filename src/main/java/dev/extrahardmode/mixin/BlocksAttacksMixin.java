package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Players;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlocksAttacks.class)
public abstract class BlocksAttacksMixin {
    @Redirect(
            method = "hurtBlockingItem",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V"))
    private void extrahardmode$doubleShieldDurability(
            ItemStack stack, int amount, LivingEntity entity, EquipmentSlot slot) {
        stack.hurtAndBreak(Players.scaleShieldDurability(entity, entity.level(), amount), entity, slot);
    }
}

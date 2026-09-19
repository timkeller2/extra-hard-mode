package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Hunger;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodProperties.class)
public abstract class FoodPropertiesMixin {
    @Inject(method = "onConsume", at = @At("RETURN"))
    private void ehm$varietyBonus(Level level, LivingEntity entity, ItemStack stack, Consumable consumable, CallbackInfo ci) {
        if (entity instanceof Player player) {
            Hunger.onFoodEaten(player, stack);
        }
    }
}

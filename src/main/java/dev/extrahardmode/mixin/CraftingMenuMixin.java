package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.MoreTnt;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {
    @Inject(method = "slotChangedCraftingGrid", at = @At("HEAD"))
    private static void extrahardmode$captureCraftLevel(
            AbstractContainerMenu menu,
            ServerLevel level,
            Player player,
            CraftingContainer container,
            ResultContainer result,
            RecipeHolder<CraftingRecipe> recipe,
            CallbackInfo ci) {
        MoreTnt.captureCraftLevel(level);
    }

    @ModifyArg(
            method = "slotChangedCraftingGrid",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/inventory/ResultContainer;setItem(ILnet/minecraft/world/item/ItemStack;)V"),
            index = 1)
    private static ItemStack extrahardmode$tntCount(ItemStack stack) {
        return MoreTnt.adjustCaptured(stack);
    }

    @Inject(method = "slotChangedCraftingGrid", at = @At("RETURN"))
    private static void extrahardmode$clearCraftLevel(
            AbstractContainerMenu menu,
            ServerLevel level,
            Player player,
            CraftingContainer container,
            ResultContainer result,
            RecipeHolder<CraftingRecipe> recipe,
            CallbackInfo ci) {
        MoreTnt.clearCraftLevel();
    }
}

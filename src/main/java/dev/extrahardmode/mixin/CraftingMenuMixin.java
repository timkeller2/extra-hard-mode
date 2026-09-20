package dev.extrahardmode.mixin;

import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.feature.AntiFarming;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import dev.extrahardmode.feature.MoreTnt;
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
    /** InventoryMenu.slotsChanged also invokes this static for the 2x2 grid. */
    @Inject(method = "slotChangedCraftingGrid", at = @At("RETURN"))
    private static void ehm$noMelonSeeds(
            AbstractContainerMenu menu,
            ServerLevel level,
            Player player,
            CraftingContainer craftSlots,
            ResultContainer resultSlots,
            RecipeHolder<CraftingRecipe> recipe,
            CallbackInfo ci) {
        if (!WorldGate.isModuleActive(level, AntiFarming.ID)) {
            return;
        }
        if (!ConfigManager.world(level).cantCraftMelonSeeds()) {
            return;
        }
        if (player instanceof ServerPlayer serverPlayer && EhmApi.playerBypasses(serverPlayer)) {
            return;
        }
        ItemStack result = resultSlots.getItem(0);
        if (AntiFarming.isSeedResult(result)) {
            resultSlots.setItem(0, ItemStack.EMPTY);
            AntiFarming.notifyNoMelonSeeds(player);
        }
    }

    @Inject(method = "slotChangedCraftingGrid", at = @At("HEAD"))
    private static void tougher$captureCraftLevel(
            AbstractContainerMenu menu,
            ServerLevel level,
            Player player,
            CraftingContainer craftSlots,
            ResultContainer resultSlots,
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
    private static ItemStack tougher$tntCount(ItemStack stack) {
        return MoreTnt.adjustCaptured(stack);
    }

    @Inject(method = "slotChangedCraftingGrid", at = @At("RETURN"))
    private static void tougher$clearCraftLevel(
            AbstractContainerMenu menu,
            ServerLevel level,
            Player player,
            CraftingContainer craftSlots,
            ResultContainer resultSlots,
            RecipeHolder<CraftingRecipe> recipe,
            CallbackInfo ci) {
        MoreTnt.clearCraftLevel();
    }
}

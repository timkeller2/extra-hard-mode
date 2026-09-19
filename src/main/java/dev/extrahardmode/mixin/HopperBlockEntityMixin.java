package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.FurnaceXp;
import dev.extrahardmode.feature.FurnaceXpRules;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {
    @Unique
    private static final ThreadLocal<int[]> EHM$MOVE = new ThreadLocal<>();

    @Inject(method = "tryTakeInItemFromSlot", at = @At("RETURN"))
    private static void ehm$furnaceXpFromResult(
            Hopper hopper,
            Container container,
            int slot,
            Direction direction,
            CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }
        if (slot != FurnaceXpRules.FURNACE_RESULT_SLOT || !(container instanceof AbstractFurnaceBlockEntity furnace)) {
            return;
        }
        if (!(furnace.getLevel() instanceof ServerLevel level) || !WorldGate.isActive(level)) {
            return;
        }
        if (hopper instanceof Container destination) {
            FurnaceXp.onFurnaceExtracted(furnace, destination, level);
        }
    }

    @Inject(
            method =
                    "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"))
    private static void ehm$rememberMove(
            Container source,
            Container destination,
            ItemStack stack,
            Direction direction,
            CallbackInfoReturnable<ItemStack> cir) {
        int count = stack == null ? 0 : stack.getCount();
        EHM$MOVE.set(new int[] {FurnaceXp.countItems(source) + count, count});
    }

    @Inject(
            method =
                    "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN"))
    private static void ehm$moveCookingXp(
            Container source,
            Container destination,
            ItemStack stack,
            Direction direction,
            CallbackInfoReturnable<ItemStack> cir) {
        int[] snap = EHM$MOVE.get();
        EHM$MOVE.remove();
        if (snap == null) {
            return;
        }
        ItemStack leftover = cir.getReturnValue();
        int leftoverCount = leftover == null || leftover.isEmpty() ? 0 : leftover.getCount();
        int moved = snap[1] - leftoverCount;
        FurnaceXp.onItemsMoved(source, destination, moved, snap[0]);
    }
}

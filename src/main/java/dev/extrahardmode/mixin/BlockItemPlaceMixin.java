package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Achievements;
import dev.extrahardmode.feature.AntiFarming;
import dev.extrahardmode.feature.CopperChests;
import dev.extrahardmode.feature.Dragon;
import dev.extrahardmode.feature.FeatureBus;
import dev.extrahardmode.feature.HardenedStone;
import dev.extrahardmode.feature.LapisPortals;
import dev.extrahardmode.feature.OwnedHoppers;
import dev.extrahardmode.feature.PlacementRules;
import dev.extrahardmode.feature.Torches;
import dev.extrahardmode.feature.UnsafeBeds;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemPlaceMixin {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void tougher$blockOreAgainstStone(
            BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (PlacementRules.denyOffhandRedstone(
                context.getHand() == InteractionHand.OFF_HAND, context.getItemInHand().is(Items.REDSTONE))) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }
        Block placed = ((BlockItem) (Object) this).getBlock();
        InteractionResult copperChest = CopperChests.denyPlacement(context, placed);
        if (copperChest != InteractionResult.PASS) {
            cir.setReturnValue(copperChest);
            return;
        }
        InteractionResult hopper = OwnedHoppers.denyPlacement(context, placed);
        if (hopper != InteractionResult.PASS) {
            cir.setReturnValue(hopper);
            return;
        }
        Level level = context.getLevel();
        if (!FeatureBus.guard(level, Dragon.ID)
                && !FeatureBus.guard(level, HardenedStone.ID)
                && !FeatureBus.guard(level, Torches.ID)) {
            return;
        }
        if (FeatureBus.guard(level, Dragon.ID)) {
            InteractionResult endDeny = Dragon.denyPlacement(context);
            if (endDeny != InteractionResult.PASS) {
                cir.setReturnValue(endDeny);
                return;
            }
        }
        if (FeatureBus.guard(level, Torches.ID)) {
            InteractionResult torchDeny = Torches.tryDenyPlacement(context);
            if (torchDeny != InteractionResult.PASS) {
                cir.setReturnValue(torchDeny);
                return;
            }
        }
        if (!FeatureBus.guard(level, HardenedStone.ID)) {
            return;
        }
        InteractionResult deny = HardenedStone.denyOrePlacement(context);
        if (deny != InteractionResult.PASS) {
            cir.setReturnValue(deny);
        }
    }

    @Inject(method = "place", at = @At("RETURN"))
    private void tougher$builderPlace(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult result = cir.getReturnValue();
        if (result == null || !result.consumesAction()) {
            return;
        }
        Block block = ((BlockItem) (Object) this).getBlock();
        if (UnsafeBeds.vanishes(UnsafeBeds.hostileDimension(context.getLevel()), block instanceof AbstractBedBlock)) {
            UnsafeBeds.remove(context.getLevel(), context.getClickedPos());
        }
        Achievements.onPlaced(context, block);
        AntiFarming.onCropPlanted(context, block);
        Torches.onPlaced(context, block);
        LapisPortals.onPlaced(context, block);
        CopperChests.onPlaced(context, block);
        OwnedHoppers.onPlaced(context, block);
    }
}

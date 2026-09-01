package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.FeatureBus;
import dev.extrahardmode.feature.HardenedStone;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemPlaceMixin {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void extrahardmode$blockOreAgainstStone(
            BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!FeatureBus.guard(context.getLevel(), HardenedStone.ID)) {
            return;
        }
        InteractionResult deny = HardenedStone.denyOrePlacement(context);
        if (deny != InteractionResult.PASS) {
            cir.setReturnValue(deny);
        }
    }
}

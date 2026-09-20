package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.FallingBlocks;
import dev.extrahardmode.feature.FeatureBus;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class FallingBlockPlaceMixin {
    @Inject(method = "place", at = @At("RETURN"))
    private void tougher$fallingPlace(
            BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!FeatureBus.guard(context.getLevel(), FallingBlocks.ID)) {
            return;
        }
        InteractionResult result = cir.getReturnValue();
        if (result == null || !result.consumesAction()) {
            return;
        }
        FallingBlocks.onPlaced(context);
    }
}

package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Dragon;
import dev.extrahardmode.feature.FeatureBus;
import dev.extrahardmode.feature.HardenedStone;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemPlaceMixin {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void extrahardmode$blockOreAgainstStone(
            BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Level level = context.getLevel();
        if (!FeatureBus.guard(level, Dragon.ID) && !FeatureBus.guard(level, HardenedStone.ID)) {
            return;
        }
        if (FeatureBus.guard(level, Dragon.ID)) {
            InteractionResult endDeny = Dragon.denyPlacement(context);
            if (endDeny != InteractionResult.PASS) {
                cir.setReturnValue(endDeny);
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
}

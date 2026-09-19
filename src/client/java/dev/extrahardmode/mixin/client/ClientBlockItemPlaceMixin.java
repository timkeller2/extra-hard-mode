package dev.extrahardmode.mixin.client;

import dev.extrahardmode.client.ExtraHardModeClient;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class ClientBlockItemPlaceMixin {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void extrahardmode$clientBlockOreAgainstStone(
            BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (ExtraHardModeClient.lastSync() == null) {
            return;
        }
        if (context.getLevel() instanceof ServerLevel) {
            return;
        }
        if (ExtraHardModeClient.denyEndBuilding(context)) {
            ExtraHardModeClient.toast("limited_end_building");
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }
        if (ExtraHardModeClient.denyTorchPlacement(context)) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }
        if (ExtraHardModeClient.denyOrePlacement(context)) {
            ExtraHardModeClient.toast("no_placing_ore_against_stone");
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}

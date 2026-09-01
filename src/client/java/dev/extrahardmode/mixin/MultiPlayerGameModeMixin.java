package dev.extrahardmode.mixin;

import dev.extrahardmode.client.ExtraHardModeClient;
import dev.extrahardmode.feature.LimitedBuilding;
import dev.extrahardmode.feature.Torches;
import dev.extrahardmode.network.ClientboundSyncPayload;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    @Inject(method = "performUseItemOn", at = @At("HEAD"), cancellable = true)
    private void extrahardmode$cancelPlace(
            LocalPlayer player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        // lastSync() null / inactive() is the client WorldGate skip (payload-only, never TOML).
        ClientboundSyncPayload sync = ExtraHardModeClient.lastSync();
        if (sync == null) {
            return;
        }
        if (!sync.torchYDeny() && !sync.torchSoftDeny() && !sync.limitedBuilding()) {
            return;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return;
        }
        BlockPlaceContext context = new BlockPlaceContext(new UseOnContext(player.level(), player, hand, stack, hit));
        if (Torches.denyReasonFromPayload(
                        sync.torchYDeny(),
                        sync.torchNoPlacementUnderY(),
                        sync.torchSoftDeny(),
                        sync.depthLimitedLights(),
                        sync.softTorchSurfaces(),
                        context,
                        blockItem)
                != Torches.DenyReason.NONE) {
            cir.setReturnValue(InteractionResult.FAIL);
            return;
        }
        if (sync.limitedBuilding() && LimitedBuilding.shouldDeny(player.level(), player, context, blockItem)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}

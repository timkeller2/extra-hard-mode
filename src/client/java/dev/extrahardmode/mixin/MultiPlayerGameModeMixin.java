package dev.extrahardmode.mixin;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.client.ExtraHardModeClient;
import dev.extrahardmode.feature.LimitedBuilding;
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
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void tougher$cancelPlace(
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
        try {
            if (sync.torchYDeny()
                    && ExtraHardModeClient.denyCampfireLightFromPayload(player.level(), hit.getBlockPos(), stack)) {
                cir.setReturnValue(InteractionResult.FAIL);
                return;
            }
            BlockPlaceContext context = new BlockPlaceContext(new UseOnContext(player.level(), player, hand, stack, hit));
            if (ExtraHardModeClient.denyTorchPlacement(context)) {
                cir.setReturnValue(InteractionResult.FAIL);
                return;
            }
            if (!(stack.getItem() instanceof BlockItem blockItem)) {
                return;
            }
            if (sync.limitedBuilding() && LimitedBuilding.shouldDeny(player.level(), player, context, blockItem)) {
                cir.setReturnValue(InteractionResult.FAIL);
            }
        } catch (Throwable t) {
            ExtraHardModeMod.LOGGER.warn("Client block-place prediction failed", t);
        }
    }
}

package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Hunger;
import dev.extrahardmode.feature.Torches;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handlePunch", at = @At("TAIL"))
    private void tougher$manaAbilitySwing(ServerboundPunchPacket packet, CallbackInfo ci) {
        if (this.player == null) {
            return;
        }
        Hunger.noteActivity(this.player);
    }

    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
    private void tougher$blockOffhandLightSwap(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (this.player == null) {
            return;
        }
        if (packet.getAction() != ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND) {
            return;
        }
        if (Torches.denyOffhandSwap(this.player)) {
            ci.cancel();
        }
    }
}

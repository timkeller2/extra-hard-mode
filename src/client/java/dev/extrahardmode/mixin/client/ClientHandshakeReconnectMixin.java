package dev.extrahardmode.mixin.client;

import dev.extrahardmode.client.BriefReconnect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.DisconnectionDetails;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Login does not extend the play listener, so a failed rejoin is handled here.
 * A first connect from the server list is not a retry and is left on the disconnect screen.
 */
@Mixin(ClientHandshakePacketListenerImpl.class)
public class ClientHandshakeReconnectMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private ServerData serverData;

    @Shadow
    @Final
    private boolean wasTransferredTo;

    @Inject(method = "onDisconnect", at = @At("RETURN"))
    private void tougher$reconnectFailedJoin(DisconnectionDetails details, CallbackInfo ci) {
        if (this.wasTransferredTo || this.serverData == null) {
            return;
        }
        BriefReconnect.consider(
                this.minecraft,
                this.serverData,
                details == null ? null : details.reason(),
                false,
                false);
    }
}

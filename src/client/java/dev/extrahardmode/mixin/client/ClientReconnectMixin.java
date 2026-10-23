package dev.extrahardmode.mixin.client;

import dev.extrahardmode.client.BriefReconnect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.DisconnectionDetails;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The play connection is already closed here, so there is no level for WorldGate.
 * A transfer or a singleplayer world is left alone.
 */
@Mixin(ClientCommonPacketListenerImpl.class)
public class ClientReconnectMixin {
    @Shadow
    @Final
    protected Minecraft minecraft;

    @Shadow
    @Final
    protected ServerData serverData;

    @Shadow
    protected boolean isTransferring;

    @Inject(method = "onDisconnect", at = @At("RETURN"))
    private void tougher$reconnectBriefDrop(DisconnectionDetails details, CallbackInfo ci) {
        if (this.isTransferring || this.serverData == null) {
            return;
        }
        BriefReconnect.consider(
                this.minecraft,
                this.serverData,
                details == null ? null : details.reason(),
                (Object) this instanceof ClientPacketListener,
                false);
    }
}

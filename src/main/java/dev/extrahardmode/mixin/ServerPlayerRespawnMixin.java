package dev.extrahardmode.mixin;

import dev.extrahardmode.network.EhmNetworking;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerRespawnMixin {
    @Inject(method = "setRespawnPosition", at = @At("RETURN"))
    private void tougher$syncCompassSpawn(ServerPlayer.RespawnConfig config, boolean sendMessage, CallbackInfo ci) {
        EhmNetworking.sendCompassSpawn((ServerPlayer) (Object) this);
    }
}

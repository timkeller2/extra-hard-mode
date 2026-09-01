package dev.extrahardmode.mixin.client;

import dev.extrahardmode.client.ExtraHardModeClient;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class ClientPlayerDestroySpeedMixin {
    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    private void extrahardmode$clientHardenedDestroySpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
        if (ExtraHardModeClient.lastSync() == null) {
            return;
        }
        Player self = (Player) (Object) this;
        if (self.level() instanceof net.minecraft.server.level.ServerLevel) {
            return;
        }
        if (ExtraHardModeClient.denyHardened(self, state)) {
            cir.setReturnValue(0.0F);
        }
    }
}

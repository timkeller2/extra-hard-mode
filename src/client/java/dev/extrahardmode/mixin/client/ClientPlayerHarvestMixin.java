package dev.extrahardmode.mixin.client;

import dev.extrahardmode.client.ExtraHardModeClient;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class ClientPlayerHarvestMixin {
    @Inject(method = "hasCorrectToolForDrops", at = @At("HEAD"), cancellable = true)
    private void extrahardmode$clientHardenedHarvest(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (ExtraHardModeClient.lastSync() == null) {
            return;
        }
        Player self = (Player) (Object) this;
        if (self.level() instanceof ServerLevel) {
            return;
        }
        if (ExtraHardModeClient.denyHardened(self, state)) {
            cir.setReturnValue(false);
        }
    }
}

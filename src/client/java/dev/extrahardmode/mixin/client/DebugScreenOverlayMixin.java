package dev.extrahardmode.mixin.client;

import dev.extrahardmode.client.ExtraHardModeClient;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DebugScreenOverlay.class)
public class DebugScreenOverlayMixin {
    @Inject(method = "showDebugScreen", at = @At("HEAD"), cancellable = true)
    private void extrahardmode$hideF3(CallbackInfoReturnable<Boolean> cir) {
        if (!ExtraHardModeClient.f3Allowed()) {
            cir.setReturnValue(false);
        }
    }
}

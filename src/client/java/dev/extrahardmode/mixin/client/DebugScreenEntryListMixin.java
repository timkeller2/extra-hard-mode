package dev.extrahardmode.mixin.client;

import dev.extrahardmode.client.ExtraHardModeClient;
import net.minecraft.client.gui.components.debug.DebugScreenEntryList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugScreenEntryList.class)
public class DebugScreenEntryListMixin {
    @Inject(method = "toggleDebugOverlay", at = @At("HEAD"), cancellable = true)
    private void tougher$blockF3Toggle(CallbackInfo ci) {
        if (!ExtraHardModeClient.f3Allowed()) {
            ci.cancel();
        }
    }
}

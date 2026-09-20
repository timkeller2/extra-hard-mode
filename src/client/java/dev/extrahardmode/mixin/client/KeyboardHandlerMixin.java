package dev.extrahardmode.mixin.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.extrahardmode.client.AbilityHelp;
import dev.extrahardmode.client.ExtraHardModeClient;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "handleDebugKeys", at = @At("HEAD"), cancellable = true)
    private void tougher$blockF3Combos(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!ExtraHardModeClient.f3Allowed()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void tougher$abilityHelpKey(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (action != InputConstants.PRESS) {
            return;
        }
        if (event.key() != InputConstants.KEY_SLASH) {
            return;
        }
        if (!event.hasShiftDown() && !this.minecraft.hasShiftDown()) {
            return;
        }
        if (AbilityHelp.tryShow(this.minecraft)) {
            ci.cancel();
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void tougher$abilityHelpChar(long window, CharacterEvent event, CallbackInfo ci) {
        if (event.codepoint() != '?') {
            return;
        }
        if (AbilityHelp.tryShow(this.minecraft)) {
            ci.cancel();
        }
    }
}

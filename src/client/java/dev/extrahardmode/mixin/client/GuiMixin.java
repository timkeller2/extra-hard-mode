package dev.extrahardmode.mixin.client;

import dev.extrahardmode.client.AbilityHelp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "handleKeybinds", at = @At("HEAD"))
    private void tougher$questionHelp(CallbackInfo ci) {
        if (!this.minecraft.hasShiftDown()) {
            return;
        }
        if (!AbilityHelp.shouldHandle(this.minecraft)) {
            return;
        }
        if (this.minecraft.options.keyCommand.consumeClick()) {
            AbilityHelp.tryShow(this.minecraft);
        }
    }
}

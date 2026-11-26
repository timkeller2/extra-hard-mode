package dev.extrahardmode.mixin.client;

import dev.extrahardmode.client.AutoStackClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.sdl.SDLMouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Yellow outline on excluded slots, and Alt-click to toggle one or a whole inventory. */
@Mixin(AbstractContainerScreen.class)
public abstract class AutoStackScreenMixin {
    @Shadow
    protected AbstractContainerMenu menu;

    @Shadow
    private Slot getHoveredSlot(double mouseX, double mouseY) {
        return null;
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void tougher$autoStackExclude(
            MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        // 26.3 numbers buttons the SDL way. The left button is 1, so a check for 0 never runs.
        if (event == null || event.button() != SDLMouse.SDL_BUTTON_LEFT || !altDown(event)) {
            return;
        }
        if (AutoStackClient.tryToggle(this.getHoveredSlot(event.x(), event.y()), doubleClick)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * The vanilla hover frame is a 4-pixel border drawn after each slot, and it covers
     * a mark painted inside {@code extractSlot}. Draw once that frame is in, still in
     * the translated container pose.
     */
    @Inject(
            method = "extractContents",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;"
                            + "extractSlotHighlightFront(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V",
                    shift = At.Shift.AFTER))
    private void tougher$autoStackOutline(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        AutoStackClient.drawExcluded(graphics, this.menu.slots);
    }

    /** The click's modifier bit can miss Alt. The live key state still has it. */
    private static boolean altDown(MouseButtonEvent event) {
        return event.hasAltDown() || Minecraft.getInstance().hasAltDown();
    }
}

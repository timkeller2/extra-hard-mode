package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.AntiFarming;
import dev.extrahardmode.player.EhmAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Display.class)
public abstract class DisplayMixin {
    @Inject(method = "tick", at = @At("RETURN"))
    private void ehm$floatSoilModifier(CallbackInfo ci) {
        Display self = (Display) (Object) this;
        if (!(self.level() instanceof ServerLevel)) {
            return;
        }
        Integer left = self.getAttached(EhmAttachments.EHM_FLOAT_TEXT);
        if (left == null || left <= 0) {
            return;
        }
        int next = left - 1;
        self.setPos(self.getX(), self.getY() + AntiFarming.FLOAT_TEXT_RISE, self.getZ());
        if (next <= 0) {
            self.discard();
            return;
        }
        self.setAttached(EhmAttachments.EHM_FLOAT_TEXT, next);
    }
}

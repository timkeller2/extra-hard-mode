package dev.extrahardmode.mixin;

import dev.extrahardmode.player.EhmAttachments;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityFloatTextMixin {
    @Inject(method = "shouldBeSaved", at = @At("HEAD"), cancellable = true)
    private void ehm$skipFloatTextSave(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        Integer left = self.getAttached(EhmAttachments.EHM_FLOAT_TEXT);
        if (left != null && left > 0) {
            cir.setReturnValue(false);
        }
    }
}

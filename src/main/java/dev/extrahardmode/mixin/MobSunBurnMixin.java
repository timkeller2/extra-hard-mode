package dev.extrahardmode.mixin;

import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A marked mob, such as the biome-boss finale phantom, does not burn in sunlight. */
@Mixin(Mob.class)
public abstract class MobSunBurnMixin {
    @Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
    private void tougher$ignoreSunlight(CallbackInfoReturnable<Boolean> cir) {
        Mob self = (Mob) (Object) this;
        if (!(self.level() instanceof ServerLevel level) || !WorldGate.isActive(level)) {
            return;
        }
        if (Boolean.TRUE.equals(self.getAttachedOrElse(EhmAttachments.EHM_NO_SUN_BURN, Boolean.FALSE))) {
            cir.setReturnValue(false);
        }
    }
}

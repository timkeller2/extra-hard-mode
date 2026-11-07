package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.WiseLessons;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla detonation never calls die(), so a hunt creeper has to be queued here. */
@Mixin(Creeper.class)
public abstract class CreeperExplodeMixin {
    @Inject(method = "explodeCreeper", at = @At("HEAD"))
    private void ehm$requeueHuntCreeper(CallbackInfo ci) {
        Creeper creeper = (Creeper) (Object) this;
        if (!(creeper.level() instanceof ServerLevel level) || !WorldGate.isActive(level)) {
            return;
        }
        WiseLessons.onCreeperExploded(creeper);
    }
}

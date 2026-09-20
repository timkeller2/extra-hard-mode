package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.monster.Endermen;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Enderman;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Enderman.class)
public abstract class EnderManMixin {
    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void ehm$teleportPlayer(ServerLevel level, CallbackInfo ci) {
        if (!WorldGate.isModuleActive(level, Endermen.ID)) {
            return;
        }
        Endermen.tick((Enderman) (Object) this, level);
    }
}

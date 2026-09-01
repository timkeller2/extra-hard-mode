package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.monster.PigMen;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only {@code customServerAiStep} is declared on 26.2 {@link ZombifiedPiglin}. Calm/damage live on NeutralMob/Mob. */
@Mixin(ZombifiedPiglin.class)
public abstract class ZombifiedPiglinMixin {
    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void ehm$alwaysAngry(ServerLevel level, CallbackInfo ci) {
        if (!WorldGate.isModuleActive(level, PigMen.ID)) {
            return;
        }
        PigMen.keepAngry((ZombifiedPiglin) (Object) this, level);
    }

    /** Re-apply after vanilla {@code updatePersistentAnger} may have calmed. */
    @Inject(method = "customServerAiStep", at = @At("RETURN"))
    private void ehm$alwaysAngryAfter(ServerLevel level, CallbackInfo ci) {
        if (!WorldGate.isModuleActive(level, PigMen.ID)) {
            return;
        }
        PigMen.keepAngry((ZombifiedPiglin) (Object) this, level);
    }
}

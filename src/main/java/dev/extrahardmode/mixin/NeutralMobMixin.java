package dev.extrahardmode.mixin;

import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.feature.monster.PigMen;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NeutralMob.class)
public interface NeutralMobMixin {
    @Inject(method = "stopBeingAngry", at = @At("HEAD"), cancellable = true)
    default void ehm$pigmenNeverCalm(CallbackInfo ci) {
        NeutralMob self = (NeutralMob) this;
        if (!(self instanceof ZombifiedPiglin piglin)) {
            return;
        }
        if (!(piglin.level() instanceof ServerLevel level)) {
            return;
        }
        if (!WorldGate.isModuleActive(level, PigMen.ID)) {
            return;
        }
        if (!PigMen.shouldStayAngry(level)) {
            return;
        }
        ci.cancel();
    }

    @Inject(method = "isAngryAtAllPlayers", at = @At("HEAD"), cancellable = true)
    default void ehm$pigmenAngryAtPlayers(ServerLevel level, CallbackInfoReturnable<Boolean> cir) {
        if (!WorldGate.isModuleActive(level, PigMen.ID)) {
            return;
        }
        if (!ConfigManager.world(level).pigmenAlwaysAngry()) {
            return;
        }
        NeutralMob self = (NeutralMob) this;
        if (!(self instanceof ZombifiedPiglin)) {
            return;
        }
        if (self.isAngry()) {
            cir.setReturnValue(true);
        }
    }
}

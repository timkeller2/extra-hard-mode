package dev.extrahardmode.mixin;

import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.feature.monster.PigMen;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ZombifiedPiglin.class)
public abstract class ZombifiedPiglinMixin {
    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void ehm$alwaysAngry(ServerLevel level, CallbackInfo ci) {
        if (!WorldGate.isModuleActive(level, PigMen.ID)) {
            return;
        }
        PigMen.keepAngry((ZombifiedPiglin) (Object) this, level);
    }

    @Inject(method = "stopBeingAngry", at = @At("HEAD"), cancellable = true, require = 0)
    private void ehm$neverCalm(CallbackInfo ci) {
        ZombifiedPiglin self = (ZombifiedPiglin) (Object) this;
        if (!(self.level() instanceof ServerLevel level)) {
            return;
        }
        if (!PigMen.shouldStayAngry(level)) {
            return;
        }
        ci.cancel();
    }

    @Inject(method = "isAngryAtAllPlayers", at = @At("HEAD"), cancellable = true, require = 0)
    private void ehm$angryAtPlayers(ServerLevel level, CallbackInfoReturnable<Boolean> cir) {
        if (!WorldGate.isModuleActive(level, PigMen.ID)) {
            return;
        }
        if (!ConfigManager.world(level).pigmenAlwaysAngry()) {
            return;
        }
        ZombifiedPiglin self = (ZombifiedPiglin) (Object) this;
        if (self.isAngry()) {
            cir.setReturnValue(true);
        }
    }

    @Redirect(
            method = "doHurtTarget",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/Entity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean ehm$scalePlayerDamage(Entity target, ServerLevel level, DamageSource source, float amount) {
        if (!WorldGate.isModuleActive(level, PigMen.ID)) {
            return target.hurtServer(level, source, amount);
        }
        if (target instanceof Player player) {
            amount = PigMen.scaleIfPlayerHit(
                    player, (Entity) (Object) this, amount, ConfigManager.world(level).pigmenDamagePercent());
        }
        return target.hurtServer(level, source, amount);
    }
}

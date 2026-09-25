package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.ManaAbilities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Diamond Skin divides the damage that is about to leave the health bar, after armor,
 * protection, and absorption. The combat log uses the same reduced amount.
 */
@Mixin(Player.class)
public class PlayerActuallyHurtMixin {
    private static final ThreadLocal<DamageSource> SOURCE = new ThreadLocal<>();

    @Inject(
            method = "actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V",
            at = @At("HEAD"))
    private void tougher$captureHurt(ServerLevel level, DamageSource source, float amount, CallbackInfo ci) {
        SOURCE.set(source);
    }

    @Inject(
            method = "actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V",
            at = @At("RETURN"))
    private void tougher$clearHurt(ServerLevel level, DamageSource source, float amount, CallbackInfo ci) {
        SOURCE.remove();
    }

    @ModifyArg(
            method = "actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/damagesource/CombatTracker;recordDamage(Lnet/minecraft/world/damagesource/DamageSource;F)V"),
            index = 1)
    private float tougher$scaleRecorded(float healthDamage) {
        return scaled(healthDamage, false);
    }

    @Redirect(
            method = "actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setHealth(F)V"))
    private void tougher$scaleHealth(Player player, float proposed) {
        float current = player.getHealth();
        float incoming = current - proposed;
        if (incoming <= 0.0F) {
            player.setHealth(proposed);
            return;
        }
        player.setHealth(current - scaled(incoming, true));
    }

    private float scaled(float healthDamage, boolean sparkle) {
        if (!((Object) this instanceof ServerPlayer player)) {
            return healthDamage;
        }
        return ManaAbilities.scaleDiamondSkin(player, SOURCE.get(), healthDamage, sparkle);
    }
}

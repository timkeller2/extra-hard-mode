package dev.extrahardmode.mixin;

import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.feature.monster.PigMen;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** {@code doHurtTarget} / {@code Entity.hurtServer} live on {@link Mob}, not {@link ZombifiedPiglin}. */
@Mixin(Mob.class)
public abstract class MobPiglinDamageMixin {
    @Redirect(
            method = "doHurtTarget",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/entity/Entity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean ehm$scalePiglinPlayerDamage(Entity target, ServerLevel level, DamageSource source, float amount) {
        if (!((Object) this instanceof ZombifiedPiglin)) {
            return target.hurtServer(level, source, amount);
        }
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

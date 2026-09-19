package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.HealthRules;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Heal players up to their current max health (heavy armor and other bonuses
 * can raise it above 20). Also re-apply NBT health after equipment attributes
 * load, because vanilla clamps to 20 while the player is still unarmored.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHealthMixin {
    @Shadow
    public abstract float getHealth();

    @Shadow
    public abstract float getMaxHealth();

    @Shadow
    public abstract void setHealth(float health);

    @Unique
    private float ehm$savedHealth = Float.NaN;

    @Inject(method = "heal", at = @At("HEAD"), cancellable = true)
    private void ehm$healToAdjustedMax(float amount, CallbackInfo ci) {
        if (!((Object) this instanceof Player)) {
            return;
        }
        float health = this.getHealth();
        if (health <= 0.0F) {
            ci.cancel();
            return;
        }
        this.setHealth(HealthRules.afterHeal(health, amount, this.getMaxHealth()));
        ci.cancel();
    }

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void ehm$captureLoadedHealth(ValueInput input, CallbackInfo ci) {
        if (!((Object) this instanceof Player)) {
            return;
        }
        this.ehm$savedHealth = input.getFloatOr("Health", Float.NaN);
    }

    @Inject(method = "detectEquipmentUpdates", at = @At("RETURN"))
    private void ehm$restoreLoadedHealth(CallbackInfo ci) {
        if (Float.isNaN(this.ehm$savedHealth) || !((Object) this instanceof Player)) {
            return;
        }
        float saved = this.ehm$savedHealth;
        this.ehm$savedHealth = Float.NaN;
        this.setHealth(HealthRules.restoreLoadedHealth(this.getHealth(), saved, this.getMaxHealth()));
    }
}

package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Hunger;
import dev.extrahardmode.feature.HungerRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class FoodDataMixin {
    @Shadow
    private int foodLevel;

    @Shadow
    private float saturationLevel;

    @Shadow
    private float exhaustionLevel;

    @Shadow
    private int tickTimer;

    @Shadow
    public abstract void addExhaustion(float exhaustion);

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void ehm$slowRegen(ServerPlayer player, CallbackInfo ci) {
        if (!Hunger.overridesRegen(player)) {
            return;
        }
        ServerLevel level = player.level();
        Difficulty difficulty = level.getDifficulty();
        if (exhaustionLevel > 4.0F) {
            exhaustionLevel -= 4.0F;
            if (saturationLevel > 0.0F) {
                saturationLevel = Math.max(saturationLevel - 1.0F, 0.0F);
            } else if (difficulty != Difficulty.PEACEFUL) {
                foodLevel = Math.max(foodLevel - 1, 0);
            }
        }
        boolean naturalRegen = level.getGameRules().get(GameRules.NATURAL_HEALTH_REGENERATION);
        if (naturalRegen && HungerRules.canRegenHealth(foodLevel, player.getHealth(), player.getMaxHealth())) {
            tickTimer++;
            if (tickTimer >= Hunger.slowRegenTicks(player)) {
                player.heal(1.0F);
                addExhaustion(6.0F);
                tickTimer = 0;
            }
        } else if (foodLevel <= 0) {
            tickTimer++;
            if (tickTimer >= HungerRules.starveTicks()) {
                if (player.getHealth() > 10.0F || difficulty == Difficulty.HARD || player.getHealth() > 1.0F && difficulty == Difficulty.NORMAL) {
                    player.hurtServer(level, player.damageSources().starve(), 1.0F);
                }
                tickTimer = 0;
            }
        } else {
            tickTimer = 0;
        }
        ci.cancel();
    }
}

package dev.extrahardmode.mixin;

import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.feature.AntiFarming;
import dev.extrahardmode.feature.SheepGrazeRules;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Sheep wait longer between the grass bites that regrow wool. */
@Mixin(EatBlockGoal.class)
public abstract class EatBlockGoalMixin {
    @Shadow
    private Mob mob;

    @Redirect(
            method = "canUse",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt:(I)I"))
    private int tougher$slowerWoolGraze(RandomSource random, int bound) {
        if (!(this.mob.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, AntiFarming.ID)) {
            return random.nextInt(bound);
        }
        int scaled = SheepGrazeRules.eatRollBound(bound, ConfigManager.world(level).sheepWoolGrazeMultiplier());
        return random.nextInt(scaled);
    }
}

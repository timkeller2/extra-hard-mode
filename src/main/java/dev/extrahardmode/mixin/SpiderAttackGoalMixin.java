package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.BiomeBosses;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla spiders drop their target in light. A brood mother that has been hit keeps chasing.
 */
@Mixin(targets = "net.minecraft.world.entity.monster.spider.Spider$SpiderAttackGoal")
public abstract class SpiderAttackGoalMixin extends MeleeAttackGoal {
    protected SpiderAttackGoalMixin(PathfinderMob mob, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(mob, speedModifier, followingTargetEvenIfNotSeen);
    }

    @Inject(method = "canContinueToUse", at = @At("HEAD"), cancellable = true)
    private void tougher$broodKeepsTarget(CallbackInfoReturnable<Boolean> cir) {
        if (!(this.mob.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, BiomeBosses.ID)) {
            return;
        }
        if (!BiomeBosses.isBroodBoss(this.mob)) {
            return;
        }
        cir.setReturnValue(super.canContinueToUse());
    }
}

package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.monster.Silverfish;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.entity.monster.Silverfish$SilverfishMergeWithStoneGoal")
public abstract class SilverfishMergeMixin extends RandomStrollGoal {
    @Shadow
    private boolean doMerge;

    private SilverfishMergeMixin(PathfinderMob mob, double speedModifier) {
        super(mob, speedModifier);
    }

    @Inject(method = "start", at = @At("HEAD"), cancellable = true)
    private void ehm$blockEnterBlocks(CallbackInfo ci) {
        if (!(this.mob.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, Silverfish.ID)) {
            return;
        }
        if (!this.doMerge || !Silverfish.cantEnterBlocks(level)) {
            return;
        }
        this.doMerge = false;
        ci.cancel();
    }
}

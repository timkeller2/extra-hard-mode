package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.monster.Silverfish;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.entity.monster.Silverfish$SilverfishMergeWithStoneGoal")
public abstract class SilverfishMergeMixin {
    @Shadow
    private boolean doMerge;

    @Shadow
    protected PathfinderMob mob;

    @Inject(method = "start", at = @At("HEAD"), cancellable = true)
    private void ehm$blockEnterBlocks(CallbackInfo ci) {
        if (!(mob.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, Silverfish.ID)) {
            return;
        }
        if (!doMerge || !Silverfish.cantEnterBlocks(level)) {
            return;
        }
        doMerge = false;
        ci.cancel();
    }
}

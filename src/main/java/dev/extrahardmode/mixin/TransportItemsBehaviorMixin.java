package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.CopperGolems;
import java.util.Set;
import net.minecraft.world.Container;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The searching golem is known while a container is chosen, and again when items move. */
@Mixin(TransportItemsBetweenContainers.class)
public abstract class TransportItemsBehaviorMixin {
    @Shadow
    private TransportItemsBetweenContainers.TransportItemTarget target;

    @Inject(method = "isTargetValidToPick", at = @At("HEAD"))
    private void tougher$rememberGolem(
            PathfinderMob mob,
            Level level,
            BlockEntity entity,
            Set<?> visited,
            Set<?> unreachable,
            AABB area,
            CallbackInfoReturnable<TransportItemsBetweenContainers.TransportItemTarget> cir) {
        CopperGolems.beginSearch(mob);
    }

    @Inject(method = "isTargetValidToPick", at = @At("RETURN"))
    private void tougher$forgetGolem(
            PathfinderMob mob,
            Level level,
            BlockEntity entity,
            Set<?> visited,
            Set<?> unreachable,
            AABB area,
            CallbackInfoReturnable<TransportItemsBetweenContainers.TransportItemTarget> cir) {
        CopperGolems.endSearch();
    }

    @Inject(method = "hasValidTarget", at = @At("HEAD"), cancellable = true)
    private void tougher$dropForbidden(Level level, PathfinderMob mob, CallbackInfoReturnable<Boolean> cir) {
        if (target != null && CopperGolems.refusesWork(mob, level, target.pos())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "pickUpItems", at = @At("HEAD"), cancellable = true)
    private void tougher$blockPickup(PathfinderMob mob, Container container, CallbackInfo ci) {
        if (CopperGolems.refusesContainer(mob, container)) {
            ci.cancel();
        }
    }

    @Inject(method = "putDownItem", at = @At("HEAD"), cancellable = true)
    private void tougher$blockPutDown(PathfinderMob mob, Container container, CallbackInfo ci) {
        if (CopperGolems.refusesContainer(mob, container)) {
            ci.cancel();
        }
    }
}

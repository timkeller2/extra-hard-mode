package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.NetherPortals;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityPortalMixin {
    /**
     * Runs after vanilla has already stored the short portal cooldown, and before the move.
     * Cancelling here leaves that cooldown, so standing in the frame does not try again every tick.
     */
    @Inject(method = "teleportToPortalDestination", at = @At("HEAD"), cancellable = true)
    private void tougher$netherPortalFuel(ServerLevel level, TeleportTransition transition, CallbackInfo ci) {
        if (NetherPortals.blockTeleport((Entity) (Object) this, level, transition)) {
            ci.cancel();
        }
    }
}

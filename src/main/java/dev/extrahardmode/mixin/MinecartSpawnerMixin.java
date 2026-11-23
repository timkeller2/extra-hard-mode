package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.AntiGrinder;
import net.minecraft.world.entity.vehicle.minecart.MinecartSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A minecart spawner stays one spawner when the cart moves. */
@Mixin(MinecartSpawner.class)
public abstract class MinecartSpawnerMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void tougher$beginMinecart(CallbackInfo ci) {
        AntiGrinder.beginMinecartSpawner(((MinecartSpawner) (Object) this).getUUID());
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void tougher$endMinecart(CallbackInfo ci) {
        AntiGrinder.endMinecartSpawner();
    }
}

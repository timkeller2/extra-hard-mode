package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.AntiGrinder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ties a mob to the spawner tick that is adding it, including custom-rule spawners. */
@Mixin(ServerLevel.class)
public abstract class SpawnerAddMixin {
    @Inject(method = "tryAddFreshEntityWithPassengers", at = @At("HEAD"))
    private void tougher$noteSpawnerAdd(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        AntiGrinder.noteAddedDuringSpawnerTick(entity);
    }

    @Inject(method = "tryAddFreshEntityWithPassengers", at = @At("RETURN"))
    private void tougher$noteSpawnerPassengers(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        AntiGrinder.noteAddedDuringSpawnerTick(entity);
    }
}

package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.AntiGrinder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BaseSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Remember which spawner is spawning so each player can loot it eight times. */
@Mixin(BaseSpawner.class)
public abstract class BaseSpawnerMixin {
    @Inject(method = "serverTick", at = @At("HEAD"))
    private void tougher$beginSpawner(ServerLevel level, BlockPos pos, CallbackInfo ci) {
        AntiGrinder.beginSpawnerTick(level, pos);
    }

    @Inject(method = "serverTick", at = @At("RETURN"))
    private void tougher$endSpawner(ServerLevel level, BlockPos pos, CallbackInfo ci) {
        AntiGrinder.endSpawnerTick();
    }
}

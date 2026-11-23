package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.AntiGrinder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cave spiders replace finalizeSpawn and do not call through to Mob, so the
 * spawn-reason stamp never sees them. The mob that actually entered the level is marked here.
 */
@Mixin(TrialSpawner.class)
public abstract class TrialSpawnerMixin {
    @Inject(method = "spawnMob", at = @At("RETURN"))
    private void tougher$markTrialMob(ServerLevel level, BlockPos pos, CallbackInfoReturnable<Optional<UUID>> cir) {
        Optional<UUID> id = cir.getReturnValue();
        if (level == null || id == null || id.isEmpty()) {
            return;
        }
        AntiGrinder.markTrialGroup(level.getEntity(id.get()));
    }
}

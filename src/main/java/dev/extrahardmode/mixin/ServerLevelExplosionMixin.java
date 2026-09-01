package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Explosions;
import dev.extrahardmode.feature.FeatureBus;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelExplosionMixin {
    @Inject(method = "explode", at = @At("HEAD"), cancellable = true)
    private void extrahardmode$replace(
            Entity source,
            DamageSource damageSource,
            ExplosionDamageCalculator damageCalculator,
            double x,
            double y,
            double z,
            float radius,
            boolean fire,
            Level.ExplosionInteraction explosionInteraction,
            ParticleOptions smallParticle,
            ParticleOptions largeParticle,
            WeightedList<ExplosionParticleInfo> blockParticles,
            Holder<SoundEvent> sound,
            CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;
        if (!FeatureBus.guard((Level) level, Explosions.ID)) {
            return;
        }
        Explosions.interceptLevelExplode(level, source, x, y, z, ci);
    }
}

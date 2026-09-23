package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Explosions;
import dev.extrahardmode.feature.FeatureBus;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin
        implements Explosions.RadiusMutator, Explosions.FireMutator, Explosions.BlockInteractionMutator {
    @Shadow
    @Final
    @Mutable
    private float radius;

    @Shadow
    @Final
    @Mutable
    private boolean fire;

    @Shadow
    @Final
    @Mutable
    private Explosion.BlockInteraction blockInteraction;

    @Shadow
    @Final
    private ServerLevel level;

    @Shadow
    @Final
    private Entity source;

    @Override
    public void tougher$setRadius(float radius) {
        this.radius = radius;
    }

    @Override
    public void tougher$setFire(boolean fire) {
        this.fire = fire;
    }

    @Override
    public void tougher$setBlockInteraction(Explosion.BlockInteraction interaction) {
        this.blockInteraction = interaction;
    }

    @Inject(method = "explode", at = @At("HEAD"))
    private void tougher$event(CallbackInfoReturnable<Integer> cir) {
        if (!FeatureBus.guard((Level) this.level, Explosions.ID)) {
            return;
        }
        Explosions.applyEvent(this.level, (Explosion) (Object) this, this.source, this.fire);
    }

    @Inject(method = "interactWithBlocks", at = @At("HEAD"))
    private void tougher$beforeBlocks(List<BlockPos> positions, CallbackInfo ci) {
        if (!FeatureBus.guard((Level) this.level, Explosions.ID)) {
            return;
        }
        Explosions.beforeBlocks(this.level, (Explosion) (Object) this, positions);
    }

    @Inject(method = "interactWithBlocks", at = @At("RETURN"))
    private void tougher$afterBlocks(List<BlockPos> positions, CallbackInfo ci) {
        if (!FeatureBus.guard((Level) this.level, Explosions.ID)) {
            return;
        }
        Explosions.afterBlocks(this.level, (Explosion) (Object) this);
    }
}

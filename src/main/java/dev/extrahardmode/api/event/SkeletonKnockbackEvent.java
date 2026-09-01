package dev.extrahardmode.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.phys.Vec3;

/**
 * Fired when a Skeleton or Bogged firework arrow knocks a living target back.
 * Cancel to skip the extra knockback. Velocity may be replaced.
 */
public final class SkeletonKnockbackEvent {
    public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, listeners -> event -> {
        for (Listener listener : listeners) {
            listener.onSkeletonKnockback(event);
            if (event.isCanceled()) {
                return;
            }
        }
    });

    private final LivingEntity target;
    private final AbstractSkeleton skeleton;
    private final int fireworkPercent;
    private Vec3 velocity;
    private boolean canceled;

    public SkeletonKnockbackEvent(
            LivingEntity target, AbstractSkeleton skeleton, Vec3 velocity, int fireworkPercent) {
        this.target = target;
        this.skeleton = skeleton;
        this.velocity = velocity;
        this.fireworkPercent = fireworkPercent;
    }

    public LivingEntity target() {
        return target;
    }

    public AbstractSkeleton skeleton() {
        return skeleton;
    }

    public int fireworkPercent() {
        return fireworkPercent;
    }

    public Vec3 velocity() {
        return velocity;
    }

    public void setVelocity(Vec3 velocity) {
        this.velocity = velocity;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void cancel() {
        this.canceled = true;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    @FunctionalInterface
    public interface Listener {
        void onSkeletonKnockback(SkeletonKnockbackEvent event);
    }
}

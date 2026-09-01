package dev.extrahardmode.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

/**
 * Fired when a Skeleton or Bogged would deflect an incoming arrow.
 * Cancel to let the skeleton take normal arrow damage.
 */
public final class SkeletonDeflectEvent {
    public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, listeners -> event -> {
        for (Listener listener : listeners) {
            listener.onSkeletonDeflect(event);
            if (event.isCanceled()) {
                return;
            }
        }
    });

    private final ServerPlayer shooter;
    private final AbstractSkeleton skeleton;
    private final AbstractArrow arrow;
    private final int deflectPercent;
    private boolean canceled;

    public SkeletonDeflectEvent(
            ServerPlayer shooter, AbstractSkeleton skeleton, AbstractArrow arrow, int deflectPercent) {
        this.shooter = shooter;
        this.skeleton = skeleton;
        this.arrow = arrow;
        this.deflectPercent = deflectPercent;
    }

    public ServerPlayer shooter() {
        return shooter;
    }

    public AbstractSkeleton skeleton() {
        return skeleton;
    }

    public AbstractArrow arrow() {
        return arrow;
    }

    public int deflectPercent() {
        return deflectPercent;
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
        void onSkeletonDeflect(SkeletonDeflectEvent event);
    }
}

package dev.extrahardmode.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.zombie.Zombie;

/** Fired after an Extra Hard Mode zombie reanimates. Cancel to discard the new zombie. */
public final class ZombieRespawnEvent {
    public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, listeners -> event -> {
        for (Listener listener : listeners) {
            listener.onZombieRespawn(event);
            if (event.isCanceled()) {
                return;
            }
        }
    });

    private final ServerPlayer target;
    private final Zombie zombie;
    private boolean canceled;

    public ZombieRespawnEvent(ServerPlayer target, Zombie zombie) {
        this.target = target;
        this.zombie = zombie;
    }

    public ServerPlayer target() {
        return target;
    }

    public Zombie zombie() {
        return zombie;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void cancel() {
        this.canceled = true;
    }

    @FunctionalInterface
    public interface Listener {
        void onZombieRespawn(ZombieRespawnEvent event);
    }
}

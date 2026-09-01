package dev.extrahardmode.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;

/** Fired before a dying creeper drops primed TNT. Cancel to skip the drop. */
public final class CreeperDropTntEvent {
    public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, listeners -> event -> {
        for (Listener listener : listeners) {
            listener.onCreeperDropTnt(event);
            if (event.isCanceled()) {
                return;
            }
        }
    });

    private final ServerPlayer killer;
    private final Creeper creeper;
    private final Vec3 location;
    private boolean canceled;

    public CreeperDropTntEvent(ServerPlayer killer, Creeper creeper, Vec3 location) {
        this.killer = killer;
        this.creeper = creeper;
        this.location = location;
    }

    public ServerPlayer killer() {
        return killer;
    }

    public Creeper creeper() {
        return creeper;
    }

    public Vec3 location() {
        return location;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void cancel() {
        this.canceled = true;
    }

    @FunctionalInterface
    public interface Listener {
        void onCreeperDropTnt(CreeperDropTntEvent event);
    }
}

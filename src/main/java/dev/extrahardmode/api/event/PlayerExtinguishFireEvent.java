package dev.extrahardmode.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fired when a player punches fire and Extra Hard Mode would ignite them.
 * Cancel to skip ignition.
 */
public final class PlayerExtinguishFireEvent {
    public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class, listeners -> event -> {
        for (Callback listener : listeners) {
            listener.onExtinguishFire(event);
        }
    });

    private final ServerPlayer player;
    private int burnTicks;
    private boolean cancelled;

    public PlayerExtinguishFireEvent(ServerPlayer player, int burnTicks) {
        this.player = player;
        this.burnTicks = burnTicks;
    }

    public ServerPlayer player() {
        return player;
    }

    public int burnTicks() {
        return burnTicks;
    }

    public void setBurnTicks(int burnTicks) {
        this.burnTicks = burnTicks;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @FunctionalInterface
    public interface Callback {
        void onExtinguishFire(PlayerExtinguishFireEvent event);
    }
}

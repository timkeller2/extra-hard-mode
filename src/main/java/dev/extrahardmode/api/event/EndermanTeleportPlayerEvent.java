package dev.extrahardmode.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.phys.Vec3;

/**
 * Fired when an angry enderman would teleport the player onto (or beside) it.
 * Cancel to leave the player in place.
 */
public final class EndermanTeleportPlayerEvent {
    public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, listeners -> event -> {
        for (Listener listener : listeners) {
            listener.onEndermanTeleportPlayer(event);
            if (event.isCanceled()) {
                return;
            }
        }
    });

    private final ServerPlayer player;
    private final Enderman enderman;
    private Vec3 teleportTo;
    private boolean canceled;

    public EndermanTeleportPlayerEvent(ServerPlayer player, Enderman enderman, Vec3 teleportTo) {
        this.player = player;
        this.enderman = enderman;
        this.teleportTo = teleportTo;
    }

    public ServerPlayer player() {
        return player;
    }

    public Enderman enderman() {
        return enderman;
    }

    public Vec3 teleportTo() {
        return teleportTo;
    }

    public void setTeleportTo(Vec3 teleportTo) {
        this.teleportTo = teleportTo;
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
        void onEndermanTeleportPlayer(EndermanTeleportPlayerEvent event);
    }
}

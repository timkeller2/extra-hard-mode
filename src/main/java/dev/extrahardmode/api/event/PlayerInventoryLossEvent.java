package dev.extrahardmode.api.event;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Fired when Extra Hard Mode is about to forfeit item stacks on player death.
 *
 * <p>Forfeit still runs when {@code keepInventory} is true — suicide-reset is the
 * thing this punishes. Remaining stacks still drop via vanilla when
 * {@code keepInventory} is false.
 *
 * <p>Cancel to skip the forfeit. Listeners may mutate {@link #stacksToRemove()}.
 */
public final class PlayerInventoryLossEvent {
    public static final Event<Callback> EVENT = EventFactory.createArrayBacked(Callback.class, listeners -> event -> {
        for (Callback listener : listeners) {
            listener.onInventoryLoss(event);
        }
    });

    private final ServerPlayer player;
    private final List<ItemStack> drops;
    private final List<ItemStack> stacksToRemove;
    private boolean cancelled;

    public PlayerInventoryLossEvent(ServerPlayer player, List<ItemStack> drops, List<ItemStack> stacksToRemove) {
        this.player = player;
        this.drops = drops;
        this.stacksToRemove = new ArrayList<>(stacksToRemove);
    }

    public ServerPlayer player() {
        return player;
    }

    public List<ItemStack> drops() {
        return drops;
    }

    public List<ItemStack> stacksToRemove() {
        return stacksToRemove;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @FunctionalInterface
    public interface Callback {
        void onInventoryLoss(PlayerInventoryLossEvent event);
    }
}

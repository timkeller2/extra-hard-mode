package dev.extrahardmode.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fired before Tougher applies hardened-stone harvest deny or extra durability.
 * Cancel to allow silk-touch drills / other mods to skip EHM handling.
 */
public final class HardenedStoneMineEvent {
    public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, listeners -> event -> {
        for (Listener listener : listeners) {
            listener.onHardenedStoneMine(event);
            if (event.isCanceled()) {
                return;
            }
        }
    });

    private final ServerPlayer player;
    private final ItemStack tool;
    private final BlockState state;
    private int budget;
    private boolean canceled;

    public HardenedStoneMineEvent(ServerPlayer player, ItemStack tool, BlockState state, int budget) {
        this.player = player;
        this.tool = tool;
        this.state = state;
        this.budget = budget;
    }

    public ServerPlayer player() {
        return player;
    }

    public ItemStack tool() {
        return tool;
    }

    public BlockState state() {
        return state;
    }

    public int budget() {
        return budget;
    }

    public void setBudget(int budget) {
        this.budget = budget;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void cancel() {
        this.canceled = true;
    }

    @FunctionalInterface
    public interface Listener {
        void onHardenedStoneMine(HardenedStoneMineEvent event);
    }
}

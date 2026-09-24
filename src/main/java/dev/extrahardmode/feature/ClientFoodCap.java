package dev.extrahardmode.feature;

import net.minecraft.world.entity.player.Player;

/** Client-side hunger cap, set when the client mod starts. Defaults to 20. */
public final class ClientFoodCap {
    @FunctionalInterface
    public interface Lookup {
        int cap(Player player);
    }

    public static Lookup lookup = player -> WellFedRules.BASE_FOOD;

    private ClientFoodCap() {}
}

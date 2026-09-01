package dev.extrahardmode.player;

/**
 * Death item-stack forfeit math. Kept free of Minecraft types for JUnit.
 *
 * <p>RootNode default is 10% of stacks, with a minimum of 1 when the percent is
 * greater than 0 so suicide-reset still costs something on a small inventory.
 */
public final class DeathForfeit {
    private DeathForfeit() {}

    public static int stacksToRemove(int stackCount, int percent) {
        if (stackCount <= 0 || percent <= 0) {
            return 0;
        }
        int remove = (int) (stackCount * (percent / 100.0f));
        if (remove == 0) {
            return 1;
        }
        return Math.min(remove, stackCount);
    }
}

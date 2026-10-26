package dev.extrahardmode.feature;

/**
 * The vanilla novice fletcher buy is 32 sticks for 1 emerald. Tougher asks for 32 flint.
 * Uses, experience, and the price multiplier stay on the offer. The apprentice 26-flint buy is unchanged.
 */
public final class FletcherTradeRules {
    public static final int COUNT = 32;

    private FletcherTradeRules() {}

    public static boolean isNoviceStickBuy(
            boolean costIsStick,
            int costCount,
            boolean hasSecondCost,
            boolean resultIsEmerald,
            int resultCount) {
        return costIsStick
                && costCount == COUNT
                && !hasSecondCost
                && resultIsEmerald
                && resultCount == 1;
    }
}

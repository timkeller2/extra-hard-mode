package dev.extrahardmode.feature;

/**
 * Two right-clicks of the same held item cast a mana ability. Minecraft-free.
 * Clicks in the same game tick are one gesture, because a block click and an item click
 * can both arrive from that one press.
 */
public final class AbilityClickRules {
    /** Half a second. The second click after this starts a new pair. */
    public static final int DOUBLE_CLICK_TICKS = 10;

    public record Press(long tick, String itemId) {}

    private AbilityClickRules() {}

    /**
     * True when {@code tick} is a later tick, within {@link #DOUBLE_CLICK_TICKS}, and the held
     * item matches. A same-tick press is the same gesture and does not complete the pair.
     */
    public static boolean completes(Press previous, long tick, String itemId) {
        if (previous == null || itemId == null || itemId.isEmpty()) {
            return false;
        }
        if (tick <= previous.tick()) {
            return false;
        }
        if (tick - previous.tick() > DOUBLE_CLICK_TICKS) {
            return false;
        }
        return itemId.equals(previous.itemId());
    }
}

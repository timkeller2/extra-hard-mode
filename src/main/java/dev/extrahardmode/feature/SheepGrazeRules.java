package dev.extrahardmode.feature;

/** How long sheep wait before eating grass to regrow wool. Minecraft-free for JUnit. */
public final class SheepGrazeRules {
    /** Six times the vanilla wait. 1 is vanilla. */
    public static final int DEFAULT_MULTIPLIER = 6;

    private SheepGrazeRules() {}

    /**
     * Bound for the grass-eat roll. Vanilla passes the baby or adult delay already
     * adjusted for the goal tick. A larger bound makes a successful roll rarer.
     */
    public static int eatRollBound(int vanillaBound, int multiplier) {
        return AntiFarming.scaleTicks(vanillaBound, multiplier);
    }
}

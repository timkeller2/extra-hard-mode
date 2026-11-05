package dev.extrahardmode.feature;

/** Crowding scope. Minecraft-free for JUnit. */
public final class AnimalCrowdRules {
    /** One minute between full crowding passes. A mob that just loaded is still checked at once. */
    public static final int CHECK_INTERVAL_TICKS = 1200;

    private AnimalCrowdRules() {}

    /** Livestock join the crowd only when the villagers-only option is off. */
    public static boolean animalsCount(boolean villagersOnly) {
        return !villagersOnly;
    }
}

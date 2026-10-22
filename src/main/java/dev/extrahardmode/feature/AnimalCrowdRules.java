package dev.extrahardmode.feature;

/** Crowding scope. Minecraft-free for JUnit. */
public final class AnimalCrowdRules {
    private AnimalCrowdRules() {}

    /** Livestock join the crowd only when the villagers-only option is off. */
    public static boolean animalsCount(boolean villagersOnly) {
        return !villagersOnly;
    }
}

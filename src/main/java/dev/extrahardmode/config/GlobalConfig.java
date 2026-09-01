package dev.extrahardmode.config;

import dev.extrahardmode.module.PhysicsBudget;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

public record GlobalConfig(
        boolean enabledByDefault,
        boolean debug,
        int budgetConversionsPerTick,
        int maxLiveEhmFallingEntities,
        int maxFloodFillPerConversion,
        int maxQueueDepth) {
    public static GlobalConfig defaults() {
        return new GlobalConfig(
                defaultEnabledByDefault(),
                false,
                PhysicsBudget.CONVERSIONS_PER_TICK,
                PhysicsBudget.MAX_LIVE_EHM_FALLING,
                PhysicsBudget.MAX_FLOOD_FILL,
                PhysicsBudget.MAX_QUEUE_DEPTH);
    }

    public GlobalConfig withDebug(boolean debug) {
        return new GlobalConfig(
                enabledByDefault,
                debug,
                budgetConversionsPerTick,
                maxLiveEhmFallingEntities,
                maxFloodFillPerConversion,
                maxQueueDepth);
public record GlobalConfig(boolean enabledByDefault, boolean debug, int tutorialMaxShows) {
    public static final int DEFAULT_TUTORIAL_MAX_SHOWS = 3;
        return new GlobalConfig(defaultEnabledByDefault(), false, DEFAULT_TUTORIAL_MAX_SHOWS);
        return new GlobalConfig(enabledByDefault, debug, tutorialMaxShows);
    public GlobalConfig withEnabledByDefault(boolean enabledByDefault) {
    public GlobalConfig withTutorialMaxShows(int tutorialMaxShows) {
        return new GlobalConfig(enabledByDefault, debug, Math.max(0, tutorialMaxShows));
    }

    /**
     * Missing {@code enabledByDefault} is true on the client (integrated) and false on dedicated.
     */
    public static boolean defaultEnabledByDefault() {
        return FabricLoader.getInstance().getEnvironmentType() != EnvType.SERVER;
    }
}

package dev.extrahardmode.config;

import dev.extrahardmode.module.PhysicsBudget;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

public record GlobalConfig(
        boolean enabledByDefault,
        boolean debug,
        boolean f3Enabled,
        int tutorialMaxShows,
        int budgetConversionsPerTick,
        int maxLiveEhmFallingEntities,
        int maxFloodFillPerConversion,
        int maxQueueDepth) {
    public static final int DEFAULT_TUTORIAL_MAX_SHOWS = 3;

    public static GlobalConfig defaults() {
        return new GlobalConfig(
                defaultEnabledByDefault(),
                false,
                false,
                DEFAULT_TUTORIAL_MAX_SHOWS,
                PhysicsBudget.CONVERSIONS_PER_TICK,
                PhysicsBudget.MAX_LIVE_EHM_FALLING,
                PhysicsBudget.MAX_FLOOD_FILL,
                PhysicsBudget.MAX_QUEUE_DEPTH);
    }

    public GlobalConfig withDebug(boolean debug) {
        return new GlobalConfig(
                enabledByDefault,
                debug,
                f3Enabled,
                tutorialMaxShows,
                budgetConversionsPerTick,
                maxLiveEhmFallingEntities,
                maxFloodFillPerConversion,
                maxQueueDepth);
    }

    public GlobalConfig withEnabledByDefault(boolean enabledByDefault) {
        return new GlobalConfig(
                enabledByDefault,
                debug,
                f3Enabled,
                tutorialMaxShows,
                budgetConversionsPerTick,
                maxLiveEhmFallingEntities,
                maxFloodFillPerConversion,
                maxQueueDepth);
    }

    public GlobalConfig withF3Enabled(boolean f3Enabled) {
        return new GlobalConfig(
                enabledByDefault,
                debug,
                f3Enabled,
                tutorialMaxShows,
                budgetConversionsPerTick,
                maxLiveEhmFallingEntities,
                maxFloodFillPerConversion,
                maxQueueDepth);
    }

    public GlobalConfig withTutorialMaxShows(int tutorialMaxShows) {
        return new GlobalConfig(
                enabledByDefault,
                debug,
                f3Enabled,
                Math.max(0, tutorialMaxShows),
                budgetConversionsPerTick,
                maxLiveEhmFallingEntities,
                maxFloodFillPerConversion,
                maxQueueDepth);
    }

    /**
     * Missing {@code enabledByDefault} is true on the client (integrated) and false on dedicated.
     */
    public static boolean defaultEnabledByDefault() {
        return FabricLoader.getInstance().getEnvironmentType() != EnvType.SERVER;
    }
}

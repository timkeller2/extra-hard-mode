package dev.extrahardmode.config;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

public record GlobalConfig(boolean enabledByDefault, boolean debug, int tutorialMaxShows) {
    public static final int DEFAULT_TUTORIAL_MAX_SHOWS = 3;

    public static GlobalConfig defaults() {
        return new GlobalConfig(defaultEnabledByDefault(), false, DEFAULT_TUTORIAL_MAX_SHOWS);
    }

    public GlobalConfig withDebug(boolean debug) {
        return new GlobalConfig(enabledByDefault, debug, tutorialMaxShows);
    }

    public GlobalConfig withEnabledByDefault(boolean enabledByDefault) {
        return new GlobalConfig(enabledByDefault, debug, tutorialMaxShows);
    }

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

package dev.extrahardmode.config;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

public record GlobalConfig(boolean enabledByDefault, boolean debug) {
    public static GlobalConfig defaults() {
        return new GlobalConfig(defaultEnabledByDefault(), false);
    }

    public GlobalConfig withDebug(boolean debug) {
        return new GlobalConfig(enabledByDefault, debug);
    }

    /**
     * Missing {@code enabledByDefault} is true on the client (integrated) and false on dedicated.
     */
    public static boolean defaultEnabledByDefault() {
        return FabricLoader.getInstance().getEnvironmentType() != EnvType.SERVER;
    }
}

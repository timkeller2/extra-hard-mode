package dev.extrahardmode.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import dev.extrahardmode.ExtraHardModeMod;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class ConfigManager {
    public static final String FILE_NAME = "extrahardmode.toml";

    private static GlobalConfig global = GlobalConfig.defaults();

    private ConfigManager() {}

    public static GlobalConfig global() {
        return global;
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        boolean defaultEnabled = GlobalConfig.defaultEnabledByDefault();
        try {
            Files.createDirectories(path.getParent());
            try (CommentedFileConfig config = CommentedFileConfig.builder(path)
                    .sync()
                    .preserveInsertionOrder()
                    .build()) {
                if (Files.exists(path)) {
                    config.load();
                }
                if (!config.contains("enabledByDefault")) {
                    config.setComment(
                            "enabledByDefault",
                            "Copied into a world's gamerule on first load. Missing key defaults to true in singleplayer and false on dedicated servers.");
                    config.set("enabledByDefault", defaultEnabled);
                }
                if (!config.contains("debug")) {
                    config.setComment("debug", "Extra debug logging.");
                    config.set("debug", false);
                }
                config.save();
                global = new GlobalConfig(
                        config.getOrElse("enabledByDefault", defaultEnabled),
                        config.getOrElse("debug", false));
            }
        } catch (Exception e) {
            ExtraHardModeMod.LOGGER.error("Failed to load {}; using defaults", FILE_NAME, e);
            global = new GlobalConfig(defaultEnabled, false);
        }
    }
}

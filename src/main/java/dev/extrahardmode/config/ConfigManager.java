package dev.extrahardmode.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.module.PhysicsBudget;
import dev.extrahardmode.network.EhmNetworking;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public final class ConfigManager {
    public static final String FILE_NAME = "extrahardmode.toml";

    private static GlobalConfig global = GlobalConfig.defaults();
    private static final Map<Identifier, WorldConfig> worlds = new ConcurrentHashMap<>();

    private ConfigManager() {}

    public static GlobalConfig global() {
        return global;
    }

    public static WorldConfig world(ServerLevel level) {
        return worlds.computeIfAbsent(level.dimension().identifier(), id -> WorldConfig.loadOrCreate(level));
    }

    public static void loadWorld(ServerLevel level) {
        worlds.put(level.dimension().identifier(), WorldConfig.loadOrCreate(level));
    }

    public static boolean save(ServerLevel level) {
        return world(level).save(level.getServer());
    }

    public static void reload(MinecraftServer server) {
        load();
        worlds.clear();
        for (ServerLevel level : server.getAllLevels()) {
            loadWorld(level);
        }
        EhmNetworking.syncAll(server);
    }

    public static void clearWorldCache() {
        worlds.clear();
    }

    public static void setDebug(boolean debug) {
        global = global.withDebug(debug);
        Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        try (CommentedFileConfig config = CommentedFileConfig.builder(path, TomlFormat.instance())
                .sync()
                .preserveInsertionOrder()
                .build()) {
            if (Files.exists(path)) {
                config.load();
            }
            config.set("debug", debug);
            config.save();
        } catch (Exception e) {
            ExtraHardModeMod.LOGGER.error("Failed to persist debug flag", e);
        }
    }

    public static void load() {
        Path dir = FabricLoader.getInstance().getConfigDir();
        Path bukkit = dir.resolve("config.yml");
        if (Files.exists(bukkit)) {
            ExtraHardModeMod.LOGGER.warn(
                    "Found config.yml next to Extra Hard Mode — old Bukkit configs are unsupported, see docs");
        }
        Path path = dir.resolve(FILE_NAME);
        boolean defaultEnabled = GlobalConfig.defaultEnabledByDefault();
        try {
            Files.createDirectories(path.getParent());
            try (CommentedFileConfig config = CommentedFileConfig.builder(path, TomlFormat.instance())
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
                writeIntIfMissing(
                        config,
                        "performance.budgetConversionsPerTick",
                        "Max PhysicsQueue conversions per tick per world. Overflow remaining wait in the queue.",
                        PhysicsBudget.CONVERSIONS_PER_TICK);
                writeIntIfMissing(
                        config,
                        "performance.maxLiveEhmFallingEntities",
                        "Live EHM FallingBlockEntity cap. Overflow uses instant setBlock (no entity, no damage).",
                        PhysicsBudget.MAX_LIVE_EHM_FALLING);
                writeIntIfMissing(
                        config,
                        "performance.maxFloodFillPerConversion",
                        "Max extra-falling neighbors enqueued from one conversion/land.",
                        PhysicsBudget.MAX_FLOOD_FILL);
                writeIntIfMissing(
                        config,
                        "performance.maxQueueDepth",
                        "Drop oldest physics request past this depth; increments ehm_physics_dropped.",
                        PhysicsBudget.MAX_QUEUE_DEPTH);
                config.save();
                global = new GlobalConfig(
                        config.getOrElse("enabledByDefault", defaultEnabled),
                        config.getOrElse("debug", false),
                        config.getIntOrElse("performance.budgetConversionsPerTick", PhysicsBudget.CONVERSIONS_PER_TICK),
                        config.getIntOrElse(
                                "performance.maxLiveEhmFallingEntities", PhysicsBudget.MAX_LIVE_EHM_FALLING),
                        config.getIntOrElse("performance.maxFloodFillPerConversion", PhysicsBudget.MAX_FLOOD_FILL),
                        config.getIntOrElse("performance.maxQueueDepth", PhysicsBudget.MAX_QUEUE_DEPTH));
            }
        } catch (Exception e) {
            ExtraHardModeMod.LOGGER.error("Failed to load {}; using defaults", FILE_NAME, e);
            global = GlobalConfig.defaults();
        }
    }

    private static void writeIntIfMissing(CommentedFileConfig config, String path, String comment, int value) {
        if (!config.contains(path)) {
            config.setComment(path, comment);
            config.set(path, value);
        }
    }
}

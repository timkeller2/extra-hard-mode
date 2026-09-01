package dev.extrahardmode.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import dev.extrahardmode.ExtraHardModeMod;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

/** Per-dimension TOML, stored under the overworld save and keyed by {@code dimension().identifier()}. */
public final class WorldConfig {
    public static final int CONFIG_VERSION = 1;

    private final Identifier dimensionId;
    private final Map<Identifier, Boolean> modules = new LinkedHashMap<>();
    private boolean checkPermission = true;
    private boolean creativeBypasses = true;
    private boolean operatorsBypass = false;
    private boolean limitedBuilding = true;
    private boolean torchSoftDeny = true;
    private int torchNoPlacementUnderY = 0;
    private boolean torchYDeny = true;
    private boolean skeletonSnowballEnable = true;
    private int skeletonSnowballPercent = 20;
    private int skeletonSnowballBlindTicks = 100;
    private boolean skeletonFireworkEnable = true;
    private int skeletonFireworkPercent = 30;
    private double skeletonFireworkKnockback = 1.0;
    private boolean skeletonFireballEnable = true;
    private int skeletonFireballPercent = 10;
    private int skeletonFireballFireTicks = 40;
    private boolean skeletonSilverfishEnable = true;
    private int skeletonSilverfishPercent = 20;
    private int skeletonSilverfishMaxAtOnce = 5;
    private int skeletonSilverfishMaxTotal = 15;
    private boolean skeletonKillSilverfishOnDeath = true;
    private int skeletonDeflectArrowsPercent = 100;
    private boolean silverfishCantEnterBlocks = true;
    private boolean silverfishDropCobble = true;
    private boolean silverfishVisibilityParticles = true;
    private boolean enabled = true;
    private boolean enabledPresent;

    public WorldConfig(Identifier dimensionId) {
        this.dimensionId = dimensionId;
    }

    public Identifier dimensionId() {
        return dimensionId;
    }

    public boolean checkPermission() {
        return checkPermission;
    }

    public boolean creativeBypasses() {
        return creativeBypasses;
    }

    public boolean operatorsBypass() {
        return operatorsBypass;
    }

    public boolean limitedBuilding() {
        return limitedBuilding;
    }

    public boolean torchSoftDeny() {
        return torchSoftDeny;
    }

    public int torchNoPlacementUnderY() {
        return torchNoPlacementUnderY;
    }

    public boolean torchYDeny() {
        return torchYDeny;
    }

    public boolean skeletonSnowballEnable() {
        return skeletonSnowballEnable;
    }

    public int skeletonSnowballPercent() {
        return skeletonSnowballPercent;
    }

    public int skeletonSnowballBlindTicks() {
        return skeletonSnowballBlindTicks;
    }

    public boolean skeletonFireworkEnable() {
        return skeletonFireworkEnable;
    }

    public int skeletonFireworkPercent() {
        return skeletonFireworkPercent;
    }

    public double skeletonFireworkKnockback() {
        return skeletonFireworkKnockback;
    }

    public boolean skeletonFireballEnable() {
        return skeletonFireballEnable;
    }

    public int skeletonFireballPercent() {
        return skeletonFireballPercent;
    }

    public int skeletonFireballFireTicks() {
        return skeletonFireballFireTicks;
    }

    public boolean skeletonSilverfishEnable() {
        return skeletonSilverfishEnable;
    }

    public int skeletonSilverfishPercent() {
        return skeletonSilverfishPercent;
    }

    public int skeletonSilverfishMaxAtOnce() {
        return skeletonSilverfishMaxAtOnce;
    }

    public int skeletonSilverfishMaxTotal() {
        return skeletonSilverfishMaxTotal;
    }

    public boolean skeletonKillSilverfishOnDeath() {
        return skeletonKillSilverfishOnDeath;
    }

    public int skeletonDeflectArrowsPercent() {
        return skeletonDeflectArrowsPercent;
    }

    public boolean silverfishCantEnterBlocks() {
        return silverfishCantEnterBlocks;
    }

    public boolean silverfishDropCobble() {
        return silverfishDropCobble;
    }

    public boolean silverfishVisibilityParticles() {
        return silverfishVisibilityParticles;
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean hasEnabledKey() {
        return enabledPresent;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.enabledPresent = true;
    }

    public boolean isModuleEnabled(Identifier moduleId) {
        Boolean value = modules.get(moduleId);
        return value == null || value;
    }

    public void setModuleEnabled(Identifier moduleId, boolean enabled) {
        modules.put(moduleId, enabled);
    }

    public static Path pathFor(MinecraftServer server, Identifier dimensionId) {
        return server.getWorldPath(LevelResource.ROOT)
                .resolve("data")
                .resolve(ExtraHardModeMod.MOD_ID)
                .resolve(dimensionId.getNamespace())
                .resolve(dimensionId.getPath() + ".toml");
    }

    public static WorldConfig loadOrCreate(ServerLevel level) {
        Identifier id = level.dimension().identifier();
        Path path = pathFor(level.getServer(), id);
        WorldConfig config = new WorldConfig(id);
        try {
            Files.createDirectories(path.getParent());
            boolean created = !Files.exists(path);
            try (CommentedFileConfig file = CommentedFileConfig.builder(path, TomlFormat.instance())
                    .sync()
                    .preserveInsertionOrder()
                    .build()) {
                if (Files.exists(path)) {
                    file.load();
                }
                if (!file.contains("configVersion")) {
                    file.setComment(
                            "configVersion",
                            "EHM world config schema. extrahardmode:enabled is a server-global master gamerule; this file's enabled is per-dimension.");
                    file.set("configVersion", CONFIG_VERSION);
                }
                writeDefaultIfMissing(
                        file,
                        "bypassing.checkPermission",
                        "Honor extrahardmode.bypass (and silent) permission nodes.",
                        true);
                writeDefaultIfMissing(
                        file,
                        "bypassing.creativeBypasses",
                        "Creative players skip player-triggered features only, not world physics.",
                        true);
                writeDefaultIfMissing(
                        file, "bypassing.operatorsBypass", "Vanilla operators skip EHM. Default false; ops play EHM.", false);
                writeDefaultIfMissing(
                        file,
                        "worldRules.limitedBlockPlacement",
                        "No jump-pillaring / unsupported sky bridges.",
                        true);
                writeDefaultIfMissing(
                        file,
                        "torches.noPlacement.enable",
                        "Deny depth-limited lights below noPlacementUnderY. Disable with this boolean, not Y=0.",
                        true);
                if (!file.contains("torches.noPlacementUnderY")) {
                    file.setComment("torches.noPlacementUnderY", "original: 30");
                    file.set("torches.noPlacementUnderY", 0);
                }
                writeDefaultIfMissing(file, "torches.noPlacementOnSoft", "No torches on soft surfaces.", true);
                writeDefaultIfMissing(
                        file,
                        "skeletons.snowballEnable",
                        "Bogged share this table; strays/wither skeletons do not.",
                        true);
                writeDefaultIfMissing(file, "skeletons.snowballPercent", "Ordered specials: first success wins.", 20);
                writeDefaultIfMissing(file, "skeletons.snowballBlindTicks", "Blindness duration on tagged arrow hit.", 100);
                writeDefaultIfMissing(file, "skeletons.fireworkEnable", true);
                writeDefaultIfMissing(file, "skeletons.fireworkPercent", 30);
                writeDefaultIfMissing(file, "skeletons.fireworkKnockback", "Multiply arrow velocity on hit.", 1.0);
                writeDefaultIfMissing(file, "skeletons.fireballEnable", true);
                writeDefaultIfMissing(file, "skeletons.fireballPercent", 10);
                writeDefaultIfMissing(file, "skeletons.fireballFireTicks", 40);
                writeDefaultIfMissing(file, "skeletons.silverfishEnable", true);
                writeDefaultIfMissing(file, "skeletons.silverfishPercent", 20);
                writeDefaultIfMissing(file, "skeletons.silverfishMaxAtOnce", 5);
                writeDefaultIfMissing(file, "skeletons.silverfishMaxTotal", 15);
                writeDefaultIfMissing(
                        file, "skeletons.killSilverfishOnSkeletonDeath", "Discard owner-tagged minions on skeleton death.", true);
                writeDefaultIfMissing(file, "skeletons.deflectArrowsPercent", "Arrows pass through Skeleton/Bogged.", 100);
                writeDefaultIfMissing(file, "silverfish.cantEnterBlocks", "Block merge-into-stone.", true);
                writeDefaultIfMissing(file, "silverfish.dropCobble", true);
                writeDefaultIfMissing(
                        file, "silverfish.visibilityParticles", "Portal particles so floor-glitched silverfish stay visible.", true);
                if (!file.contains("modules")) {
                    file.setComment("modules", "Runtime per-module toggles for this dimension. Missing keys default true.");
                }
                file.save();
                config.read(file);
            }
            if (created) {
                ExtraHardModeMod.LOGGER.info("Created per-dimension config {}", path.toAbsolutePath());
            }
        } catch (Exception e) {
            ExtraHardModeMod.LOGGER.error("Failed to load world config {}; using defaults", path, e);
        }
        return config;
    }

    public boolean save(MinecraftServer server) {
        Path path = pathFor(server, dimensionId);
        try {
            Files.createDirectories(path.getParent());
            try (CommentedFileConfig file = CommentedFileConfig.builder(path, TomlFormat.instance())
                    .sync()
                    .preserveInsertionOrder()
                    .build()) {
                if (Files.exists(path)) {
                    file.load();
                }
                file.set("configVersion", CONFIG_VERSION);
                if (!file.contains("enabled")) {
                    file.setComment(
                            "enabled",
                            "Per-dimension opt-out. Default true. The gamerule extrahardmode:enabled is the server-wide master switch.");
                }
                file.set("enabled", enabled);
                file.set("bypassing.checkPermission", checkPermission);
                file.set("bypassing.creativeBypasses", creativeBypasses);
                file.set("bypassing.operatorsBypass", operatorsBypass);
                file.set("worldRules.limitedBlockPlacement", limitedBuilding);
                file.set("torches.noPlacement.enable", torchYDeny);
                file.set("torches.noPlacementUnderY", torchNoPlacementUnderY);
                file.set("torches.noPlacementOnSoft", torchSoftDeny);
                file.set("skeletons.snowballEnable", skeletonSnowballEnable);
                file.set("skeletons.snowballPercent", skeletonSnowballPercent);
                file.set("skeletons.snowballBlindTicks", skeletonSnowballBlindTicks);
                file.set("skeletons.fireworkEnable", skeletonFireworkEnable);
                file.set("skeletons.fireworkPercent", skeletonFireworkPercent);
                file.set("skeletons.fireworkKnockback", skeletonFireworkKnockback);
                file.set("skeletons.fireballEnable", skeletonFireballEnable);
                file.set("skeletons.fireballPercent", skeletonFireballPercent);
                file.set("skeletons.fireballFireTicks", skeletonFireballFireTicks);
                file.set("skeletons.silverfishEnable", skeletonSilverfishEnable);
                file.set("skeletons.silverfishPercent", skeletonSilverfishPercent);
                file.set("skeletons.silverfishMaxAtOnce", skeletonSilverfishMaxAtOnce);
                file.set("skeletons.silverfishMaxTotal", skeletonSilverfishMaxTotal);
                file.set("skeletons.killSilverfishOnSkeletonDeath", skeletonKillSilverfishOnDeath);
                file.set("skeletons.deflectArrowsPercent", skeletonDeflectArrowsPercent);
                file.set("silverfish.cantEnterBlocks", silverfishCantEnterBlocks);
                file.set("silverfish.dropCobble", silverfishDropCobble);
                file.set("silverfish.visibilityParticles", silverfishVisibilityParticles);
                for (Map.Entry<Identifier, Boolean> entry : modules.entrySet()) {
                    file.set(moduleKey(entry.getKey()), entry.getValue());
                }
                file.save();
            }
            return true;
        } catch (Exception e) {
            ExtraHardModeMod.LOGGER.error("Failed to save world config {}", path, e);
            return false;
        }
    }

    private void read(CommentedFileConfig file) {
        enabledPresent = file.contains("enabled");
        enabled = file.getOrElse("enabled", true);
        checkPermission = file.getOrElse("bypassing.checkPermission", true);
        creativeBypasses = file.getOrElse("bypassing.creativeBypasses", true);
        operatorsBypass = file.getOrElse("bypassing.operatorsBypass", false);
        limitedBuilding = file.getOrElse("worldRules.limitedBlockPlacement", true);
        torchYDeny = file.getOrElse("torches.noPlacement.enable", true);
        torchNoPlacementUnderY = file.getOrElse("torches.noPlacementUnderY", 0);
        torchSoftDeny = file.getOrElse("torches.noPlacementOnSoft", true);
        skeletonSnowballEnable = file.getOrElse("skeletons.snowballEnable", true);
        skeletonSnowballPercent = percent(getInt(file, "skeletons.snowballPercent", 20));
        skeletonSnowballBlindTicks = Math.max(0, getInt(file, "skeletons.snowballBlindTicks", 100));
        skeletonFireworkEnable = file.getOrElse("skeletons.fireworkEnable", true);
        skeletonFireworkPercent = percent(getInt(file, "skeletons.fireworkPercent", 30));
        skeletonFireworkKnockback = getDouble(file, "skeletons.fireworkKnockback", 1.0);
        skeletonFireballEnable = file.getOrElse("skeletons.fireballEnable", true);
        skeletonFireballPercent = percent(getInt(file, "skeletons.fireballPercent", 10));
        skeletonFireballFireTicks = Math.max(0, getInt(file, "skeletons.fireballFireTicks", 40));
        skeletonSilverfishEnable = file.getOrElse("skeletons.silverfishEnable", true);
        skeletonSilverfishPercent = percent(getInt(file, "skeletons.silverfishPercent", 20));
        skeletonSilverfishMaxAtOnce = Math.max(0, getInt(file, "skeletons.silverfishMaxAtOnce", 5));
        skeletonSilverfishMaxTotal = Math.max(0, getInt(file, "skeletons.silverfishMaxTotal", 15));
        skeletonKillSilverfishOnDeath = file.getOrElse("skeletons.killSilverfishOnSkeletonDeath", true);
        skeletonDeflectArrowsPercent = percent(getInt(file, "skeletons.deflectArrowsPercent", 100));
        silverfishCantEnterBlocks = file.getOrElse("silverfish.cantEnterBlocks", true);
        silverfishDropCobble = file.getOrElse("silverfish.dropCobble", true);
        silverfishVisibilityParticles = file.getOrElse("silverfish.visibilityParticles", true);
        modules.clear();
        Object raw = file.get("modules");
        if (raw instanceof Config table) {
            readModules("", table);
        }
    }

    private void readModules(String prefix, Config table) {
        for (var entry : table.entrySet()) {
            Object value = entry.getValue();
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            if (value instanceof Boolean enabled) {
                modules.put(parseModuleKey(key), enabled);
            } else if (value instanceof Config nested) {
                readModules(key, nested);
            }
        }
    }

    static String moduleKey(Identifier id) {
        return "modules." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    static Identifier parseModuleKey(String key) {
        int dot = key.indexOf('.');
        if (dot < 0) {
            return ExtraHardModeMod.id(key);
        }
        return Identifier.fromNamespaceAndPath(key.substring(0, dot), key.substring(dot + 1).replace('.', '/'));
    }

    private static void writeDefaultIfMissing(CommentedFileConfig file, String path, String comment, boolean value) {
        if (!file.contains(path)) {
            if (comment != null) {
                file.setComment(path, comment);
            }
            file.set(path, value);
        }
    }

    private static void writeDefaultIfMissing(CommentedFileConfig file, String path, boolean value) {
        writeDefaultIfMissing(file, path, null, value);
    }

    private static void writeDefaultIfMissing(CommentedFileConfig file, String path, int value) {
        writeDefaultIfMissing(file, path, null, value);
    }

    private static void writeDefaultIfMissing(CommentedFileConfig file, String path, String comment, int value) {
        if (!file.contains(path)) {
            if (comment != null) {
                file.setComment(path, comment);
            }
            file.set(path, value);
        }
    }

    private static void writeDefaultIfMissing(CommentedFileConfig file, String path, String comment, double value) {
        if (!file.contains(path)) {
            if (comment != null) {
                file.setComment(path, comment);
            }
            file.set(path, value);
        }
    }

    private static int percent(int value) {
        return Math.clamp(value, 0, 100);
    }

    private static int getInt(CommentedFileConfig file, String path, int fallback) {
        Object raw = file.get(path);
        if (raw instanceof Number number) {
            return number.intValue();
        }
        return fallback;
    }

    private static double getDouble(CommentedFileConfig file, String path, double fallback) {
        Object raw = file.get(path);
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        return fallback;
    }
}

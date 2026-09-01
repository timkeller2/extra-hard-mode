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
    private boolean enabled = true;
    private boolean enabledPresent;
    private boolean weakCrops = true;
    private int lossRate = 25;
    private boolean infertileDeserts = true;
    private boolean snowBreaksCrops = true;
    private boolean cantCraftMelonSeeds = true;
    private boolean noBonemealOnMushrooms = true;
    private boolean noFarmNetherWart = true;
    private boolean sheepWhiteWool = true;
    private boolean squidOceanOnly = true;
    private boolean bucketsDontMoveSources = true;
    private boolean animalXpNerf = true;
    private boolean ironGolemNerf = true;
    private boolean overcrowdEnable = true;
    private int overcrowdThreshold = 10;

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

    public boolean weakCrops() {
        return weakCrops;
    }

    public int lossRate() {
        return lossRate;
    }

    public boolean infertileDeserts() {
        return infertileDeserts;
    }

    public boolean snowBreaksCrops() {
        return snowBreaksCrops;
    }

    public boolean cantCraftMelonSeeds() {
        return cantCraftMelonSeeds;
    }

    public boolean noBonemealOnMushrooms() {
        return noBonemealOnMushrooms;
    }

    public boolean noFarmNetherWart() {
        return noFarmNetherWart;
    }

    public boolean sheepWhiteWool() {
        return sheepWhiteWool;
    }

    public boolean squidOceanOnly() {
        return squidOceanOnly;
    }

    public boolean bucketsDontMoveSources() {
        return bucketsDontMoveSources;
    }

    public boolean animalXpNerf() {
        return animalXpNerf;
    }

    public boolean ironGolemNerf() {
        return ironGolemNerf;
    }

    public boolean overcrowdEnable() {
        return overcrowdEnable;
    }

    public int overcrowdThreshold() {
        return overcrowdThreshold;
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
                writeFarmingDefaults(file);
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
                file.set("farming.weakCrops", weakCrops);
                file.set("farming.lossRate", lossRate);
                file.set("farming.infertileDeserts", infertileDeserts);
                file.set("farming.snowBreaksCrops", snowBreaksCrops);
                file.set("farming.cantCraftMelonSeeds", cantCraftMelonSeeds);
                file.set("farming.noBonemealOnMushrooms", noBonemealOnMushrooms);
                file.set("farming.noFarmNetherWart", noFarmNetherWart);
                file.set("farming.sheepWhiteWool", sheepWhiteWool);
                file.set("farming.squidOceanOnly", squidOceanOnly);
                file.set("farming.bucketsDontMoveSources", bucketsDontMoveSources);
                file.set("farming.animalXpNerf", animalXpNerf);
                file.set("farming.ironGolemNerf", ironGolemNerf);
                file.set("farming.overcrowd.enable", overcrowdEnable);
                file.set("farming.overcrowd.threshold", overcrowdThreshold);
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
        weakCrops = file.getOrElse("farming.weakCrops", true);
        lossRate = file.getOrElse("farming.lossRate", 25);
        infertileDeserts = file.getOrElse("farming.infertileDeserts", true);
        snowBreaksCrops = file.getOrElse("farming.snowBreaksCrops", true);
        cantCraftMelonSeeds = file.getOrElse("farming.cantCraftMelonSeeds", true);
        noBonemealOnMushrooms = file.getOrElse("farming.noBonemealOnMushrooms", true);
        noFarmNetherWart = file.getOrElse("farming.noFarmNetherWart", true);
        sheepWhiteWool = file.getOrElse("farming.sheepWhiteWool", true);
        squidOceanOnly = file.getOrElse("farming.squidOceanOnly", true);
        bucketsDontMoveSources = file.getOrElse("farming.bucketsDontMoveSources", true);
        animalXpNerf = file.getOrElse("farming.animalXpNerf", true);
        ironGolemNerf = file.getOrElse("farming.ironGolemNerf", true);
        overcrowdEnable = file.getOrElse("farming.overcrowd.enable", true);
        overcrowdThreshold = file.getOrElse("farming.overcrowd.threshold", 10);
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

    private static void writeFarmingDefaults(CommentedFileConfig file) {
        writeDefaultIfMissing(
                file,
                "farming.weakCrops",
                "Crops may die at full growth. 25% even when tended; dark always dies; no separate needWater toggle.",
                true);
        writeDefaultIfMissing(file, "farming.lossRate", "Base death percent at full growth (original 25).", 25);
        writeDefaultIfMissing(file, "farming.infertileDeserts", "Desert biomes add +50% crop death; trees/mushrooms will not grow.", true);
        writeDefaultIfMissing(file, "farming.snowBreaksCrops", "Snow-covered crops die on random tick.", true);
        writeDefaultIfMissing(file, "farming.cantCraftMelonSeeds", "Melon and pumpkin seed recipes are uncraftable.", true);
        writeDefaultIfMissing(file, "farming.noBonemealOnMushrooms", "Bone meal does not grow mushrooms.", true);
        writeDefaultIfMissing(
                file,
                "farming.noFarmNetherWart",
                "Cannot place nether wart; breaking always drops exactly 1. Find it in fortresses or piglin drops.",
                true);
        writeDefaultIfMissing(file, "farming.sheepWhiteWool", "Sheep regrow and breed white. Dyeing is one-shot.", true);
        writeDefaultIfMissing(file, "farming.squidOceanOnly", "Natural squid only spawn in #minecraft:is_ocean. Glow squid unchanged.", true);
        writeDefaultIfMissing(
                file,
                "farming.bucketsDontMoveSources",
                "Buckets place flowing water LEVEL=1 (not a source). Does not disable canConvertToSource globally.",
                true);
        writeDefaultIfMissing(file, "farming.animalXpNerf", "Animals drop no experience.", true);
        writeDefaultIfMissing(file, "farming.ironGolemNerf", "Iron golems drop nothing (anti iron farm).", true);
        writeDefaultIfMissing(file, "farming.overcrowd.enable", "Animals over the threshold in a 3x3x3 take damage.", true);
        writeDefaultIfMissing(file, "farming.overcrowd.threshold", "Maximum animals in 3x3x3 before damage. Original 10.", 10);
    }

    private static void writeDefaultIfMissing(CommentedFileConfig file, String path, String comment, boolean value) {
        if (!file.contains(path)) {
            file.setComment(path, comment);
            file.set(path, value);
        }
    }

    private static void writeDefaultIfMissing(CommentedFileConfig file, String path, String comment, int value) {
        if (!file.contains(path)) {
            file.setComment(path, comment);
            file.set(path, value);
        }
    }
}

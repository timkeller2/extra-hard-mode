package dev.extrahardmode.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.feature.HardenedBudget;
import dev.extrahardmode.feature.SoftenMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
    private boolean hardenedEnable = true;
    private boolean blockOreNextToStone = true;
    private boolean blockPistonMove = true;
    private final Map<Identifier, Integer> hardenedBudgets = new LinkedHashMap<>();
    private boolean caveInsEnable = true;
    private boolean caveInsApplyPhysics = true;
    private final Map<Identifier, Identifier> softenMap = new LinkedHashMap<>();
    private boolean fallingEnable = true;
    private boolean fallingBreakTorches = false;
    private int fallingDamage = 2;
    private boolean fallingTurnGrassToDirt = true;
    private boolean fallingCascade = true;
    private boolean fallingDropAsItemWhenBlocked = false;
    private boolean enabled = true;
    private boolean enabledPresent;
    private final ExplosionConfig explosions = new ExplosionConfig();

    public WorldConfig(Identifier dimensionId) {
        this.dimensionId = dimensionId;
        for (Map.Entry<String, String> entry : SoftenMap.parseAll(SoftenMap.DEFAULT_ENTRIES).entrySet()) {
            softenMap.put(Identifier.parse(entry.getKey()), Identifier.parse(entry.getValue()));
        }
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

    public boolean hardenedEnable() {
        return hardenedEnable;
    }

    public boolean blockOreNextToStone() {
        return blockOreNextToStone;
    }

    public boolean blockPistonMove() {
        return blockPistonMove;
    }

    public int hardenedBudget(Identifier itemId) {
        return hardenedBudgets.getOrDefault(itemId, 0);
    }

    public Map<Identifier, Integer> hardenedBudgets() {
        return hardenedBudgets;
    }

    public boolean caveInsEnable() {
        return caveInsEnable;
    }

    public boolean caveInsApplyPhysics() {
        return caveInsApplyPhysics;
    }

    public Identifier softenTo(Identifier from) {
        return softenMap.get(from);
    }

    public boolean fallingEnable() {
        return fallingEnable;
    }

    public boolean fallingBreakTorches() {
        return fallingBreakTorches;
    }

    public int fallingDamage() {
        return fallingDamage;
    }

    public boolean fallingTurnGrassToDirt() {
        return fallingTurnGrassToDirt;
    }

    public boolean fallingCascade() {
        return fallingCascade;
    }

    public boolean fallingDropAsItemWhenBlocked() {
        return fallingDropAsItemWhenBlocked;
    }

    public ExplosionConfig explosions() {
        return explosions;
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
                        "mining.hardened.enable",
                        "Hardened stone/deepslate/tuff. Unlisted tools cannot harvest; listed tools last N breaks.",
                        true);
                writeDefaultIfMissing(file, "mining.hardened.blockOreNextToStone", "Cancel placing cave-in ores next to hardened blocks.", true);
                writeDefaultIfMissing(file, "mining.hardened.blockPistonMove", "Cancel pistons pushing hardened blocks or cave-in ores.", true);
                if (!file.contains("mining.hardened.budgets")) {
                    file.setComment(
                            "mining.hardened.budgets",
                            "Current RootNode: IRON@128 DIAMOND@512 NETHERITE@1024. Copper: Math.round(128 * 190 / 250.0) = 97. Same list for stone, deepslate, tuff.");
                    file.set("mining.hardened.budgets", new ArrayList<>(HardenedBudget.DEFAULT_ENTRIES));
                }
                writeDefaultIfMissing(
                        file,
                        "mining.caveIns.enable",
                        "Mining cave-in ores softens 6 neighbors (stone→cobble, deepslate→cobbled deepslate).",
                        true);
                writeDefaultIfMissing(
                        file,
                        "mining.caveIns.applyPhysics",
                        "Converted neighbors become FallingBlockEntity (budgeted). False only sets the block.",
                        true);
                if (!file.contains("mining.caveIns.softenMap")) {
                    file.setComment(
                            "mining.caveIns.softenMap",
                            "from>to. Copper ore neighbors roll 50%. Ancient debris only softens #hardened neighbors.");
                    file.set("mining.caveIns.softenMap", new ArrayList<>(SoftenMap.DEFAULT_ENTRIES));
                }
                writeDefaultIfMissing(
                        file,
                        "falling.enable",
                        "Extra falling blocks (#extrahardmode:extra_falling) drop when unsupported. v1 hooks: player break, BlockItem place, EHM land cascade. Piston/other non-player support removal is not scanned.",
                        true);
                writeDefaultIfMissing(
                        file,
                        "falling.breakTorches",
                        "Implemented, default off (upstream buggy).",
                        false);
                if (!file.contains("falling.damage")) {
                    file.setComment("falling.damage", "KD-18: restore docs-era falling-block player damage. Gated to extra_falling ∪ EHM_OURS.");
                    file.set("falling.damage", 2);
                }
                writeDefaultIfMissing(file, "falling.turnGrassToDirt", "Grass/mycelium/podzol land as dirt.", true);
                writeDefaultIfMissing(file, "falling.cascade", "Landed EHM falling blocks can make neighbors fall.", true);
                writeDefaultIfMissing(
                        file,
                        "falling.dropAsItemWhenBlocked",
                        "Drop an item when a falling block cannot place.",
                        false);
                config.explosions.writeDefaults(file);
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
                file.set("mining.hardened.enable", hardenedEnable);
                file.set("mining.hardened.blockOreNextToStone", blockOreNextToStone);
                file.set("mining.hardened.blockPistonMove", blockPistonMove);
                file.set("mining.caveIns.enable", caveInsEnable);
                file.set("mining.caveIns.applyPhysics", caveInsApplyPhysics);
                List<String> softenEntries = new ArrayList<>();
                for (Map.Entry<Identifier, Identifier> entry : softenMap.entrySet()) {
                    softenEntries.add(entry.getKey() + ">" + entry.getValue());
                }
                if (softenEntries.isEmpty()) {
                    softenEntries.addAll(SoftenMap.DEFAULT_ENTRIES);
                }
                file.set("mining.caveIns.softenMap", softenEntries);
                file.set("falling.enable", fallingEnable);
                file.set("falling.breakTorches", fallingBreakTorches);
                file.set("falling.damage", fallingDamage);
                file.set("falling.turnGrassToDirt", fallingTurnGrassToDirt);
                file.set("falling.cascade", fallingCascade);
                file.set("falling.dropAsItemWhenBlocked", fallingDropAsItemWhenBlocked);
                explosions.write(file);
                List<String> budgetEntries = new ArrayList<>();
                for (Map.Entry<Identifier, Integer> budget : hardenedBudgets.entrySet()) {
                    budgetEntries.add(budget.getKey().toString() + "@" + budget.getValue());
                }
                if (budgetEntries.isEmpty()) {
                    budgetEntries.addAll(HardenedBudget.DEFAULT_ENTRIES);
                }
                file.set("mining.hardened.budgets", budgetEntries);
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
        hardenedEnable = file.getOrElse("mining.hardened.enable", true);
        blockOreNextToStone = file.getOrElse("mining.hardened.blockOreNextToStone", true);
        blockPistonMove = file.getOrElse("mining.hardened.blockPistonMove", true);
        caveInsEnable = file.getOrElse("mining.caveIns.enable", true);
        caveInsApplyPhysics = file.getOrElse("mining.caveIns.applyPhysics", true);
        softenMap.clear();
        try {
            for (Map.Entry<String, String> entry :
                    SoftenMap.parseAll(readStringList(file, "mining.caveIns.softenMap", SoftenMap.DEFAULT_ENTRIES))
                            .entrySet()) {
                softenMap.put(Identifier.parse(entry.getKey()), Identifier.parse(entry.getValue()));
            }
        } catch (RuntimeException e) {
            ExtraHardModeMod.LOGGER.warn("Invalid mining.caveIns.softenMap; using defaults", e);
            for (Map.Entry<String, String> entry : SoftenMap.parseAll(SoftenMap.DEFAULT_ENTRIES).entrySet()) {
                softenMap.put(Identifier.parse(entry.getKey()), Identifier.parse(entry.getValue()));
            }
        }
        fallingEnable = file.getOrElse("falling.enable", true);
        fallingBreakTorches = file.getOrElse("falling.breakTorches", false);
        fallingDamage = file.getIntOrElse("falling.damage", 2);
        fallingTurnGrassToDirt = file.getOrElse("falling.turnGrassToDirt", true);
        fallingCascade = file.getOrElse("falling.cascade", true);
        fallingDropAsItemWhenBlocked = file.getOrElse("falling.dropAsItemWhenBlocked", false);
        explosions.read(file);
        hardenedBudgets.clear();
        try {
            for (Map.Entry<String, Integer> entry :
                    HardenedBudget.parseAll(readStringList(file, "mining.hardened.budgets", HardenedBudget.DEFAULT_ENTRIES))
                            .entrySet()) {
                hardenedBudgets.put(Identifier.parse(entry.getKey()), entry.getValue());
            }
        } catch (RuntimeException e) {
            ExtraHardModeMod.LOGGER.warn("Invalid mining.hardened.budgets; using defaults", e);
            for (Map.Entry<String, Integer> entry : HardenedBudget.parseAll(HardenedBudget.DEFAULT_ENTRIES).entrySet()) {
                hardenedBudgets.put(Identifier.parse(entry.getKey()), entry.getValue());
            }
        }
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
            file.setComment(path, comment);
            file.set(path, value);
        }
    }

    private static List<String> readStringList(CommentedFileConfig file, String path, List<String> fallback) {
        Object raw = file.get(path);
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            return fallback;
        }
        List<String> values = new ArrayList<>(list.size());
        for (Object value : list) {
            if (value != null) {
                values.add(value.toString());
            }
        }
        return values.isEmpty() ? fallback : values;
    }
}

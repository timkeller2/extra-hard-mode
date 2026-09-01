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
    private boolean torchFizz = true;
    private boolean creeperTntWarning = true;
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

    public boolean torchFizz() {
        return torchFizz;
    }

    public boolean creeperTntWarning() {
        return creeperTntWarning;
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

    public void setCheckPermission(boolean checkPermission) {
        this.checkPermission = checkPermission;
    }

    public void setCreativeBypasses(boolean creativeBypasses) {
        this.creativeBypasses = creativeBypasses;
    }

    public void setOperatorsBypass(boolean operatorsBypass) {
        this.operatorsBypass = operatorsBypass;
    }

    public void setLimitedBuilding(boolean limitedBuilding) {
        this.limitedBuilding = limitedBuilding;
    }

    public void setTorchSoftDeny(boolean torchSoftDeny) {
        this.torchSoftDeny = torchSoftDeny;
    }

    public void setTorchNoPlacementUnderY(int torchNoPlacementUnderY) {
        this.torchNoPlacementUnderY = torchNoPlacementUnderY;
    }

    public void setTorchYDeny(boolean torchYDeny) {
        this.torchYDeny = torchYDeny;
    }

    public void setTorchFizz(boolean torchFizz) {
        this.torchFizz = torchFizz;
    }

    public void setCreeperTntWarning(boolean creeperTntWarning) {
        this.creeperTntWarning = creeperTntWarning;
    }

    public void applyCloth(
            boolean checkPermission,
            boolean creativeBypasses,
            boolean operatorsBypass,
            boolean limitedBuilding,
            boolean torchYDeny,
            boolean torchSoftDeny,
            int torchNoPlacementUnderY,
            boolean torchFizz,
            boolean creeperTntWarning) {
        this.checkPermission = checkPermission;
        this.creativeBypasses = creativeBypasses;
        this.operatorsBypass = operatorsBypass;
        this.limitedBuilding = limitedBuilding;
        this.torchYDeny = torchYDeny;
        this.torchSoftDeny = torchSoftDeny;
        this.torchNoPlacementUnderY = torchNoPlacementUnderY;
        this.torchFizz = torchFizz;
        this.creeperTntWarning = creeperTntWarning;
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
                writeDefaultIfMissing(file, "sounds.torchFizz", "Lava fizz when torch placement is denied.", true);
                writeDefaultIfMissing(
                        file, "sounds.creeperTntWarning", "Ghast warn before a creeper drops primed TNT.", true);
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
                file.set("sounds.torchFizz", torchFizz);
                file.set("sounds.creeperTntWarning", creeperTntWarning);
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
        torchFizz = file.getOrElse("sounds.torchFizz", true);
        creeperTntWarning = file.getOrElse("sounds.creeperTntWarning", true);
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
}

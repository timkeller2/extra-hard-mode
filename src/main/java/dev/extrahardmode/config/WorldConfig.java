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
    private boolean betterTreeFelling = true;
    private boolean torchSoftDeny = true;
    private int torchNoPlacementUnderY = 0;
    private boolean torchYDeny = true;
    private boolean hardenedEnable = true;
    private boolean blockOreNextToStone = true;
    private boolean blockPistonMove = true;
    private final Map<Identifier, Integer> hardenedBudgets = new LinkedHashMap<>();
    private boolean rainBreaksTorches = true;
    private boolean rainExtinguishesCampfires = false;
    private boolean torchFizz = true;
    private int netherrackFirePercent = 20;
    private boolean caveInsEnable = true;
    private boolean caveInsApplyPhysics = true;
    private final Map<Identifier, Identifier> softenMap = new LinkedHashMap<>();
    private boolean fallingEnable = true;
    private boolean fallingBreakTorches = false;
    private int fallingDamage = 2;
    private boolean fallingTurnGrassToDirt = true;
    private boolean fallingCascade = true;
    private boolean fallingDropAsItemWhenBlocked = false;
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
    private boolean endermenTeleportPlayers = true;
    private boolean witchesAdditionalAttacks = true;
    private int witchesBonusSpawnPercent = 5;
    private boolean horseBlockChest = true;
    private int horseBlockChestBelowY = 48;
    private boolean enabled = true;
    private boolean enabledPresent;
    private final PlayerSettings player = new PlayerSettings();
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
    private int killerBunnyPercent = 1;
    private int vindicatorPercent = 20;
    private int caveSpiderPercent = 5;
    private int guardianPercent = 20;
    private int vexPercent = 5;
    private boolean inhibitGrinders = true;
    private boolean moreMonstersEnable = true;
    private int moreMonstersMaxY = 48;
    private int moreMonstersMultiplier = 2;
    private boolean spawnInLightEnable = true;
    private int spawnInLightMaxY = 48;
    private int spawnInLightMaxLight = 10;
    private int spawnInLightPercent = 100;
    private final ExplosionConfig explosions = new ExplosionConfig();
    private final MonsterConfig monsters = new MonsterConfig();
    private boolean blazeNearBedrockEnable = true;
    private int blazeNearBedrockPercent = 50;
    private int blazeNearBedrockMaxY = -56;
    private boolean blazeBlockOverworldDrops = true;
    private int blazeBonusNetherPercent = 20;
    private boolean blazeDropFireOnDamage = true;
    private boolean blazeBonusLoot = true;
    private int blazeNetherSplitPercent = 25;
    private int magmaSpawnWithNetherBlazePercent = 100;
    private boolean magmaGrowIntoBlazesOnDamage = true;
    private boolean pigmenAlwaysAngry = true;
    private int pigmenDamagePercent = 70;
    private boolean pigmenFortressNetherwart = true;
    private int pigmenElsewhereNetherwartPercent = 25;
    private boolean pigmenLightningSpawns = true;
    private int ghastArrowDamagePercent = 20;
    private int ghastExpMultiplier = 10;
    private int ghastDropsMultiplier = 5;

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

    public boolean betterTreeFelling() {
        return betterTreeFelling;
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
    public boolean rainBreaksTorches() {
        return rainBreaksTorches;
    public boolean rainExtinguishesCampfires() {
        return rainExtinguishesCampfires;
    public boolean torchFizz() {
        return torchFizz;
    public int netherrackFirePercent() {
        return netherrackFirePercent;
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
    public boolean skeletonSnowballEnable() {
        return skeletonSnowballEnable;
    public int skeletonSnowballPercent() {
        return skeletonSnowballPercent;
    public int skeletonSnowballBlindTicks() {
        return skeletonSnowballBlindTicks;
    public boolean skeletonFireworkEnable() {
        return skeletonFireworkEnable;
    public int skeletonFireworkPercent() {
        return skeletonFireworkPercent;
    public double skeletonFireworkKnockback() {
        return skeletonFireworkKnockback;
    public boolean skeletonFireballEnable() {
        return skeletonFireballEnable;
    public int skeletonFireballPercent() {
        return skeletonFireballPercent;
    public int skeletonFireballFireTicks() {
        return skeletonFireballFireTicks;
    public boolean skeletonSilverfishEnable() {
        return skeletonSilverfishEnable;
    public int skeletonSilverfishPercent() {
        return skeletonSilverfishPercent;
    public int skeletonSilverfishMaxAtOnce() {
        return skeletonSilverfishMaxAtOnce;
    public int skeletonSilverfishMaxTotal() {
        return skeletonSilverfishMaxTotal;
    public boolean skeletonKillSilverfishOnDeath() {
        return skeletonKillSilverfishOnDeath;
    public int skeletonDeflectArrowsPercent() {
        return skeletonDeflectArrowsPercent;
    public boolean silverfishCantEnterBlocks() {
        return silverfishCantEnterBlocks;
    public boolean silverfishDropCobble() {
        return silverfishDropCobble;
    public boolean silverfishVisibilityParticles() {
        return silverfishVisibilityParticles;
    public boolean endermenTeleportPlayers() {
        return endermenTeleportPlayers;
    public boolean witchesAdditionalAttacks() {
        return witchesAdditionalAttacks;
    public int witchesBonusSpawnPercent() {
        return witchesBonusSpawnPercent;
    public boolean horseBlockChest() {
        return horseBlockChest;
    public int horseBlockChestBelowY() {
        return horseBlockChestBelowY;
    }

    public ExplosionConfig explosions() {
        return explosions;
    }

    public MonsterConfig monsters() {
        return monsters;
    public boolean blazeNearBedrockEnable() {
        return blazeNearBedrockEnable;
    }

    public int blazeNearBedrockPercent() {
        return blazeNearBedrockPercent;
    public int blazeNearBedrockMaxY() {
        return blazeNearBedrockMaxY;
    public boolean blazeBlockOverworldDrops() {
        return blazeBlockOverworldDrops;
    public int blazeBonusNetherPercent() {
        return blazeBonusNetherPercent;
    public boolean blazeDropFireOnDamage() {
        return blazeDropFireOnDamage;
    public boolean blazeBonusLoot() {
        return blazeBonusLoot;
    public int blazeNetherSplitPercent() {
        return blazeNetherSplitPercent;
    public int magmaSpawnWithNetherBlazePercent() {
        return magmaSpawnWithNetherBlazePercent;
    public boolean magmaGrowIntoBlazesOnDamage() {
        return magmaGrowIntoBlazesOnDamage;
    public boolean pigmenAlwaysAngry() {
        return pigmenAlwaysAngry;
    public int pigmenDamagePercent() {
        return pigmenDamagePercent;
    public boolean pigmenFortressNetherwart() {
        return pigmenFortressNetherwart;
    public int pigmenElsewhereNetherwartPercent() {
        return pigmenElsewhereNetherwartPercent;
    public boolean pigmenLightningSpawns() {
        return pigmenLightningSpawns;
    public int ghastArrowDamagePercent() {
        return ghastArrowDamagePercent;
    public int ghastExpMultiplier() {
        return ghastExpMultiplier;
    public int ghastDropsMultiplier() {
        return ghastDropsMultiplier;
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean hasEnabledKey() {
        return enabledPresent;
    }

    public PlayerSettings player() {
        return player;
    public boolean inhibitGrinders() {
        return inhibitGrinders;
    }

    public boolean moreMonstersEnable() {
        return moreMonstersEnable;
    public int moreMonstersMaxY() {
        return moreMonstersMaxY;
    public int moreMonstersMultiplier() {
        return moreMonstersMultiplier;
    public boolean spawnInLightEnable() {
        return spawnInLightEnable;
    public int spawnInLightMaxY() {
        return spawnInLightMaxY;
    public int spawnInLightMaxLight() {
        return spawnInLightMaxLight;
    public int spawnInLightPercent() {
        return spawnInLightPercent;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.enabledPresent = true;
    }

    public void setTorchYDeny(boolean torchYDeny) {
        this.torchYDeny = torchYDeny;
    }

    public void setNetherrackFirePercent(int netherrackFirePercent) {
        this.netherrackFirePercent = netherrackFirePercent;
    public boolean weakCrops() {
        return weakCrops;
    public int lossRate() {
        return lossRate;
    public boolean infertileDeserts() {
        return infertileDeserts;
    public boolean snowBreaksCrops() {
        return snowBreaksCrops;
    public boolean cantCraftMelonSeeds() {
        return cantCraftMelonSeeds;
    public boolean noBonemealOnMushrooms() {
        return noBonemealOnMushrooms;
    public boolean noFarmNetherWart() {
        return noFarmNetherWart;
    public boolean sheepWhiteWool() {
        return sheepWhiteWool;
    public boolean squidOceanOnly() {
        return squidOceanOnly;
    public boolean bucketsDontMoveSources() {
        return bucketsDontMoveSources;
    public boolean animalXpNerf() {
        return animalXpNerf;
    public boolean ironGolemNerf() {
        return ironGolemNerf;
    public boolean overcrowdEnable() {
        return overcrowdEnable;
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

    public int killerBunnyPercent() {
        return killerBunnyPercent;
    }

    public void setKillerBunnyPercent(int percent) {
        killerBunnyPercent = clampPercent(percent);
    }

    public int vindicatorPercent() {
        return vindicatorPercent;
    }

    public void setVindicatorPercent(int percent) {
        vindicatorPercent = clampPercent(percent);
    }

    public int caveSpiderPercent() {
        return caveSpiderPercent;
    }

    public void setCaveSpiderPercent(int percent) {
        caveSpiderPercent = clampPercent(percent);
    }

    public int guardianPercent() {
        return guardianPercent;
    }

    public void setGuardianPercent(int percent) {
        guardianPercent = clampPercent(percent);
    }

    public int vexPercent() {
        return vexPercent;
    }

    public void setVexPercent(int percent) {
        vexPercent = clampPercent(percent);
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
                        "worldRules.betterTreeFelling",
                        "Realistic chopping: BFS same-wood logs fall. Default on; upstream had commented it out.",
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
                        "torches.rainBreaksTorches",
                        "Rain drops exposed torches as items. Covered torches survive.",
                writeDefaultIfMissing(
                        file, "campfires.rainExtinguishes", "Optional. Same rain pass as torches; default off.", false);
                writeDefaultIfMissing(file, "sounds.torchFizz", "Lava fizz when torch placement is denied.", true);
                        file,
                        "worldRules.netherrackFirePercent",
                        "Chance that breaking netherrack places fire in the empty space. Nylium not included.",
                        20);
                PlayerSettings.writeDefaults(file);
                writeFarmingDefaults(file);
                        "mining.caveIns.enable",
                        "Mining cave-in ores softens 6 neighbors (stone→cobble, deepslate→cobbled deepslate).",
                        true);
                        "mining.caveIns.applyPhysics",
                        "Converted neighbors become FallingBlockEntity (budgeted). False only sets the block.",
                if (!file.contains("mining.caveIns.softenMap")) {
                    file.setComment(
                            "mining.caveIns.softenMap",
                            "from>to. Copper ore neighbors roll 50%. Ancient debris only softens #hardened neighbors.");
                    file.set("mining.caveIns.softenMap", new ArrayList<>(SoftenMap.DEFAULT_ENTRIES));
                }
                        "falling.enable",
                        "Extra falling blocks (#extrahardmode:extra_falling) drop when unsupported. v1 hooks: player break, BlockItem place, EHM land cascade. Piston/other non-player support removal is not scanned.",
                        "falling.breakTorches",
                        "Implemented, default off (upstream buggy).",
                        false);
                if (!file.contains("falling.damage")) {
                    file.setComment("falling.damage", "KD-18: restore docs-era falling-block player damage. Gated to extra_falling ∪ EHM_OURS.");
                    file.set("falling.damage", 2);
                writeDefaultIfMissing(file, "falling.turnGrassToDirt", "Grass/mycelium/podzol land as dirt.", true);
                writeDefaultIfMissing(file, "falling.cascade", "Landed EHM falling blocks can make neighbors fall.", true);
                        "falling.dropAsItemWhenBlocked",
                        "Drop an item when a falling block cannot place.",
                        "skeletons.snowballEnable",
                        "Bogged share this table; strays/wither skeletons do not.",
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
                        file, "skeletons.killSilverfishOnSkeletonDeath", "Discard owner-tagged minions on skeleton death.", true);
                writeDefaultIfMissing(file, "skeletons.deflectArrowsPercent", "Arrows pass through Skeleton/Bogged.", 100);
                writeDefaultIfMissing(file, "silverfish.cantEnterBlocks", "Block merge-into-stone.", true);
                writeDefaultIfMissing(file, "silverfish.dropCobble", true);
                        file, "silverfish.visibilityParticles", "Portal particles so floor-glitched silverfish stay visible.", true);
                        "endermen.teleportPlayers",
                        "In a fight, an enderman may teleport the player onto it (2-high roof cheese).",
                        "witches.additionalAttacks",
                        "30/30/30/10 splash: baby zombie, teleport, explosion+3 generic, vanilla poison (non-players intensity 0).",
                        "witches.bonusSpawnPercent",
                        "NATURAL overworld zombies on grass_block become witches.",
                        5);
                        "horses.blockChest.enable",
                        "Block chested-horse inventory below blockChestBelowY. Disable with this boolean, not Y=0.",
                if (!file.contains("horses.blockChestBelowY")) {
                    file.setComment("horses.blockChestBelowY", "original: 55; cave band. Integer.MIN_VALUE also disables.");
                    file.set("horses.blockChestBelowY", 48);
                        "replacements.killerBunnyPercent",
                        "NATURAL rabbits become killer bunnies. RootNode KillerBunny.Bonus Spawn Percent: 1.",
                        1);
                        "replacements.vindicatorPercent",
                        "NATURAL skeletons in #extrahardmode:vindicator_replace (dark_forest). RootNode 20.",
                        "replacements.caveSpiderPercent",
                        "NATURAL spiders in #extrahardmode:cave_spider_replace (swamp, mangrove_swamp). RootNode 5.",
                        "replacements.guardianPercent",
                        "NATURAL squid in #extrahardmode:guardian_replace (#minecraft:is_ocean). RootNode 20 (docs 10).",
                        "replacements.vexPercent",
                        "NATURAL bats become vexes. Deep Dark skipped by SpawnReplaceService. RootNode 5.",
                        "monsters.inhibitGrinders",
                        "No drops/XP for grinders: unnatural floor, too much env damage, no path to player, water.",
                        "monsters.more.enable",
                        "Pack-size multiplier below maxY. Disable with this boolean, not Y=0.",
                if (!file.contains("monsters.more.maxY")) {
                    file.setComment("monsters.more.maxY", "original: 55; cave band");
                    file.set("monsters.more.maxY", 48);
                if (!file.contains("monsters.more.multiplier")) {
                            "monsters.more.multiplier",
                            "Pack size only; does not raise the vanilla monster cap.");
                    file.set("monsters.more.multiplier", 2);
                        "monsters.spawnInLight.enable",
                        "Spawn extra monsters in light<=maxLight below maxY. Disable with this boolean, not Y=0.",
                if (!file.contains("monsters.spawnInLight.maxY")) {
                    file.setComment("monsters.spawnInLight.maxY", "original RootNode 50; cave band");
                    file.set("monsters.spawnInLight.maxY", 48);
                if (!file.contains("monsters.spawnInLight.maxLight")) {
                            "monsters.spawnInLight.maxLight",
                            "0-3 bats, 0-7 vanilla, 8-11 hostile no burn, 12+ burn");
                    file.set("monsters.spawnInLight.maxLight", 10);
                if (!file.contains("monsters.spawnInLight.percent")) {
                            "monsters.spawnInLight.percent",
                            "Chance per attempt at previously visited cave sections.");
                    file.set("monsters.spawnInLight.percent", 100);
                        false);
                config.explosions.writeDefaults(file);
                config.monsters.writeDefaults(file);
                writeDefaultIfMissing(
                        file,
                        "blazes.nearBedrock.enable",
                        "Replace NATURAL overworld skeletons near bedrock with blazes. Disable with this boolean, not Y=-64.",
                        true);
                        "blazes.nearBedrockPercent",
                        "Chance a near-bedrock skeleton becomes a blaze.",
                        50);
                if (!file.contains("blazes.nearBedrockMaxY")) {
                    file.setComment("blazes.nearBedrockMaxY", "original: ~Y=0. Integer.MIN_VALUE also disables.");
                    file.set("blazes.nearBedrockMaxY", -56);
                }
                writeDefaultIfMissing(file, "blazes.blockOverworldDrops", "No blaze rods from overworld blazes.", true);
                        "blazes.bonusNetherPercent",
                        "Replace NATURAL nether zombified piglins outside fortresses with blazes.",
                        20);
                writeDefaultIfMissing(file, "blazes.dropFireOnDamage", "Blazes above half health place fire when hit.", true);
                        file, "blazes.bonusLoot", "Extra gunpowder + blaze rod from nether blazes.", true);
                        file, "blazes.netherSplitPercent", "Nether blazes may split into two full-HP blazes on death.", 25);
                        "magmaCubes.spawnWithNetherBlazePercent",
                        "When a nether piglin is replaced with a blaze, also spawn a size-1 magma cube.",
                        100);
                        "magmaCubes.growIntoBlazesOnDamage",
                        "On damage, magma cubes explode (MAGMACUBE_FIRE) and become blazes.",
                writeDefaultIfMissing(file, "pigmen.alwaysAngry", "Zombified piglins never calm and aggro players.", true);
                        file, "pigmen.damagePercent", "Outgoing zombified piglin damage to players. original: 70.", 70);
                        file, "pigmen.fortressNetherwart", "Always drop nether wart when slain in a fortress.", true);
                        "pigmen.elsewhereNetherwartPercent",
                        "Chance to drop nether wart elsewhere in the Nether.",
                        25);
                        "pigmen.lightningSpawns",
                        "Overworld lightning spawns 1-3 angry zombified piglins.",
                writeDefaultIfMissing(file, "ghasts.arrowDamagePercent", "Incoming arrow damage to ghasts.", 20);
                writeDefaultIfMissing(file, "ghasts.expMultiplier", "Ghast XP multiplier.", 10);
                writeDefaultIfMissing(file, "ghasts.dropsMultiplier", "Ghast drop-count multiplier.", 5);
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
                file.set("worldRules.betterTreeFelling", betterTreeFelling);
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
                monsters.write(file);
                file.set("blazes.nearBedrock.enable", blazeNearBedrockEnable);
                file.set("blazes.nearBedrockPercent", blazeNearBedrockPercent);
                file.set("blazes.nearBedrockMaxY", blazeNearBedrockMaxY);
                file.set("blazes.blockOverworldDrops", blazeBlockOverworldDrops);
                file.set("blazes.bonusNetherPercent", blazeBonusNetherPercent);
                file.set("blazes.dropFireOnDamage", blazeDropFireOnDamage);
                file.set("blazes.bonusLoot", blazeBonusLoot);
                file.set("blazes.netherSplitPercent", blazeNetherSplitPercent);
                file.set("magmaCubes.spawnWithNetherBlazePercent", magmaSpawnWithNetherBlazePercent);
                file.set("magmaCubes.growIntoBlazesOnDamage", magmaGrowIntoBlazesOnDamage);
                file.set("pigmen.alwaysAngry", pigmenAlwaysAngry);
                file.set("pigmen.damagePercent", pigmenDamagePercent);
                file.set("pigmen.fortressNetherwart", pigmenFortressNetherwart);
                file.set("pigmen.elsewhereNetherwartPercent", pigmenElsewhereNetherwartPercent);
                file.set("pigmen.lightningSpawns", pigmenLightningSpawns);
                file.set("ghasts.arrowDamagePercent", ghastArrowDamagePercent);
                file.set("ghasts.expMultiplier", ghastExpMultiplier);
                file.set("ghasts.dropsMultiplier", ghastDropsMultiplier);
                List<String> budgetEntries = new ArrayList<>();
                for (Map.Entry<Identifier, Integer> budget : hardenedBudgets.entrySet()) {
                    budgetEntries.add(budget.getKey().toString() + "@" + budget.getValue());
                }
                if (budgetEntries.isEmpty()) {
                    budgetEntries.addAll(HardenedBudget.DEFAULT_ENTRIES);
                }
                file.set("mining.hardened.budgets", budgetEntries);
                file.set("torches.rainBreaksTorches", rainBreaksTorches);
                file.set("campfires.rainExtinguishes", rainExtinguishesCampfires);
                file.set("sounds.torchFizz", torchFizz);
                file.set("worldRules.netherrackFirePercent", netherrackFirePercent);
                player.write(file);
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
                file.set("endermen.teleportPlayers", endermenTeleportPlayers);
                file.set("witches.additionalAttacks", witchesAdditionalAttacks);
                file.set("witches.bonusSpawnPercent", witchesBonusSpawnPercent);
                file.set("horses.blockChest.enable", horseBlockChest);
                file.set("horses.blockChestBelowY", horseBlockChestBelowY);
                file.set("replacements.killerBunnyPercent", killerBunnyPercent);
                file.set("replacements.vindicatorPercent", vindicatorPercent);
                file.set("replacements.caveSpiderPercent", caveSpiderPercent);
                file.set("replacements.guardianPercent", guardianPercent);
                file.set("replacements.vexPercent", vexPercent);
                file.set("monsters.inhibitGrinders", inhibitGrinders);
                file.set("monsters.more.enable", moreMonstersEnable);
                file.set("monsters.more.maxY", moreMonstersMaxY);
                file.set("monsters.more.multiplier", moreMonstersMultiplier);
                file.set("monsters.spawnInLight.enable", spawnInLightEnable);
                file.set("monsters.spawnInLight.maxY", spawnInLightMaxY);
                file.set("monsters.spawnInLight.maxLight", spawnInLightMaxLight);
                file.set("monsters.spawnInLight.percent", spawnInLightPercent);
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
        betterTreeFelling = file.getOrElse("worldRules.betterTreeFelling", true);
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
        monsters.read(file);
        blazeNearBedrockEnable = file.getOrElse("blazes.nearBedrock.enable", true);
        blazeNearBedrockPercent = percent(getInt(file, "blazes.nearBedrockPercent", 50));
        blazeNearBedrockMaxY = getInt(file, "blazes.nearBedrockMaxY", -56);
        blazeBlockOverworldDrops = file.getOrElse("blazes.blockOverworldDrops", true);
        blazeBonusNetherPercent = percent(getInt(file, "blazes.bonusNetherPercent", 20));
        blazeDropFireOnDamage = file.getOrElse("blazes.dropFireOnDamage", true);
        blazeBonusLoot = file.getOrElse("blazes.bonusLoot", true);
        blazeNetherSplitPercent = percent(getInt(file, "blazes.netherSplitPercent", 25));
        magmaSpawnWithNetherBlazePercent = percent(getInt(file, "magmaCubes.spawnWithNetherBlazePercent", 100));
        magmaGrowIntoBlazesOnDamage = file.getOrElse("magmaCubes.growIntoBlazesOnDamage", true);
        pigmenAlwaysAngry = file.getOrElse("pigmen.alwaysAngry", true);
        pigmenDamagePercent = percent(getInt(file, "pigmen.damagePercent", 70));
        pigmenFortressNetherwart = file.getOrElse("pigmen.fortressNetherwart", true);
        pigmenElsewhereNetherwartPercent = percent(getInt(file, "pigmen.elsewhereNetherwartPercent", 25));
        pigmenLightningSpawns = file.getOrElse("pigmen.lightningSpawns", true);
        ghastArrowDamagePercent = percent(getInt(file, "ghasts.arrowDamagePercent", 20));
        ghastExpMultiplier = Math.max(0, getInt(file, "ghasts.expMultiplier", 10));
        ghastDropsMultiplier = Math.max(0, getInt(file, "ghasts.dropsMultiplier", 5));
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
        rainBreaksTorches = file.getOrElse("torches.rainBreaksTorches", true);
        rainExtinguishesCampfires = file.getOrElse("campfires.rainExtinguishes", false);
        torchFizz = file.getOrElse("sounds.torchFizz", true);
        netherrackFirePercent = file.getOrElse("worldRules.netherrackFirePercent", 20);
        player.read(file);
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
        endermenTeleportPlayers = file.getOrElse("endermen.teleportPlayers", true);
        witchesAdditionalAttacks = file.getOrElse("witches.additionalAttacks", true);
        witchesBonusSpawnPercent = percent(getInt(file, "witches.bonusSpawnPercent", 5));
        horseBlockChest = file.getOrElse("horses.blockChest.enable", true);
        horseBlockChestBelowY = getInt(file, "horses.blockChestBelowY", 48);
        killerBunnyPercent = percentOr(file, "replacements.killerBunnyPercent", 1);
        vindicatorPercent = percentOr(file, "replacements.vindicatorPercent", 20);
        caveSpiderPercent = percentOr(file, "replacements.caveSpiderPercent", 5);
        guardianPercent = percentOr(file, "replacements.guardianPercent", 20);
        vexPercent = percentOr(file, "replacements.vexPercent", 5);
        inhibitGrinders = file.getOrElse("monsters.inhibitGrinders", true);
        moreMonstersEnable = file.getOrElse("monsters.more.enable", true);
        moreMonstersMaxY = getInt(file, "monsters.more.maxY", 48);
        moreMonstersMultiplier = getInt(file, "monsters.more.multiplier", 2);
        spawnInLightEnable = file.getOrElse("monsters.spawnInLight.enable", true);
        spawnInLightMaxY = getInt(file, "monsters.spawnInLight.maxY", 48);
        spawnInLightMaxLight = getInt(file, "monsters.spawnInLight.maxLight", 10);
        spawnInLightPercent = getInt(file, "monsters.spawnInLight.percent", 100);
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
            if (comment != null) {
                file.setComment(path, comment);
            }
            file.set(path, value);
        }
    }

    private static void writeDefaultIfMissing(CommentedFileConfig file, String path, String comment, int value) {
        if (!file.contains(path)) {
            file.setComment(path, comment);
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
    private static void writeDefaultIfMissing(CommentedFileConfig file, String path, String comment, int value) {
        if (!file.contains(path)) {
            file.setComment(path, comment);
            file.set(path, value);
        }
    }
    private static void writeDefaultIfMissing(CommentedFileConfig file, String path, boolean value) {
        writeDefaultIfMissing(file, path, null, value);

    private static void writeDefaultIfMissing(CommentedFileConfig file, String path, int value) {
            if (comment != null) {
                file.setComment(path, comment);
    private static void writeDefaultIfMissing(CommentedFileConfig file, String path, String comment, double value) {
    private static int percent(int value) {
        return Math.clamp(value, 0, 100);
    private static int getInt(CommentedFileConfig file, String path, int fallback) {
        if (raw instanceof Number number) {
            return number.intValue();
        return fallback;
    private static double getDouble(CommentedFileConfig file, String path, double fallback) {
            return number.doubleValue();
    private static void writeDefaultIfMissing(CommentedFileConfig file, String path, String comment, int value) {
        if (!file.contains(path)) {
            file.setComment(path, comment);
            file.set(path, value);
        }
    }
        Object raw = file.get(path);

    private static int percentOr(CommentedFileConfig file, String path, int fallback) {
        if (raw instanceof Number number) {
            return clampPercent(number.intValue());
        }
        return fallback;
    }
    static int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
}

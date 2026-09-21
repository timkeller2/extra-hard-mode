package dev.extrahardmode.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.feature.HardenedBudget;
import dev.extrahardmode.feature.HungerRules;
import dev.extrahardmode.feature.OvergrazingRules;
import dev.extrahardmode.feature.SoftenMap;
import dev.extrahardmode.feature.TorchLifetimeRules;
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
    public static final int CONFIG_VERSION = 6;

    private final Identifier dimensionId;
    private final Map<Identifier, Boolean> modules = new LinkedHashMap<>();
    private boolean checkPermission = true;
    private boolean creativeBypasses = true;
    private boolean operatorsBypass = false;
    private boolean limitedBuilding = true;
    private boolean betterTreeFelling = true;
    private boolean f3Enabled = false;
    private boolean competitiveAchievements = true;
    private boolean torchSoftDeny = true;
    private int torchNoPlacementUnderY = 0;
    private boolean torchYDeny = true;
    private boolean hardenedEnable = true;
    private boolean blockOreNextToStone = true;
    private boolean blockPistonMove = true;
    private final Map<Identifier, Integer> hardenedBudgets = new LinkedHashMap<>();
    private boolean rainBreaksTorches = true;
    private int torchBurnDays = TorchLifetimeRules.DEFAULT_DAYS;
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
    private int skeletonDeflectArrowsPercent = 75;
    private boolean silverfishCantEnterBlocks = true;
    private boolean silverfishDropCobble = true;
    private boolean silverfishVisibilityParticles = true;
    private boolean endermenTeleportPlayers = true;
    private boolean witchesAdditionalAttacks = true;
    private int witchesBonusSpawnPercent = 5;
    private boolean horseBlockChest = true;
    private int horseBlockChestBelowY = 48;
    private boolean creeperTntWarning = true;
    private boolean villagerNerfBlockDiamondGear = true;
    private boolean villagerNerfNoviceMending = true;
    private boolean enabled = true;
    private boolean enabledPresent;
    private final PlayerSettings player = new PlayerSettings();
    private boolean weakCrops = true;
    private int lossRate = 25;
    private boolean infertileDeserts = true;
    private boolean snowBreaksCrops = true;
    private boolean cantCraftMelonSeeds = true;
    private boolean noBonemealOnMushrooms = true;
    private boolean noFarmNetherWart = false;
    private boolean sheepWhiteWool = false;
    private boolean squidOceanOnly = true;
    private boolean bucketsDontMoveSources = true;
    private boolean animalXpNerf = true;
    private boolean ironGolemNerf = true;
    private boolean overcrowdEnable = true;
    private int overcrowdThreshold = 8;
    private boolean overgrazingEnable = true;
    private int overgrazingIntervalTicks = OvergrazingRules.DEFAULT_INTERVAL_TICKS;
    private int overgrazingLargeMarks = OvergrazingRules.DEFAULT_LARGE_MARKS;
    private int overgrazingSmallMarks = OvergrazingRules.DEFAULT_SMALL_MARKS;
    private int overgrazingLookChance = OvergrazingRules.LOOK_CHANCE_PERCENT;
    private int overgrazingChestRange = OvergrazingRules.DEFAULT_CHEST_RANGE;
    private int cropMatureDurationPercent = 300;
    private int animalBreedCooldownMultiplier = 6;
    private int eggLayTimeMultiplier = 6;
    private int hiveHoneycombCount = 1;
    private float movingExhaustionPerSecond = HungerRules.DEFAULT_MOVING_EXHAUSTION_PER_SECOND;
    private int foodHistorySize = 7;
    private int slowRegenTicks = HungerRules.DEFAULT_SLOW_REGEN_TICKS;
    private boolean disableFastSaturationRegen = true;
    private int afkTimeoutSeconds = 30;
    private int fishHealthyCount = 3;
    private int fishHealthyRatePercent = 33;
    private int fishScarceRatePercent = 5;
    private int fishingWaitMultiplier = 3;
    private int killerBunnyPercent = 1;
    private int vindicatorPercent = 20;
    private int caveSpiderPercent = 5;
    private int guardianPercent = 20;
    private int vexPercent = 5;
    private boolean inhibitGrinders = true;
    private boolean moreMonstersEnable = true;
    private int moreMonstersMaxY = 48;
    private int moreMonstersMultiplier = 3;
    private boolean spawnInLightEnable = true;
    private int spawnInLightMaxY = 48;
    private int spawnInLightMaxLight = 11;
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
    private final DragonConfig dragon = new DragonConfig();
    private int bossMinDistanceFromSpawn = 300;
    private int bossCooldownHours = 8;
    private int bossSpawnChancePercent = 1;
    private int bossCheckIntervalTicks = 1200;
    private double bossScale = 1.6;
    private int bossHealthMultiplierMin = 6;
    private int bossHealthMultiplierMax = 12;
    private double bossAttackMultiplier = 1.75;
    private int bossHeavyArmorDropPercent = 25;
    private int bossDistanceStepBlocks = 25;
    private double bossDifficultyPercentPerStep = 1.0;
    private double bossTreasurePercentScale = 3.0;
    private int inhabitantMinVolume = 24;
    private int inhabitantMaxVolume = 300;
    private int inhabitantMinLight = 8;
    private int inhabitantMinScore = 12;
    private int inhabitantSpacing = 48;
    private int inhabitantBaseChancePercent = 8;
    private int inhabitantChancePerPoint = 2;
    private int inhabitantMaxChancePercent = 40;
    private int inhabitantLeaveGraceDays = 3;
    private int inhabitantKillCooldownDays = 7;

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

    public boolean f3Enabled() {
        return f3Enabled;
    }

    public boolean competitiveAchievements() {
        return competitiveAchievements;
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

    public boolean rainBreaksTorches() {
        return rainBreaksTorches;
    }

    public int torchBurnDays() {
        return torchBurnDays;
    }

    public boolean rainExtinguishesCampfires() {
        return rainExtinguishesCampfires;
    }

    public boolean torchFizz() {
        return torchFizz;
    }

    public int netherrackFirePercent() {
        return netherrackFirePercent;
    }

    public boolean caveInsEnable() {
        return caveInsEnable;
    }

    public boolean caveInsApplyPhysics() {
        return caveInsApplyPhysics;
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

    public boolean endermenTeleportPlayers() {
        return endermenTeleportPlayers;
    }

    public boolean witchesAdditionalAttacks() {
        return witchesAdditionalAttacks;
    }

    public int witchesBonusSpawnPercent() {
        return witchesBonusSpawnPercent;
    }

    public boolean horseBlockChest() {
        return horseBlockChest;
    }

    public int horseBlockChestBelowY() {
        return horseBlockChestBelowY;
    }

    public boolean creeperTntWarning() {
        return creeperTntWarning;
    }

    public boolean villagerNerfBlockDiamondGear() {
        return villagerNerfBlockDiamondGear;
    }

    public boolean villagerNerfNoviceMending() {
        return villagerNerfNoviceMending;
    }

    public boolean enabled() {
        return enabled;
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

    public boolean overgrazingEnable() {
        return overgrazingEnable;
    }

    public int overgrazingIntervalTicks() {
        return overgrazingIntervalTicks;
    }

    public int overgrazingLargeMarks() {
        return overgrazingLargeMarks;
    }

    public int overgrazingSmallMarks() {
        return overgrazingSmallMarks;
    }

    public int overgrazingLookChance() {
        return overgrazingLookChance;
    }

    public int overgrazingChestRange() {
        return overgrazingChestRange;
    }

    public int cropMatureDurationPercent() {
        return cropMatureDurationPercent;
    }

    public int animalBreedCooldownMultiplier() {
        return animalBreedCooldownMultiplier;
    }

    public int eggLayTimeMultiplier() {
        return eggLayTimeMultiplier;
    }

    public int hiveHoneycombCount() {
        return hiveHoneycombCount;
    }

    public float movingExhaustionPerSecond() {
        return movingExhaustionPerSecond;
    }

    public int foodHistorySize() {
        return foodHistorySize;
    }

    public int slowRegenTicks() {
        return slowRegenTicks;
    }

    public boolean disableFastSaturationRegen() {
        return disableFastSaturationRegen;
    }

    public int afkTimeoutSeconds() {
        return afkTimeoutSeconds;
    }

    public int fishHealthyCount() {
        return fishHealthyCount;
    }

    public int fishHealthyRatePercent() {
        return fishHealthyRatePercent;
    }

    public int fishScarceRatePercent() {
        return fishScarceRatePercent;
    }

    public int fishingWaitMultiplier() {
        return fishingWaitMultiplier;
    }

    public int killerBunnyPercent() {
        return killerBunnyPercent;
    }

    public int vindicatorPercent() {
        return vindicatorPercent;
    }

    public int caveSpiderPercent() {
        return caveSpiderPercent;
    }

    public int guardianPercent() {
        return guardianPercent;
    }

    public int vexPercent() {
        return vexPercent;
    }

    public boolean inhibitGrinders() {
        return inhibitGrinders;
    }

    public boolean moreMonstersEnable() {
        return moreMonstersEnable;
    }

    public int moreMonstersMaxY() {
        return moreMonstersMaxY;
    }

    public int moreMonstersMultiplier() {
        return moreMonstersMultiplier;
    }

    public boolean spawnInLightEnable() {
        return spawnInLightEnable;
    }

    public int spawnInLightMaxY() {
        return spawnInLightMaxY;
    }

    public int spawnInLightMaxLight() {
        return spawnInLightMaxLight;
    }

    public int spawnInLightPercent() {
        return spawnInLightPercent;
    }

    public boolean blazeNearBedrockEnable() {
        return blazeNearBedrockEnable;
    }

    public int blazeNearBedrockPercent() {
        return blazeNearBedrockPercent;
    }

    public int blazeNearBedrockMaxY() {
        return blazeNearBedrockMaxY;
    }

    public boolean blazeBlockOverworldDrops() {
        return blazeBlockOverworldDrops;
    }

    public int blazeBonusNetherPercent() {
        return blazeBonusNetherPercent;
    }

    public boolean blazeDropFireOnDamage() {
        return blazeDropFireOnDamage;
    }

    public boolean blazeBonusLoot() {
        return blazeBonusLoot;
    }

    public int blazeNetherSplitPercent() {
        return blazeNetherSplitPercent;
    }

    public int magmaSpawnWithNetherBlazePercent() {
        return magmaSpawnWithNetherBlazePercent;
    }

    public boolean magmaGrowIntoBlazesOnDamage() {
        return magmaGrowIntoBlazesOnDamage;
    }

    public boolean pigmenAlwaysAngry() {
        return pigmenAlwaysAngry;
    }

    public int pigmenDamagePercent() {
        return pigmenDamagePercent;
    }

    public boolean pigmenFortressNetherwart() {
        return pigmenFortressNetherwart;
    }

    public int pigmenElsewhereNetherwartPercent() {
        return pigmenElsewhereNetherwartPercent;
    }

    public boolean pigmenLightningSpawns() {
        return pigmenLightningSpawns;
    }

    public int ghastArrowDamagePercent() {
        return ghastArrowDamagePercent;
    }

    public int ghastExpMultiplier() {
        return ghastExpMultiplier;
    }

    public int ghastDropsMultiplier() {
        return ghastDropsMultiplier;
    }

    public boolean hasEnabledKey() {
        return enabledPresent;
    }

    public int hardenedBudget(Identifier itemId) {
        return hardenedBudgets.getOrDefault(itemId, 0);
    }

    public Map<Identifier, Integer> hardenedBudgets() {
        return hardenedBudgets;
    }

    public Identifier softenTo(Identifier from) {
        return softenMap.get(from);
    }

    public ExplosionConfig explosions() {
        return explosions;
    }

    public MonsterConfig monsters() {
        return monsters;
    }

    public DragonConfig dragon() {
        return dragon;
    }

    public int bossMinDistanceFromSpawn() {
        return bossMinDistanceFromSpawn;
    }

    public int bossCooldownHours() {
        return bossCooldownHours;
    }

    public int bossSpawnChancePercent() {
        return bossSpawnChancePercent;
    }

    public int bossCheckIntervalTicks() {
        return bossCheckIntervalTicks;
    }

    public double bossScale() {
        return bossScale;
    }

    public int bossHealthMultiplierMin() {
        return bossHealthMultiplierMin;
    }

    public int bossHealthMultiplierMax() {
        return bossHealthMultiplierMax;
    }

    public double bossAttackMultiplier() {
        return bossAttackMultiplier;
    }

    public int bossHeavyArmorDropPercent() {
        return bossHeavyArmorDropPercent;
    }

    public int bossDistanceStepBlocks() {
        return bossDistanceStepBlocks;
    }

    public double bossDifficultyPercentPerStep() {
        return bossDifficultyPercentPerStep;
    }

    public double bossTreasurePercentScale() {
        return bossTreasurePercentScale;
    }

    public int inhabitantMinVolume() {
        return inhabitantMinVolume;
    }

    public int inhabitantMaxVolume() {
        return inhabitantMaxVolume;
    }

    public int inhabitantMinLight() {
        return inhabitantMinLight;
    }

    public int inhabitantMinScore() {
        return inhabitantMinScore;
    }

    public int inhabitantSpacing() {
        return inhabitantSpacing;
    }

    public int inhabitantBaseChancePercent() {
        return inhabitantBaseChancePercent;
    }

    public int inhabitantChancePerPoint() {
        return inhabitantChancePerPoint;
    }

    public int inhabitantMaxChancePercent() {
        return inhabitantMaxChancePercent;
    }

    public int inhabitantLeaveGraceDays() {
        return inhabitantLeaveGraceDays;
    }

    public int inhabitantKillCooldownDays() {
        return inhabitantKillCooldownDays;
    }

    public PlayerSettings player() {
        return player;
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

    public void setNetherrackFirePercent(int netherrackFirePercent) {
        this.netherrackFirePercent = netherrackFirePercent;
    }

    public void setTorchFizz(boolean torchFizz) {
        this.torchFizz = torchFizz;
    }

    public void setTorchBurnDays(int torchBurnDays) {
        this.torchBurnDays = TorchLifetimeRules.clampDays(torchBurnDays);
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
            int torchBurnDays,
            boolean creeperTntWarning) {
        this.checkPermission = checkPermission;
        this.creativeBypasses = creativeBypasses;
        this.operatorsBypass = operatorsBypass;
        this.limitedBuilding = limitedBuilding;
        this.torchYDeny = torchYDeny;
        this.torchSoftDeny = torchSoftDeny;
        this.torchNoPlacementUnderY = torchNoPlacementUnderY;
        this.torchFizz = torchFizz;
        this.torchBurnDays = TorchLifetimeRules.clampDays(torchBurnDays);
        this.creeperTntWarning = creeperTntWarning;
    }

    public boolean isModuleEnabled(Identifier moduleId) {
        Boolean value = modules.get(moduleId);
        if (value != null) {
            return value;
        }
        return ExtraHardModeMod.FEATURES.defaultEnabled(moduleId);
    }

    public void setModuleEnabled(Identifier moduleId, boolean enabled) {
        modules.put(moduleId, enabled);
    }

    public void setKillerBunnyPercent(int percent) {
        killerBunnyPercent = clampPercent(percent);
    }

    public void setVindicatorPercent(int percent) {
        vindicatorPercent = clampPercent(percent);
    }

    public void setCaveSpiderPercent(int percent) {
        caveSpiderPercent = clampPercent(percent);
    }

    public void setGuardianPercent(int percent) {
        guardianPercent = clampPercent(percent);
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
                            "EHM world config schema. tougher:enabled is a server-global master gamerule; this file's enabled is per-dimension.");
                    file.set("configVersion", CONFIG_VERSION);
                }
                writeDefaultIfMissing(
                        file,
                        "bypassing.checkPermission",
                        "Honor tougher.bypass (and silent) permission nodes.",
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
                        "client.f3Enabled",
                        "Allow the F3 debug screen (coordinates, light, profiling). Default false.",
                        false);
                writeDefaultIfMissing(
                        file,
                        "achievements.competitive",
                        "The first player to claim a builder or slayer achievement server-wide gets 25 extra experience. Everyone can still earn every achievement. Default true.",
                        true);
                writeDefaultIfMissing(
                        file,
                        "torches.noPlacement.enable",
                        "Deny depth-limited lights and campfires below noPlacementUnderY. Disable with this boolean, not Y=0.",
                        true);
                if (!file.contains("torches.noPlacementUnderY")) {
                    file.setComment("torches.noPlacementUnderY", "original: 30");
                    file.set("torches.noPlacementUnderY", 0);
                }
                writeDefaultIfMissing(file, "torches.noPlacementOnSoft", "No torches on soft surfaces.", true);
                writeDefaultIfMissing(
                        file,
                        "mining.hardened.enable",
                        "Hardened stone/deepslate. Unlisted tools cannot harvest; listed tools take extra durability per break (Unbreaking applies).",
                        true);
                writeDefaultIfMissing(
                        file, "mining.hardened.blockOreNextToStone", "Cancel placing cave-in ores next to hardened blocks.", true);
                writeDefaultIfMissing(
                        file, "mining.hardened.blockPistonMove", "Cancel pistons pushing hardened blocks or cave-in ores.", true);
                if (!file.contains("mining.hardened.budgets")) {
                    file.setComment(
                            "mining.hardened.budgets",
                            "Expected hardened-stone lifetime without Unbreaking. Extra durability per break is round(maxDurability / N) - 1 on top of vanilla's 1; Unbreaking can skip that extra damage. copper@3 iron@7 diamond@128 netherite@512.");
                    file.set("mining.hardened.budgets", new ArrayList<>(HardenedBudget.DEFAULT_ENTRIES));
                }
                writeDefaultIfMissing(
                        file,
                        "torches.rainBreaksTorches",
                        "Rain drops exposed torches as items. Covered torches survive.",
                        true);
                writeDefaultIfMissing(
                        file,
                        "torches.burnDays",
                        "Minecraft days (24000 ticks) a newly placed torch or campfire lasts before it disappears. Copper torches last twice as long. Torches that pull coal or charcoal from a chest within 16 blocks become permanent. Campfires pull one log from a chest within 12 blocks to add another period. 0 = permanent. Lights with no recorded place time (older worlds, worldgen) never burn out. Default 7.",
                        TorchLifetimeRules.DEFAULT_DAYS);
                writeDefaultIfMissing(
                        file, "campfires.rainExtinguishes", "Optional. Same rain pass as torches; default off.", false);
                writeDefaultIfMissing(file, "sounds.torchFizz", "Lava fizz when torch placement is denied.", true);
                writeDefaultIfMissing(
                        file,
                        "worldRules.netherrackFirePercent",
                        "Chance that breaking netherrack places fire in the empty space. Nylium not included.",
                        20);
                PlayerSettings.writeDefaults(file);
                writeFarmingDefaults(file);
                writeDefaultIfMissing(
                        file,
                        "mining.caveIns.enable",
                        "Mining cave-in ores softens 6 neighbors (stone->cobble, deepslate->cobbled deepslate).",
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
                        "Extra falling blocks (#tougher:extra_falling) drop when unsupported. v1 hooks: player break, BlockItem place, EHM land cascade. Piston/other non-player support removal is not scanned.",
                        true);
                writeDefaultIfMissing(
                        file, "falling.breakTorches", "Implemented, default off (upstream buggy).", false);
                if (!file.contains("falling.damage")) {
                    file.setComment(
                            "falling.damage",
                            "KD-18: restore docs-era falling-block player damage. Gated to extra_falling union EHM_OURS.");
                    file.set("falling.damage", 2);
                }
                writeDefaultIfMissing(file, "falling.turnGrassToDirt", "Grass/mycelium/podzol land as dirt.", true);
                writeDefaultIfMissing(
                        file,
                        "falling.cascade",
                        "When an EHM block starts falling (and when it lands), adjacent #extra_falling blocks that are now unsupported are queued. Chains across ticks.",
                        true);
                writeDefaultIfMissing(
                        file, "falling.dropAsItemWhenBlocked", "Drop an item when a falling block cannot place.", false);
                writeDefaultIfMissing(
                        file, "skeletons.snowballEnable", "Bogged share this table; strays/wither skeletons do not.", true);
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
                writeDefaultIfMissing(file, "skeletons.deflectArrowsPercent", "Arrows pass through Skeleton/Bogged.", 75);
                writeDefaultIfMissing(file, "silverfish.cantEnterBlocks", "Block merge-into-stone.", true);
                writeDefaultIfMissing(file, "silverfish.dropCobble", true);
                writeDefaultIfMissing(
                        file, "silverfish.visibilityParticles", "Portal particles so floor-glitched silverfish stay visible.", true);
                writeDefaultIfMissing(
                        file,
                        "endermen.teleportPlayers",
                        "In a fight, an enderman may teleport the player onto it (2-high roof cheese).",
                        true);
                writeDefaultIfMissing(
                        file,
                        "witches.additionalAttacks",
                        "30/30/30/10 splash: baby zombie, teleport, explosion+3 generic, vanilla poison (non-players intensity 0).",
                        true);
                writeDefaultIfMissing(
                        file, "witches.bonusSpawnPercent", "NATURAL overworld zombies on grass_block become witches.", 5);
                writeDefaultIfMissing(
                        file,
                        "horses.blockChest.enable",
                        "Block chested-horse inventory below blockChestBelowY. Disable with this boolean, not Y=0.",
                        true);
                if (!file.contains("horses.blockChestBelowY")) {
                    file.setComment("horses.blockChestBelowY", "original: 55; cave band. Integer.MIN_VALUE also disables.");
                    file.set("horses.blockChestBelowY", 48);
                }
                writeDefaultIfMissing(
                        file,
                        "replacements.killerBunnyPercent",
                        "NATURAL rabbits become killer bunnies. RootNode KillerBunny.Bonus Spawn Percent: 1.",
                        1);
                writeDefaultIfMissing(
                        file,
                        "replacements.vindicatorPercent",
                        "NATURAL skeletons in #tougher:vindicator_replace (dark_forest). RootNode 20.",
                        20);
                writeDefaultIfMissing(
                        file,
                        "replacements.caveSpiderPercent",
                        "NATURAL spiders in #tougher:cave_spider_replace (swamp, mangrove_swamp). RootNode 5.",
                        5);
                writeDefaultIfMissing(
                        file,
                        "replacements.guardianPercent",
                        "NATURAL squid in #tougher:guardian_replace (#minecraft:is_ocean). RootNode 20 (docs 10).",
                        20);
                writeDefaultIfMissing(
                        file,
                        "replacements.vexPercent",
                        "NATURAL bats become vexes. Deep Dark skipped by SpawnReplaceService. RootNode 5.",
                        5);
                writeDefaultIfMissing(
                        file,
                        "monsters.inhibitGrinders",
                        "No drops/XP for grinders: unnatural floor, too much env damage, no path to player, water.",
                        true);
                writeDefaultIfMissing(
                        file, "monsters.more.enable", "Pack-size multiplier below maxY. Disable with this boolean, not Y=0.", true);
                if (!file.contains("monsters.more.maxY")) {
                    file.setComment("monsters.more.maxY", "original: 55; cave band");
                    file.set("monsters.more.maxY", 48);
                }
                if (!file.contains("monsters.more.multiplier")) {
                    file.setComment("monsters.more.multiplier", "Pack size only; does not raise the vanilla monster cap.");
                    file.set("monsters.more.multiplier", 3);
                }
                writeDefaultIfMissing(
                        file,
                        "monsters.spawnInLight.enable",
                        "Spawn extra monsters in light<=maxLight below maxY. Disable with this boolean, not Y=0.",
                        true);
                if (!file.contains("monsters.spawnInLight.maxY")) {
                    file.setComment("monsters.spawnInLight.maxY", "original RootNode 50; cave band");
                    file.set("monsters.spawnInLight.maxY", 48);
                }
                if (!file.contains("monsters.spawnInLight.maxLight")) {
                    file.setComment("monsters.spawnInLight.maxLight", "0-3 bats, 0-7 vanilla, 8-11 hostile no burn, 12+ burn");
                    file.set("monsters.spawnInLight.maxLight", 11);
                }
                if (!file.contains("monsters.spawnInLight.percent")) {
                    file.setComment("monsters.spawnInLight.percent", "Chance per attempt at previously visited cave sections.");
                    file.set("monsters.spawnInLight.percent", 100);
                }
                config.explosions.writeDefaults(file);
                config.monsters.writeDefaults(file);
                writeDefaultIfMissing(
                        file,
                        "blazes.nearBedrock.enable",
                        "Replace NATURAL overworld skeletons near bedrock with blazes. Disable with this boolean, not Y=-64.",
                        true);
                writeDefaultIfMissing(file, "blazes.nearBedrockPercent", "Chance a near-bedrock skeleton becomes a blaze.", 50);
                if (!file.contains("blazes.nearBedrockMaxY")) {
                    file.setComment("blazes.nearBedrockMaxY", "original: ~Y=0. Integer.MIN_VALUE also disables.");
                    file.set("blazes.nearBedrockMaxY", -56);
                }
                writeDefaultIfMissing(file, "blazes.blockOverworldDrops", "No blaze rods from overworld blazes.", true);
                writeDefaultIfMissing(
                        file, "blazes.bonusNetherPercent", "Replace NATURAL nether zombified piglins outside fortresses with blazes.", 20);
                writeDefaultIfMissing(file, "blazes.dropFireOnDamage", "Blazes above half health place fire when hit.", true);
                writeDefaultIfMissing(file, "blazes.bonusLoot", "Extra gunpowder + blaze rod from nether blazes.", true);
                writeDefaultIfMissing(file, "blazes.netherSplitPercent", "Nether blazes may split into two full-HP blazes on death.", 25);
                writeDefaultIfMissing(
                        file,
                        "magmaCubes.spawnWithNetherBlazePercent",
                        "When a nether piglin is replaced with a blaze, also spawn a size-1 magma cube.",
                        100);
                writeDefaultIfMissing(
                        file, "magmaCubes.growIntoBlazesOnDamage", "On damage, magma cubes explode (MAGMACUBE_FIRE) and become blazes.", true);
                writeDefaultIfMissing(file, "pigmen.alwaysAngry", "Zombified piglins never calm and aggro players.", true);
                writeDefaultIfMissing(file, "pigmen.damagePercent", "Outgoing zombified piglin damage to players. original: 70.", 70);
                writeDefaultIfMissing(file, "pigmen.fortressNetherwart", "Always drop nether wart when slain in a fortress.", true);
                writeDefaultIfMissing(
                        file, "pigmen.elsewhereNetherwartPercent", "Chance to drop nether wart elsewhere in the Nether.", 25);
                writeDefaultIfMissing(file, "pigmen.lightningSpawns", "Overworld lightning spawns 1-3 angry zombified piglins.", true);
                writeDefaultIfMissing(file, "ghasts.arrowDamagePercent", "Incoming arrow damage to ghasts.", 20);
                writeDefaultIfMissing(file, "ghasts.expMultiplier", "Ghast XP multiplier.", 10);
                writeDefaultIfMissing(file, "ghasts.dropsMultiplier", "Ghast drop-count multiplier.", 5);
                config.dragon.writeDefaults(file);
                writeBossDefaults(file);
                writeDefaultIfMissing(
                        file, "sounds.creeperTntWarning", "Ghast warn before a creeper drops primed TNT.", true);
                if (!file.contains("modules")) {
                    file.setComment(
                            "modules",
                            "Runtime per-module toggles for this dimension. Missing keys default true.");
                }
                writeDefaultIfMissing(
                        file,
                        "modules.tougher.villager_nerf",
                        "Optional diamond-gear / novice-Mending trade nerf. Not original EHM; default true.",
                        true);
                writeDefaultIfMissing(
                        file,
                        "villagerNerf.blockDiamondGearTrades",
                        "When villager_nerf is on, remove trades whose result is diamond/netherite sword, tools, or armor.",
                        true);
                writeDefaultIfMissing(
                        file,
                        "villagerNerf.nerfNoviceMending",
                        "When villager_nerf is on, strip Mending from novice-apprentice librarians. Master may sell at 2x emeralds.",
                        true);
                writeDefaultIfMissing(
                        file,
                        "modules.tougher.inhabitants",
                        "Homes can attract one traveler. Default true.",
                        true);
                writeInhabitantDefaults(file);
                migrate(file);
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
                            "Per-dimension opt-out. Default true. The gamerule tougher:enabled is the server-wide master switch.");
                }
                file.set("enabled", enabled);
                file.set("bypassing.checkPermission", checkPermission);
                file.set("bypassing.creativeBypasses", creativeBypasses);
                file.set("bypassing.operatorsBypass", operatorsBypass);
                file.set("worldRules.limitedBlockPlacement", limitedBuilding);
                file.set("worldRules.betterTreeFelling", betterTreeFelling);
                file.set("client.f3Enabled", f3Enabled);
                file.set("achievements.competitive", competitiveAchievements);
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
                dragon.write(file);
                file.set("bosses.minDistanceFromSpawn", bossMinDistanceFromSpawn);
                file.set("bosses.cooldownHours", bossCooldownHours);
                file.set("bosses.spawnChancePercent", bossSpawnChancePercent);
                file.set("bosses.checkIntervalTicks", bossCheckIntervalTicks);
                file.set("bosses.scale", bossScale);
                file.set("bosses.healthMultiplierMin", bossHealthMultiplierMin);
                file.set("bosses.healthMultiplierMax", bossHealthMultiplierMax);
                file.set("bosses.attackMultiplier", bossAttackMultiplier);
                file.set("bosses.heavyArmorDropPercent", bossHeavyArmorDropPercent);
                file.set("bosses.distanceStepBlocks", bossDistanceStepBlocks);
                file.set("bosses.difficultyPercentPerStep", bossDifficultyPercentPerStep);
                file.set("bosses.treasurePercentScale", bossTreasurePercentScale);
                List<String> budgetEntries = new ArrayList<>();
                for (Map.Entry<Identifier, Integer> budget : hardenedBudgets.entrySet()) {
                    budgetEntries.add(budget.getKey().toString() + "@" + budget.getValue());
                }
                if (budgetEntries.isEmpty()) {
                    budgetEntries.addAll(HardenedBudget.DEFAULT_ENTRIES);
                }
                file.set("mining.hardened.budgets", budgetEntries);
                file.set("torches.rainBreaksTorches", rainBreaksTorches);
                file.set("torches.burnDays", torchBurnDays);
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
                file.set("farming.overgrazing.enable", overgrazingEnable);
                file.set("farming.overgrazing.intervalTicks", overgrazingIntervalTicks);
                file.set("farming.overgrazing.largeMarks", overgrazingLargeMarks);
                file.set("farming.overgrazing.smallMarks", overgrazingSmallMarks);
                file.set("farming.overgrazing.lookChance", overgrazingLookChance);
                file.set("farming.overgrazing.chestRange", overgrazingChestRange);
                file.set("farming.cropMatureDurationPercent", cropMatureDurationPercent);
                file.set("farming.animalBreedCooldownMultiplier", animalBreedCooldownMultiplier);
                file.set("farming.eggLayTimeMultiplier", eggLayTimeMultiplier);
                file.set("farming.hiveHoneycombCount", hiveHoneycombCount);
                file.set("hunger.movingExhaustionPerSecond", movingExhaustionPerSecond);
                file.set("hunger.foodHistorySize", foodHistorySize);
                file.set("hunger.slowRegenTicks", slowRegenTicks);
                file.set("hunger.disableFastSaturationRegen", disableFastSaturationRegen);
                file.set("hunger.afkTimeoutSeconds", afkTimeoutSeconds);
                file.set("fish.healthyCount", fishHealthyCount);
                file.set("fish.healthyRatePercent", fishHealthyRatePercent);
                file.set("fish.scarceRatePercent", fishScarceRatePercent);
                file.set("fish.biteWaitMultiplier", fishingWaitMultiplier);
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
                file.set("sounds.creeperTntWarning", creeperTntWarning);
                file.set("villagerNerf.blockDiamondGearTrades", villagerNerfBlockDiamondGear);
                file.set("villagerNerf.nerfNoviceMending", villagerNerfNoviceMending);
                file.set("inhabitants.minVolume", inhabitantMinVolume);
                file.set("inhabitants.maxVolume", inhabitantMaxVolume);
                file.set("inhabitants.minLight", inhabitantMinLight);
                file.set("inhabitants.minScore", inhabitantMinScore);
                file.set("inhabitants.spacing", inhabitantSpacing);
                file.set("inhabitants.baseChancePercent", inhabitantBaseChancePercent);
                file.set("inhabitants.chancePerPoint", inhabitantChancePerPoint);
                file.set("inhabitants.maxChancePercent", inhabitantMaxChancePercent);
                file.set("inhabitants.leaveGraceDays", inhabitantLeaveGraceDays);
                file.set("inhabitants.killCooldownDays", inhabitantKillCooldownDays);
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
        f3Enabled = file.getOrElse("client.f3Enabled", false);
        competitiveAchievements = file.getOrElse("achievements.competitive", true);
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
        dragon.read(file);
        bossMinDistanceFromSpawn = Math.max(0, getInt(file, "bosses.minDistanceFromSpawn", 300));
        bossCooldownHours = Math.max(0, getInt(file, "bosses.cooldownHours", 8));
        bossSpawnChancePercent = percent(getInt(file, "bosses.spawnChancePercent", 1));
        bossCheckIntervalTicks = Math.max(20, getInt(file, "bosses.checkIntervalTicks", 1200));
        bossScale = Math.max(1.0, getDouble(file, "bosses.scale", 1.6));
        bossHealthMultiplierMin = Math.max(1, getInt(file, "bosses.healthMultiplierMin", 6));
        bossHealthMultiplierMax = Math.max(bossHealthMultiplierMin, getInt(file, "bosses.healthMultiplierMax", 12));
        bossAttackMultiplier = Math.max(1.0, getDouble(file, "bosses.attackMultiplier", 1.75));
        bossHeavyArmorDropPercent = percent(getInt(file, "bosses.heavyArmorDropPercent", 25));
        bossDistanceStepBlocks = Math.max(1, getInt(file, "bosses.distanceStepBlocks", 25));
        bossDifficultyPercentPerStep = Math.max(0.0, getDouble(file, "bosses.difficultyPercentPerStep", 1.0));
        bossTreasurePercentScale = Math.max(0.0, getDouble(file, "bosses.treasurePercentScale", 3.0));
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
        torchBurnDays = TorchLifetimeRules.clampDays(getInt(file, "torches.burnDays", TorchLifetimeRules.DEFAULT_DAYS));
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
        noFarmNetherWart = file.getOrElse("farming.noFarmNetherWart", false);
        sheepWhiteWool = file.getOrElse("farming.sheepWhiteWool", false);
        squidOceanOnly = file.getOrElse("farming.squidOceanOnly", true);
        bucketsDontMoveSources = file.getOrElse("farming.bucketsDontMoveSources", true);
        animalXpNerf = file.getOrElse("farming.animalXpNerf", true);
        ironGolemNerf = file.getOrElse("farming.ironGolemNerf", true);
        overcrowdEnable = file.getOrElse("farming.overcrowd.enable", true);
        overcrowdThreshold = file.getOrElse("farming.overcrowd.threshold", 8);
        overgrazingEnable = file.getOrElse("farming.overgrazing.enable", true);
        overgrazingIntervalTicks = OvergrazingRules.clampInterval(
                getInt(file, "farming.overgrazing.intervalTicks", OvergrazingRules.DEFAULT_INTERVAL_TICKS));
        overgrazingLargeMarks = OvergrazingRules.clampMarks(
                getInt(file, "farming.overgrazing.largeMarks", OvergrazingRules.DEFAULT_LARGE_MARKS));
        overgrazingSmallMarks = OvergrazingRules.clampMarks(
                getInt(file, "farming.overgrazing.smallMarks", OvergrazingRules.DEFAULT_SMALL_MARKS));
        overgrazingLookChance =
                clampPercent(getInt(file, "farming.overgrazing.lookChance", OvergrazingRules.LOOK_CHANCE_PERCENT));
        overgrazingChestRange = OvergrazingRules.clampRange(
                getInt(file, "farming.overgrazing.chestRange", OvergrazingRules.DEFAULT_CHEST_RANGE));
        cropMatureDurationPercent = Math.max(1, getInt(file, "farming.cropMatureDurationPercent", 300));
        animalBreedCooldownMultiplier = Math.max(1, getInt(file, "farming.animalBreedCooldownMultiplier", 6));
        eggLayTimeMultiplier = Math.max(1, getInt(file, "farming.eggLayTimeMultiplier", 6));
        hiveHoneycombCount = Math.clamp(getInt(file, "farming.hiveHoneycombCount", 1), 0, 64);
        movingExhaustionPerSecond = (float) getDouble(
                file, "hunger.movingExhaustionPerSecond", HungerRules.DEFAULT_MOVING_EXHAUSTION_PER_SECOND);
        foodHistorySize = Math.max(1, getInt(file, "hunger.foodHistorySize", 7));
        slowRegenTicks = Math.max(1, getInt(file, "hunger.slowRegenTicks", HungerRules.DEFAULT_SLOW_REGEN_TICKS));
        disableFastSaturationRegen = file.getOrElse("hunger.disableFastSaturationRegen", true);
        afkTimeoutSeconds = Math.max(0, getInt(file, "hunger.afkTimeoutSeconds", 30));
        fishHealthyCount = Math.max(0, getInt(file, "fish.healthyCount", 3));
        fishHealthyRatePercent = percent(getInt(file, "fish.healthyRatePercent", 33));
        fishScarceRatePercent = percent(getInt(file, "fish.scarceRatePercent", 5));
        fishingWaitMultiplier = Math.max(1, getInt(file, "fish.biteWaitMultiplier", 3));
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
        skeletonDeflectArrowsPercent = percent(getInt(file, "skeletons.deflectArrowsPercent", 75));
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
        moreMonstersMultiplier = getInt(file, "monsters.more.multiplier", 3);
        spawnInLightEnable = file.getOrElse("monsters.spawnInLight.enable", true);
        spawnInLightMaxY = getInt(file, "monsters.spawnInLight.maxY", 48);
        spawnInLightMaxLight = getInt(file, "monsters.spawnInLight.maxLight", 11);
        spawnInLightPercent = getInt(file, "monsters.spawnInLight.percent", 100);
        creeperTntWarning = file.getOrElse("sounds.creeperTntWarning", true);
        villagerNerfBlockDiamondGear = file.getOrElse("villagerNerf.blockDiamondGearTrades", true);
        villagerNerfNoviceMending = file.getOrElse("villagerNerf.nerfNoviceMending", true);
        inhabitantMinVolume = Math.max(1, getInt(file, "inhabitants.minVolume", 24));
        inhabitantMaxVolume = Math.max(inhabitantMinVolume, getInt(file, "inhabitants.maxVolume", 300));
        inhabitantMinLight = Math.clamp(getInt(file, "inhabitants.minLight", 8), 0, 15);
        inhabitantMinScore = Math.max(0, getInt(file, "inhabitants.minScore", 12));
        inhabitantSpacing = Math.max(0, getInt(file, "inhabitants.spacing", 48));
        inhabitantBaseChancePercent = percent(getInt(file, "inhabitants.baseChancePercent", 8));
        inhabitantChancePerPoint = Math.max(0, getInt(file, "inhabitants.chancePerPoint", 2));
        inhabitantMaxChancePercent = percent(getInt(file, "inhabitants.maxChancePercent", 40));
        inhabitantLeaveGraceDays = Math.max(0, getInt(file, "inhabitants.leaveGraceDays", 3));
        inhabitantKillCooldownDays = Math.max(0, getInt(file, "inhabitants.killCooldownDays", 7));
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
        writeDefaultIfMissing(
                file, "farming.infertileDeserts", "Desert biomes add +50% crop death; trees/mushrooms will not grow.", true);
        writeDefaultIfMissing(file, "farming.snowBreaksCrops", "Snow-covered crops die on random tick.", true);
        writeDefaultIfMissing(file, "farming.cantCraftMelonSeeds", "Melon and pumpkin seed recipes are uncraftable.", true);
        writeDefaultIfMissing(file, "farming.noBonemealOnMushrooms", "Bone meal does not grow mushrooms.", true);
        writeDefaultIfMissing(
                file,
                "farming.noFarmNetherWart",
                "If true, cannot place nether wart and breaking always drops exactly 1. Default false: wart is farmable at 1/20 vanilla growth.",
                false);
        writeDefaultIfMissing(file, "farming.sheepWhiteWool", "Sheep regrow and breed white. Dyeing is one-shot.", false);
        writeDefaultIfMissing(
                file, "farming.squidOceanOnly", "Natural squid only spawn in #minecraft:is_ocean. Glow squid unchanged.", true);
        writeDefaultIfMissing(
                file,
                "farming.bucketsDontMoveSources",
                "Buckets place flowing water LEVEL=1 (not a source). Does not disable canConvertToSource globally.",
                true);
        writeDefaultIfMissing(file, "farming.animalXpNerf", "Animals drop no experience.", true);
        writeDefaultIfMissing(file, "farming.ironGolemNerf", "Iron golems drop nothing (anti iron farm).", true);
        writeDefaultIfMissing(
                file,
                "farming.overcrowd.enable",
                "Animals and villagers over the threshold in a 3x3x3 take damage. Villagers count villagers; animals count animals.",
                true);
        writeDefaultIfMissing(
                file, "farming.overcrowd.threshold", "Maximum crowding animals or villagers in 3x3x3 before damage. Default 8.", 8);
        writeDefaultIfMissing(
                file,
                "farming.overgrazing.enable",
                "Livestock graze by claiming nearby grass once per interval. If they cannot claim enough unmarked pathable grass, they may raid chests or starve.",
                true);
        writeDefaultIfMissing(
                file,
                "farming.overgrazing.intervalTicks",
                "Ticks between overgrazing checks per animal. 24000 is one Minecraft day. Each animal caches its next check time.",
                OvergrazingRules.DEFAULT_INTERVAL_TICKS);
        writeDefaultIfMissing(
                file,
                "farming.overgrazing.largeMarks",
                "Grass blocks a large animal (cow, pig, sheep, and similar) must claim each check. Default 9.",
                OvergrazingRules.DEFAULT_LARGE_MARKS);
        writeDefaultIfMissing(
                file,
                "farming.overgrazing.smallMarks",
                "Grass blocks a small animal (chicken or rabbit) must claim each check. Default 4.",
                OvergrazingRules.DEFAULT_SMALL_MARKS);
        writeDefaultIfMissing(
                file,
                "farming.overgrazing.lookChance",
                "Percent chance an animal that cannot claim enough grass looks in chests (then may starve). Default 33.",
                OvergrazingRules.LOOK_CHANCE_PERCENT);
        writeDefaultIfMissing(
                file,
                "farming.overgrazing.chestRange",
                "Blocks around an overgrazing animal to search for pathable chests or barrels. Default 20.",
                OvergrazingRules.DEFAULT_CHEST_RANGE);
        writeDefaultIfMissing(
                file,
                "farming.cropMatureDurationPercent",
                "How long crops take to mature. 100 is vanilla, 200 is twice as long, 50 is twice as fast. Default 300.",
                300);
        writeDefaultIfMissing(
                file,
                "farming.animalBreedCooldownMultiplier",
                "Adult animals wait this many times the vanilla 5-minute cooldown before they can breed again. Default 6 (30 minutes). 1 is vanilla.",
                6);
        writeDefaultIfMissing(
                file,
                "farming.eggLayTimeMultiplier",
                "Chickens take this many times as long to lay eggs. Vanilla is 5–10 minutes; default 6 (30–60 minutes). 1 is vanilla.",
                6);
        writeDefaultIfMissing(
                file,
                "farming.hiveHoneycombCount",
                "Honeycombs dropped when shearing a full hive or nest. Vanilla is 3; default 1.",
                1);
        writeDefaultIfMissing(
                file,
                "hunger.movingExhaustionPerSecond",
                "Extra exhaustion per second while the player is doing anything. Suspended only after afkTimeoutSeconds with no activity. Default 0.1333 empties a full bar with no saturation in 10 minutes.",
                HungerRules.DEFAULT_MOVING_EXHAUSTION_PER_SECOND);
        writeDefaultIfMissing(file, "hunger.foodHistorySize", "Last N distinct-slot foods. A food not in that list restores +1 hunger, or +1 saturation if the bar is full.", 7);
        writeDefaultIfMissing(
                file,
                "hunger.slowRegenTicks",
                "Ticks per 1 HP of food regen after fast saturation regen is disabled. 300 = 15 seconds. Configurable; vanilla slow regen is 80 ticks (4 seconds).",
                HungerRules.DEFAULT_SLOW_REGEN_TICKS);
        writeDefaultIfMissing(
                file,
                "hunger.disableFastSaturationRegen",
                "Disable vanilla 0.5s saturation-funded healing. Slow food regen uses slowRegenTicks.",
                true);
        writeDefaultIfMissing(
                file,
                "hunger.afkTimeoutSeconds",
                "Seconds without movement, look, keys, or actions before extra hunger drain pauses. 0 = never AFK.",
                30);
        writeDefaultIfMissing(
                file, "fish.healthyCount", "Same-type fish in the chunk at or above this use healthyRatePercent.", 3);
        writeDefaultIfMissing(
                file, "fish.healthyRatePercent", "Natural spawn chance when the chunk has a healthy school. 33 ≈ one third.", 33);
        writeDefaultIfMissing(
                file, "fish.scarceRatePercent", "Natural spawn chance when the chunk has fewer than healthyCount. 5 = 1/20.", 5);
        writeDefaultIfMissing(
                file, "fish.biteWaitMultiplier", "Fishing rod time-until-lured multiplier. 3 ≈ three times longer between bites.", 3);
    }

    private static void writeInhabitantDefaults(CommentedFileConfig file) {
        writeDefaultIfMissing(
                file,
                "inhabitants.minVolume",
                "Interior air blocks required for a home. Smaller rooms do not qualify.",
                24);
        writeDefaultIfMissing(
                file, "inhabitants.maxVolume", "Interior air blocks above this are too large for one resident.", 300);
        writeDefaultIfMissing(file, "inhabitants.minLight", "Minimum block light on interior floor air.", 8);
        writeDefaultIfMissing(file, "inhabitants.minScore", "Amenity points required to attract a resident.", 12);
        writeDefaultIfMissing(
                file, "inhabitants.spacing", "Horizontal blocks between occupied home beds.", 48);
        writeDefaultIfMissing(
                file, "inhabitants.baseChancePercent", "Dawn spawn chance at the minimum score.", 8);
        writeDefaultIfMissing(
                file, "inhabitants.chancePerPoint", "Added dawn chance percent per amenity point over minScore.", 2);
        writeDefaultIfMissing(file, "inhabitants.maxChancePercent", "Cap on dawn spawn chance.", 40);
        writeDefaultIfMissing(
                file, "inhabitants.leaveGraceDays", "Days a broken home stays occupied before the resident leaves.", 3);
        writeDefaultIfMissing(
                file,
                "inhabitants.killCooldownDays",
                "Days after a resident is slain before that home can attract another.",
                7);
    }

    private static void writeBossDefaults(CommentedFileConfig file) {
        writeDefaultIfMissing(
                file,
                "bosses.minDistanceFromSpawn",
                "Horizontal cubes from world spawn (or 0,0 in other dimensions) before a biome boss can appear.",
                300);
        writeDefaultIfMissing(
                file,
                "bosses.cooldownHours",
                "Wall-clock hours after a family spawns before that family can spawn again. Living bosses still block a second spawn.",
                8);
        writeDefaultIfMissing(
                file,
                "bosses.spawnChancePercent",
                "Chance per eligible player check that a family boss appears. 1% every minute by default.",
                1);
        writeDefaultIfMissing(
                file,
                "bosses.checkIntervalTicks",
                "Ticks between spawn checks. 1200 = 60 seconds.",
                1200);
        writeDefaultIfMissing(file, "bosses.scale", "generic.scale applied to biome bosses. 1.6 is bigger than vanilla.", 1.6);
        writeDefaultIfMissing(
                file,
                "bosses.healthMultiplierMin",
                "Minimum max-health multiplier rolled per boss. Inclusive with healthMultiplierMax. Default 6.",
                6);
        writeDefaultIfMissing(
                file,
                "bosses.healthMultiplierMax",
                "Maximum max-health multiplier rolled per boss. Inclusive. Default 12.",
                12);
        writeDefaultIfMissing(
                file, "bosses.attackMultiplier", "Attack-damage multiplier for biome bosses.", 1.75);
        writeDefaultIfMissing(
                file,
                "bosses.heavyArmorDropPercent",
                "Chance a slain biome boss drops one random heavy iron or heavy diamond armor piece.",
                25);
        writeDefaultIfMissing(
                file,
                "bosses.distanceStepBlocks",
                "Horizontal blocks past minDistanceFromSpawn per difficulty step. Default 25.",
                25);
        writeDefaultIfMissing(
                file,
                "bosses.difficultyPercentPerStep",
                "Health, damage, and speed increase this many percent per distance step. Default 1.",
                1.0);
        writeDefaultIfMissing(
                file,
                "bosses.treasurePercentScale",
                "Treasure stack sizes and drop chances rise this many times the difficulty percent. Default 3.",
                3.0);
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

    private static void writeDefaultIfMissing(CommentedFileConfig file, String path, String comment, int value) {
        if (!file.contains(path)) {
            if (comment != null) {
                file.setComment(path, comment);
            }
            file.set(path, value);
        }
    }

    private static void writeDefaultIfMissing(CommentedFileConfig file, String path, int value) {
        writeDefaultIfMissing(file, path, null, value);
    }

    private static void writeDefaultIfMissing(CommentedFileConfig file, String path, String comment, double value) {
        if (!file.contains(path)) {
            if (comment != null) {
                file.setComment(path, comment);
            }
            file.set(path, value);
        }
    }

    /**
     * Schema 2: egg lay default 4× → 2× (double the previous Tougher rate).
     * Schema 3: diamond armor slowdown default 40% → 0. Only rewrites a stored 40
     * so a custom value is kept.
     * Schema 4: nether wart farming default true (blocked) → false (growable at 1/20).
     * Schema 5: hunger drain 0.175/s → 0.1333/s (full bar, no saturation, 10 minutes).
     * Schema 6: food regen 600 ticks (30s) → 300 ticks (15s per 1 HP).
     */
    private static void migrate(CommentedFileConfig file) {
        int version = getInt(file, "configVersion", 1);
        if (version >= CONFIG_VERSION) {
            return;
        }
        if (version < 2 && getInt(file, "farming.eggLayTimeMultiplier", 2) == 4) {
            file.set("farming.eggLayTimeMultiplier", 2);
        }
        if (version < 3 && getInt(file, "player.armor.fullDiamondSlowdownPercent", 0) == 40) {
            file.set("player.armor.fullDiamondSlowdownPercent", 0);
        }
        if (version < 4 && file.getOrElse("farming.noFarmNetherWart", true)) {
            file.set("farming.noFarmNetherWart", false);
        }
        if (version < 5
                && Math.abs(getDouble(file, "hunger.movingExhaustionPerSecond", 0.175) - 0.175) < 1.0e-6) {
            file.set("hunger.movingExhaustionPerSecond", (double) HungerRules.DEFAULT_MOVING_EXHAUSTION_PER_SECOND);
        }
        if (version < 6 && getInt(file, "hunger.slowRegenTicks", 600) == 600) {
            file.set("hunger.slowRegenTicks", HungerRules.DEFAULT_SLOW_REGEN_TICKS);
        }
        file.set("configVersion", CONFIG_VERSION);
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

    private static int percentOr(CommentedFileConfig file, String path, int fallback) {
        Object raw = file.get(path);
        if (raw instanceof Number number) {
            return clampPercent(number.intValue());
        }
        return fallback;
    }

    static int clampPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }
}

package dev.extrahardmode.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import dev.extrahardmode.feature.ArmorSpeedRules;
import dev.extrahardmode.feature.LivesRules;
import dev.extrahardmode.feature.ShieldRules;

/** Per-dimension player death / environment / weight / armor settings. RootNode defaults. */
public final class PlayerSettings {
    public static final PotionEffectHolder FALL_EFFECT = new PotionEffectHolder("minecraft:slowness", 80, 2);
    public static final PotionEffectHolder EXPLOSION_EFFECT = new PotionEffectHolder("minecraft:nausea", 300, 3);
    public static final PotionEffectHolder BURN_EFFECT = new PotionEffectHolder("minecraft:blindness", 20, 1);

    private boolean environmentEnable = true;
    private double fallMultiplier = 2.0;
    private PotionEffectHolder fallEffect = FALL_EFFECT;
    private double explosionMultiplier = 1.0;
    private PotionEffectHolder explosionEffect = EXPLOSION_EFFECT;
    private double suffocationMultiplier = 5.0;
    private PotionEffectHolder suffocationEffect = PotionEffectHolder.NONE;
    private double lavaMultiplier = 2.0;
    private PotionEffectHolder lavaEffect = PotionEffectHolder.NONE;
    private double burnMultiplier = 1.0;
    private PotionEffectHolder burnEffect = BURN_EFFECT;
    private double starvationMultiplier = 2.0;
    private PotionEffectHolder starvationEffect = PotionEffectHolder.NONE;
    private double drownMultiplier = 2.0;
    private PotionEffectHolder drownEffect = PotionEffectHolder.NONE;

    private boolean extinguishIgnites = true;
    private int extinguishBurnTicks = 80;

    private boolean forfeitEnable = false;
    private int forfeitPercent = 0;
    private int toolDamagePercent = 0;
    private boolean keepHeavilyDamagedTools = true;
    private boolean respawnHealthEnable = true;
    private int respawnHealthPercent = 25;
    private int respawnFood = 6;
    private int lives = LivesRules.DEFAULT_LIVES;
    private boolean restoreLifeAvailable = true;

    private boolean weightEnable = true;
    private boolean blockWaterfalls = true;
    private double maxPoints = 18.0;
    private double armorPiece = 2.0;
    private double stack = 1.0;
    private double tool = 0.5;
    private int drownRate = 35;
    private int overencumbranceAdds = 2;

    private boolean armorEnable = true;
    private double baseSpeed = 0.22;
    private int copperIronGoldSlowdownPercent = ArmorSpeedRules.DEFAULT_COPPER_IRON_GOLD_PERCENT;
    private int diamondSlowdownPercent = ArmorSpeedRules.DEFAULT_DIAMOND_PERCENT;
    private int leatherChainmailSlowdownPercent = ArmorSpeedRules.DEFAULT_LEATHER_CHAINMAIL_PERCENT;
    private int heavyExtraSlowdownPercent = ArmorSpeedRules.DEFAULT_HEAVY_EXTRA_PERCENT;
    private int fullDiamondSlowdownPercent = 0;
    private int shieldAbsorbPercent = ShieldRules.DEFAULT_ABSORB_PERCENT;
    private int shieldDurabilityMultiplier = ShieldRules.DEFAULT_DURABILITY_MULTIPLIER;

    public boolean environmentEnable() {
        return environmentEnable;
    }

    public double fallMultiplier() {
        return fallMultiplier;
    }

    public PotionEffectHolder fallEffect() {
        return fallEffect;
    }

    public double explosionMultiplier() {
        return explosionMultiplier;
    }

    public PotionEffectHolder explosionEffect() {
        return explosionEffect;
    }

    public double suffocationMultiplier() {
        return suffocationMultiplier;
    }

    public PotionEffectHolder suffocationEffect() {
        return suffocationEffect;
    }

    public double lavaMultiplier() {
        return lavaMultiplier;
    }

    public PotionEffectHolder lavaEffect() {
        return lavaEffect;
    }

    public double burnMultiplier() {
        return burnMultiplier;
    }

    public PotionEffectHolder burnEffect() {
        return burnEffect;
    }

    public double starvationMultiplier() {
        return starvationMultiplier;
    }

    public PotionEffectHolder starvationEffect() {
        return starvationEffect;
    }

    public double drownMultiplier() {
        return drownMultiplier;
    }

    public PotionEffectHolder drownEffect() {
        return drownEffect;
    }

    public boolean extinguishIgnites() {
        return extinguishIgnites;
    }

    public int extinguishBurnTicks() {
        return extinguishBurnTicks;
    }

    public boolean forfeitEnable() {
        return forfeitEnable;
    }

    public int forfeitPercent() {
        return forfeitPercent;
    }

    public int toolDamagePercent() {
        return toolDamagePercent;
    }

    public boolean keepHeavilyDamagedTools() {
        return keepHeavilyDamagedTools;
    }

    public boolean respawnHealthEnable() {
        return respawnHealthEnable;
    }

    public int respawnHealthPercent() {
        return respawnHealthPercent;
    }

    public int respawnFood() {
        return respawnFood;
    }

    /** Starting lives. Below 0 is off. 0 or above is on. The overworld file is the value the server uses. */
    public int lives() {
        return lives;
    }

    /** Restore Life Available? Applies only while lives are on. The overworld file is the value the server uses. */
    public boolean restoreLifeAvailable() {
        return restoreLifeAvailable;
    }

    public boolean weightEnable() {
        return weightEnable;
    }

    public boolean blockWaterfalls() {
        return blockWaterfalls;
    }

    public double maxPoints() {
        return maxPoints;
    }

    public double armorPiece() {
        return armorPiece;
    }

    public double stack() {
        return stack;
    }

    public double tool() {
        return tool;
    }

    public int drownRate() {
        return drownRate;
    }

    public int overencumbranceAdds() {
        return overencumbranceAdds;
    }

    public boolean armorEnable() {
        return armorEnable;
    }

    public double baseSpeed() {
        return baseSpeed;
    }

    public int copperIronGoldSlowdownPercent() {
        return copperIronGoldSlowdownPercent;
    }

    public int diamondSlowdownPercent() {
        return diamondSlowdownPercent;
    }

    public int leatherChainmailSlowdownPercent() {
        return leatherChainmailSlowdownPercent;
    }

    public int heavyExtraSlowdownPercent() {
        return heavyExtraSlowdownPercent;
    }

    /** Kept so an older world file still stores the value. The material percents are what apply. */
    public int fullDiamondSlowdownPercent() {
        return fullDiamondSlowdownPercent;
    }

    public int shieldAbsorbPercent() {
        return shieldAbsorbPercent;
    }

    public int shieldDurabilityMultiplier() {
        return shieldDurabilityMultiplier;
    }

    static void writeDefaults(CommentedFileConfig file) {
        comment(
                file,
                "player.environment.enable",
                "Extra damage multipliers and potion effects for environmental injuries.");
        setIfMissing(file, "player.environment.enable", true);
        setIfMissing(file, "player.environment.fallMultiplier", 2.0);
        PotionEffectHolder.writeDefaultIfMissing(file, "player.environment.fallEffect", FALL_EFFECT);
        setIfMissing(file, "player.environment.explosionMultiplier", 1.0);
        PotionEffectHolder.writeDefaultIfMissing(file, "player.environment.explosionEffect", EXPLOSION_EFFECT);
        setIfMissing(file, "player.environment.suffocationMultiplier", 5.0);
        PotionEffectHolder.writeDefaultIfMissing(file, "player.environment.suffocationEffect", PotionEffectHolder.NONE);
        setIfMissing(file, "player.environment.lavaMultiplier", 2.0);
        PotionEffectHolder.writeDefaultIfMissing(file, "player.environment.lavaEffect", PotionEffectHolder.NONE);
        setIfMissing(file, "player.environment.burnMultiplier", 1.0);
        PotionEffectHolder.writeDefaultIfMissing(file, "player.environment.burnEffect", BURN_EFFECT);
        setIfMissing(file, "player.environment.starvationMultiplier", 2.0);
        PotionEffectHolder.writeDefaultIfMissing(file, "player.environment.starvationEffect", PotionEffectHolder.NONE);
        setIfMissing(file, "player.environment.drownMultiplier", 2.0);
        PotionEffectHolder.writeDefaultIfMissing(file, "player.environment.drownEffect", PotionEffectHolder.NONE);

        file.setComment(
                "player.extinguishIgnites",
                "Punching fire, smothering it with a block, or breaking a block that is on fire ignites the player. Water buckets still work.");
        setIfMissing(file, "player.extinguishIgnites", true);
        setIfMissing(file, "player.extinguishBurnTicks", 80);

        comment(
                file,
                "player.death.forfeitEnable",
                "Forfeit a percent of item stacks on death. Still runs when keepInventory is true; remaining stacks still drop otherwise. Tag tougher:death_item_blacklist is empty by default; datapack in minecraft:recovery_compass and minecraft:totem_of_undying to keep those.");
        setIfMissing(file, "player.death.forfeitEnable", false);
        setIfMissing(file, "player.death.forfeitPercent", 0);
        setIfMissing(file, "player.death.toolDamagePercent", 0);
        setIfMissing(file, "player.death.keepHeavilyDamagedTools", true);
        setIfMissing(file, "player.death.respawnHealthEnable", true);
        setIfMissing(file, "player.death.respawnHealthPercent", 25);
        setIfMissing(file, "player.death.respawnFood", 6);
        comment(
                file,
                "player.death.lives",
                "Lives each player starts with. -1 is off. 0 or above is on. Each death spends one, and the count can go below 0. Below 0, base max health is 20 plus 2 per life, and at least 6. At 0 lives, base health stays 20. The overworld file is the value the server uses. On death, a count above -1 says how many lives remain. Below 0 it says you are down that many hearts.");
        setIfMissing(file, "player.death.lives", LivesRules.DEFAULT_LIVES);
        comment(
                file,
                "player.death.restoreLifeAvailable",
                "Restore Life Available? When yes, and lives are on, the Restore Life ability can give one life back. The target must have fewer lives than player.death.lives. Defaults to yes. The overworld file is the value the server uses.");
        setIfMissing(file, "player.death.restoreLifeAvailable", true);

        comment(
                file,
                "player.weight.enable",
                "No swimming when inventory weight exceeds maxPoints. Checked every 20 ticks.");
        setIfMissing(file, "player.weight.enable", true);
        setIfMissing(file, "player.weight.blockWaterfalls", true);
        setIfMissing(file, "player.weight.maxPoints", 18.0);
        setIfMissing(file, "player.weight.armorPiece", 2.0);
        setIfMissing(file, "player.weight.stack", 1.0);
        setIfMissing(file, "player.weight.tool", 0.5);
        setIfMissing(file, "player.weight.drownRate", 35);
        setIfMissing(file, "player.weight.overencumbranceAdds", 2);

        file.setComment(
                "player.armor.enable",
                "Movement speed. Unarmored walk speed is baseSpeed (default 0.22, 10% faster than vanilla 0.2). Worn armor slows that pace by material.");
        setIfMissing(file, "player.armor.enable", true);
        setIfMissing(file, "player.armor.baseSpeed", 0.22);
        comment(
                file,
                "player.armor.copperIronGoldSlowdownPercent",
                "Percent a full set of copper, iron, or gold armor slows the unarmored pace. Each worn piece applies one quarter. Default 10.");
        setIfMissing(
                file,
                "player.armor.copperIronGoldSlowdownPercent",
                ArmorSpeedRules.DEFAULT_COPPER_IRON_GOLD_PERCENT);
        comment(
                file,
                "player.armor.diamondSlowdownPercent",
                "Percent a full set of diamond or netherite armor slows the unarmored pace. Each worn piece applies one quarter. Default 5.");
        setIfMissing(file, "player.armor.diamondSlowdownPercent", ArmorSpeedRules.DEFAULT_DIAMOND_PERCENT);
        comment(
                file,
                "player.armor.leatherChainmailSlowdownPercent",
                "Percent a full set of leather or chainmail slows the unarmored pace. Default 0, so those sets keep the unarmored speed.");
        setIfMissing(
                file,
                "player.armor.leatherChainmailSlowdownPercent",
                ArmorSpeedRules.DEFAULT_LEATHER_CHAINMAIL_PERCENT);
        comment(
                file,
                "player.armor.heavyExtraSlowdownPercent",
                "Extra percentage points a full set of heavy armor adds on top of its material. Each heavy piece adds one quarter of this. Default 5.");
        setIfMissing(file, "player.armor.heavyExtraSlowdownPercent", ArmorSpeedRules.DEFAULT_HEAVY_EXTRA_PERCENT);
        file.setComment(
                "player.armor.fullDiamondSlowdownPercent",
                "Unused. The material percents set the slowdown. This value stays so an older file still stores it.");
        setIfMissing(file, "player.armor.fullDiamondSlowdownPercent", 0);
        comment(
                file,
                "player.shield.absorbPercent",
                "Percent of a blocked hit the shield soaks. 100 blocks all of it; 75 lets 25% through. Default 100.");
        setIfMissing(file, "player.shield.absorbPercent", ShieldRules.DEFAULT_ABSORB_PERCENT);
        comment(
                file,
                "player.shield.durabilityMultiplier",
                "Shield durability loss is vanilla times this. 1 is vanilla; 3 wears three times as fast. Default 3.");
        setIfMissing(file, "player.shield.durabilityMultiplier", ShieldRules.DEFAULT_DURABILITY_MULTIPLIER);
    }

    void read(CommentedFileConfig file) {
        environmentEnable = file.getOrElse("player.environment.enable", true);
        fallMultiplier = number(file, "player.environment.fallMultiplier", 2.0);
        fallEffect = PotionEffectHolder.read(file, "player.environment.fallEffect", FALL_EFFECT);
        explosionMultiplier = number(file, "player.environment.explosionMultiplier", 1.0);
        explosionEffect = PotionEffectHolder.read(file, "player.environment.explosionEffect", EXPLOSION_EFFECT);
        suffocationMultiplier = number(file, "player.environment.suffocationMultiplier", 5.0);
        suffocationEffect =
                PotionEffectHolder.read(file, "player.environment.suffocationEffect", PotionEffectHolder.NONE);
        lavaMultiplier = number(file, "player.environment.lavaMultiplier", 2.0);
        lavaEffect = PotionEffectHolder.read(file, "player.environment.lavaEffect", PotionEffectHolder.NONE);
        burnMultiplier = number(file, "player.environment.burnMultiplier", 1.0);
        burnEffect = PotionEffectHolder.read(file, "player.environment.burnEffect", BURN_EFFECT);
        starvationMultiplier = number(file, "player.environment.starvationMultiplier", 2.0);
        starvationEffect =
                PotionEffectHolder.read(file, "player.environment.starvationEffect", PotionEffectHolder.NONE);
        drownMultiplier = number(file, "player.environment.drownMultiplier", 2.0);
        drownEffect = PotionEffectHolder.read(file, "player.environment.drownEffect", PotionEffectHolder.NONE);

        extinguishIgnites = file.getOrElse("player.extinguishIgnites", true);
        extinguishBurnTicks = file.getOrElse("player.extinguishBurnTicks", 80);

        forfeitEnable = file.getOrElse("player.death.forfeitEnable", false);
        forfeitPercent = file.getOrElse("player.death.forfeitPercent", 0);
        toolDamagePercent = file.getOrElse("player.death.toolDamagePercent", 0);
        keepHeavilyDamagedTools = file.getOrElse("player.death.keepHeavilyDamagedTools", true);
        respawnHealthEnable = file.getOrElse("player.death.respawnHealthEnable", true);
        respawnHealthPercent = file.getOrElse("player.death.respawnHealthPercent", 25);
        respawnFood = file.getOrElse("player.death.respawnFood", 6);
        lives = file.getOrElse("player.death.lives", LivesRules.DEFAULT_LIVES);
        restoreLifeAvailable = file.getOrElse("player.death.restoreLifeAvailable", true);

        weightEnable = file.getOrElse("player.weight.enable", true);
        blockWaterfalls = file.getOrElse("player.weight.blockWaterfalls", true);
        maxPoints = number(file, "player.weight.maxPoints", 18.0);
        armorPiece = number(file, "player.weight.armorPiece", 2.0);
        stack = number(file, "player.weight.stack", 1.0);
        tool = number(file, "player.weight.tool", 0.5);
        drownRate = file.getOrElse("player.weight.drownRate", 35);
        overencumbranceAdds = file.getOrElse("player.weight.overencumbranceAdds", 2);

        armorEnable = file.getOrElse("player.armor.enable", true);
        baseSpeed = number(file, "player.armor.baseSpeed", 0.22);
        copperIronGoldSlowdownPercent = file.getOrElse(
                "player.armor.copperIronGoldSlowdownPercent", ArmorSpeedRules.DEFAULT_COPPER_IRON_GOLD_PERCENT);
        diamondSlowdownPercent =
                file.getOrElse("player.armor.diamondSlowdownPercent", ArmorSpeedRules.DEFAULT_DIAMOND_PERCENT);
        leatherChainmailSlowdownPercent = file.getOrElse(
                "player.armor.leatherChainmailSlowdownPercent", ArmorSpeedRules.DEFAULT_LEATHER_CHAINMAIL_PERCENT);
        heavyExtraSlowdownPercent = file.getOrElse(
                "player.armor.heavyExtraSlowdownPercent", ArmorSpeedRules.DEFAULT_HEAVY_EXTRA_PERCENT);
        fullDiamondSlowdownPercent = file.getOrElse("player.armor.fullDiamondSlowdownPercent", 0);
        shieldAbsorbPercent = ShieldRules.clampAbsorbPercent(
                file.getOrElse("player.shield.absorbPercent", ShieldRules.DEFAULT_ABSORB_PERCENT));
        shieldDurabilityMultiplier = ShieldRules.clampDurabilityMultiplier(
                file.getOrElse("player.shield.durabilityMultiplier", ShieldRules.DEFAULT_DURABILITY_MULTIPLIER));
    }

    void write(CommentedFileConfig file) {
        file.set("player.environment.enable", environmentEnable);
        file.set("player.environment.fallMultiplier", fallMultiplier);
        fallEffect.write(file, "player.environment.fallEffect");
        file.set("player.environment.explosionMultiplier", explosionMultiplier);
        explosionEffect.write(file, "player.environment.explosionEffect");
        file.set("player.environment.suffocationMultiplier", suffocationMultiplier);
        suffocationEffect.write(file, "player.environment.suffocationEffect");
        file.set("player.environment.lavaMultiplier", lavaMultiplier);
        lavaEffect.write(file, "player.environment.lavaEffect");
        file.set("player.environment.burnMultiplier", burnMultiplier);
        burnEffect.write(file, "player.environment.burnEffect");
        file.set("player.environment.starvationMultiplier", starvationMultiplier);
        starvationEffect.write(file, "player.environment.starvationEffect");
        file.set("player.environment.drownMultiplier", drownMultiplier);
        drownEffect.write(file, "player.environment.drownEffect");

        file.set("player.extinguishIgnites", extinguishIgnites);
        file.set("player.extinguishBurnTicks", extinguishBurnTicks);

        file.set("player.death.forfeitEnable", forfeitEnable);
        file.set("player.death.forfeitPercent", forfeitPercent);
        file.set("player.death.toolDamagePercent", toolDamagePercent);
        file.set("player.death.keepHeavilyDamagedTools", keepHeavilyDamagedTools);
        file.set("player.death.respawnHealthEnable", respawnHealthEnable);
        file.set("player.death.respawnHealthPercent", respawnHealthPercent);
        file.set("player.death.respawnFood", respawnFood);
        file.set("player.death.lives", lives);
        file.set("player.death.restoreLifeAvailable", restoreLifeAvailable);

        file.set("player.weight.enable", weightEnable);
        file.set("player.weight.blockWaterfalls", blockWaterfalls);
        file.set("player.weight.maxPoints", maxPoints);
        file.set("player.weight.armorPiece", armorPiece);
        file.set("player.weight.stack", stack);
        file.set("player.weight.tool", tool);
        file.set("player.weight.drownRate", drownRate);
        file.set("player.weight.overencumbranceAdds", overencumbranceAdds);

        file.set("player.armor.enable", armorEnable);
        file.set("player.armor.baseSpeed", baseSpeed);
        file.set("player.armor.copperIronGoldSlowdownPercent", copperIronGoldSlowdownPercent);
        file.set("player.armor.diamondSlowdownPercent", diamondSlowdownPercent);
        file.set("player.armor.leatherChainmailSlowdownPercent", leatherChainmailSlowdownPercent);
        file.set("player.armor.heavyExtraSlowdownPercent", heavyExtraSlowdownPercent);
        file.set("player.armor.fullDiamondSlowdownPercent", fullDiamondSlowdownPercent);
        file.set("player.shield.absorbPercent", shieldAbsorbPercent);
        file.set("player.shield.durabilityMultiplier", shieldDurabilityMultiplier);
    }

    private static void comment(CommentedFileConfig file, String path, String comment) {
        if (file.getComment(path) == null) {
            file.setComment(path, comment);
        }
    }

    private static void setIfMissing(CommentedFileConfig file, String path, Object value) {
        if (!file.contains(path)) {
            file.set(path, value);
        }
    }

    private static double number(CommentedFileConfig file, String path, double fallback) {
        Object raw = file.get(path);
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        return fallback;
    }
}

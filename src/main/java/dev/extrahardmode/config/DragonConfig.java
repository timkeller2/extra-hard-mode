package dev.extrahardmode.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import dev.extrahardmode.feature.DragonRules;

/** Per-dimension Glydia / Ender Dragon nodes. Auto-respawn defaults off (KD-7). */
public final class DragonConfig {
    private boolean autoRespawn = false;
    private boolean dropEgg = true;
    private boolean dropVillagerEggs = true;
    private boolean harderBattle = true;
    private boolean alternativeMinions = false;
    private boolean announcements = true;
    private boolean noBuilding = true;
    private int health = DragonRules.DEFAULT_HEALTH;
    private int healOnPlayerKillPercent = DragonRules.DEFAULT_HEAL_PERCENT;

    public boolean autoRespawn() {
        return autoRespawn;
    }

    public boolean dropEgg() {
        return dropEgg;
    }

    public boolean dropVillagerEggs() {
        return dropVillagerEggs;
    }

    public boolean harderBattle() {
        return harderBattle;
    }

    public boolean alternativeMinions() {
        return alternativeMinions;
    }

    public boolean announcements() {
        return announcements;
    }

    public boolean noBuilding() {
        return noBuilding;
    }

    public int health() {
        return health;
    }

    public int healOnPlayerKillPercent() {
        return healOnPlayerKillPercent;
    }

    void writeDefaults(CommentedFileConfig file) {
        writeBool(
                file,
                "dragon.autoRespawn",
                "original true; vanilla crystals own respawn (KD-7). Only fires if the dragon is dead and no crystal ritual is in progress.",
                false);
        writeBool(file, "dragon.dropEgg", "Always drop a dragon egg at the podium, even if one already exists.", true);
        writeBool(file, "dragon.dropVillagerEggs", "Drop 2 villager spawn eggs on dragon death.", true);
        writeBool(
                file,
                "dragon.harderBattle",
                "Explosive fireballs with flaming shrapnel; summons minions and aggros nearby endermen.",
                true);
        writeBool(
                file,
                "dragon.alternativeMinions",
                "More diverse minions (blazes, zombie villagers, skeletons). Harder battle must be on.",
                false);
        writeBool(file, "dragon.announcements", "Server-wide chat when challenging, dying to, or defeating the dragon.", true);
        writeBool(
                file,
                "dragon.noBuilding",
                "Cancel block place in the End except end crystal, ender chest, and chorus flower. Creative bypasses.",
                true);
        if (!file.contains("dragon.health")) {
            file.setComment("dragon.health", "Vanilla 200. <=0 leaves vanilla health.");
            file.set("dragon.health", DragonRules.DEFAULT_HEALTH);
        }
        if (!file.contains("dragon.healOnPlayerKillPercent")) {
            file.setComment("dragon.healOnPlayerKillPercent", "Heal this percent of max health when a fighter dies or leaves.");
            file.set("dragon.healOnPlayerKillPercent", DragonRules.DEFAULT_HEAL_PERCENT);
        }
    }

    void read(CommentedFileConfig file) {
        autoRespawn = file.getOrElse("dragon.autoRespawn", false);
        dropEgg = file.getOrElse("dragon.dropEgg", true);
        dropVillagerEggs = file.getOrElse("dragon.dropVillagerEggs", true);
        harderBattle = file.getOrElse("dragon.harderBattle", true);
        alternativeMinions = file.getOrElse("dragon.alternativeMinions", false);
        announcements = file.getOrElse("dragon.announcements", true);
        noBuilding = file.getOrElse("dragon.noBuilding", true);
        health = readInt(file, "dragon.health", DragonRules.DEFAULT_HEALTH);
        healOnPlayerKillPercent = readInt(file, "dragon.healOnPlayerKillPercent", DragonRules.DEFAULT_HEAL_PERCENT);
    }

    void write(CommentedFileConfig file) {
        file.set("dragon.autoRespawn", autoRespawn);
        file.set("dragon.dropEgg", dropEgg);
        file.set("dragon.dropVillagerEggs", dropVillagerEggs);
        file.set("dragon.harderBattle", harderBattle);
        file.set("dragon.alternativeMinions", alternativeMinions);
        file.set("dragon.announcements", announcements);
        file.set("dragon.noBuilding", noBuilding);
        file.set("dragon.health", health);
        file.set("dragon.healOnPlayerKillPercent", healOnPlayerKillPercent);
    }

    private static void writeBool(CommentedFileConfig file, String path, String comment, boolean value) {
        if (!file.contains(path)) {
            file.setComment(path, comment);
            file.set(path, value);
        }
    }

    private static int readInt(CommentedFileConfig file, String path, int fallback) {
        Object raw = file.get(path);
        return raw instanceof Number number ? number.intValue() : fallback;
    }
}

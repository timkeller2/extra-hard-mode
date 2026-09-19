package dev.extrahardmode.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Zombie / spider / creeper nodes. Defaults match RootNode + DESIGN 26.2 Y remap. */
public final class MonsterConfig {
    public static final int CAVE_Y = 48;
    public static final int SEA_Y = 63;
    public static final int WEB_CAP = 2048;
    public static final int WEB_CLEANUP_INTERVAL = 100;
    public static final int WEB_BREAK_CHANCE = 5;

    private boolean zombiesSlowPlayers = true;
    private Identifier slowEffectId = Identifier.withDefaultNamespace("slowness");
    private int slowDurationTicks = 100;
    private int slowAmplifier = 1;
    private boolean slowStack = true;
    private int slowStackMax = 3;
    private int reanimatePercent = 50;
    private boolean placeSkulls = true;
    private int skullDropPercent = 5;

    private int spiderBonusPercent = 20;
    private boolean spidersDropWeb = true;

    private int chargedPercent = 5;
    private int dropTntPercent = 20;
    private int dropTntMaxY = CAVE_Y;
    private boolean chargedExplodeOnDamage = true;
    private boolean fireExplosion = true;
    private int fireworkCount = 3;
    private double launchSpeed = 0.5;
    private boolean creeperTntWarning = true;

    public boolean zombiesSlowPlayers() {
        return zombiesSlowPlayers;
    }

    public Identifier slowEffectId() {
        return slowEffectId;
    }

    public int slowDurationTicks() {
        return slowDurationTicks;
    }

    public int slowAmplifier() {
        return slowAmplifier;
    }

    public boolean slowStack() {
        return slowStack;
    }

    public int slowStackMax() {
        return slowStackMax;
    }

    public int reanimatePercent() {
        return reanimatePercent;
    }

    public boolean placeSkulls() {
        return placeSkulls;
    }

    public int skullDropPercent() {
        return skullDropPercent;
    }

    public int spiderBonusPercent() {
        return spiderBonusPercent;
    }

    public boolean spidersDropWeb() {
        return spidersDropWeb;
    }

    public int chargedPercent() {
        return chargedPercent;
    }

    public int dropTntPercent() {
        return dropTntPercent;
    }

    public int dropTntMaxY() {
        return dropTntMaxY;
    }

    public boolean chargedExplodeOnDamage() {
        return chargedExplodeOnDamage;
    }

    public boolean fireExplosion() {
        return fireExplosion;
    }

    public int fireworkCount() {
        return fireworkCount;
    }

    public double launchSpeed() {
        return launchSpeed;
    }

    public boolean creeperTntWarning() {
        return creeperTntWarning;
    }

    public MobEffectInstance slowEffect(int amplifier) {
        return new MobEffectInstance(slowEffectHolder(), Math.max(1, slowDurationTicks), Math.max(0, amplifier));
    }

    public Holder<MobEffect> slowEffectHolder() {
        return BuiltInRegistries.MOB_EFFECT
                .get(slowEffectId)
                .map(holder -> (Holder<MobEffect>) holder)
                .orElse(MobEffects.SLOWNESS);
    }

    void writeDefaults(CommentedFileConfig file) {
        writeBool(file, "zombies.slowPlayers", "Zombies apply slowness on hit.", true);
        writeString(
                file,
                "zombies.slowEffect.type",
                "Potion id. original: slowness 5s amplifier 1.",
                "minecraft:slowness");
        writeInt(file, "zombies.slowEffect.durationTicks", null, 100);
        writeInt(file, "zombies.slowEffect.amplifier", null, 1);
        writeBool(file, "zombies.slowStack", "Successive hits raise amplifier.", true);
        writeInt(file, "zombies.slowStackMax", "Max slowness amplifier (inclusive).", 3);
        writeInt(
                file,
                "zombies.reanimatePercent",
                "First-death chance. Later deaths use (1/n)*percent. original RootNode comment 7.5% is wrong.",
                50);
        writeBool(file, "zombies.placeSkulls", "Zombie head marks a pending reanimate. Break cancels.", true);
        writeInt(file, "zombies.skullDropPercent", "Chance a broken reanimate skull drops.", 5);

        writeInt(
                file,
                "spiders.bonusUndergroundPercent",
                "NATURAL zombies below sea Y=63 become spiders. original: under sea level.",
                20);
        writeBool(file, "spiders.dropWebOnDeath", "Place cobwebs on death. Y>=48 cleaned; caves persist.", true);

        writeInt(file, "creepers.chargedPercent", "NATURAL creepers spawn charged.", 5);
        writeInt(file, "creepers.dropTntPercent", "Death chance to drop primed TNT.", 20);
        if (!file.contains("creepers.dropTntMaxY")) {
            file.setComment("creepers.dropTntMaxY", "Cave band. original: 50");
            file.set("creepers.dropTntMaxY", CAVE_Y);
        }
        writeBool(file, "creepers.chargedExplodeOnDamage", "Powered creepers explode when a player hits them.", true);
        writeBool(file, "creepers.fireExplosion", "Fire/lava starts CoolCreeperExplosion (fireworks + launch).", true);
        writeInt(file, "creepers.fireworkCount", null, 3);
        writeDouble(file, "creepers.launchSpeed", null, 0.5);
        writeBool(file, "sounds.creeperTntWarning", "Ghast warn before a creeper TNT drop.", true);
    }

    void read(CommentedFileConfig file) {
        zombiesSlowPlayers = file.getOrElse("zombies.slowPlayers", true);
        slowEffectId = parseId(file.getOrElse("zombies.slowEffect.type", "minecraft:slowness"), "minecraft:slowness");
        slowDurationTicks = readInt(file, "zombies.slowEffect.durationTicks", 100);
        slowAmplifier = readInt(file, "zombies.slowEffect.amplifier", 1);
        slowStack = file.getOrElse("zombies.slowStack", true);
        slowStackMax = readInt(file, "zombies.slowStackMax", 3);
        reanimatePercent = readInt(file, "zombies.reanimatePercent", 50);
        placeSkulls = file.getOrElse("zombies.placeSkulls", true);
        skullDropPercent = readInt(file, "zombies.skullDropPercent", 5);

        spiderBonusPercent = readInt(file, "spiders.bonusUndergroundPercent", 20);
        spidersDropWeb = file.getOrElse("spiders.dropWebOnDeath", true);

        chargedPercent = readInt(file, "creepers.chargedPercent", 5);
        dropTntPercent = readInt(file, "creepers.dropTntPercent", 20);
        dropTntMaxY = readInt(file, "creepers.dropTntMaxY", CAVE_Y);
        chargedExplodeOnDamage = file.getOrElse("creepers.chargedExplodeOnDamage", true);
        fireExplosion = file.getOrElse("creepers.fireExplosion", true);
        fireworkCount = readInt(file, "creepers.fireworkCount", 3);
        launchSpeed = readDouble(file, "creepers.launchSpeed", 0.5);
        creeperTntWarning = file.getOrElse("sounds.creeperTntWarning", true);
    }

    void write(CommentedFileConfig file) {
        file.set("zombies.slowPlayers", zombiesSlowPlayers);
        file.set("zombies.slowEffect.type", slowEffectId.toString());
        file.set("zombies.slowEffect.durationTicks", slowDurationTicks);
        file.set("zombies.slowEffect.amplifier", slowAmplifier);
        file.set("zombies.slowStack", slowStack);
        file.set("zombies.slowStackMax", slowStackMax);
        file.set("zombies.reanimatePercent", reanimatePercent);
        file.set("zombies.placeSkulls", placeSkulls);
        file.set("zombies.skullDropPercent", skullDropPercent);
        file.set("spiders.bonusUndergroundPercent", spiderBonusPercent);
        file.set("spiders.dropWebOnDeath", spidersDropWeb);
        file.set("creepers.chargedPercent", chargedPercent);
        file.set("creepers.dropTntPercent", dropTntPercent);
        file.set("creepers.dropTntMaxY", dropTntMaxY);
        file.set("creepers.chargedExplodeOnDamage", chargedExplodeOnDamage);
        file.set("creepers.fireExplosion", fireExplosion);
        file.set("creepers.fireworkCount", fireworkCount);
        file.set("creepers.launchSpeed", launchSpeed);
        file.set("sounds.creeperTntWarning", creeperTntWarning);
    }

    private static Identifier parseId(String raw, String fallback) {
        try {
            return Identifier.parse(raw);
        } catch (RuntimeException e) {
            return Identifier.parse(fallback);
        }
    }

    private static void writeBool(CommentedFileConfig file, String path, String comment, boolean value) {
        if (!file.contains(path)) {
            if (comment != null) {
                file.setComment(path, comment);
            }
            file.set(path, value);
        }
    }

    private static void writeString(CommentedFileConfig file, String path, String comment, String value) {
        if (!file.contains(path)) {
            if (comment != null) {
                file.setComment(path, comment);
            }
            file.set(path, value);
        }
    }

    private static void writeInt(CommentedFileConfig file, String path, String comment, int value) {
        if (!file.contains(path)) {
            if (comment != null) {
                file.setComment(path, comment);
            }
            file.set(path, value);
        }
    }

    private static void writeDouble(CommentedFileConfig file, String path, String comment, double value) {
        if (!file.contains(path)) {
            if (comment != null) {
                file.setComment(path, comment);
            }
            file.set(path, value);
        }
    }

    private static int readInt(CommentedFileConfig file, String path, int fallback) {
        Object raw = file.get(path);
        return raw instanceof Number number ? number.intValue() : fallback;
    }

    private static double readDouble(CommentedFileConfig file, String path, double fallback) {
        Object raw = file.get(path);
        return raw instanceof Number number ? number.doubleValue() : fallback;
    }
}

package dev.extrahardmode.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import dev.extrahardmode.api.ExplosionType;
import dev.extrahardmode.feature.ExplosionFireRules;

/** Per-dimension explosion nodes. Defaults match RootNode (TNT 5/3, ghast 2). */
public final class ExplosionConfig {
    private boolean turnStoneToCobble = true;
    private int borderY = ExplosionSettings.BORDER_Y;
    private boolean flyingBlocks = true;
    private int flyingPercent = 20;
    private double upVelocity = 2.0;
    private double spreadVelocity = 3.0;
    private double autoremoveRadius = 10.0;
    private boolean otherModExplosions = false;
    private boolean tntCustom = true;
    private boolean tntMultiple = true;
    private int tntPerRecipe = 3;
    private int tntFirePercent = ExplosionFireRules.DEFAULT_TNT_FIRE_PERCENT;
    private final TypeConfig tnt = TypeConfig.of(ExplosionType.TNT);
    private final TypeConfig creeper = TypeConfig.of(ExplosionType.CREEPER);
    private final TypeConfig chargedCreeper = TypeConfig.of(ExplosionType.CREEPER_CHARGED);
    private final TypeConfig blazeDeath = TypeConfig.of(ExplosionType.OVERWORLD_BLAZE);
    private final TypeConfig ghast = TypeConfig.of(ExplosionType.GHAST_FIREBALL);
    private final TypeConfig magmaCube = TypeConfig.of(ExplosionType.MAGMACUBE_FIRE);

    public boolean turnStoneToCobble() {
        return turnStoneToCobble;
    }

    public int borderY() {
        return borderY;
    }

    public boolean flyingBlocks() {
        return flyingBlocks;
    }

    public int flyingPercent() {
        return flyingPercent;
    }

    public double upVelocity() {
        return upVelocity;
    }

    public double spreadVelocity() {
        return spreadVelocity;
    }

    public double autoremoveRadius() {
        return autoremoveRadius;
    }

    public boolean otherModExplosions() {
        return otherModExplosions;
    }

    public boolean tntMultiple() {
        return tntMultiple;
    }

    public int tntPerRecipe() {
        return tntPerRecipe;
    }

    /** Percent of blocks a TNT blast affects that catch fire. {@code 0} disables. */
    public int tntFirePercent() {
        return tntFirePercent;
    }

    public boolean custom(ExplosionType type) {
        return switch (type) {
            case TNT -> tntCustom;
            case CREEPER -> creeper.custom;
            case CREEPER_CHARGED -> chargedCreeper.custom;
            case OVERWORLD_BLAZE -> blazeDeath.custom;
            case GHAST_FIREBALL -> ghast.custom;
            case MAGMACUBE_FIRE -> magmaCube.custom;
            case DRAGON_FIREBALL, EFFECT -> false;
        };
    }

    public ExplosionSettings.Applied applied(ExplosionType type, double y) {
        TypeConfig config = typeConfig(type);
        if (config == null) {
            return ExplosionSettings.resolve(type, y, borderY);
        }
        return ExplosionSettings.resolve(y, borderY, config.below, config.above);
    }

    private TypeConfig typeConfig(ExplosionType type) {
        return switch (type) {
            case TNT -> tnt;
            case CREEPER -> creeper;
            case CREEPER_CHARGED -> chargedCreeper;
            case OVERWORLD_BLAZE -> blazeDeath;
            case GHAST_FIREBALL -> ghast;
            case MAGMACUBE_FIRE -> magmaCube;
            case DRAGON_FIREBALL, EFFECT -> null;
        };
    }

    void writeDefaults(CommentedFileConfig file) {
        writeBool(file, "explosions.turnStoneToCobble", "Stone/deepslate in the blast become cobble instead of air.", true);
        if (!file.contains("explosions.borderY")) {
            file.setComment("explosions.borderY", "Cave vs surface. original: 55");
            file.set("explosions.borderY", ExplosionSettings.BORDER_Y);
        }
        writeBool(file, "explosions.flyingBlocks", "20% of remaining blast blocks launch as FallingBlockEntity.", true);
        writeInt(file, "explosions.flyingPercent", "Chance a destroyed solid block flies.", 20);
        writeDouble(file, "explosions.upVelocity", "Initial upward velocity for flying debris.", 2.0);
        writeDouble(file, "explosions.spreadVelocity", "Horizontal spread velocity for flying debris.", 3.0);
        writeDouble(file, "explosions.autoremoveRadius", "Discard flying debris that lands farther than this.", 10.0);
        writeBool(
                file,
                "explosions.otherModExplosions",
                "Apply stone/flying physics to explosions we did not classify.",
                false);
        writeBool(file, "explosions.tnt.custom", "Replace vanilla TNT power/fire/world-damage.", true);
        writeBool(file, "explosions.tnt.multiple", "Three nearby delayed bursts for natural craters.", true);
        writeInt(file, "explosions.tnt.perRecipe", "Datapack minecraft:tnt result count. original: 1", 3);
        writeInt(
                file,
                "explosions.tnt.firePercent",
                "Percent of blocks a TNT blast hits that catch fire, rounded. 0 disables.",
                ExplosionFireRules.DEFAULT_TNT_FIRE_PERCENT);
        writeType(file, "explosions.tnt", ExplosionType.TNT, false);
        writeType(file, "explosions.creeper", ExplosionType.CREEPER, false);
        writeType(file, "explosions.chargedCreeper", ExplosionType.CREEPER_CHARGED, false);
        writeType(file, "explosions.blazeDeath", ExplosionType.OVERWORLD_BLAZE, true);
        writeType(file, "explosions.ghast", ExplosionType.GHAST_FIREBALL, false);
        writeType(file, "explosions.magmaCube", ExplosionType.MAGMACUBE_FIRE, false);
    }

    void read(CommentedFileConfig file) {
        turnStoneToCobble = file.getOrElse("explosions.turnStoneToCobble", true);
        borderY = readInt(file, "explosions.borderY", ExplosionSettings.BORDER_Y);
        flyingBlocks = file.getOrElse("explosions.flyingBlocks", true);
        flyingPercent = readInt(file, "explosions.flyingPercent", 20);
        upVelocity = readDouble(file, "explosions.upVelocity", 2.0);
        spreadVelocity = readDouble(file, "explosions.spreadVelocity", 3.0);
        autoremoveRadius = readDouble(file, "explosions.autoremoveRadius", 10.0);
        otherModExplosions = file.getOrElse("explosions.otherModExplosions", false);
        tntCustom = file.getOrElse("explosions.tnt.custom", true);
        tntMultiple = file.getOrElse("explosions.tnt.multiple", true);
        tntPerRecipe = readInt(file, "explosions.tnt.perRecipe", 3);
        tntFirePercent = Math.clamp(
                readInt(file, "explosions.tnt.firePercent", ExplosionFireRules.DEFAULT_TNT_FIRE_PERCENT), 0, 100);
        readType(file, "explosions.tnt", tnt, ExplosionType.TNT);
        readType(file, "explosions.creeper", creeper, ExplosionType.CREEPER);
        readType(file, "explosions.chargedCreeper", chargedCreeper, ExplosionType.CREEPER_CHARGED);
        blazeDeath.custom = file.getOrElse(
                "explosions.blazeDeath.enable", file.getOrElse("explosions.blazeDeath.custom", true));
        readType(file, "explosions.blazeDeath", blazeDeath, ExplosionType.OVERWORLD_BLAZE);
        ghast.custom = file.getOrElse("explosions.ghast.custom", true);
        readType(file, "explosions.ghast", ghast, ExplosionType.GHAST_FIREBALL);
        magmaCube.custom = file.getOrElse("explosions.magmaCube.custom", true);
        readType(file, "explosions.magmaCube", magmaCube, ExplosionType.MAGMACUBE_FIRE);
        creeper.custom = file.getOrElse("explosions.creeper.custom", true);
        chargedCreeper.custom = file.getOrElse("explosions.chargedCreeper.custom", true);
    }

    void write(CommentedFileConfig file) {
        file.set("explosions.turnStoneToCobble", turnStoneToCobble);
        file.set("explosions.borderY", borderY);
        file.set("explosions.flyingBlocks", flyingBlocks);
        file.set("explosions.flyingPercent", flyingPercent);
        file.set("explosions.upVelocity", upVelocity);
        file.set("explosions.spreadVelocity", spreadVelocity);
        file.set("explosions.autoremoveRadius", autoremoveRadius);
        file.set("explosions.otherModExplosions", otherModExplosions);
        file.set("explosions.tnt.custom", tntCustom);
        file.set("explosions.tnt.multiple", tntMultiple);
        file.set("explosions.tnt.perRecipe", tntPerRecipe);
        file.set("explosions.tnt.firePercent", tntFirePercent);
        writeTypeValues(file, "explosions.tnt", tnt);
        file.set("explosions.creeper.custom", creeper.custom);
        writeTypeValues(file, "explosions.creeper", creeper);
        file.set("explosions.chargedCreeper.custom", chargedCreeper.custom);
        writeTypeValues(file, "explosions.chargedCreeper", chargedCreeper);
        file.set("explosions.blazeDeath.enable", blazeDeath.custom);
        writeTypeValues(file, "explosions.blazeDeath", blazeDeath);
        file.set("explosions.ghast.custom", ghast.custom);
        writeTypeValues(file, "explosions.ghast", ghast);
        file.set("explosions.magmaCube.custom", magmaCube.custom);
        writeTypeValues(file, "explosions.magmaCube", magmaCube);
    }

    private static void writeType(CommentedFileConfig file, String prefix, ExplosionType type, boolean enableKey) {
        if (enableKey) {
            writeBool(file, prefix + ".enable", "Custom blaze-death explosion.", true);
        } else if (!prefix.endsWith(".tnt")) {
            writeBool(file, prefix + ".custom", "Replace vanilla power/fire/world-damage.", true);
        }
        writePower(file, prefix + ".belowPower", type.belowPower());
        writeBool(file, prefix + ".belowFire", null, type.belowFire());
        writeBool(file, prefix + ".belowWorldDamage", null, type.belowWorldDamage());
        writePower(file, prefix + ".abovePower", type.abovePower());
        writeBool(file, prefix + ".aboveFire", null, type.aboveFire());
        writeBool(file, prefix + ".aboveWorldDamage", null, type.aboveWorldDamage());
    }

    private static void writeTypeValues(CommentedFileConfig file, String prefix, TypeConfig config) {
        file.set(prefix + ".belowPower", config.below.power());
        file.set(prefix + ".belowFire", config.below.fire());
        file.set(prefix + ".belowWorldDamage", config.below.worldDamage());
        file.set(prefix + ".abovePower", config.above.power());
        file.set(prefix + ".aboveFire", config.above.fire());
        file.set(prefix + ".aboveWorldDamage", config.above.worldDamage());
    }

    private static void readType(CommentedFileConfig file, String prefix, TypeConfig target, ExplosionType type) {
        target.below = new ExplosionSettings.Applied(
                readFloat(file, prefix + ".belowPower", type.belowPower()),
                file.getOrElse(prefix + ".belowFire", type.belowFire()),
                file.getOrElse(prefix + ".belowWorldDamage", type.belowWorldDamage()));
        target.above = new ExplosionSettings.Applied(
                readFloat(file, prefix + ".abovePower", type.abovePower()),
                file.getOrElse(prefix + ".aboveFire", type.aboveFire()),
                file.getOrElse(prefix + ".aboveWorldDamage", type.aboveWorldDamage()));
    }

    private static void writeBool(CommentedFileConfig file, String path, String comment, boolean value) {
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

    private static void writePower(CommentedFileConfig file, String path, float value) {
        if (!file.contains(path)) {
            file.set(path, value);
        }
    }

    private static int readInt(CommentedFileConfig file, String path, int fallback) {
        Object raw = file.get(path);
        return raw instanceof Number number ? number.intValue() : fallback;
    }

    private static float readFloat(CommentedFileConfig file, String path, float fallback) {
        Object raw = file.get(path);
        return raw instanceof Number number ? number.floatValue() : fallback;
    }

    private static double readDouble(CommentedFileConfig file, String path, double fallback) {
        Object raw = file.get(path);
        return raw instanceof Number number ? number.doubleValue() : fallback;
    }

    private static final class TypeConfig {
        private boolean custom = true;
        private ExplosionSettings.Applied below;
        private ExplosionSettings.Applied above;

        private static TypeConfig of(ExplosionType type) {
            TypeConfig config = new TypeConfig();
            config.below = new ExplosionSettings.Applied(type.belowPower(), type.belowFire(), type.belowWorldDamage());
            config.above = new ExplosionSettings.Applied(type.abovePower(), type.aboveFire(), type.aboveWorldDamage());
            return config;
        }
    }
}

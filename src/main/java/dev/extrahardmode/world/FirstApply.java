package dev.extrahardmode.world;

import java.util.Set;

/**
 * Per-dimension first-apply predicate, extracted so it can be unit-tested
 * without a running Minecraft server.
 *
 * <p>26.2 game rules are server-global. This helper only decides each dimension's
 * own enabled flag, never a shared {@code GameRules.set}.
 */
public final class FirstApply {
    public static final String OVERWORLD = "minecraft:overworld";
    public static final String NETHER = "minecraft:the_nether";
    public static final String END = "minecraft:the_end";

    private FirstApply() {}

    public static boolean alreadyApplied(Set<String> applied, String dimensionId) {
        return applied.contains(dimensionId);
    }

    public static boolean isVanillaDimension(String dimensionId) {
        return OVERWORLD.equals(dimensionId) || NETHER.equals(dimensionId) || END.equals(dimensionId);
    }

    /**
     * Vanilla overworld/nether/end copy {@code enabledByDefault}. Custom dimensions
     * inherit the overworld dimension's enabled flag when it is known, otherwise
     * {@code enabledByDefault}.
     */
    public static boolean resolveEnabled(String dimensionId, boolean enabledByDefault, Boolean overworldDimensionFlag) {
        if (isVanillaDimension(dimensionId)) {
            return enabledByDefault;
        }
        return overworldDimensionFlag != null ? overworldDimensionFlag : enabledByDefault;
    }
}

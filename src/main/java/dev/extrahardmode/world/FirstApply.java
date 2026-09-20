package dev.extrahardmode.world;

import java.util.Set;

/**
 * Per-dimension first-apply predicate, extracted so it can be unit-tested
 * without a running Minecraft server.
 *
 * <p>26.2 game rules are server-global. {@code enabledByDefault} is copied into
 * the gamerule once (overworld). Dimension {@code enabled} flags default true
 * (opt-out) so {@code /gamerule tougher:enabled true} can actually enable
 * EHM. Custom dimensions inherit the overworld's <em>live</em> flag.
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
     * Vanilla overworld/nether/end default {@code true} (opt-out). Custom dimensions
     * inherit the overworld live enabled flag when known, otherwise {@code true}.
     */
    public static boolean resolveDimensionEnabled(String dimensionId, Boolean overworldLiveEnabled) {
        if (isVanillaDimension(dimensionId)) {
            return true;
        }
        return overworldLiveEnabled != null ? overworldLiveEnabled : true;
    }
}

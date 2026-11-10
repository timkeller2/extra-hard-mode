package dev.extrahardmode.feature;

import java.util.Locale;

/** Compass needle target, and the facing named by one right-click. */
public final class CompassRules {
    private CompassRules() {}

    public static boolean pointAtPersonalSpawn(
            boolean sneaking, boolean localOwner, boolean hasSpawn, boolean sameDimension) {
        return sneaking && localOwner && hasSpawn && sameDimension;
    }

    /**
     * Capitalized eight-way facing for a Minecraft yaw. 0 looks south, 90 looks west,
     * and east is the rising sun.
     */
    public static String facingName(double yawDegrees) {
        double rad = Math.toRadians(yawDegrees);
        String name = InhabitantRules.compassDirection(-Math.sin(rad), Math.cos(rad));
        if (name == null || name.isEmpty()) {
            return "North";
        }
        return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
    }
}

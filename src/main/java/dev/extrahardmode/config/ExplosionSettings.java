package dev.extrahardmode.config;

import dev.extrahardmode.api.ExplosionType;

/**
 * Y-band resolution. Minecraft-free so JUnit can cover below/above border
 * without a server. {@code y <= borderY} is the cave band (inclusive).
 */
public final class ExplosionSettings {
    public static final int BORDER_Y = 48;

    private ExplosionSettings() {}

    public static Applied resolve(double y, int borderY, Applied below, Applied above) {
        return y <= borderY ? below : above;
    }

    public static Applied resolve(ExplosionType type, double y, int borderY) {
        Applied below = new Applied(type.belowPower(), type.belowFire(), type.belowWorldDamage());
        Applied above = new Applied(type.abovePower(), type.aboveFire(), type.aboveWorldDamage());
        return resolve(y, borderY, below, above);
    }

    public record Applied(float power, boolean fire, boolean worldDamage) {}
}

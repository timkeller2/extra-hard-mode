package dev.extrahardmode.feature;

/** Splits one Healing cast across the sparkle so the last tick pays whatever is left. */
public final class HealSparkleRules {
    public record Slice(float apply, float remaining) {}

    private HealSparkleRules() {}

    public static Slice slice(float remaining, long ticksLeft) {
        if (remaining <= 0.0F || ticksLeft <= 0L) {
            return new Slice(0.0F, Math.max(0.0F, remaining));
        }
        if (ticksLeft == 1L) {
            return new Slice(remaining, 0.0F);
        }
        float apply = remaining / (float) ticksLeft;
        return new Slice(apply, remaining - apply);
    }
}

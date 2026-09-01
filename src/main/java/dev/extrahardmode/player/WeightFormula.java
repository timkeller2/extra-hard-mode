package dev.extrahardmode.player;

/**
 * Inventory weight and drown-rate math. Kept free of Minecraft types for JUnit.
 *
 * <p>{@code weight = armorPieces * armorPiece + stackUnits * stack + tools * tool}
 * matching RootNode ({@code armor*2 + stacks*1 + tools*0.5}).
 */
public final class WeightFormula {
    private WeightFormula() {}

    public static double weight(
            int armorPieces,
            double stackUnits,
            int tools,
            double armorPiece,
            double stack,
            double tool) {
        return armorPieces * armorPiece + stackUnits * stack + tools * tool;
    }

    /**
     * Drown chance per 20-tick check, percent 0–100.
     * {@code drownRate + (weight - maxPoints) * overencumbranceAdds}.
     */
    public static int drownRatePercent(double weight, double maxPoints, int drownRate, int overencumbranceAdds) {
        double extra = Math.max(0.0, weight - maxPoints);
        return (int) Math.min(100.0, drownRate + extra * overencumbranceAdds);
    }
}

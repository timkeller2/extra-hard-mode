package dev.extrahardmode.feature;

/**
 * Fishing rods lose three times the durability they would have lost.
 * A point Unbreaking prevents is not tripled. Minecraft-free.
 */
public final class FishingRodRules {
    public static final int DURABILITY_MULTIPLIER = 3;

    private FishingRodRules() {}

    /**
     * Durability removed. {@code takenAfterUnbreaking} is the vanilla loss after
     * Unbreaking, and it cannot exceed {@code vanillaLoss}.
     */
    public static int durabilityLoss(int vanillaLoss, int takenAfterUnbreaking) {
        int vanilla = Math.max(0, vanillaLoss);
        int taken = Math.max(0, takenAfterUnbreaking);
        if (taken > vanilla) {
            taken = vanilla;
        }
        return taken * DURABILITY_MULTIPLIER;
    }
}

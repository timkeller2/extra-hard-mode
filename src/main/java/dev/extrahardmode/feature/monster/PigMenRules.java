package dev.extrahardmode.feature.monster;

/**
 * Nether wart from slain zombified piglins. Minecraft-free for JUnit.
 */
public final class PigMenRules {
    private PigMenRules() {}

    /**
     * Fortress kills always drop when {@code fortressAlways} is on. Elsewhere uses
     * {@code elsewherePercent} ({@code roll} is {@code nextInt(100)}).
     */
    public static boolean dropsNetherWart(
            boolean nether,
            boolean lootless,
            boolean fortress,
            boolean fortressAlways,
            int elsewherePercent,
            int roll) {
        if (!nether || lootless) {
            return false;
        }
        if (fortress) {
            return fortressAlways;
        }
        return percentChance(elsewherePercent, roll);
    }

    static boolean percentChance(int percent, int roll) {
        if (percent <= 0) {
            return false;
        }
        if (percent >= 100) {
            return true;
        }
        return roll < percent;
    }
}

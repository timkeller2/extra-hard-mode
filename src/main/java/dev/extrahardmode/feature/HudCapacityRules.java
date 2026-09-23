package dev.extrahardmode.feature;

/** Which status icon is only half a slot. Minecraft-free. */
public final class HudCapacityRules {
    private HudCapacityRules() {}

    /** {@code iconIndex} is the slot that holds points {@code 2 * index + 1} and {@code 2 * index + 2}. */
    public static boolean halfShadow(int maxPoints, int iconIndex) {
        if (maxPoints <= 0 || (maxPoints & 1) == 0 || iconIndex < 0) {
            return false;
        }
        return iconIndex == (maxPoints - 1) / 2;
    }
}

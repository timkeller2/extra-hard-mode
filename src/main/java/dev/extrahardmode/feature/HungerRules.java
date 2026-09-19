package dev.extrahardmode.feature;

/**
 * Hunger drain, variety bonus, and regen timing. Minecraft-free so JUnit can cover the math.
 */
public final class HungerRules {
    public static final float DEFAULT_MOVING_EXHAUSTION_PER_SECOND = 0.175F;
    public static final int DEFAULT_FOOD_HISTORY = 7;
    public static final int UNIQUE_WINDOW_EXPERIENCE = 3;
    public static final int TIRED_STREAK = 5;
    public static final int SICK_STREAK = 7;
    public static final int DEFAULT_SLOW_REGEN_TICKS = 600;
    public static final int DEFAULT_AFK_TIMEOUT_SECONDS = 30;
    public static final int VANILLA_SLOW_REGEN_TICKS = 80;
    public static final int VANILLA_STARVE_TICKS = 80;
    public static final int STARVE_SLOW_DIVISOR = 5;
    public static final int TICKS_PER_SECOND = 20;
    private static final double STILL_EPSILON_SQR = 1.0E-8;
    private static final float LOOK_EPSILON = 0.05F;

    private HungerRules() {}

    /** Vanilla starves every 80 ticks; Extra Hard Mode uses 1/5 that rate. */
    public static int starveTicks() {
        return VANILLA_STARVE_TICKS * STARVE_SLOW_DIVISOR;
    }

    public static float exhaustionPerTick(float perSecond) {
        return Math.max(0.0F, perSecond) / TICKS_PER_SECOND;
    }

    public static boolean isMoving(double dx, double dy, double dz) {
        return dx * dx + dy * dy + dz * dz > STILL_EPSILON_SQR;
    }

    public static boolean isLooking(float deltaYaw, float deltaPitch) {
        return Math.abs(deltaYaw) > LOOK_EPSILON || Math.abs(deltaPitch) > LOOK_EPSILON;
    }

    public static boolean hasControlInput(
            boolean forward, boolean backward, boolean left, boolean right, boolean jump, boolean shift, boolean sprint) {
        return forward || backward || left || right || jump || shift || sprint;
    }

    public static int activityTimeoutTicks(int afkTimeoutSeconds) {
        return Math.max(0, afkTimeoutSeconds) * TICKS_PER_SECOND;
    }

    /** {@code timeoutTicks <= 0} means never AFK. Missing activity is AFK. */
    public static boolean isAfk(long nowTick, Long lastActivityTick, int timeoutTicks) {
        if (timeoutTicks <= 0) {
            return false;
        }
        if (lastActivityTick == null) {
            return true;
        }
        return nowTick - lastActivityTick >= timeoutTicks;
    }

    public static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0F;
        if (wrapped >= 180.0F) {
            wrapped -= 360.0F;
        }
        if (wrapped < -180.0F) {
            wrapped += 360.0F;
        }
        return wrapped;
    }

    /**
     * Novel food: +1 hunger, or +1 saturation when the bar is already full.
     * Saturation cannot exceed the current food level (vanilla clamp).
     */
    public static int foodAfterVarietyBonus(int foodLevel, boolean novel) {
        if (!novel) {
            return clampFood(foodLevel);
        }
        if (foodLevel >= 20) {
            return 20;
        }
        return clampFood(foodLevel + 1);
    }

    public static float saturationAfterVarietyBonus(int foodLevel, float saturation, boolean novel) {
        if (!novel || foodLevel < 20) {
            return clampSaturation(saturation, foodLevel);
        }
        return clampSaturation(saturation + 1.0F, 20);
    }

    /**
     * Last {@link #DEFAULT_FOOD_HISTORY} meals were all different: +1 hunger,
     * +1 saturation, and {@link #UNIQUE_WINDOW_EXPERIENCE} XP, stacked on the
     * novel-food bonus.
     */
    public static int uniqueWindowExperience(boolean uniqueWindow) {
        return uniqueWindow ? UNIQUE_WINDOW_EXPERIENCE : 0;
    }

    public static int foodAfterUniqueWindowBonus(int foodLevel, boolean uniqueWindow) {
        if (!uniqueWindow) {
            return clampFood(foodLevel);
        }
        return clampFood(foodLevel + 1);
    }

    public static float saturationAfterUniqueWindowBonus(int foodLevel, float saturation, boolean uniqueWindow) {
        if (!uniqueWindow) {
            return clampSaturation(saturation, foodLevel);
        }
        return clampSaturation(saturation + 1.0F, foodLevel);
    }

    public static int clampFood(int foodLevel) {
        return Math.max(0, Math.min(20, foodLevel));
    }

    /**
     * Same food in the last {@link #DEFAULT_FOOD_HISTORY} meals: 5–6 times −1
     * hunger and saturation, 7 times −2 each. Applied after vanilla restoration.
     */
    public static int repeatFoodPenalty(int countInWindow) {
        if (countInWindow >= SICK_STREAK) {
            return 2;
        }
        if (countInWindow >= TIRED_STREAK) {
            return 1;
        }
        return 0;
    }

    public static int foodAfterRepeatPenalty(int foodLevel, int penalty) {
        return clampFood(foodLevel - Math.max(0, penalty));
    }

    public static float saturationAfterRepeatPenalty(int foodLevel, float saturation, int penalty) {
        return clampSaturation(saturation - Math.max(0, penalty), foodLevel);
    }

    public static float clampSaturation(float saturation, int foodLevel) {
        float max = Math.max(0, foodLevel);
        if (saturation < 0.0F) {
            return 0.0F;
        }
        return Math.min(saturation, max);
    }

    /** Slow food regen while hunger is high, up to the player's current max health. */
    public static boolean canRegenHealth(int foodLevel, float health, float maxHealth) {
        return foodLevel >= 18 && HealthRules.canHeal(health, maxHealth);
    }
}

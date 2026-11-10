package dev.extrahardmode.feature;

/**
 * Remaining time on a mission HUD line. Expiry is the start of an overworld day.
 * A Minecraft day is 24000 ticks and an hour is 1000.
 */
public final class MissionTimerRules {
    public static final long TICKS_PER_DAY = 24000L;
    public static final long TICKS_PER_HOUR = 1000L;

    private MissionTimerRules() {}

    /** {@code (2d 3h)}. Both counts stay, including zeros. Empty when the mission has no expiry day. */
    public static String label(long expireDay, long clockTime) {
        if (expireDay < 0L) {
            return "";
        }
        long now = Math.max(0L, clockTime);
        long left = expireDay * TICKS_PER_DAY - now;
        if (left < 0L) {
            left = 0L;
        }
        long days = left / TICKS_PER_DAY;
        long hours = (left % TICKS_PER_DAY) / TICKS_PER_HOUR;
        return "(" + days + "d " + hours + "h)";
    }

    /** The line plus {@code (Nd Mh)}. A missing line or a mission with no expiry is left alone. */
    public static String append(String line, long expireDay, long clockTime) {
        if (line == null || line.isEmpty()) {
            return line == null ? "" : line;
        }
        String timer = label(expireDay, clockTime);
        if (timer.isEmpty()) {
            return line;
        }
        return line + " " + timer;
    }
}

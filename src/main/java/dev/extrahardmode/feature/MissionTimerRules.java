package dev.extrahardmode.feature;

/**
 * Remaining time on a mission HUD line. Expiry is the start of an overworld day.
 * A Minecraft day is 24000 ticks and an hour is 1000. The label appears only
 * inside the last four days: {@code (2d)} until the last day, then {@code (13h)}.
 */
public final class MissionTimerRules {
    public static final long TICKS_PER_DAY = 24000L;
    public static final long TICKS_PER_HOUR = 1000L;
    /** The timer stays off at this many days and above. */
    public static final long SHOW_UNDER_DAYS = 4L;

    private MissionTimerRules() {}

    /** {@code (2d)} or {@code (13h)}. Empty at four days or more, and when there is no expiry day. */
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
        if (days >= SHOW_UNDER_DAYS) {
            return "";
        }
        if (days >= 1L) {
            return "(" + days + "d)";
        }
        long hours = (left % TICKS_PER_DAY) / TICKS_PER_HOUR;
        return "(" + hours + "h)";
    }

    /** The line plus the short timer. A missing line, a long mission, or no expiry is left alone. */
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

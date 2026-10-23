package dev.extrahardmode.feature;

import java.util.Locale;
import java.util.Set;

/**
 * Which disconnects are a broken pipe rather than a quit or a kick.
 * Minecraft-free so JUnit can cover the gate.
 */
public final class ReconnectRules {
    /** Two seconds, long enough for a few-second path blip to come back. */
    public static final int DELAY_TICKS = 40;

    /** Tries at 2, 4, 6, and 8 seconds, then the disconnect screen stays. */
    public static final int MAX_ATTEMPTS = 4;

    private static final Set<String> TRANSPORT_KEYS = Set.of(
            "multiplayer.disconnect.generic",
            "disconnect.endOfStream",
            "disconnect.timeout",
            "disconnect.unknownHost");

    private ReconnectRules() {}

    /**
     * @param translationKey the disconnect component's translation key, or empty for a literal
     * @param message the disconnect component's plain text
     */
    public static boolean transportDrop(String translationKey, String message) {
        if (translationKey != null && TRANSPORT_KEYS.contains(translationKey)) {
            return true;
        }
        if (!"disconnect.genericReason".equals(translationKey)) {
            return false;
        }
        return transportException(message);
    }

    static boolean transportException(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("decoderexception")
                || lower.contains("encoderexception")
                || lower.contains("packetdecoder")
                || lower.contains("badly compressed")
                || lower.contains("corrupt")) {
            return false;
        }
        return lower.contains("connection reset")
                || lower.contains("forcibly closed")
                || lower.contains("broken pipe")
                || lower.contains("timed out")
                || lower.contains("connection abort")
                || lower.contains("connection refused")
                || lower.contains("unreachable")
                || lower.contains("unknown host")
                || lower.contains("socketexception")
                || lower.contains("closedchannelexception")
                || lower.contains("nativeioexception")
                || lower.contains("connectexception")
                || lower.contains("end of stream");
    }
}

package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReconnectRulesTest {
    @Test
    void socketResetIsATransportDrop() {
        assertTrue(ReconnectRules.transportDrop(
                "disconnect.genericReason",
                "Internal Exception: java.net.SocketException: Connection reset"));
        assertTrue(ReconnectRules.transportDrop(
                "disconnect.genericReason",
                "java.net.SocketException: Connection reset"));
        assertTrue(ReconnectRules.transportDrop(
                "disconnect.genericReason",
                "An existing connection was forcibly closed by the remote host"));
    }

    @Test
    void silentCloseAndTimeoutAreTransportDrops() {
        assertTrue(ReconnectRules.transportDrop("multiplayer.disconnect.generic", "Disconnected"));
        assertTrue(ReconnectRules.transportDrop("disconnect.endOfStream", "End of stream"));
        assertTrue(ReconnectRules.transportDrop("disconnect.timeout", "Timed out"));
        assertTrue(ReconnectRules.transportDrop("disconnect.unknownHost", "Unknown host"));
        assertTrue(ReconnectRules.transportDrop(
                "disconnect.genericReason", "Internal Exception: Unknown host"));
    }

    @Test
    void quitsKicksAndProtocolErrorsStayDisconnected() {
        assertFalse(ReconnectRules.transportDrop("multiplayer.status.quitting", "Quitting"));
        assertFalse(ReconnectRules.transportDrop("multiplayer.disconnect.kicked", "Kicked by an operator"));
        assertFalse(ReconnectRules.transportDrop("multiplayer.disconnect.banned", "You are banned from this server"));
        assertFalse(ReconnectRules.transportDrop(
                "multiplayer.disconnect.not_whitelisted", "You are not white-listed on this server!"));
        assertFalse(ReconnectRules.transportDrop(
                "multiplayer.disconnect.duplicate_login", "You logged in from another location"));
        assertFalse(ReconnectRules.transportDrop(
                "multiplayer.disconnect.flying", "Flying is not enabled on this server"));
        assertFalse(ReconnectRules.transportDrop("multiplayer.disconnect.server_shutdown", "Server closed"));
        assertFalse(ReconnectRules.transportDrop("connect.aborted", "Aborted"));
        assertFalse(ReconnectRules.transportDrop("disconnect.transfer", "Transferred to another server"));
        assertFalse(ReconnectRules.transportDrop(
                "disconnect.genericReason",
                "io.netty.handler.codec.DecoderException: Badly compressed packet"));
        assertFalse(ReconnectRules.transportDrop(
                "", "This server requires Fabric Loader and Fabric API installed on your client!"));
        assertFalse(ReconnectRules.transportDrop(null, null));
        assertFalse(ReconnectRules.transportDrop("disconnect.genericReason", ""));
    }

    @Test
    void retryWindowIsAFewSeconds() {
        assertEquals(40, ReconnectRules.DELAY_TICKS);
        assertEquals(4, ReconnectRules.MAX_ATTEMPTS);
        assertTrue(ReconnectRules.DELAY_TICKS * ReconnectRules.MAX_ATTEMPTS <= 20 * 10);
    }
}

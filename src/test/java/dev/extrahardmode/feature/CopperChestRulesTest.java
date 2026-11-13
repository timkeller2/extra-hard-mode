package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CopperChestRulesTest {
    @Test
    void claimLastsNinetyDaysAndResetsFromTheOpen() {
        long placed = 1_000L;
        long expires = CopperChestRules.expiresAt(placed);
        assertEquals(placed + 90L * CopperChestRules.TICKS_PER_DAY, expires);
        assertTrue(CopperChestRules.locked(expires, placed));
        assertTrue(CopperChestRules.locked(expires, expires - 1L));
        assertFalse(CopperChestRules.locked(expires, expires));
        assertEquals(expires, CopperChestRules.expiresAt(placed));
        long opened = expires - 5_000L;
        assertEquals(opened + 90L * CopperChestRules.TICKS_PER_DAY, CopperChestRules.expiresAt(opened));
    }

    @Test
    void daysLeftRoundsUpUntilTheChestIsPublic() {
        long expires = 90L * CopperChestRules.TICKS_PER_DAY;
        assertEquals(90, CopperChestRules.daysLeft(expires, 0L));
        assertEquals(90, CopperChestRules.daysLeft(expires, 1L));
        assertEquals(89, CopperChestRules.daysLeft(expires, CopperChestRules.TICKS_PER_DAY));
        assertEquals(1, CopperChestRules.daysLeft(expires, expires - 1L));
        assertEquals(0, CopperChestRules.daysLeft(expires, expires));
        assertEquals("Public in 90 days", CopperChestRules.lookLabel(90));
        assertEquals("Public in 1 day", CopperChestRules.lookLabel(1));
        assertEquals("", CopperChestRules.lookLabel(0));
        assertEquals("1 day", CopperChestRules.daysPhrase(1));
        assertEquals("87 days", CopperChestRules.daysPhrase(87));
    }

    @Test
    void signNamesOnePlayerOnEachLine() {
        List<String> lines = List.of(" Ada ", "", "SELL", "bob", "BUY");
        assertTrue(CopperChestRules.nameListed(lines, "ada"));
        assertTrue(CopperChestRules.nameListed(lines, "Bob"));
        assertFalse(CopperChestRules.nameListed(lines, "SELL"));
        assertFalse(CopperChestRules.nameListed(lines, "BUY"));
        assertFalse(CopperChestRules.nameListed(lines, "Ad"));
        assertFalse(CopperChestRules.nameListed(lines, ""));
        assertFalse(CopperChestRules.nameListed(null, "Ada"));
        assertTrue(CopperChestRules.allowed("owner-id", "owner-id", lines, "someone"));
        assertTrue(CopperChestRules.allowed("owner-id", "guest-id", lines, "Ada"));
        assertFalse(CopperChestRules.allowed("owner-id", "guest-id", lines, "Cara"));
    }

    @Test
    void claimKeyRoundTrips() {
        CopperChestRules.Claim claim =
                new CopperChestRules.Claim("minecraft:the_nether", 3, -4, 5, "player-id", 90L);
        assertEquals(claim, CopperChestRules.parse(CopperChestRules.key(claim)));
        assertNull(CopperChestRules.parse(null));
        assertNull(CopperChestRules.parse("minecraft:overworld|1|2"));
        assertNull(CopperChestRules.key(new CopperChestRules.Claim("bad|dim", 0, 0, 0, "id", 1L)));
    }

    @Test
    void helpTellsTheOwnerAndTheSign() {
        String text = String.join(" ", EhmHelp.resolve("building").lines());
        assertTrue(text.contains("90 Minecraft days"));
        assertTrue(text.contains("one on each line"));
        assertTrue(text.contains("until it is public"));
        assertTrue(text.contains("Sneak to place yours"));
        assertTrue(text.contains("copper golems"));
    }
}

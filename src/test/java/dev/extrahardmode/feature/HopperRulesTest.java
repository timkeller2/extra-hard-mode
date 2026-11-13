package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class HopperRulesTest {
    @Test
    void placerAndSignNameMayPull() {
        assertTrue(HopperRules.mayPull("owner-id", "Ada", "owner-id", List.of()));
        assertTrue(HopperRules.mayPull("guest-id", "Ada", "owner-id", List.of(" ada ")));
        assertFalse(HopperRules.mayPull("guest-id", "Cara", "owner-id", List.of("Ada")));
        assertFalse(HopperRules.mayPull("guest-id", "SELL", "owner-id", List.of("SELL")));
        assertFalse(HopperRules.mayPull("", "", "owner-id", List.of()));
        assertTrue(HopperRules.sameOwner("owner-id", "owner-id"));
        assertFalse(HopperRules.sameOwner("guest-id", "owner-id"));
        assertFalse(HopperRules.sameOwner("", "owner-id"));
        assertFalse(HopperRules.sameOwner(null, "owner-id"));
    }

    @Test
    void claimKeyKeepsThePlacerName() {
        HopperRules.Claim claim = new HopperRules.Claim("minecraft:the_nether", 3, -4, 5, "player-id", "Ada", 90L);
        assertEquals(claim, HopperRules.parse(HopperRules.key(claim)));
        assertEquals(claim.withExpiry(10L).expiresAt(), 10L);
        assertNull(HopperRules.parse(null));
        assertNull(HopperRules.parse("minecraft:overworld|1|2|3|id|90"));
        assertNull(HopperRules.key(new HopperRules.Claim("bad|dim", 0, 0, 0, "id", "Ada", 1L)));
        assertNull(HopperRules.key(new HopperRules.Claim("minecraft:overworld", 0, 0, 0, "id", "A|da", 1L)));
    }

    @Test
    void helpTellsTheHopperOwnerAndThePull() {
        String text = String.join(" ", EhmHelp.resolve("building").lines());
        assertTrue(text.contains("A hopper you place is yours for 90 Minecraft days"));
        assertTrue(text.contains("can pull from a copper chest or hopper you own"));
        assertTrue(text.contains("put items into a copper chest with the same owner"));
        assertTrue(text.contains("copper golems"));
    }
}

package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HopperRulesTest {
    @Test
    void ownedHopperTransfersWithItsOwnerOrNobody() {
        assertTrue(HopperRules.mayTransfer("owner-id", null));
        assertTrue(HopperRules.mayTransfer("owner-id", ""));
        assertTrue(HopperRules.mayTransfer("owner-id", "owner-id"));
        assertFalse(HopperRules.mayTransfer("owner-id", "other-id"));
        assertFalse(HopperRules.mayTransfer("", "other-id"));
        assertFalse(HopperRules.mayTransfer(null, "other-id"));
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
        assertTrue(text.contains("pull from and put items into a container you own, or one nobody owns"));
        assertTrue(text.contains("Another player's locked chest or hopper stays closed"));
        assertTrue(text.contains("copper golems"));
    }
}

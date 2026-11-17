package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CopperGolemRulesTest {
    @Test
    void publicChestAcceptsAnyGolem() {
        assertTrue(CopperGolemRules.mayUse(null, null));
        assertTrue(CopperGolemRules.mayUse(null, ""));
        assertTrue(CopperGolemRules.mayUse("owner", null));
        assertTrue(CopperGolemRules.mayUse("owner", ""));
    }

    @Test
    void onlyTheSummonersGolemUsesAnOwnedChest() {
        assertTrue(CopperGolemRules.mayUse("owner", "owner"));
        assertFalse(CopperGolemRules.mayUse("other", "owner"));
        assertFalse(CopperGolemRules.mayUse(null, "owner"));
        assertFalse(CopperGolemRules.mayUse("", "owner"));
    }

    @Test
    void helpSaysWhoseGolemMayUseTheChest() {
        String text = String.join(" ", EhmHelp.resolve("building").lines());
        assertTrue(text.contains("A copper golem you summon can use a copper chest you placed"));
        assertTrue(text.contains("The chest that summon makes is yours"));
        assertTrue(text.contains("Other copper golems ignore a chest someone else placed"));
    }
}

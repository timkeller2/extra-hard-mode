package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DragonRulesTest {
    @Test
    void autoRespawnDefaultOffRequiresDeadDragonAndNoRitual() {
        assertFalse(DragonRules.shouldAutoRespawn(false, true, false, false));
        assertTrue(DragonRules.shouldAutoRespawn(true, true, false, false));
        assertFalse(DragonRules.shouldAutoRespawn(true, true, true, false));
        assertFalse(DragonRules.shouldAutoRespawn(true, false, false, false));
        assertFalse(DragonRules.shouldAutoRespawn(true, true, false, true));
    }

    @Test
    void healIsQuarterOfMaxByDefault() {
        assertEquals(200.0F, DragonRules.healAmount(800.0F, 25));
        assertEquals(0.0F, DragonRules.healAmount(800.0F, 0));
        assertEquals(0.0F, DragonRules.healAmount(0.0F, 25));
    }

    @Test
    void fillHealthWhenWasAtPreviousMax() {
        assertTrue(DragonRules.shouldFillHealth(200.0, 200.0F, 800.0));
        assertFalse(DragonRules.shouldFillHealth(200.0, 50.0F, 800.0));
        assertFalse(DragonRules.shouldFillHealth(800.0, 800.0F, 800.0));
        assertFalse(DragonRules.shouldFillHealth(200.0, 200.0F, 0.0));
    }

    @Test
    void endPlaceAllowsCrystalChestChorusOnly() {
        assertTrue(DragonRules.allowEndPlace("minecraft:end_crystal"));
        assertTrue(DragonRules.allowEndPlace("minecraft:ender_chest"));
        assertTrue(DragonRules.allowEndPlace("minecraft:chorus_flower"));
        assertFalse(DragonRules.allowEndPlace("minecraft:cobblestone"));
        assertFalse(DragonRules.allowEndPlace("minecraft:obsidian"));
        assertFalse(DragonRules.allowEndPlace(""));
    }

    @Test
    void endNoBuildDeniesBucketsAndBlocksAllowsCrystal() {
        assertTrue(DragonRules.denyEndUse(true, true, false, false, false, true));
        assertFalse(DragonRules.denyEndUse(true, true, false, false, true, true));
        assertFalse(DragonRules.denyEndUse(true, true, false, true, false, false));
        assertFalse(DragonRules.denyEndUse(true, true, true, false, false, true));
        assertFalse(DragonRules.denyEndUse(true, false, false, false, false, true));
        assertFalse(DragonRules.denyEndUse(false, true, false, false, false, true));
        assertFalse(DragonRules.denyEndUse(true, true, false, false, false, false));
    }

    @Test
    void defaultMinionTable() {
        assertEquals(DragonRules.MinionRoll.BLAZE, DragonRules.roll(0, false));
        assertEquals(DragonRules.MinionRoll.BLAZE, DragonRules.roll(39, false));
        assertEquals(DragonRules.MinionRoll.ZOMBIE_VILLAGERS, DragonRules.roll(40, false));
        assertEquals(DragonRules.MinionRoll.ZOMBIE_VILLAGERS, DragonRules.roll(69, false));
        assertEquals(DragonRules.MinionRoll.ENDERMAN, DragonRules.roll(70, false));
        assertEquals(DragonRules.MinionRoll.ENDERMAN, DragonRules.roll(99, false));
        assertEquals(DragonRules.MinionRoll.NONE, DragonRules.roll(100, false));
    }

    @Test
    void alternativeMinionTableIncludesSkeletons() {
        assertEquals(DragonRules.MinionRoll.BLAZE, DragonRules.roll(9, true));
        assertEquals(DragonRules.MinionRoll.ZOMBIE_VILLAGERS, DragonRules.roll(10, true));
        assertEquals(DragonRules.MinionRoll.SKELETONS, DragonRules.roll(50, true));
        assertEquals(DragonRules.MinionRoll.SKELETONS, DragonRules.roll(79, true));
        assertEquals(DragonRules.MinionRoll.ENDERMAN, DragonRules.roll(80, true));
        assertEquals(DragonRules.MinionRoll.NONE, DragonRules.roll(100, true));
        assertEquals(DragonRules.MinionRoll.NONE, DragonRules.roll(149, true));
    }
}

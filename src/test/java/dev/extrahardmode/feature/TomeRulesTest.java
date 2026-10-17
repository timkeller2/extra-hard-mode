package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TomeRulesTest {
    @Test
    void nextLevelCostMatchesTheGameCurve() {
        assertEquals(7, TomeRules.xpForNextLevel(0));
        assertEquals(35, TomeRules.xpForNextLevel(14));
        assertEquals(37, TomeRules.xpForNextLevel(15));
        assertEquals(107, TomeRules.xpForNextLevel(29));
        assertEquals(112, TomeRules.xpForNextLevel(30));
        assertEquals(121, TomeRules.xpForNextLevel(31));
        assertEquals(7, TomeRules.xpForNextLevel(-4));
    }

    @Test
    void level30Costs1395AndKeepsNothingAboveIt() {
        assertEquals(0, TomeRules.xpToReachLevel(0));
        assertEquals(7, TomeRules.xpToReachLevel(1));
        assertEquals(315, TomeRules.xpToReachLevel(15));
        assertEquals(1395, TomeRules.xpToReachLevel(30));
        assertEquals(1507, TomeRules.xpToReachLevel(31));
        assertEquals(1395, TomeRules.totalXp(30, 0.0F));
        assertEquals(1451, TomeRules.totalXp(30, 0.5F));
        assertEquals(1507, TomeRules.totalXp(31, 0.0F));
        assertEquals(0, TomeRules.excessAboveKeptLevel(1395));
        assertEquals(56, TomeRules.excessAboveKeptLevel(1451));
        assertEquals(112, TomeRules.excessAboveKeptLevel(1507));
        assertEquals(0, TomeRules.excessAboveKeptLevel(0));
    }

    @Test
    void writeRequiresALevelAbove30() {
        assertFalse(TomeRules.canWrite(30));
        assertFalse(TomeRules.canWrite(0));
        assertTrue(TomeRules.canWrite(31));
    }

    @Test
    void bothStepsKeepTheFullAmountPlusFivePercentPerLevel() {
        assertEquals(118, TomeRules.share(112, 1.0));
        assertEquals(129, TomeRules.share(112, 3.0));
        assertEquals(11, TomeRules.share(10, 1.0));
        assertEquals(9, TomeRules.share(9, 1.0));
        assertEquals(1, TomeRules.share(1, 1.0));
        assertEquals(0, TomeRules.share(0, 5.0));
        assertEquals(0, TomeRules.share(100, 0.0));
        assertEquals(6, TomeRules.share(6, 1.0));
        assertEquals(7, TomeRules.share(6, 4.0));
        assertEquals(9, TomeRules.share(6, 10.0));
    }

    @Test
    void noteNamesTheAuthorAndTheSavedExperience() {
        assertEquals("Tim saved 6 experience in this tome.", TomeRules.note("Tim", 6));
        assertEquals("Someone saved 6 experience in this tome.", TomeRules.note("  ", 6));
        assertEquals("Someone saved 0 experience in this tome.", TomeRules.note(null, -3));
        assertEquals(16, TomeRules.TITLE.length());
        assertEquals("tougher_xp", TomeRules.XP_KEY);
    }
}

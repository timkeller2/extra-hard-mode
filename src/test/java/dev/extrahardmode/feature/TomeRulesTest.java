package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void level30OrBelowBanksEverythingAndAbove30BanksOnlyTheExcess() {
        assertEquals(0, TomeRules.levelAfterWrite(30));
        assertEquals(0, TomeRules.levelAfterWrite(0));
        assertEquals(0, TomeRules.levelAfterWrite(15));
        assertEquals(30, TomeRules.levelAfterWrite(31));
        assertEquals(1395, TomeRules.bankedXp(30, 1395));
        assertEquals(1451, TomeRules.bankedXp(30, 1451));
        assertEquals(315, TomeRules.bankedXp(15, 315));
        assertEquals(0, TomeRules.bankedXp(0, 0));
        assertEquals(112, TomeRules.bankedXp(31, 1507));
        assertEquals(56, TomeRules.bankedXp(31, 1451));
    }

    @Test
    void bothStepsUseEightyPercentPlusTwoPercentPerLevel() {
        assertEquals(92, TomeRules.share(112, 1.0));
        assertEquals(96, TomeRules.share(112, 3.0));
        assertEquals(8, TomeRules.share(10, 1.0));
        assertEquals(7, TomeRules.share(9, 1.0));
        assertEquals(1, TomeRules.share(1, 1.0));
        assertEquals(0, TomeRules.share(0, 5.0));
        assertEquals(0, TomeRules.share(100, 0.0));
        assertEquals(5, TomeRules.share(6, 1.0));
        assertEquals(5, TomeRules.share(6, 4.0));
        assertEquals(6, TomeRules.share(6, 10.0));
        assertEquals(98, TomeRules.share(100, 9.0));
        assertEquals(100, TomeRules.share(100, 10.0));
        assertEquals(102, TomeRules.share(100, 11.0));
        assertEquals(100, TomeRules.share(TomeRules.share(100, 10.0), 10.0));
        assertEquals(96, TomeRules.share(TomeRules.share(100, 9.0), 9.0));
        assertEquals(104, TomeRules.share(TomeRules.share(100, 11.0), 11.0));
        assertEquals(92, TomeRules.share(TomeRules.bankedXp(31, 1507), 1.0));
        assertEquals(1144, TomeRules.share(TomeRules.bankedXp(30, 1395), 1.0));
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

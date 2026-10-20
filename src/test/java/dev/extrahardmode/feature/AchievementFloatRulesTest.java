package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AchievementFloatRulesTest {
    @Test
    void placementCountShowsOnlyWhileSneaking() {
        assertTrue(AchievementFloatRules.showPlacementCount(true));
        assertFalse(AchievementFloatRules.showPlacementCount(false));
    }

    @Test
    void remainingIsTheGapToTheNextTier() {
        assertEquals(255, AchievementFloatRules.remaining(256, 1));
        assertEquals(1, AchievementFloatRules.remaining(25, 24));
        assertEquals(256, AchievementFloatRules.remaining(512, 256));
    }

    @Test
    void finishedTrackShowsNothing() {
        int awarded = AchievementRules.awardedAfter(0, 1024, AchievementRules.BUILDER_THRESHOLDS);
        int next = AchievementRules.nextTarget(1024, AchievementRules.BUILDER_THRESHOLDS, awarded);
        assertEquals(0, AchievementFloatRules.remaining(next, 1024));
        assertEquals(0, AchievementFloatRules.remaining(0, 10));
        assertEquals(0, AchievementFloatRules.remaining(25, 25));
    }

    @Test
    void numberLivesForThreeSecondsAndDriftsDownLeft() {
        assertTrue(AchievementFloatRules.alive(0));
        assertTrue(AchievementFloatRules.alive(2999));
        assertFalse(AchievementFloatRules.alive(3000));
        assertFalse(AchievementFloatRules.alive(-1));
        assertEquals(0f, AchievementFloatRules.driftBlocks(0), 1.0E-4);
        assertEquals(AchievementFloatRules.DRIFT_BLOCKS / 2f, AchievementFloatRules.driftBlocks(1500), 1.0E-3);
        assertEquals(AchievementFloatRules.DRIFT_BLOCKS, AchievementFloatRules.driftBlocks(3000), 1.0E-4);
    }
}

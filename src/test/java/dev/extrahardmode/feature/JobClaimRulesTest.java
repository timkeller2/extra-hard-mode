package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JobClaimRulesTest {
    @Test
    void capIsTheRoundedUpEighth() {
        assertEquals(0, JobClaimRules.cap(0));
        assertEquals(1, JobClaimRules.cap(1));
        assertEquals(1, JobClaimRules.cap(8));
        assertEquals(2, JobClaimRules.cap(9));
        assertEquals(2, JobClaimRules.cap(16));
        assertEquals(3, JobClaimRules.cap(17));
    }

    @Test
    void claimStopsWhenTheJobAlreadyHasItsShare() {
        assertFalse(JobClaimRules.claimBlocked(0, 1));
        assertFalse(JobClaimRules.claimBlocked(0, 8));
        assertTrue(JobClaimRules.claimBlocked(1, 8));
        assertFalse(JobClaimRules.claimBlocked(1, 9));
        assertTrue(JobClaimRules.claimBlocked(2, 9));
        assertTrue(JobClaimRules.claimBlocked(2, 16));
        assertFalse(JobClaimRules.claimBlocked(2, 17));
    }

    @Test
    void rangeIncludesExactlySixtyFourBlocks() {
        assertTrue(JobClaimRules.within(0));
        assertTrue(JobClaimRules.within(64.0 * 64.0));
        assertFalse(JobClaimRules.within(64.0 * 64.0 + 0.01));
    }

    @Test
    void nitwitAndUnemployedAreNotJobs() {
        assertFalse(JobClaimRules.isClaimedJob(true, false));
        assertFalse(JobClaimRules.isClaimedJob(false, true));
        assertTrue(JobClaimRules.isClaimedJob(false, false));
    }
}

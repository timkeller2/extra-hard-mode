package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JobSiteLossRulesTest {
    @Test
    void aLostWorkBlockMakesAWorkingVillagerANitwit() {
        assertTrue(JobSiteLossRules.workBlockLost(true, false));
        assertFalse(JobSiteLossRules.workBlockLost(true, true));
        assertFalse(JobSiteLossRules.workBlockLost(false, false));
        assertFalse(JobSiteLossRules.workBlockLost(false, true));

        assertTrue(JobSiteLossRules.becomesNitwit(false, false, false));
        assertFalse(JobSiteLossRules.becomesNitwit(true, false, false));
        assertFalse(JobSiteLossRules.becomesNitwit(false, true, false));
        assertFalse(JobSiteLossRules.becomesNitwit(false, false, true));
    }
}

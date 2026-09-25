package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DiamondSkinRulesTest {
    @Test
    void dividesHealthDamageByAbilityLevel() {
        assertEquals(8.0F, DiamondSkinRules.scaleHealthDamage(8.0F, 1.0, false), 1e-4F);
        assertEquals(4.0F, DiamondSkinRules.scaleHealthDamage(8.0F, 2.0, false), 1e-4F);
        assertEquals(2.0F, DiamondSkinRules.scaleHealthDamage(8.0F, 4.0, false), 1e-4F);
        assertEquals(3.2F, DiamondSkinRules.scaleHealthDamage(8.0F, 2.5, false), 1e-4F);
        assertEquals(8.0F, DiamondSkinRules.scaleHealthDamage(8.0F, 0.5, false), 1e-4F);
        assertEquals(0.0F, DiamondSkinRules.scaleHealthDamage(0.0F, 4.0, false), 1e-4F);
        assertEquals(8.0F, DiamondSkinRules.scaleHealthDamage(8.0F, 4.0, true), 1e-4F);
        assertTrue(DiamondSkinRules.reduced(8.0F, 2.0F));
        assertFalse(DiamondSkinRules.reduced(8.0F, 8.0F));
        assertEquals(0, DiamondSkinRules.durationLossTicks(0.0F));
        assertEquals(60, DiamondSkinRules.durationLossTicks(1.0F));
        assertEquals(180, DiamondSkinRules.durationLossTicks(3.0F));
        assertEquals(150, DiamondSkinRules.durationLossTicks(2.5F));
        assertEquals(40, DiamondSkinRules.remainingAfterAbsorption(100, 1.0F));
        assertEquals(0, DiamondSkinRules.remainingAfterAbsorption(30, 1.0F));
    }

    @Test
    void poisonMarkerOnlyCoversThatVictim() {
        Object victim = new Object();
        Object other = new Object();
        assertFalse(DiamondSkinRules.isPoisonVictim(victim));
        DiamondSkinRules.beginPoison(victim);
        assertTrue(DiamondSkinRules.isPoisonVictim(victim));
        assertFalse(DiamondSkinRules.isPoisonVictim(other));
        DiamondSkinRules.endPoison(other);
        assertTrue(DiamondSkinRules.isPoisonVictim(victim));
        DiamondSkinRules.endPoison(victim);
        assertFalse(DiamondSkinRules.isPoisonVictim(victim));
    }
}

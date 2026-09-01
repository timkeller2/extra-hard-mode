package dev.extrahardmode.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.extrahardmode.api.ExplosionType;
import org.junit.jupiter.api.Test;

class ExplosionSettingsTest {
    @Test
    void borderInclusiveIsCaveBand() {
        ExplosionSettings.Applied below = new ExplosionSettings.Applied(5.0F, false, true);
        ExplosionSettings.Applied above = new ExplosionSettings.Applied(3.0F, false, true);
        assertEquals(5.0F, ExplosionSettings.resolve(48.0, 48, below, above).power());
        assertEquals(3.0F, ExplosionSettings.resolve(48.001, 48, below, above).power());
        assertEquals(5.0F, ExplosionSettings.resolve(-64.0, 48, below, above).power());
    }

    @Test
    void rootNodeTntIsFiveBelowThreeAbove() {
        ExplosionSettings.Applied below = ExplosionSettings.resolve(ExplosionType.TNT, 0, ExplosionSettings.BORDER_Y);
        ExplosionSettings.Applied above = ExplosionSettings.resolve(ExplosionType.TNT, 64, ExplosionSettings.BORDER_Y);
        assertEquals(5.0F, below.power());
        assertFalse(below.fire());
        assertTrue(below.worldDamage());
        assertEquals(3.0F, above.power());
        assertFalse(above.fire());
        assertTrue(above.worldDamage());
    }

    @Test
    void ghastAndBlazeUseFire() {
        ExplosionSettings.Applied ghast = ExplosionSettings.resolve(ExplosionType.GHAST_FIREBALL, 0, 48);
        ExplosionSettings.Applied blaze = ExplosionSettings.resolve(ExplosionType.OVERWORLD_BLAZE, 100, 48);
        ExplosionSettings.Applied magma = ExplosionSettings.resolve(ExplosionType.MAGMACUBE_FIRE, 20, 48);
        assertEquals(2.0F, ghast.power());
        assertTrue(ghast.fire());
        assertEquals(4.0F, blaze.power());
        assertTrue(blaze.fire());
        assertEquals(2.0F, magma.power());
        assertTrue(magma.fire());
        ExplosionSettings.Applied dragon = ExplosionSettings.resolve(ExplosionType.DRAGON_FIREBALL, 0, 48);
        assertEquals(2.0F, dragon.power());
        assertTrue(dragon.fire());
        assertTrue(dragon.worldDamage());
    }

    @Test
    void creeperChargedAndEffect() {
        assertEquals(3.0F, ExplosionSettings.resolve(ExplosionType.CREEPER, 0, 48).power());
        assertEquals(4.0F, ExplosionSettings.resolve(ExplosionType.CREEPER_CHARGED, 80, 48).power());
        ExplosionSettings.Applied effect = ExplosionSettings.resolve(ExplosionType.EFFECT, 0, 48);
        assertEquals(0.0F, effect.power());
        assertFalse(effect.fire());
        assertFalse(effect.worldDamage());
    }

    @Test
    void cancelAndNoWorldDamageExtinguishFire() {
        assertFalse(ExplosionSettings.allowFire(true, true, true));
        assertFalse(ExplosionSettings.allowFire(false, false, true));
        assertTrue(ExplosionSettings.allowFire(false, true, true));
        assertFalse(ExplosionSettings.allowFire(false, true, false));
    }

    @Test
    void cratersOnlyWhenFirstBlastBreaksBlocks() {
        assertTrue(ExplosionSettings.shouldScheduleCraters(true, true));
        assertFalse(ExplosionSettings.shouldScheduleCraters(true, false));
        assertFalse(ExplosionSettings.shouldScheduleCraters(false, true));
    }
}

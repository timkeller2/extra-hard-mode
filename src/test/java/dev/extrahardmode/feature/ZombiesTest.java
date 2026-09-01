package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.extrahardmode.feature.monster.Zombies;
import dev.extrahardmode.module.EntityHelper;
import dev.extrahardmode.task.CoolCreeperExplosion;
import org.junit.jupiter.api.Test;

class ZombiesTest {
    @Test
    void reanimateChanceDecaysByCount() {
        assertEquals(50, Zombies.reanimateChance(50, 1));
        assertEquals(25, Zombies.reanimateChance(50, 2));
        assertEquals(16, Zombies.reanimateChance(50, 3));
        assertEquals(12, Zombies.reanimateChance(50, 4));
    }

    @Test
    void reanimateChanceTreatsZeroAsFirstDeath() {
        assertEquals(50, Zombies.reanimateChance(50, 0));
        assertEquals(50, Zombies.reanimateChance(50, -1));
    }

    @Test
    void delayIsThreeToEightSeconds() {
        assertEquals(60, Zombies.reanimateDelayTicks(0));
        assertEquals(80, Zombies.reanimateDelayTicks(1));
        assertEquals(160, Zombies.reanimateDelayTicks(5));
        for (int n = 0; n < 6; n++) {
            int ticks = Zombies.reanimateDelayTicks(n);
            assertTrue(ticks >= 60 && ticks <= 160, "delay ticks " + ticks);
            assertEquals(0, ticks % 20);
        }
    }

    @Test
    void burningCreeperExplodesOnOriginalSuicideOffset() {
        assertEquals(26, CoolCreeperExplosion.explodeDelayTicks(3));
        assertEquals(3 + 8, CoolCreeperExplosion.explodeDelayTicks(0));
        assertEquals(5 + 3 + 8, CoolCreeperExplosion.explodeDelayTicks(1));
    }

    @Test
    void percentRolls() {
        assertFalse(EntityHelper.percent(0, 0));
        assertTrue(EntityHelper.percent(0, 100));
        assertTrue(EntityHelper.percent(0, 50));
        assertFalse(EntityHelper.percent(50, 50));
        assertTrue(EntityHelper.percent(49, 50));
    }
}

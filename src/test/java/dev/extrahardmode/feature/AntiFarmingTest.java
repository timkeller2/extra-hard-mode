package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AntiFarmingTest {
    @Test
    void darkAlwaysDies() {
        assertEquals(100, AntiFarming.deathProbability(25, 9, false, true, 7));
        assertEquals(100, AntiFarming.deathProbability(25, 0, true, true, 0));
    }

    @Test
    void tendedCropUsesBaseLossRate() {
        assertEquals(25, AntiFarming.deathProbability(25, 15, false, true, 7));
    }

    @Test
    void unwateredAddsTwentyFive() {
        assertEquals(50, AntiFarming.deathProbability(25, 15, false, true, 0));
    }

    @Test
    void desertAddsFifty() {
        assertEquals(75, AntiFarming.deathProbability(25, 15, true, true, 7));
        assertEquals(25, AntiFarming.deathProbability(25, 15, true, false, 7));
    }

    @Test
    void desertAndDryCapsAtOneHundred() {
        assertEquals(100, AntiFarming.deathProbability(25, 15, true, true, 0));
    }

    @Test
    void breedCooldownIsSixTimesVanillaByDefault() {
        assertEquals(6000, AntiFarming.VANILLA_BREED_COOLDOWN_TICKS);
        assertEquals(6, AntiFarming.DEFAULT_BREED_COOLDOWN_MULTIPLIER);
        assertEquals(6000, AntiFarming.breedCooldownTicks(6000, 1));
        assertEquals(36000, AntiFarming.breedCooldownTicks(6000, 6));
        assertEquals(6000, AntiFarming.breedCooldownTicks(6000, 0));
        assertEquals(12000, AntiFarming.breedCooldownTicks(6000, 2));
    }

    @Test
    void eggLayTimeIsTwiceVanillaByDefault() {
        assertEquals(6000, AntiFarming.VANILLA_EGG_LAY_SPAN_TICKS);
        assertEquals(2, AntiFarming.DEFAULT_EGG_LAY_TIME_MULTIPLIER);
        assertEquals(6000, AntiFarming.eggLayTime(6000, 1));
        assertEquals(12000, AntiFarming.eggLayTime(6000, 2));
        assertEquals(6000, AntiFarming.eggLayTime(6000, 0));
        assertEquals(24000, AntiFarming.eggLayTime(6000, 4));
    }

    @Test
    void soilModifierRaisesAndLowersDeathChance() {
        assertEquals(35, AntiFarming.deathProbability(25, 15, false, true, 7, 10));
        assertEquals(10, AntiFarming.deathProbability(25, 15, false, true, 7, -15));
        assertEquals(26, AntiFarming.deathProbability(51, 15, false, true, 7, -25));
        assertEquals(0, AntiFarming.deathProbability(25, 15, false, true, 7, -40));
        assertEquals(100, AntiFarming.deathProbability(25, 9, false, true, 7, -15));
        assertEquals(100, AntiFarming.deathProbability(25, 15, true, true, 0, 10));
    }

    @Test
    void floatTextIsSlowerAndLarger() {
        assertEquals(40, AntiFarming.FLOAT_TEXT_TICKS);
        assertEquals(0.5F, AntiFarming.FLOAT_TEXT_SCALE, 1.0E-4F);
        assertEquals(0.02, AntiFarming.FLOAT_TEXT_RISE, 1.0E-6);
    }

    @Test
    void hiveHoneycombDefaultsToOne() {
        assertEquals(3, AntiFarming.VANILLA_HIVE_HONEYCOMB);
        assertEquals(1, AntiFarming.DEFAULT_HIVE_HONEYCOMB);
        assertEquals(1, AntiFarming.hiveHoneycombCount(1));
        assertEquals(3, AntiFarming.hiveHoneycombCount(3));
        assertEquals(0, AntiFarming.hiveHoneycombCount(0));
        assertEquals(0, AntiFarming.hiveHoneycombCount(-4));
        assertEquals(64, AntiFarming.hiveHoneycombCount(99));
    }
}

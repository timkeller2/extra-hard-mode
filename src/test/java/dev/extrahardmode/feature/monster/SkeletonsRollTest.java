package dev.extrahardmode.feature.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

class SkeletonsRollTest {
    @Test
    void hundredPercentSnowballStarvesLaterEntries() {
        RandomSource random = RandomSource.create(0L);
        assertEquals(
                Skeletons.Special.SNOWBALL,
                Skeletons.roll(random, true, 100, true, 100, true, 100, true, 100, true));
    }

    @Test
    void disabledSnowballFallsThroughToFirework() {
        RandomSource random = RandomSource.create(0L);
        assertEquals(
                Skeletons.Special.FIREWORK,
                Skeletons.roll(random, false, 100, true, 100, true, 100, true, 100, true));
    }

    @Test
    void specialChanceIsOnePercentPerTenBlocks() {
        assertEquals(10, Skeletons.SPECIAL_BLOCKS_PER_PERCENT);
        assertEquals(1000, Skeletons.SPECIAL_FULL_DISTANCE);
        assertEquals(0, Skeletons.specialChancePercent(0.0));
        assertEquals(0, Skeletons.specialChancePercent(9.9));
        assertEquals(1, Skeletons.specialChancePercent(10.0));
        assertEquals(10, Skeletons.specialChancePercent(100.0));
        assertEquals(50, Skeletons.specialChancePercent(500.0));
        assertEquals(99, Skeletons.specialChancePercent(999.0));
        assertEquals(100, Skeletons.specialChancePercent(1000.0));
        assertEquals(100, Skeletons.specialChancePercent(2500.0));
        assertEquals(0, Skeletons.specialChancePercent(Double.NaN));
    }

    @Test
    void zeroPercentNeverSelects() {
        RandomSource random = RandomSource.create(0L);
        assertEquals(
                Skeletons.Special.NONE, Skeletons.roll(random, true, 0, true, 0, true, 0, true, 0, true));
    }
}

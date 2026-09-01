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
    void zeroPercentNeverSelects() {
        RandomSource random = RandomSource.create(0L);
        assertEquals(
                Skeletons.Special.NONE, Skeletons.roll(random, true, 0, true, 0, true, 0, true, 0, true));
    }
}

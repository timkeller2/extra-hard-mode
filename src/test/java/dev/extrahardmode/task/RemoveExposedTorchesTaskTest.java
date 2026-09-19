package dev.extrahardmode.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RemoveExposedTorchesTaskTest {
    @Test
    void rainPassScansEveryTickingChunkEachSecond() {
        assertEquals(20, RemoveExposedTorchesTask.CHUNK_STAGGER_TICKS);
        assertEquals(0.01F, RemoveExposedTorchesTask.BREAK_CHANCE);
        assertEquals(16, RemoveExposedTorchesTask.MAX_TORCHES_PER_TICK);
        assertEquals(8, RemoveExposedTorchesTask.COLUMN_SCAN_DEPTH);
    }

    @Test
    void surfaceBlockYStepsOffTheAirAboveTheHeightmap() {
        assertEquals(64, RemoveExposedTorchesTask.surfaceBlockY(65, true));
        assertEquals(64, RemoveExposedTorchesTask.surfaceBlockY(64, false));
        assertEquals(-64, RemoveExposedTorchesTask.surfaceBlockY(-63, true));
    }

    @Test
    void coveredColumnsStopTheScan() {
        assertTrue(RemoveExposedTorchesTask.coveredFromSky(false));
        assertFalse(RemoveExposedTorchesTask.coveredFromSky(true));
    }
}

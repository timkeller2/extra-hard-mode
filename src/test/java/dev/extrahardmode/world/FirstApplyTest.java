package dev.extrahardmode.world;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FirstApplyTest {
    @Test
    void alreadyAppliedSkipsRewrite() {
        Set<String> applied = new HashSet<>();
        applied.add(FirstApply.OVERWORLD);
        assertTrue(FirstApply.alreadyApplied(applied, FirstApply.OVERWORLD));
        assertFalse(FirstApply.alreadyApplied(applied, FirstApply.NETHER));
    }

    @Test
    void vanillaDimensionFlagsDefaultTrue() {
        assertTrue(FirstApply.resolveDimensionEnabled(FirstApply.OVERWORLD, false));
        assertTrue(FirstApply.resolveDimensionEnabled(FirstApply.NETHER, false));
        assertTrue(FirstApply.resolveDimensionEnabled(FirstApply.END, null));
    }

    @Test
    void customDimensionsInheritOverworldLiveFlag() {
        assertFalse(FirstApply.resolveDimensionEnabled("custom:dim", false));
        assertTrue(FirstApply.resolveDimensionEnabled("custom:dim", true));
        assertTrue(FirstApply.resolveDimensionEnabled("custom:dim", null));
    }

    @Test
    void vanillaPredicate() {
        assertTrue(FirstApply.isVanillaDimension(FirstApply.OVERWORLD));
        assertTrue(FirstApply.isVanillaDimension(FirstApply.NETHER));
        assertTrue(FirstApply.isVanillaDimension(FirstApply.END));
        assertFalse(FirstApply.isVanillaDimension("mod:custom"));
    }
}

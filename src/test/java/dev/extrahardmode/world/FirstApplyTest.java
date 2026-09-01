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
    void vanillaDimensionsUseToml() {
        assertTrue(FirstApply.resolveEnabled(FirstApply.OVERWORLD, true, false));
        assertFalse(FirstApply.resolveEnabled(FirstApply.NETHER, false, true));
        assertTrue(FirstApply.resolveEnabled(FirstApply.END, true, null));
    }

    @Test
    void customDimensionsInheritOverworldGamerule() {
        assertFalse(FirstApply.resolveEnabled("custom:dim", true, false));
        assertTrue(FirstApply.resolveEnabled("custom:dim", false, true));
        assertTrue(FirstApply.resolveEnabled("custom:dim", true, null));
        assertFalse(FirstApply.resolveEnabled("custom:dim", false, null));
    }

    @Test
    void vanillaPredicate() {
        assertTrue(FirstApply.isVanillaDimension(FirstApply.OVERWORLD));
        assertTrue(FirstApply.isVanillaDimension(FirstApply.NETHER));
        assertTrue(FirstApply.isVanillaDimension(FirstApply.END));
        assertFalse(FirstApply.isVanillaDimension("mod:custom"));
    }
}

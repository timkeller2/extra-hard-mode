package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SoftenMapTest {
    @Test
    void stoneBecomesCobble() {
        assertEquals("minecraft:cobblestone", SoftenMap.resultId("minecraft:stone"));
        assertEquals("minecraft:cobblestone", SoftenMap.resultId("stone"));
    }

    @Test
    void deepslateBecomesCobbledDeepslate() {
        assertEquals("minecraft:cobbled_deepslate", SoftenMap.resultId("minecraft:deepslate"));
        assertEquals("minecraft:cobbled_deepslate", SoftenMap.resultId("minecraft:infested_deepslate"));
    }

    @Test
    void tuffIsNotSoftened() {
        assertNull(SoftenMap.resultId("minecraft:tuff"));
    }

    @Test
    void parseRejectsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> SoftenMap.parseAll(List.of()));
        assertThrows(IllegalArgumentException.class, () -> SoftenMap.parseAll(List.of("stone")));
    }

    @Test
    void parsePreservesOrder() {
        Map<String, String> parsed = SoftenMap.parseAll(SoftenMap.DEFAULT_ENTRIES);
        assertEquals("minecraft:cobblestone", parsed.get("minecraft:stone"));
        assertEquals("minecraft:cobbled_deepslate", parsed.get("minecraft:deepslate"));
        assertEquals(4, parsed.size());
    }
}

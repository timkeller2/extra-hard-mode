package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MoreTntTest {
    @Test
    void gameruleOffYieldsVanillaOne() {
        assertEquals(1, MoreTnt.resultCount(false, 3));
        assertEquals(1, MoreTnt.resultCount(false, 1));
    }

    @Test
    void moduleOnHonorsPerRecipe() {
        assertEquals(3, MoreTnt.resultCount(true, 3));
        assertEquals(1, MoreTnt.resultCount(true, 1));
        assertEquals(1, MoreTnt.resultCount(true, 0));
    }
}

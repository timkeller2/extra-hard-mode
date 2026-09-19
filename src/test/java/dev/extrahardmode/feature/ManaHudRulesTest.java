package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ManaHudRulesTest {
    @Test
    void preferredSpacingUntilTen() {
        ManaHudRules.Layout five = ManaHudRules.layout(5);
        assertEquals(8, five.spacing());
        assertEquals(10, five.perRow());
        assertEquals(1, five.rows());
        ManaHudRules.Layout ten = ManaHudRules.layout(10);
        assertEquals(8, ten.spacing());
        assertEquals(1, ten.rows());
    }

    @Test
    void packsCloserBeforeWrapping() {
        ManaHudRules.Layout packed = ManaHudRules.layout(15);
        assertEquals(1, packed.rows());
        assertEquals(5, packed.spacing());
        assertEquals(15, packed.perRow());
    }

    @Test
    void wrapsWhenMinSpacingCannotFitOneRow() {
        ManaHudRules.Layout wrapped = ManaHudRules.layout(30);
        assertEquals(4, wrapped.spacing());
        assertEquals(19, wrapped.perRow());
        assertEquals(2, wrapped.rows());
        assertEquals(20, wrapped.height());
    }

    @Test
    void twoManaLevelsMakeOneCrystal() {
        assertEquals(0, ManaHudRules.crystalCount(0));
        assertEquals(1, ManaHudRules.crystalCount(1));
        assertEquals(1, ManaHudRules.crystalCount(2));
        assertEquals(3, ManaHudRules.crystalCount(5));
        assertEquals(6, ManaHudRules.crystalCount(12));
        assertEquals(10, ManaHudRules.crystalCount(20));
        assertEquals(10, ManaHudRules.crystalCount(30));
        assertEquals(4, ManaHudRules.crystalCount(2, 8.0));
        assertEquals(10, ManaHudRules.crystalCount(2, 20.0));
        assertEquals(1, ManaHudRules.crystalCount(2, 2.0));
    }

    @Test
    void fillIsFullHalfOrEmptyLikeHearts() {
        assertEquals(ManaHudRules.Fill.FULL, ManaHudRules.fill(0, 3.2F));
        assertEquals(ManaHudRules.Fill.HALF, ManaHudRules.fill(1, 3.2F));
        assertEquals(ManaHudRules.Fill.EMPTY, ManaHudRules.fill(2, 3.2F));
        assertEquals(ManaHudRules.Fill.EMPTY, ManaHudRules.fill(0, 0.0F));
        assertEquals(ManaHudRules.Fill.EMPTY, ManaHudRules.fill(0, 0.5F));
        assertEquals(ManaHudRules.Fill.HALF, ManaHudRules.fill(0, 1.0F));
        assertEquals(ManaHudRules.Fill.FULL, ManaHudRules.fill(0, 2.0F));
    }

    @Test
    void offsetsWrapUpward() {
        ManaHudRules.Layout layout = ManaHudRules.layout(30);
        assertEquals(0, ManaHudRules.offsetX(0, layout));
        assertEquals(0, ManaHudRules.offsetY(0, layout));
        assertEquals(4, ManaHudRules.offsetX(1, layout));
        assertEquals(0, ManaHudRules.offsetY(18, layout));
        assertEquals(0, ManaHudRules.offsetX(19, layout));
        assertEquals(-10, ManaHudRules.offsetY(19, layout));
    }
}

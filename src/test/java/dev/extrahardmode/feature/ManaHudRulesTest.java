package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals(15, ManaHudRules.crystalCount(30));
        assertEquals(20, ManaHudRules.crystalCount(40));
        assertEquals(4, ManaHudRules.crystalCount(2, 8.0));
        assertEquals(10, ManaHudRules.crystalCount(2, 20.0));
        assertEquals(20, ManaHudRules.crystalCount(2, 40.0));
        assertEquals(20, ManaHudRules.crystalCount(2, 50.0));
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

    @Test
    void tenMinutePreviewCompoundsRealRegen() {
        assertEquals(10, ManaHudRules.PREVIEW_MINUTES);
        assertEquals(0.0, ManaHudRules.previewMana(0, 5.0, 20.0, 4.0), 0.0);
        assertEquals(40.0, ManaHudRules.previewMana(10, 40.0, 20.0, 4.0), 0.0);
        assertEquals(40.0, ManaHudRules.previewMana(10, 39.9, 20.0, 100.0), 0.0);

        double hungry = stepped(10, 0.0, 0.0, 1.0);
        assertEquals(hungry, ManaHudRules.previewMana(10, 0.0, 0.0, 1.0), 0.0);
        assertTrue(hungry > 0.50 && hungry < 0.53);

        double fed = ManaHudRules.previewMana(10, 0.0, 18.0, 1.0);
        assertEquals(stepped(10, 0.0, 18.0, 1.0), fed, 0.0);
        assertEquals(fed, ManaHudRules.previewMana(10, 0.0, 0.0, 2.0), 1e-9);
        assertTrue(fed > ManaHudRules.previewMana(10, 0.0, 17.9, 1.0) * 1.9);

        double overflow = ManaHudRules.previewMana(10, 10.0, 0.0, 1.0);
        assertEquals(stepped(10, 10.0, 0.0, 1.0), overflow, 0.0);
        assertTrue(overflow - 10.0 < hungry * 0.6);
        assertTrue(overflow < AchievementRules.manaCap(10));
    }

    @Test
    void previewSpanCoversOnlyFutureMana() {
        assertFalse(ManaHudRules.previewSpan(0, 4.0, 4.0).visible());
        assertEquals(2, ManaHudRules.fillPixels(0.4));
        assertEquals(5, ManaHudRules.fillPixels(1.0));
        assertEquals(9, ManaHudRules.fillPixels(2.0));

        ManaHudRules.PreviewSpan partial = ManaHudRules.previewSpan(0, 0.4, 0.9);
        assertEquals(ManaHudRules.fillPixels(0.4), partial.fromPixel());
        assertEquals(ManaHudRules.fillPixels(0.9), partial.toPixel());
        assertTrue(partial.fromPixel() > 0);

        assertFalse(ManaHudRules.previewSpan(0, 2.0, 2.4).visible());
        ManaHudRules.PreviewSpan next = ManaHudRules.previewSpan(1, 2.0, 2.4);
        assertEquals(0, next.fromPixel());
        assertTrue(next.toPixel() > 0);

        assertFalse(ManaHudRules.previewSpan(0, 3.2, 4.0).visible());
        assertTrue(ManaHudRules.previewSpan(1, 3.2, 4.0).visible());
        assertEquals(3, ManaHudRules.drawnCrystalCount(4, 4.0, 4.4));
        assertEquals(5, ManaHudRules.drawnCrystalCount(10, 0.0, 0.5));

        ManaHudRules.PreviewSpan clipped =
                ManaHudRules.visibleSpan(new ManaHudRules.PreviewSpan(4, 9), true);
        assertEquals(4, clipped.fromPixel());
        assertEquals(ManaHudRules.HALF_WIDTH, clipped.toPixel());
        assertFalse(ManaHudRules.visibleSpan(new ManaHudRules.PreviewSpan(6, 9), true).visible());
    }

    @Test
    void batteryPreviewNeedsAnActiveDurationAndSyncedPower() {
        assertEquals(1.0, ManaHudRules.previewBatteryMultiplier(false, true, 9.0), 0.0);
        assertEquals(1.0, ManaHudRules.previewBatteryMultiplier(true, false, 0.0), 0.0);
        assertEquals(
                AbilityRules.batteryRegenMultiplier(4.0),
                ManaHudRules.previewBatteryMultiplier(true, true, 4.0),
                0.0);
    }

    private static double stepped(int level, double current, double saturation, double battery) {
        double mana = current;
        for (int minute = 0; minute < ManaHudRules.PREVIEW_MINUTES; minute++) {
            double rate = AchievementRules.regenPerMinute(level, mana, battery);
            mana = AchievementRules.addMana(mana, level, AchievementRules.withSaturationRegen(rate, saturation));
        }
        return mana;
    }
}

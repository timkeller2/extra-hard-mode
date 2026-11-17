package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.WorldConfig;
import org.junit.jupiter.api.Test;

class EnchantingConfigTest {
    @Test
    void moduleDefaultsOnAndCanBeTurnedOff() {
        assertTrue(new Enchanting().defaultEnabled());
        boolean already = false;
        for (FeatureModule module : ExtraHardModeMod.FEATURES) {
            if (module.id().equals(Enchanting.ID)) {
                already = true;
                break;
            }
        }
        if (!already) {
            ExtraHardModeMod.FEATURES.register(new Enchanting());
        }
        WorldConfig world = new WorldConfig(ExtraHardModeMod.id("enchant-config"));
        assertTrue(world.isModuleEnabled(Enchanting.ID));
        world.setModuleEnabled(Enchanting.ID, false);
        assertFalse(world.isModuleEnabled(Enchanting.ID));
        world.setModuleEnabled(Enchanting.ID, true);
        assertTrue(world.isModuleEnabled(Enchanting.ID));
    }
}

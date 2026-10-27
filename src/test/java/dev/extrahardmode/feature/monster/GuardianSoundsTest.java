package dev.extrahardmode.feature.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GuardianSoundsTest {
    @Test
    void guardianSoundsAreOneThird() {
        assertEquals(1.0f / 3.0f, GuardianSounds.volume("minecraft", "entity.guardian.attack", 1.0f));
        assertEquals(1.0f / 3.0f, GuardianSounds.volume("minecraft", "entity.guardian.ambient", 1.0f));
        assertEquals(0.25f / 3.0f, GuardianSounds.volume("minecraft", "entity.guardian.hurt", 0.25f));
        assertEquals(0.0f, GuardianSounds.volume("minecraft", "entity.guardian.death", 0.0f));
        assertEquals(1.0f / 3.0f, GuardianSounds.volume("minecraft", "entity.elder_guardian.curse", 1.0f));
        assertEquals(1.0f / 3.0f, GuardianSounds.volume("minecraft", "entity.elder_guardian.ambient", 1.0f));
    }

    @Test
    void otherSoundsStayAtTheirVolume() {
        assertFalse(GuardianSounds.isGuardianSound("minecraft", "entity.zombie.ambient"));
        assertFalse(GuardianSounds.isGuardianSound("minecraft", "entity.guardian"));
        assertFalse(GuardianSounds.isGuardianSound("tougher", "entity.guardian.attack"));
        assertFalse(GuardianSounds.isGuardianSound("minecraft", null));
        assertEquals(1.0f, GuardianSounds.volume("minecraft", "entity.squid.ambient", 1.0f));
        assertTrue(GuardianSounds.isGuardianSound("minecraft", "entity.guardian.flop"));
    }
}

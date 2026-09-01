package dev.extrahardmode.feature.monster;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WitchesTest {
    @Test
    void splashTableIsThirtyThirtyThirtyTen() {
        assertEquals(Witches.SplashAttack.BABY_OR_EXPLODE, Witches.attackFor(0));
        assertEquals(Witches.SplashAttack.BABY_OR_EXPLODE, Witches.attackFor(29));
        assertEquals(Witches.SplashAttack.TELEPORT, Witches.attackFor(30));
        assertEquals(Witches.SplashAttack.TELEPORT, Witches.attackFor(59));
        assertEquals(Witches.SplashAttack.EXPLODE, Witches.attackFor(60));
        assertEquals(Witches.SplashAttack.EXPLODE, Witches.attackFor(89));
        assertEquals(Witches.SplashAttack.VANILLA_POISON, Witches.attackFor(90));
        assertEquals(Witches.SplashAttack.VANILLA_POISON, Witches.attackFor(99));
    }
}

package dev.extrahardmode.api;

/**
 * Per-type explosion defaults. RootNode values win over the original enum
 * (TNT 5/3 not 6, ghast 2 not 3). Y-band is applied by config at the border.
 */
public enum ExplosionType {
    TNT(5.0F, false, true, 3.0F, false, true),
    CREEPER(3.0F, false, true, 3.0F, false, true),
    CREEPER_CHARGED(4.0F, false, true, 4.0F, false, true),
    GHAST_FIREBALL(2.0F, true, true, 2.0F, true, true),
    OVERWORLD_BLAZE(4.0F, true, true, 4.0F, true, true),
    MAGMACUBE_FIRE(2.0F, true, true, 2.0F, true, true),
    DRAGON_FIREBALL(2.0F, true, true, 2.0F, true, true),
    EFFECT(0.0F, false, false, 0.0F, false, false);

    private final float belowPower;
    private final boolean belowFire;
    private final boolean belowWorldDamage;
    private final float abovePower;
    private final boolean aboveFire;
    private final boolean aboveWorldDamage;

    ExplosionType(
            float belowPower,
            boolean belowFire,
            boolean belowWorldDamage,
            float abovePower,
            boolean aboveFire,
            boolean aboveWorldDamage) {
        this.belowPower = belowPower;
        this.belowFire = belowFire;
        this.belowWorldDamage = belowWorldDamage;
        this.abovePower = abovePower;
        this.aboveFire = aboveFire;
        this.aboveWorldDamage = aboveWorldDamage;
    }

    public float belowPower() {
        return belowPower;
    }

    public boolean belowFire() {
        return belowFire;
    }

    public boolean belowWorldDamage() {
        return belowWorldDamage;
    }

    public float abovePower() {
        return abovePower;
    }

    public boolean aboveFire() {
        return aboveFire;
    }

    public boolean aboveWorldDamage() {
        return aboveWorldDamage;
    }

    public boolean isMob() {
        return this != TNT && this != EFFECT;
    }
}

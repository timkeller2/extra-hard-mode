package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import net.minecraft.resources.Identifier;

/** Toggle for tutorial toasts. Deny action-bar still fires from the feature that cancelled the action. */
public final class Tutorial implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("tutorial");

    @Override
    public Identifier id() {
        return ID;
    }
}

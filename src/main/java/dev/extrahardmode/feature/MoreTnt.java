package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import net.minecraft.resources.Identifier;

/**
 * More TNT is the datapack override {@code data/minecraft/recipe/tnt.json} (result count 3).
 * Disabling this module does not unload the recipe; the datapack always overwrites vanilla.
 */
public final class MoreTnt implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("more_tnt");

    @Override
    public Identifier id() {
        return ID;
    }
}

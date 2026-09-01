package dev.extrahardmode.world;

import dev.extrahardmode.ExtraHardModeMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

public final class EhmTags {
    public static final TagKey<Biome> DESERT_INFERTILE =
            TagKey.create(Registries.BIOME, ExtraHardModeMod.id("desert_infertile"));

    private EhmTags() {}
}

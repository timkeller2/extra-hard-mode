package dev.extrahardmode.world;

import dev.extrahardmode.ExtraHardModeMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;

public final class EhmTags {
    public static final TagKey<Block> NATURAL_SPAWN_BLOCKS = block("natural_spawn_blocks");

    public static final TagKey<Biome> NO_EXTRA_PACKS = biome("no_extra_packs");
    public static final TagKey<Biome> NO_SPAWN_IN_LIGHT = biome("no_spawn_in_light");

    public static final TagKey<Structure> NO_EXTRA_PACKS_STRUCTURES = structure("no_extra_packs_structures");
    public static final TagKey<Structure> NO_SPAWN_REPLACEMENT_STRUCTURES =
            structure("no_spawn_replacement_structures");

    private EhmTags() {}

    public static boolean noExtraPacks(ServerLevel level, BlockPos pos) {
        return level.getBiome(pos).is(NO_EXTRA_PACKS) || inStructure(level, pos, NO_EXTRA_PACKS_STRUCTURES);
    }

    public static boolean noSpawnInLight(ServerLevel level, BlockPos pos) {
        return level.getBiome(pos).is(NO_SPAWN_IN_LIGHT) || inStructure(level, pos, NO_SPAWN_REPLACEMENT_STRUCTURES);
    }

    public static boolean inStructure(ServerLevel level, BlockPos pos, TagKey<Structure> tag) {
        return level.structureManager().getStructureWithPieceAt(pos, tag).isValid();
    }

    private static TagKey<Block> block(String path) {
        return TagKey.create(Registries.BLOCK, ExtraHardModeMod.id(path));
    }

    private static TagKey<Biome> biome(String path) {
        return TagKey.create(Registries.BIOME, ExtraHardModeMod.id(path));
    }

    private static TagKey<Structure> structure(String path) {
        return TagKey.create(Registries.STRUCTURE, ExtraHardModeMod.id(path));
    }
}

package dev.extrahardmode.world;

import dev.extrahardmode.ExtraHardModeMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class EhmTags {
    public static final TagKey<Block> SOFT_TORCH_SURFACES =
            TagKey.create(Registries.BLOCK, ExtraHardModeMod.id("soft_torch_surfaces"));
    public static final TagKey<Block> DEPTH_LIMITED_LIGHTS =
            TagKey.create(Registries.BLOCK, ExtraHardModeMod.id("depth_limited_lights"));

    private EhmTags() {}

    public static List<Identifier> snapshot(TagKey<Block> tag) {
        List<Identifier> ids = new ArrayList<>();
        for (Holder<Block> holder : BuiltInRegistries.BLOCK.getTagOrEmpty(tag)) {
            holder.unwrapKey().ifPresent(key -> ids.add(key.identifier()));
        }
        return List.copyOf(ids);
import net.minecraft.world.item.Item;
    public static final TagKey<Item> DEATH_VALUABLE_TOOLS = item("death_valuable_tools");
    /** Empty by default. Recommended datapack adds: recovery_compass, totem_of_undying. */
    public static final TagKey<Item> DEATH_ITEM_BLACKLIST = item("death_item_blacklist");
    private static TagKey<Item> item(String path) {
        return TagKey.create(Registries.ITEM, ExtraHardModeMod.id(path));
    }
import net.minecraft.world.level.biome.Biome;
    public static final TagKey<Biome> DESERT_INFERTILE =
            TagKey.create(Registries.BIOME, ExtraHardModeMod.id("desert_infertile"));
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;
    public static final TagKey<Block> NATURAL_SPAWN_BLOCKS = block("natural_spawn_blocks");
    public static final TagKey<Biome> NO_EXTRA_PACKS = biome("no_extra_packs");
    public static final TagKey<Biome> NO_SPAWN_IN_LIGHT = biome("no_spawn_in_light");
    public static final TagKey<Structure> NO_EXTRA_PACKS_STRUCTURES = structure("no_extra_packs_structures");
    public static final TagKey<Structure> NO_SPAWN_REPLACEMENT_STRUCTURES =
            structure("no_spawn_replacement_structures");
    public static boolean noExtraPacks(ServerLevel level, BlockPos pos) {
        return level.getBiome(pos).is(NO_EXTRA_PACKS) || inStructure(level, pos, NO_EXTRA_PACKS_STRUCTURES);
    public static boolean noSpawnInLight(ServerLevel level, BlockPos pos) {
        return level.getBiome(pos).is(NO_SPAWN_IN_LIGHT) || inStructure(level, pos, NO_SPAWN_REPLACEMENT_STRUCTURES);
    public static boolean inStructure(ServerLevel level, BlockPos pos, TagKey<Structure> tag) {
        return level.structureManager().getStructureWithPieceAt(pos, tag).isValid();
    private static TagKey<Block> block(String path) {
        return TagKey.create(Registries.BLOCK, ExtraHardModeMod.id(path));
    private static TagKey<Biome> biome(String path) {
        return TagKey.create(Registries.BIOME, ExtraHardModeMod.id(path));
    private static TagKey<Structure> structure(String path) {
        return TagKey.create(Registries.STRUCTURE, ExtraHardModeMod.id(path));
}

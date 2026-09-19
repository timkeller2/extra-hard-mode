package dev.extrahardmode.tag;

import dev.extrahardmode.ExtraHardModeMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.Structure;

public final class EhmTags {
    public static final TagKey<Block> HARDENED = TagKey.create(Registries.BLOCK, ExtraHardModeMod.id("hardened"));
    public static final TagKey<Block> CAVE_IN_ORES = TagKey.create(Registries.BLOCK, ExtraHardModeMod.id("cave_in_ores"));
    public static final TagKey<Block> EXTRA_FALLING = TagKey.create(Registries.BLOCK, ExtraHardModeMod.id("extra_falling"));
    public static final TagKey<Block> PHYSICS_PROTECTED =
            TagKey.create(Registries.BLOCK, ExtraHardModeMod.id("physics_protected"));
    public static final TagKey<Block> FELLABLE_LOGS =
            TagKey.create(Registries.BLOCK, ExtraHardModeMod.id("fellable_logs"));
    public static final TagKey<Block> SOFT_TORCH_SURFACES =
            TagKey.create(Registries.BLOCK, ExtraHardModeMod.id("soft_torch_surfaces"));
    public static final TagKey<Block> DEPTH_LIMITED_LIGHTS =
            TagKey.create(Registries.BLOCK, ExtraHardModeMod.id("depth_limited_lights"));
    public static final TagKey<Block> NATURAL_SPAWN_BLOCKS =
            TagKey.create(Registries.BLOCK, ExtraHardModeMod.id("natural_spawn_blocks"));
    /** Right-clicking these should use the block, not a mana ability (unless sneaking). */
    public static final TagKey<Block> SKIPS_MANA_ABILITY_USE =
            TagKey.create(Registries.BLOCK, ExtraHardModeMod.id("skips_mana_ability_use"));
    public static final TagKey<Block> RESIDENCE_WINDOWS =
            TagKey.create(Registries.BLOCK, ExtraHardModeMod.id("residence/windows"));
    public static final TagKey<Block> RESIDENCE_RUGS =
            TagKey.create(Registries.BLOCK, ExtraHardModeMod.id("residence/rugs"));
    public static final TagKey<Block> RESIDENCE_SEATING =
            TagKey.create(Registries.BLOCK, ExtraHardModeMod.id("residence/seating"));
    public static final TagKey<Block> RESIDENCE_STORAGE =
            TagKey.create(Registries.BLOCK, ExtraHardModeMod.id("residence/storage"));
    public static final TagKey<Block> RESIDENCE_WORKSTATIONS =
            TagKey.create(Registries.BLOCK, ExtraHardModeMod.id("residence/workstations"));
    public static final TagKey<Block> RESIDENCE_LIGHTS =
            TagKey.create(Registries.BLOCK, ExtraHardModeMod.id("residence/lights"));
    public static final TagKey<Block> RESIDENCE_PLANTS =
            TagKey.create(Registries.BLOCK, ExtraHardModeMod.id("residence/plants"));
    public static final TagKey<Block> RESIDENCE_BOOKS =
            TagKey.create(Registries.BLOCK, ExtraHardModeMod.id("residence/books"));
    public static final TagKey<Block> RESIDENCE_KITCHEN_HEAT =
            TagKey.create(Registries.BLOCK, ExtraHardModeMod.id("residence/kitchen_heat"));
    public static final TagKey<Block> RESIDENCE_KITCHEN_WATER =
            TagKey.create(Registries.BLOCK, ExtraHardModeMod.id("residence/kitchen_water"));

    public static final TagKey<Item> HARDENED_MINER = TagKey.create(Registries.ITEM, ExtraHardModeMod.id("hardened_miner"));
    public static final TagKey<Item> DEATH_VALUABLE_TOOLS =
            TagKey.create(Registries.ITEM, ExtraHardModeMod.id("death_valuable_tools"));
    public static final TagKey<Item> DEATH_ITEM_BLACKLIST =
            TagKey.create(Registries.ITEM, ExtraHardModeMod.id("death_item_blacklist"));
    public static final TagKey<Item> VILLAGER_NERF_GEAR =
            TagKey.create(Registries.ITEM, ExtraHardModeMod.id("villager_nerf_gear"));
    public static final TagKey<Item> HEAVY_ARMOR = TagKey.create(Registries.ITEM, ExtraHardModeMod.id("heavy_armor"));
    public static final TagKey<Item> NO_OFFHAND_LIGHT =
            TagKey.create(Registries.ITEM, ExtraHardModeMod.id("no_offhand_light"));
    public static final TagKey<EntityType<?>> STOCKED_FISH =
            TagKey.create(Registries.ENTITY_TYPE, ExtraHardModeMod.id("stocked_fish"));

    public static final TagKey<Biome> NO_PHYSICS = TagKey.create(Registries.BIOME, ExtraHardModeMod.id("no_physics"));
    public static final TagKey<Biome> NO_SPAWN_REPLACEMENTS =
            TagKey.create(Registries.BIOME, ExtraHardModeMod.id("no_spawn_replacements"));
    public static final TagKey<Biome> NO_EXTRA_PACKS =
            TagKey.create(Registries.BIOME, ExtraHardModeMod.id("no_extra_packs"));
    public static final TagKey<Biome> NO_SPAWN_IN_LIGHT =
            TagKey.create(Registries.BIOME, ExtraHardModeMod.id("no_spawn_in_light"));
    public static final TagKey<Biome> DESERT_INFERTILE =
            TagKey.create(Registries.BIOME, ExtraHardModeMod.id("desert_infertile"));
    public static final TagKey<Biome> VINDICATOR_REPLACE =
            TagKey.create(Registries.BIOME, ExtraHardModeMod.id("vindicator_replace"));
    public static final TagKey<Biome> CAVE_SPIDER_REPLACE =
            TagKey.create(Registries.BIOME, ExtraHardModeMod.id("cave_spider_replace"));
    public static final TagKey<Biome> GUARDIAN_REPLACE =
            TagKey.create(Registries.BIOME, ExtraHardModeMod.id("guardian_replace"));
    public static final TagKey<Biome> NO_BIOME_BOSSES =
            TagKey.create(Registries.BIOME, ExtraHardModeMod.id("no_biome_bosses"));

    public static final TagKey<Structure> PHYSICS_PROTECTED_STRUCTURES =
            TagKey.create(Registries.STRUCTURE, ExtraHardModeMod.id("physics_protected_structures"));
    public static final TagKey<Structure> NO_SPAWN_REPLACEMENT_STRUCTURES =
            TagKey.create(Registries.STRUCTURE, ExtraHardModeMod.id("no_spawn_replacement_structures"));
    public static final TagKey<Structure> NO_EXTRA_PACKS_STRUCTURES =
            TagKey.create(Registries.STRUCTURE, ExtraHardModeMod.id("no_extra_packs_structures"));

    private EhmTags() {}
}

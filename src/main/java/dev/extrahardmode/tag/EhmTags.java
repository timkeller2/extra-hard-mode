package dev.extrahardmode.tag;

import dev.extrahardmode.ExtraHardModeMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class EhmTags {
    public static final TagKey<Block> HARDENED = TagKey.create(Registries.BLOCK, ExtraHardModeMod.id("hardened"));
    public static final TagKey<Block> CAVE_IN_ORES = TagKey.create(Registries.BLOCK, ExtraHardModeMod.id("cave_in_ores"));
    public static final TagKey<Item> HARDENED_MINER = TagKey.create(Registries.ITEM, ExtraHardModeMod.id("hardened_miner"));

    private EhmTags() {}
}

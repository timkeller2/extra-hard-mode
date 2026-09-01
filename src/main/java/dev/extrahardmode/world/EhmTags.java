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
}

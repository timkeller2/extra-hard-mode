package dev.extrahardmode.world;

import dev.extrahardmode.ExtraHardModeMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class EhmTags {
    public static final TagKey<Item> DEATH_VALUABLE_TOOLS = item("death_valuable_tools");
    public static final TagKey<Item> DEATH_ITEM_BLACKLIST = item("death_item_blacklist");

    private EhmTags() {}

    private static TagKey<Item> item(String path) {
        return TagKey.create(Registries.ITEM, ExtraHardModeMod.id(path));
    }
}

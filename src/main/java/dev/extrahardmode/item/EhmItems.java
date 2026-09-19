package dev.extrahardmode.item;

import dev.extrahardmode.ExtraHardModeMod;
import java.util.List;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;

/** Heavy copper / iron / diamond / netherite armor: vanilla protection plus extra max-health hearts. */
public final class EhmItems {
    public static final ResourceKey<CreativeModeTab> COMBAT =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("combat"));

    public static Item HEAVY_COPPER_HELMET;
    public static Item HEAVY_COPPER_CHESTPLATE;
    public static Item HEAVY_COPPER_LEGGINGS;
    public static Item HEAVY_COPPER_BOOTS;
    public static Item HEAVY_IRON_HELMET;
    public static Item HEAVY_IRON_CHESTPLATE;
    public static Item HEAVY_IRON_LEGGINGS;
    public static Item HEAVY_IRON_BOOTS;
    public static Item HEAVY_DIAMOND_HELMET;
    public static Item HEAVY_DIAMOND_CHESTPLATE;
    public static Item HEAVY_DIAMOND_LEGGINGS;
    public static Item HEAVY_DIAMOND_BOOTS;
    public static Item HEAVY_NETHERITE_HELMET;
    public static Item HEAVY_NETHERITE_CHESTPLATE;
    public static Item HEAVY_NETHERITE_LEGGINGS;
    public static Item HEAVY_NETHERITE_BOOTS;

    private EhmItems() {}

    public static void register() {
        HEAVY_COPPER_HELMET = copper("heavy_copper_helmet", ArmorType.HELMET);
        HEAVY_COPPER_CHESTPLATE = copper("heavy_copper_chestplate", ArmorType.CHESTPLATE);
        HEAVY_COPPER_LEGGINGS = copper("heavy_copper_leggings", ArmorType.LEGGINGS);
        HEAVY_COPPER_BOOTS = copper("heavy_copper_boots", ArmorType.BOOTS);
        HEAVY_IRON_HELMET = iron("heavy_iron_helmet", ArmorType.HELMET);
        HEAVY_IRON_CHESTPLATE = iron("heavy_iron_chestplate", ArmorType.CHESTPLATE);
        HEAVY_IRON_LEGGINGS = iron("heavy_iron_leggings", ArmorType.LEGGINGS);
        HEAVY_IRON_BOOTS = iron("heavy_iron_boots", ArmorType.BOOTS);
        HEAVY_DIAMOND_HELMET = diamond("heavy_diamond_helmet", ArmorType.HELMET);
        HEAVY_DIAMOND_CHESTPLATE = diamond("heavy_diamond_chestplate", ArmorType.CHESTPLATE);
        HEAVY_DIAMOND_LEGGINGS = diamond("heavy_diamond_leggings", ArmorType.LEGGINGS);
        HEAVY_DIAMOND_BOOTS = diamond("heavy_diamond_boots", ArmorType.BOOTS);
        HEAVY_NETHERITE_HELMET = netherite("heavy_netherite_helmet", ArmorType.HELMET);
        HEAVY_NETHERITE_CHESTPLATE = netherite("heavy_netherite_chestplate", ArmorType.CHESTPLATE);
        HEAVY_NETHERITE_LEGGINGS = netherite("heavy_netherite_leggings", ArmorType.LEGGINGS);
        HEAVY_NETHERITE_BOOTS = netherite("heavy_netherite_boots", ArmorType.BOOTS);
        CreativeModeTabEvents.modifyOutputEvent(COMBAT)
                .register(output -> {
                    output.insertAfter(
                            Items.COPPER_BOOTS,
                            HEAVY_COPPER_HELMET,
                            HEAVY_COPPER_CHESTPLATE,
                            HEAVY_COPPER_LEGGINGS,
                            HEAVY_COPPER_BOOTS);
                    output.insertAfter(
                            Items.DIAMOND_BOOTS,
                            HEAVY_IRON_HELMET,
                            HEAVY_IRON_CHESTPLATE,
                            HEAVY_IRON_LEGGINGS,
                            HEAVY_IRON_BOOTS,
                            HEAVY_DIAMOND_HELMET,
                            HEAVY_DIAMOND_CHESTPLATE,
                            HEAVY_DIAMOND_LEGGINGS,
                            HEAVY_DIAMOND_BOOTS);
                    output.insertAfter(
                            Items.NETHERITE_BOOTS,
                            HEAVY_NETHERITE_HELMET,
                            HEAVY_NETHERITE_CHESTPLATE,
                            HEAVY_NETHERITE_LEGGINGS,
                            HEAVY_NETHERITE_BOOTS);
                });
    }

    public static List<Item> allPieces() {
        return List.of(
                HEAVY_COPPER_HELMET,
                HEAVY_COPPER_CHESTPLATE,
                HEAVY_COPPER_LEGGINGS,
                HEAVY_COPPER_BOOTS,
                HEAVY_IRON_HELMET,
                HEAVY_IRON_CHESTPLATE,
                HEAVY_IRON_LEGGINGS,
                HEAVY_IRON_BOOTS,
                HEAVY_DIAMOND_HELMET,
                HEAVY_DIAMOND_CHESTPLATE,
                HEAVY_DIAMOND_LEGGINGS,
                HEAVY_DIAMOND_BOOTS,
                HEAVY_NETHERITE_HELMET,
                HEAVY_NETHERITE_CHESTPLATE,
                HEAVY_NETHERITE_LEGGINGS,
                HEAVY_NETHERITE_BOOTS);
    }

    public static Item randomPiece(RandomSource random) {
        List<Item> pieces = allPieces();
        return pieces.get(random.nextInt(pieces.size()));
    }

    private static Item copper(String path, ArmorType type) {
        return armor(
                path,
                ArmorMaterials.COPPER,
                type,
                Items.COPPER_BLOCK.weathering().unaffected(),
                Rarity.COMMON,
                "copper",
                false);
    }

    private static Item iron(String path, ArmorType type) {
        return armor(path, ArmorMaterials.IRON, type, Items.IRON_BLOCK, Rarity.UNCOMMON, "iron", false);
    }

    private static Item diamond(String path, ArmorType type) {
        return armor(path, ArmorMaterials.DIAMOND, type, Items.DIAMOND_BLOCK, Rarity.RARE, "diamond", false);
    }

    private static Item netherite(String path, ArmorType type) {
        return armor(path, ArmorMaterials.NETHERITE, type, Items.NETHERITE_INGOT, Rarity.EPIC, "netherite", true);
    }

    private static Item armor(
            String path,
            ArmorMaterial material,
            ArmorType type,
            Item repair,
            Rarity rarity,
            String materialName,
            boolean fireResistant) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, ExtraHardModeMod.id(path));
        ItemAttributeModifiers attributes = material.createAttributes(type)
                .withModifierAdded(
                        Attributes.MAX_HEALTH,
                        new AttributeModifier(
                                ExtraHardModeMod.id(path + "_health"),
                                HeavyArmorRules.bonusHealth(materialName, type.getName()),
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.bySlot(type.getSlot()));
        Item.Properties properties = new Item.Properties()
                .setId(key)
                .rarity(rarity)
                .humanoidArmor(material, type)
                .durability(HeavyArmorRules.durability(type.getDurability(material.durability())))
                .repairable(repair)
                .attributes(attributes);
        if (fireResistant) {
            properties.fireResistant();
        }
        return Registry.register(BuiltInRegistries.ITEM, key, new Item(properties));
    }
}

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
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
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
    public static Item HEAVY_COPPER_SWORD;
    public static Item HEAVY_COPPER_AXE;
    public static Item HEAVY_IRON_SWORD;
    public static Item HEAVY_IRON_AXE;
    public static Item HEAVY_GOLDEN_SWORD;
    public static Item HEAVY_GOLDEN_AXE;
    public static Item HEAVY_DIAMOND_SWORD;
    public static Item HEAVY_DIAMOND_AXE;
    public static Item HEAVY_NETHERITE_SWORD;
    public static Item HEAVY_NETHERITE_AXE;

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
        Item copperBlock = Items.COPPER_BLOCK.weathering().unaffected();
        HEAVY_COPPER_SWORD = sword("heavy_copper_sword", ToolMaterial.COPPER, copperBlock, Rarity.COMMON, false);
        HEAVY_COPPER_AXE = axe(
                "heavy_copper_axe",
                ToolMaterial.COPPER,
                HeavyWeaponRules.COPPER_AXE_ATTACK,
                HeavyWeaponRules.COPPER_AXE_SPEED,
                copperBlock,
                Rarity.COMMON,
                false);
        HEAVY_IRON_SWORD = sword("heavy_iron_sword", ToolMaterial.IRON, Items.IRON_BLOCK, Rarity.UNCOMMON, false);
        HEAVY_IRON_AXE = axe(
                "heavy_iron_axe",
                ToolMaterial.IRON,
                HeavyWeaponRules.IRON_AXE_ATTACK,
                HeavyWeaponRules.IRON_AXE_SPEED,
                Items.IRON_BLOCK,
                Rarity.UNCOMMON,
                false);
        HEAVY_GOLDEN_SWORD = sword("heavy_golden_sword", ToolMaterial.GOLD, Items.GOLD_BLOCK, Rarity.UNCOMMON, false);
        HEAVY_GOLDEN_AXE = axe(
                "heavy_golden_axe",
                ToolMaterial.GOLD,
                HeavyWeaponRules.GOLD_AXE_ATTACK,
                HeavyWeaponRules.GOLD_AXE_SPEED,
                Items.GOLD_BLOCK,
                Rarity.UNCOMMON,
                false);
        HEAVY_DIAMOND_SWORD =
                sword("heavy_diamond_sword", ToolMaterial.DIAMOND, Items.DIAMOND_BLOCK, Rarity.RARE, false);
        HEAVY_DIAMOND_AXE = axe(
                "heavy_diamond_axe",
                ToolMaterial.DIAMOND,
                HeavyWeaponRules.DIAMOND_AXE_ATTACK,
                HeavyWeaponRules.DIAMOND_AXE_SPEED,
                Items.DIAMOND_BLOCK,
                Rarity.RARE,
                false);
        HEAVY_NETHERITE_SWORD =
                sword("heavy_netherite_sword", ToolMaterial.NETHERITE, Items.NETHERITE_INGOT, Rarity.EPIC, true);
        HEAVY_NETHERITE_AXE = axe(
                "heavy_netherite_axe",
                ToolMaterial.NETHERITE,
                HeavyWeaponRules.NETHERITE_AXE_ATTACK,
                HeavyWeaponRules.NETHERITE_AXE_SPEED,
                Items.NETHERITE_INGOT,
                Rarity.EPIC,
                true);
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
                    output.insertAfter(Items.COPPER_SWORD, HEAVY_COPPER_SWORD);
                    output.insertAfter(Items.COPPER_AXE, HEAVY_COPPER_AXE);
                    output.insertAfter(Items.IRON_SWORD, HEAVY_IRON_SWORD);
                    output.insertAfter(Items.IRON_AXE, HEAVY_IRON_AXE);
                    output.insertAfter(Items.GOLDEN_SWORD, HEAVY_GOLDEN_SWORD);
                    output.insertAfter(Items.GOLDEN_AXE, HEAVY_GOLDEN_AXE);
                    output.insertAfter(Items.DIAMOND_SWORD, HEAVY_DIAMOND_SWORD);
                    output.insertAfter(Items.DIAMOND_AXE, HEAVY_DIAMOND_AXE);
                    output.insertAfter(Items.NETHERITE_SWORD, HEAVY_NETHERITE_SWORD);
                    output.insertAfter(Items.NETHERITE_AXE, HEAVY_NETHERITE_AXE);
                });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(output -> {
                    output.insertAfter(Items.COPPER_AXE, HEAVY_COPPER_AXE);
                    output.insertAfter(Items.IRON_AXE, HEAVY_IRON_AXE);
                    output.insertAfter(Items.GOLDEN_AXE, HEAVY_GOLDEN_AXE);
                    output.insertAfter(Items.DIAMOND_AXE, HEAVY_DIAMOND_AXE);
                    output.insertAfter(Items.NETHERITE_AXE, HEAVY_NETHERITE_AXE);
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

    public static List<Item> allWeapons() {
        return List.of(
                HEAVY_COPPER_SWORD,
                HEAVY_COPPER_AXE,
                HEAVY_IRON_SWORD,
                HEAVY_IRON_AXE,
                HEAVY_GOLDEN_SWORD,
                HEAVY_GOLDEN_AXE,
                HEAVY_DIAMOND_SWORD,
                HEAVY_DIAMOND_AXE,
                HEAVY_NETHERITE_SWORD,
                HEAVY_NETHERITE_AXE);
    }

    public static Item randomWeapon(RandomSource random) {
        List<Item> weapons = allWeapons();
        return weapons.get(random.nextInt(weapons.size()));
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

    private static Item sword(String path, ToolMaterial material, Item repair, Rarity rarity, boolean fireResistant) {
        return weapon(
                path,
                material,
                true,
                HeavyWeaponRules.swordAttack(),
                HeavyWeaponRules.SWORD_SPEED,
                repair,
                rarity,
                fireResistant);
    }

    private static Item axe(
            String path,
            ToolMaterial material,
            float vanillaAttack,
            float speed,
            Item repair,
            Rarity rarity,
            boolean fireResistant) {
        return weapon(
                path,
                material,
                false,
                HeavyWeaponRules.axeAttack(vanillaAttack),
                speed,
                repair,
                rarity,
                fireResistant);
    }

    private static Item weapon(
            String path,
            ToolMaterial material,
            boolean sword,
            float attack,
            float speed,
            Item repair,
            Rarity rarity,
            boolean fireResistant) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, ExtraHardModeMod.id(path));
        Item.Properties properties = new Item.Properties().setId(key).rarity(rarity);
        properties = sword ? properties.sword(material, attack, speed) : properties.axe(material, attack, speed);
        properties = properties.durability(HeavyWeaponRules.durability(material.durability())).repairable(repair);
        if (fireResistant) {
            properties = properties.fireResistant();
        }
        return Registry.register(BuiltInRegistries.ITEM, key, new Item(properties));
    }
}

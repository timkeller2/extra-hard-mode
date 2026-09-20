package dev.extrahardmode.feature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AbilityRulesTest {
    @Test
    void skillIsSquareRootOfUses() {
        assertEquals(0.0, AbilityRules.skill(0), 1e-9);
        assertEquals(2.0, AbilityRules.skill(4), 1e-9);
        assertEquals(3.0, AbilityRules.skill(9), 1e-9);
    }

    @Test
    void powerIsSkillPlusItemBonus() {
        assertEquals(0.0, AbilityRules.power(0, 0, 0), 1e-9);
        assertEquals(0.0, AbilityRules.power(7, 0, 0), 1e-9);
        assertEquals(2.0, AbilityRules.power(1, 4, 0), 1e-9);
        assertEquals(3.0, AbilityRules.power(1, 9, 0), 1e-9);
        assertEquals(1.0, AbilityRules.power(1, 0, 1), 1e-9);
        assertEquals(3.0, AbilityRules.power(1, 4, 1), 1e-9);
        assertEquals(0.0, AbilityRules.basePower(0), 1e-9);
        assertEquals(2.0, AbilityRules.basePower(4), 1e-9);
    }

    @Test
    void catalystItemsMapToAbilities() {
        assertEquals(AbilityRules.HEAL, AbilityRules.abilityForItem("minecraft:paper"));
        assertEquals(AbilityRules.IRON_HEART, AbilityRules.abilityForItem("minecraft:iron_ingot"));
        assertEquals(AbilityRules.FLIGHT, AbilityRules.abilityForItem("minecraft:feather"));
        assertEquals(AbilityRules.MAGIC_ARROW, AbilityRules.abilityForItem("minecraft:arrow"));
        assertEquals(AbilityRules.FIRE_BOLT, AbilityRules.abilityForItem("minecraft:charcoal"));
        assertEquals(AbilityRules.GROW, AbilityRules.abilityForItem("minecraft:wooden_hoe"));
        assertEquals(AbilityRules.GROW, AbilityRules.abilityForItem("minecraft:stone_hoe"));
        assertEquals(AbilityRules.GROW, AbilityRules.abilityForItem("minecraft:copper_hoe"));
        assertEquals(AbilityRules.GROW, AbilityRules.abilityForItem("minecraft:iron_hoe"));
        assertEquals(AbilityRules.GROW, AbilityRules.abilityForItem("minecraft:golden_hoe"));
        assertEquals(AbilityRules.GROW, AbilityRules.abilityForItem("minecraft:diamond_hoe"));
        assertEquals(AbilityRules.GROW, AbilityRules.abilityForItem("minecraft:netherite_hoe"));
        assertEquals(AbilityRules.POWER_MINE, AbilityRules.abilityForItem("minecraft:wooden_pickaxe"));
        assertEquals(AbilityRules.POWER_MINE, AbilityRules.abilityForItem("minecraft:iron_pickaxe"));
        assertEquals(AbilityRules.POWER_MINE, AbilityRules.abilityForItem("minecraft:golden_pickaxe"));
        assertEquals(AbilityRules.POWER_MINE, AbilityRules.abilityForItem("minecraft:netherite_pickaxe"));
        assertEquals(AbilityRules.DETECT_ORE, AbilityRules.abilityForItem("minecraft:compass"));
        assertEquals(AbilityRules.SLOW, AbilityRules.abilityForItem("minecraft:string"));
        assertEquals(AbilityRules.SENSE_EVIL, AbilityRules.abilityForItem("minecraft:spider_eye"));
        assertEquals(AbilityRules.SMITE_EVIL, AbilityRules.abilityForItem("minecraft:golden_sword"));
        assertEquals(null, AbilityRules.abilityForItem("minecraft:iron_sword"));
        assertEquals(AbilityRules.LIGHT, AbilityRules.abilityForItem("minecraft:coal"));
        assertEquals(null, AbilityRules.abilityForItem("minecraft:torch"));
        assertEquals(null, AbilityRules.abilityForItem("minecraft:soul_torch"));
        assertEquals(null, AbilityRules.abilityForItem("minecraft:redstone_torch"));
        assertEquals(null, AbilityRules.abilityForItem("minecraft:redstone"));
        assertEquals(null, AbilityRules.abilityForItem(null));
        assertEquals(null, AbilityRules.abilityForItem(""));
        assertEquals(null, AbilityRules.abilityForItem("minecraft:air"));
        assertEquals(null, AbilityRules.abilityForItem("minecraft:stick"));
        assertEquals(null, AbilityRules.abilityForItem("minecraft:spectral_arrow"));
        assertEquals(null, AbilityRules.abilityForItem("minecraft:tipped_arrow"));
        assertEquals(AbilityRules.MAGIC_ARROW, AbilityRules.abilityForItem("minecraft:arrow"));
        assertEquals(AbilityRules.FIRE_BOLT, AbilityRules.abilityForItem("minecraft:charcoal"));
        assertEquals(null, AbilityRules.abilityForItem("minecraft:coal_block"));
    }

    @Test
    void abilityHelpTextExistsForEveryCatalyst() {
        assertEquals(AbilityRules.HEAL, AbilityRules.abilityForItem("minecraft:paper"));
        for (String item : new String[] {
            "minecraft:paper",
            "minecraft:iron_ingot",
            "minecraft:feather",
            "minecraft:arrow",
            "minecraft:charcoal",
            "minecraft:wooden_hoe",
            "minecraft:golden_hoe",
            "minecraft:iron_pickaxe",
            "minecraft:compass",
            "minecraft:string",
            "minecraft:spider_eye",
            "minecraft:golden_sword",
            "minecraft:coal"
        }) {
            String ability = AbilityRules.abilityForItem(item);
            String help = AbilityRules.helpFallback(ability);
            assertTrue(help != null && !help.isBlank(), item);
            assertEquals("tougher.ability." + ability + ".help", AbilityRules.helpKey(ability));
        }
        String healHelp = AbilityRules.helpFallback(AbilityRules.HEAL);
        assertTrue(healHelp.contains("heal yourself"));
        assertTrue(healHelp.contains("Right-click"));
        assertTrue(healHelp.contains("sparkles blue"));
        assertTrue(healHelp.contains("2 + ability level"));
        assertTrue(healHelp.contains("honey bottle"));
        assertTrue(healHelp.contains("+6"));
        assertTrue(healHelp.contains("no mana levels"));
        assertEquals(120, AbilityRules.HEAL_SPARKLE_TICKS);
        assertEquals(6, AbilityRules.HONEY_HEAL_BONUS);
        assertTrue(AbilityRules.honeyWaivesHealMana(true));
        assertFalse(AbilityRules.honeyWaivesHealMana(false));
        assertTrue(AbilityRules.hasManaToHeal(0.0, true));
        assertFalse(AbilityRules.hasManaToHeal(0.0, false));
        assertTrue(AbilityRules.hasManaToHeal(1.0, false));
        assertEquals(36, AbilityRules.MAIN_INVENTORY_SLOTS);
        assertTrue(AbilityRules.isMainInventorySlot(0));
        assertTrue(AbilityRules.isMainInventorySlot(35));
        assertFalse(AbilityRules.isMainInventorySlot(36));
        assertFalse(AbilityRules.isMainInventorySlot(-1));
        String ironHelp = AbilityRules.helpFallback(AbilityRules.IRON_HEART);
        assertTrue(ironHelp.contains("iron ingot"));
        assertTrue(ironHelp.contains("maximum health"));
        assertTrue(ironHelp.contains("minutes"));
        assertTrue(ironHelp.contains("cancel your own"));
        assertTrue(ironHelp.contains("cannot cancel another"));
        assertTrue(ironHelp.contains("silver"));
        assertTrue(ironHelp.contains("still have mana"));
        assertTrue(ironHelp.contains("gain skill"));
        assertEquals(60, AbilityRules.IRON_HEART_SPARKLE_TICKS);
        assertTrue(AbilityRules.helpFallback(AbilityRules.GROW).contains("Right-click a plant"));
        assertTrue(AbilityRules.helpFallback(AbilityRules.GROW).contains("improves that soil"));
        String fireHelp = AbilityRules.helpFallback(AbilityRules.FIRE_BOLT);
        assertTrue(fireHelp.contains("Right-click"));
        assertTrue(fireHelp.contains("charcoal"));
        assertTrue(fireHelp.contains("10 seconds"));
        assertTrue(fireHelp.contains("mana level"));
        assertTrue(fireHelp.contains("seeks the nearest"));
        String magicHelp = AbilityRules.helpFallback(AbilityRules.MAGIC_ARROW);
        assertTrue(magicHelp.contains("Right-click"));
        assertTrue(magicHelp.contains("arrow"));
        assertTrue(magicHelp.contains("does not ignite"));
        assertTrue(magicHelp.contains("10 seconds"));
        assertTrue(magicHelp.contains("mana level"));
        assertTrue(AbilityRules.helpFallback(AbilityRules.FLIGHT).contains("Right-click"));
        assertTrue(AbilityRules.helpFallback(AbilityRules.FLIGHT).contains("gain skill"));
        assertTrue(AbilityRules.helpFallback(AbilityRules.LIGHT).contains("gain skill"));
        assertTrue(AbilityRules.autoRenews(AbilityRules.FLIGHT));
        assertTrue(AbilityRules.autoRenews(AbilityRules.LIGHT));
        assertTrue(AbilityRules.autoRenews(AbilityRules.IRON_HEART));
        assertFalse(AbilityRules.autoRenews(AbilityRules.POWER_MINE));
        assertFalse(AbilityRules.autoRenews(AbilityRules.HEAL));
        assertTrue(AbilityRules.helpFallback(AbilityRules.POWER_MINE).contains("pickaxe"));
        assertTrue(AbilityRules.helpFallback(AbilityRules.DETECT_ORE).contains("compass"));
        assertTrue(AbilityRules.helpFallback(AbilityRules.DETECT_ORE).contains("nether quartz"));
        assertTrue(AbilityRules.helpFallback(AbilityRules.DETECT_ORE).contains("Costs 1 mana"));
        assertTrue(AbilityRules.helpFallback(AbilityRules.DETECT_ORE).contains("approximate distance"));
        assertTrue(AbilityRules.helpFallback(AbilityRules.SLOW).contains("Right-click"));
        assertTrue(AbilityRules.helpFallback(AbilityRules.SLOW).contains("string"));
        assertTrue(AbilityRules.helpFallback(AbilityRules.SLOW).contains("20%"));
        String senseHelp = AbilityRules.helpFallback(AbilityRules.SENSE_EVIL);
        assertTrue(senseHelp.contains("spider eye"));
        assertTrue(senseHelp.contains("Right-click"));
        assertTrue(senseHelp.contains("250"));
        assertTrue(senseHelp.contains("biome boss"));
        assertEquals(120, AbilityRules.SENSE_EVIL_SPARKLE_TICKS);
        String smiteHelp = AbilityRules.helpFallback(AbilityRules.SMITE_EVIL);
        assertTrue(smiteHelp.contains("golden sword"));
        assertTrue(smiteHelp.contains("Right-click"));
        assertTrue(smiteHelp.contains("Undead"));
        assertTrue(smiteHelp.contains("flash of light"));
        assertEquals(5.0, AbilityRules.smiteRange(5), 1e-9);
        assertEquals(0.0, AbilityRules.smiteRange(0), 1e-9);
        assertEquals(3.0F, AbilityRules.smiteUndeadBonus(3), 1e-4F);
        assertEquals(0, AbilityRules.cooldownTicks(AbilityRules.SMITE_EVIL, 10, 9));
        assertTrue(AbilityRules.helpFallback(AbilityRules.LIGHT).contains("coal"));
        assertTrue(AbilityRules.helpFallback(AbilityRules.LIGHT).contains("30 seconds"));
        assertTrue(AbilityRules.helpFallback(AbilityRules.LIGHT).contains("Right-click"));
        assertTrue(AbilityRules.helpFallback(AbilityRules.LIGHT).contains("snuff"));
        String powerHelp = AbilityRules.powerHelpFallback();
        assertTrue(powerHelp.contains("skill"));
        assertTrue(powerHelp.contains("square root"));
        assertFalse(powerHelp.contains("1/3 mana level"));
        assertFalse(powerHelp.contains("used more times than your mana level"));
        assertTrue(powerHelp.contains("Fire bolt and Magic arrow also add your mana level"));
        assertTrue(powerHelp.contains("−3 power"));
        assertTrue(AbilityRules.indexHelpFallback().contains("Charcoal: Fire bolt"));
        assertTrue(AbilityRules.indexHelpFallback().contains("Arrow: Magic arrow"));
        assertTrue(AbilityRules.indexHelpFallback().contains("Iron ingot: Iron Heart"));
        assertTrue(AbilityRules.indexHelpFallback().contains("Redstone dust"));
        assertEquals(null, AbilityRules.helpFallback(null));
        assertEquals(null, AbilityRules.helpFallback("unknown"));
        assertEquals(15, AbilityRules.indexHelpLines().size());
        assertEquals(AbilityRules.indexHelpLines().size(), AbilityRules.indexHelpKeys().size());
        assertTrue(AbilityRules.indexHelpFallback().contains("Paper: Healing"));
        assertTrue(AbilityRules.indexHelpFallback().contains("Any hoe: Let it grow"));
        assertTrue(AbilityRules.indexHelpFallback().contains("Coal: Let there be light"));
        assertTrue(AbilityRules.indexHelpFallback().contains("Any pickaxe: Power mining"));
        assertTrue(AbilityRules.indexHelpFallback().contains("Compass: Detect ore"));
        assertTrue(AbilityRules.indexHelpFallback().contains("String: Slow"));
        assertTrue(AbilityRules.indexHelpFallback().contains("Spider eye: Sense Evil"));
        assertTrue(AbilityRules.indexHelpFallback().contains("Golden sword: Smite Evil"));
        assertTrue(AbilityRules.indexHelpFallback().contains("Redstone dust in your inventory"));
        assertTrue(AbilityRules.indexHelpFallback().contains("half your mana level"));
        assertEquals("Healing", AbilityRules.nameFallback(AbilityRules.HEAL));
        assertEquals("Iron Heart", AbilityRules.nameFallback(AbilityRules.IRON_HEART));
        assertEquals("Fire bolt", AbilityRules.nameFallback(AbilityRules.FIRE_BOLT));
        assertEquals("Magic arrow", AbilityRules.nameFallback(AbilityRules.MAGIC_ARROW));
        assertEquals("Flight", AbilityRules.nameFallback(AbilityRules.FLIGHT));
        assertEquals("Let it grow", AbilityRules.nameFallback(AbilityRules.GROW));
        assertEquals("Power mining", AbilityRules.nameFallback(AbilityRules.POWER_MINE));
        assertEquals("Detect ore", AbilityRules.nameFallback(AbilityRules.DETECT_ORE));
        assertEquals("Slow", AbilityRules.nameFallback(AbilityRules.SLOW));
        assertEquals("Sense Evil", AbilityRules.nameFallback(AbilityRules.SENSE_EVIL));
        assertEquals("Smite Evil", AbilityRules.nameFallback(AbilityRules.SMITE_EVIL));
        assertEquals("Let there be light", AbilityRules.nameFallback(AbilityRules.LIGHT));
        assertEquals("tougher.ability.detect_ore", AbilityRules.nameKey(AbilityRules.DETECT_ORE));
        assertTrue(AbilityRules.lockedFallback(AbilityRules.HEAL).contains("Healing"));
        assertTrue(AbilityRules.lockedFallback(AbilityRules.DETECT_ORE).contains("Detect ore"));
        assertFalse(AbilityRules.lockedFallback(AbilityRules.FLIGHT).contains("this ability"));
    }

    @Test
    void newAbilityRequiresAFreeHalfManaSlot() {
        assertEquals(0, AbilityRules.abilitySlotCap(0));
        assertEquals(1, AbilityRules.abilitySlotCap(1));
        assertEquals(1, AbilityRules.abilitySlotCap(2));
        assertEquals(2, AbilityRules.abilitySlotCap(3));
        assertEquals(2, AbilityRules.abilitySlotCap(4));
        assertEquals(3, AbilityRules.abilitySlotCap(5));
        assertEquals(0, AbilityRules.learnedSkillCount(Set.of()));
        assertEquals(1, AbilityRules.learnedSkillCount(Set.of(AbilityRules.HEAL)));
        assertEquals(2, AbilityRules.learnedSkillCount(Set.of(AbilityRules.HEAL, AbilityRules.FLIGHT)));
        assertFalse(AbilityRules.isTrained(0, Set.of(), AbilityRules.HEAL));
        assertTrue(AbilityRules.isTrained(1, Set.of(), AbilityRules.HEAL));
        assertTrue(AbilityRules.isTrained(1, Set.of(AbilityRules.HEAL), AbilityRules.HEAL));
        assertFalse(AbilityRules.isTrained(1, Set.of(AbilityRules.HEAL), AbilityRules.FLIGHT));
        assertFalse(AbilityRules.isTrained(2, Set.of(AbilityRules.HEAL), AbilityRules.FLIGHT));
        assertTrue(AbilityRules.isTrained(3, Set.of(AbilityRules.HEAL), AbilityRules.FLIGHT));
        assertFalse(
                AbilityRules.isTrained(
                        3, Set.of(AbilityRules.HEAL, AbilityRules.FLIGHT), AbilityRules.GROW));
        assertFalse(
                AbilityRules.isTrained(
                        4, Set.of(AbilityRules.HEAL, AbilityRules.FLIGHT), AbilityRules.GROW));
        assertTrue(
                AbilityRules.isTrained(
                        5, Set.of(AbilityRules.HEAL, AbilityRules.FLIGHT), AbilityRules.GROW));
        assertFalse(AbilityRules.canLearnAbility(1, Set.of(AbilityRules.HEAL)));
        assertTrue(AbilityRules.canLearnAbility(3, Set.of(AbilityRules.HEAL)));
        assertEquals(1.0, AbilityRules.power(AbilityRules.FLIGHT, 1, 4, 0, false), 1e-9);
        assertEquals(2.0, AbilityRules.power(AbilityRules.FLIGHT, 1, 4, 0, true), 1e-9);
    }

    @Test
    void playerSkillsListsLearnedAbilitiesInCatalogOrder() {
        assertTrue(AbilityRules.playerSkills(Map.of()).isEmpty());
        assertTrue(AbilityRules.playerSkills(null).isEmpty());
        var skills = AbilityRules.playerSkills(
                Map.of(AbilityRules.FLIGHT, 4, AbilityRules.HEAL, 9, AbilityRules.GROW, 0));
        assertEquals(2, skills.size());
        assertEquals(AbilityRules.HEAL, skills.get(0).id());
        assertEquals("Healing", skills.get(0).name());
        assertEquals(9, skills.get(0).uses());
        assertEquals(3.0, skills.get(0).skill(), 1e-9);
        assertEquals(AbilityRules.FLIGHT, skills.get(1).id());
        assertEquals(4, skills.get(1).uses());
        assertEquals(2.0, skills.get(1).skill(), 1e-9);
        assertEquals("3", AbilityRules.powerLabel(skills.get(0).skill()));
    }

    @Test
    void supplyBonusAddsRedstoneHeldCatalystAndTools() {
        assertEquals(0, AbilityRules.supplyBonus(AbilityRules.HEAL, "minecraft:stick", 1, false, false));
        assertEquals(2, AbilityRules.supplyBonus(AbilityRules.HEAL, "minecraft:stick", 1, true, false));
        assertEquals(0, AbilityRules.supplyBonus(AbilityRules.HEAL, "minecraft:paper", 1, false, false));
        assertEquals(2, AbilityRules.supplyBonus(AbilityRules.HEAL, "minecraft:paper", 8, false, false));
        assertEquals(4, AbilityRules.supplyBonus(AbilityRules.HEAL, "minecraft:paper", 8, true, false));
        assertEquals(2, AbilityRules.supplyBonus(AbilityRules.HEAL, "minecraft:paper", 1, true, true));
        assertEquals(2, AbilityRules.supplyBonus(AbilityRules.GROW, "minecraft:iron_hoe", 1, false, false));
        assertEquals(4, AbilityRules.supplyBonus(AbilityRules.GROW, "minecraft:iron_hoe", 1, true, false));
        assertEquals(2, AbilityRules.supplyBonus(AbilityRules.GROW, "minecraft:paper", 8, true, false));
        assertEquals(5, AbilityRules.supplyBonus(AbilityRules.POWER_MINE, "minecraft:netherite_pickaxe", 1, false, false));
        assertEquals(2, AbilityRules.supplyBonus(AbilityRules.DETECT_ORE, "minecraft:compass", 1, false, true));
        assertEquals(4, AbilityRules.supplyBonus(AbilityRules.DETECT_ORE, "minecraft:compass", 1, true, true));
        assertEquals(2, AbilityRules.supplyBonus(AbilityRules.DETECT_ORE, "minecraft:stick", 1, false, true));
        assertEquals(2, AbilityRules.supplyBonus(AbilityRules.LIGHT, "minecraft:coal", 4, false, false));
        assertEquals(2, AbilityRules.supplyBonus(AbilityRules.IRON_HEART, "minecraft:iron_ingot", 2, false, false));
        assertEquals(4, AbilityRules.supplyBonus(AbilityRules.IRON_HEART, "minecraft:iron_ingot", 2, true, false));
        assertEquals(0, AbilityRules.supplyBonus(AbilityRules.IRON_HEART, "minecraft:iron_ingot", 1, false, false));
        assertEquals(0, AbilityRules.supplyBonus(AbilityRules.FIRE_BOLT, "minecraft:charcoal", 1, false, false));
        assertEquals(2, AbilityRules.supplyBonus(AbilityRules.FIRE_BOLT, "minecraft:charcoal", 2, false, false));
        assertEquals(4, AbilityRules.supplyBonus(AbilityRules.FIRE_BOLT, "minecraft:charcoal", 2, true, false));
        assertEquals(0, AbilityRules.supplyBonus(AbilityRules.FIRE_BOLT, "minecraft:stick", 8, false, false));
        assertEquals(0, AbilityRules.supplyBonus(AbilityRules.SMITE_EVIL, "minecraft:golden_sword", 1, false, false));
        assertEquals(2, AbilityRules.supplyBonus(AbilityRules.SMITE_EVIL, "minecraft:golden_sword", 1, true, false));
        assertEquals(2, AbilityRules.supplyBonus(AbilityRules.MAGIC_ARROW, "minecraft:arrow", 2, false, false));
        assertEquals(0, AbilityRules.supplyBonus(null, "minecraft:coal", 4, true, true));
    }

    @Test
    void hintRequiresEnoughMana() {
        assertFalse(AbilityRules.hasManaToUse(0.0));
        assertFalse(AbilityRules.hasManaToUse(0.9));
        assertTrue(AbilityRules.hasManaToUse(1.0));
        assertTrue(AbilityRules.hasManaToUse(3.0));
        assertEquals(1, AbilityRules.manaCost(AbilityRules.HEAL));
        assertEquals(1, AbilityRules.manaCost(AbilityRules.DETECT_ORE));
        assertTrue(AbilityRules.hasManaToUse(AbilityRules.DETECT_ORE, 1.0));
        assertTrue(AbilityRules.hasManaToUse(AbilityRules.DETECT_ORE, 2.0));
        assertTrue(AbilityRules.hasManaToUse(AbilityRules.HEAL, 1.0));
        assertFalse(AbilityRules.hasManaToUse(AbilityRules.HEAL, 0.0));
        assertTrue(AbilityRules.hasManaToHeal(0.0, true));
        assertFalse(AbilityRules.showsAbilityHints(0));
        assertTrue(AbilityRules.showsAbilityHints(1));
        assertTrue(AbilityRules.showsAbilityHints(4));
    }

    @Test
    void growTimesIsRoundedAbilityLevelPlusHoeBonus() {
        assertEquals(1, AbilityRules.growTimes(0, 0));
        assertEquals(1, AbilityRules.growTimes(1, 0));
        assertEquals(2, AbilityRules.growTimes(1, 4));
        assertEquals(3, AbilityRules.growTimes(2, 9));
        assertEquals(4, AbilityRules.growTimes(1, 0, 4));
        assertEquals(3, AbilityRules.GROW_HOE_DAMAGE);
        assertEquals(2, AbilityRules.GROW_RANGE);
    }

    @Test
    void growHoeDamageUsesOneUnbreakingRollLikeTilling() {
        assertEquals(3, AbilityRules.growHoeDamageTaken(0, bound -> 0));
        assertEquals(3, AbilityRules.growHoeDamageTaken(0, bound -> 1));
        assertEquals(3, AbilityRules.growHoeDamageTaken(1, bound -> 0));
        assertEquals(0, AbilityRules.growHoeDamageTaken(1, bound -> 1));
        assertEquals(3, AbilityRules.growHoeDamageTaken(3, bound -> 0));
        assertEquals(0, AbilityRules.growHoeDamageTaken(3, bound -> 1));
        assertEquals(0, AbilityRules.growHoeDamageTaken(3, bound -> 2));
    }

    @Test
    void growHoeBonusByMaterial() {
        assertEquals(0, AbilityRules.growHoeBonus("minecraft:wooden_hoe"));
        assertEquals(0, AbilityRules.growHoeBonus("minecraft:stone_hoe"));
        assertEquals(1, AbilityRules.growHoeBonus("minecraft:copper_hoe"));
        assertEquals(2, AbilityRules.growHoeBonus("minecraft:iron_hoe"));
        assertEquals(3, AbilityRules.growHoeBonus("minecraft:diamond_hoe"));
        assertEquals(4, AbilityRules.growHoeBonus("minecraft:golden_hoe"));
        assertEquals(5, AbilityRules.growHoeBonus("minecraft:netherite_hoe"));
        assertEquals(0, AbilityRules.growHoeBonus("minecraft:modded_hoe"));
        assertEquals("", AbilityRules.growHoeBonusLabel(0));
        assertEquals("+2", AbilityRules.growHoeBonusLabel(2));
        assertEquals("+4", AbilityRules.growHoeBonusLabel(4));
        assertEquals(0, AbilityRules.pickaxeBonus("minecraft:wooden_pickaxe"));
        assertEquals(1, AbilityRules.pickaxeBonus("minecraft:copper_pickaxe"));
        assertEquals(2, AbilityRules.pickaxeBonus("minecraft:iron_pickaxe"));
        assertEquals(3, AbilityRules.pickaxeBonus("minecraft:diamond_pickaxe"));
        assertEquals(4, AbilityRules.pickaxeBonus("minecraft:golden_pickaxe"));
        assertEquals(5, AbilityRules.pickaxeBonus("minecraft:netherite_pickaxe"));
        assertEquals(480, AbilityRules.powerMineDurationTicks(2.0));
        assertEquals(240, AbilityRules.powerMineDurationTicks(1.0));
    }

    @Test
    void growPicksDistinctPlantsNotOnePlantManyStages() {
        List<String> plants = List.of("a", "b", "c", "d");
        List<String> picked = AbilityRules.pickDistinctRandom(plants, 3, size -> 0);
        assertEquals(List.of("a", "b", "c"), picked);
        assertEquals(1, AbilityRules.pickDistinctRandom(plants, 1, size -> 2).size());
        assertEquals(4, AbilityRules.pickDistinctRandom(plants, 99, size -> 0).size());
        assertEquals(0, AbilityRules.pickDistinctRandom(List.of(), 5, size -> 0).size());
        assertEquals(Set.copyOf(picked).size(), picked.size(), "the same plant must not be grown twice in one cast");
    }

    @Test
    void nearestOtherPicksClosestNotSelf() {
        int[] xs = {0, 4, 1, 8};
        int[] ys = {0, 0, 0, 0};
        int[] zs = {0, 0, 0, 0};
        assertEquals(2, AbilityRules.nearestOtherIndex(0, 0, 0, xs, ys, zs, null));
        assertEquals(0, AbilityRules.nearestOtherIndex(1, 0, 0, xs, ys, zs, null));
        boolean[] skip = {false, false, true, false};
        assertEquals(1, AbilityRules.nearestOtherIndex(0, 0, 0, xs, ys, zs, skip));
        assertEquals(-1, AbilityRules.nearestOtherIndex(0, 0, 0, new int[] {0}, new int[] {0}, new int[] {0}, null));
    }

    @Test
    void lastCatalystIsKeptAndGivesNoBonus() {
        assertFalse(AbilityRules.consumesCatalyst(1));
        assertTrue(AbilityRules.consumesCatalyst(2));
        assertEquals(0, AbilityRules.catalystBonus(1));
        assertEquals(2, AbilityRules.catalystBonus(2));
        assertEquals(0, AbilityRules.catalystBonus(0));
    }

    @Test
    void powerLabelDropsTrailingZero() {
        assertEquals("1", AbilityRules.powerLabel(1.0));
        assertEquals("3", AbilityRules.powerLabel(3.0));
        assertEquals("2.4", AbilityRules.powerLabel(1.0 + Math.sqrt(2.0)));
        assertEquals("Fire bolt", AbilityRules.abilityListName("Fire bolt", true));
        assertEquals("Fire bolt*", AbilityRules.abilityListName("Fire bolt", false));
        assertEquals("*", AbilityRules.UNLEARNED_MARK);
    }

    @Test
    void healRangeIsTwoPlusAbilityLevel() {
        assertEquals(2.0, AbilityRules.healRange(0), 1e-9);
        assertEquals(3.0, AbilityRules.healRange(1), 1e-9);
        assertEquals(4.0, AbilityRules.healRange(2), 1e-9);
        assertEquals(6.4, AbilityRules.healRange(4.4), 1e-9);
        assertEquals(2.0, AbilityRules.healRange(-3), 1e-9);
        assertEquals(2.0, AbilityRules.HEAL_BASE_RANGE, 1e-9);
        assertEquals(2.0, AbilityRules.ironHeartRange(0), 1e-9);
        assertEquals(4.0, AbilityRules.ironHeartRange(2), 1e-9);
        assertEquals(2.0F, AbilityRules.ironHeartHealth(2), 1e-4F);
        assertEquals(0.0F, AbilityRules.ironHeartHealth(0), 1e-4F);
        assertEquals(1200, AbilityRules.ironHeartDurationTicks(1));
        assertEquals(2400, AbilityRules.ironHeartDurationTicks(2));
        assertEquals(0, AbilityRules.ironHeartDurationTicks(0));
        assertEquals(60, AbilityRules.IRON_HEART_SECONDS_PER_POWER);
    }

    @Test
    void fireRangeScalesWithPower() {
        assertEquals(16.0, AbilityRules.fireRange(0), 1e-9);
        assertEquals(20.0, AbilityRules.fireRange(2), 1e-9);
        assertEquals(26.0, AbilityRules.fireRange(5), 1e-9);
    }

    @Test
    void flightDurationAndSpeedScaleWithPower() {
        assertEquals(240, AbilityRules.flightDurationTicks(2));
        assertEquals(720, AbilityRules.flightDurationTicks(6));
        assertEquals(0.0014F, AbilityRules.flightSpeed(1), 1e-6F);
        assertEquals(0.007F, AbilityRules.flightSpeed(5), 1e-6F);
        assertEquals(0.014F, AbilityRules.flightSpeed(10), 1e-6F);
        assertEquals(0.014F, AbilityRules.flightSpeed(20), 1e-6F);
        assertEquals(0.7F, AbilityRules.FLIGHT_SPEED_FACTOR, 1e-6F);
        assertEquals(0.35, AbilityRules.flightLaunchY(0.0), 1e-9);
        assertEquals(0.5, AbilityRules.flightLaunchY(0.5), 1e-9);
    }

    @Test
    void cooldownShrinksWithLevelAndSkill() {
        assertEquals(2400, AbilityRules.cooldownTicks(0, 0));
        assertEquals(2300, AbilityRules.cooldownTicks(1, 0));
        assertEquals(20, AbilityRules.cooldownTicks(24, 0));
        assertEquals(20, AbilityRules.cooldownTicks(100, 0));
        assertEquals(2100, AbilityRules.cooldownTicks(1, 4));
        assertEquals(2400, AbilityRules.cooldownTicks(AbilityRules.HEAL, 0, 0));
        assertEquals(2100, AbilityRules.cooldownTicks(AbilityRules.FLIGHT, 1, 4));
    }

    @Test
    void nearestAlongAimPicksTheTargetOnTheLookRay() {
        double[] xs = {0.2, 3.0, 0.0};
        double[] ys = {0.0, 0.0, 0.0};
        double[] zs = {8.0, 2.0, -2.0};
        assertEquals(0, AbilityRules.nearestAlongAim(0, 0, 0, 0, 0, 1, 16, xs, ys, zs));
        assertEquals(1, AbilityRules.nearestAlongAim(0, 0, 0, 0, 0, 1, 4, xs, ys, zs));
        assertEquals(-1, AbilityRules.nearestAlongAim(0, 0, 0, 0, 0, 1, 1, xs, ys, zs));
        assertEquals(-1, AbilityRules.nearestAlongAim(0, 0, 0, 0, 0, 1, 16, new double[0], new double[0], new double[0]));
        double[] vx = {0.0};
        double[] vy = {0.0};
        double[] vz = {10.0};
        assertEquals(0, AbilityRules.nearestAlongAim(0, 0, 0, 0, 0, 1, 16, vx, vy, vz));
    }

    @Test
    void seekDeltaSteersTowardTheTarget() {
        double[] next = AbilityRules.seekDelta(0, 0, 0, 0, 0, 1.5, 10, 0, 10, 1.0);
        assertTrue(next[0] > 0.4);
        assertEquals(0.0, next[1], 1e-9);
        assertTrue(next[2] > 0.4);
        double speed = Math.sqrt(next[0] * next[0] + next[1] * next[1] + next[2] * next[2]);
        assertEquals(1.5, speed, 1e-9);
        double[] same = AbilityRules.seekDelta(0, 0, 0, 0, 0, 1.5, 0, 0, 20, 0.0);
        assertEquals(0.0, same[0], 1e-9);
        assertEquals(1.5, same[2], 1e-9);
    }

    @Test
    void fireBoltCooldownIsTenSecondsMinusManaAndSkill() {
        assertEquals(200, AbilityRules.fireBoltCooldownTicks(0, 0));
        assertEquals(180, AbilityRules.fireBoltCooldownTicks(1, 0));
        assertEquals(140, AbilityRules.fireBoltCooldownTicks(1, 4));
        assertEquals(0, AbilityRules.fireBoltCooldownTicks(10, 0));
        assertEquals(0, AbilityRules.fireBoltCooldownTicks(20, 0));
        assertEquals(200, AbilityRules.cooldownTicks(AbilityRules.FIRE_BOLT, 0, 0));
        assertEquals(140, AbilityRules.cooldownTicks(AbilityRules.FIRE_BOLT, 1, 4));
        assertEquals(200, AbilityRules.cooldownTicks(AbilityRules.MAGIC_ARROW, 0, 0));
        assertEquals(140, AbilityRules.cooldownTicks(AbilityRules.MAGIC_ARROW, 1, 4));
    }

    @Test
    void fireBoltEffectiveLevelAddsManaLevel() {
        assertEquals(0.0, AbilityRules.power(AbilityRules.HEAL, 0, 0, 0), 1e-9);
        assertEquals(0.0, AbilityRules.power(AbilityRules.FIRE_BOLT, 0, 0, 0), 1e-9);
        assertEquals(0.0, AbilityRules.power(AbilityRules.HEAL, 1, 0, 0), 1e-9);
        assertEquals(1.0, AbilityRules.power(AbilityRules.FIRE_BOLT, 1, 0, 0), 1e-9);
        assertEquals(0.0, AbilityRules.power(AbilityRules.HEAL, 4, 0, 0), 1e-9);
        assertEquals(4.0, AbilityRules.power(AbilityRules.FIRE_BOLT, 4, 0, 0), 1e-9);
        assertEquals(4.0, AbilityRules.power(AbilityRules.FIRE_BOLT, 1, 4, 1), 1e-9);
        assertEquals(4.0, AbilityRules.power(AbilityRules.FIRE_BOLT, 4, 0, 0), 1e-9);
        assertEquals(1.0, AbilityRules.power(AbilityRules.MAGIC_ARROW, 1, 0, 0), 1e-9);
        assertEquals(4.0, AbilityRules.power(AbilityRules.MAGIC_ARROW, 4, 0, 0), 1e-9);
        assertEquals(1.0, AbilityRules.untrainedPower(0.0), 1e-9);
        assertEquals(1.0, AbilityRules.untrainedPower(2.0), 1e-9);
        assertEquals(1.0, AbilityRules.untrainedPower(3.0), 1e-9);
        assertEquals(2.0, AbilityRules.untrainedPower(5.0), 1e-9);
        assertEquals(1.0, AbilityRules.power(AbilityRules.HEAL, 0, 0, 0, false), 1e-9);
        assertEquals(1.0, AbilityRules.power(AbilityRules.FIRE_BOLT, 2, 0, 0, false), 1e-9);
        assertEquals(3.0, AbilityRules.power(AbilityRules.FIRE_BOLT, 4, 0, 2, false), 1e-9);
        assertEquals(3, AbilityRules.UNKNOWN_ABILITY_PENALTY);
        assertEquals(1.0, AbilityRules.MIN_POWER, 1e-9);
    }

    @Test
    void detectOreRangeBonusAndLook() {
        assertEquals(2, AbilityRules.detectOreRange(0));
        assertEquals(3, AbilityRules.detectOreRange(1));
        assertEquals(4, AbilityRules.detectOreRange(2));
        assertEquals(3, AbilityRules.detectOreRange(0.6));
        assertEquals(2, AbilityRules.detectOreRange(0.4));
        assertEquals(0, AbilityRules.redstoneDustBonus(false));
        assertEquals(2, AbilityRules.redstoneDustBonus(true));
        assertEquals(0, AbilityRules.detectOreItemBonus(false, false));
        assertEquals(2, AbilityRules.detectOreItemBonus(true, false));
        assertEquals(2, AbilityRules.detectOreItemBonus(false, true));
        assertEquals(4, AbilityRules.detectOreItemBonus(true, true));
        assertEquals(0.0, AbilityRules.lookYaw(0, 1), 1e-4);
        assertEquals(90.0, AbilityRules.lookYaw(-1, 0), 1e-4);
        assertEquals(-90.0, AbilityRules.lookYaw(1, 0), 1e-4);
        assertEquals(180.0, Math.abs(AbilityRules.lookYaw(0, -1)), 1e-4);
        assertEquals(90.0, AbilityRules.lookPitch(0, -1, 0), 1e-4);
        assertEquals(-90.0, AbilityRules.lookPitch(0, 1, 0), 1e-4);
        assertEquals(0.0, AbilityRules.lookPitch(0, 0, 1), 1e-4);
    }

    @Test
    void slowScalesRangeDurationAndAmount() {
        assertEquals(0.0, AbilityRules.slowRange(0), 1e-9);
        assertEquals(2.0, AbilityRules.slowRange(2), 1e-9);
        assertEquals(0, AbilityRules.slowDurationTicks(0));
        assertEquals(120, AbilityRules.slowDurationTicks(1));
        assertEquals(240, AbilityRules.slowDurationTicks(2));
        assertEquals(0.20, AbilityRules.slowAmount(0, 20), 1e-9);
        assertEquals(0.25, AbilityRules.slowAmount(1, 20), 1e-9);
        assertEquals(0.80, AbilityRules.slowAmount(12, 20), 1e-9);
        assertEquals(0.80, AbilityRules.slowAmount(20, 20), 1e-9);
        assertEquals(0.125, AbilityRules.slowAmount(1, 101), 1e-9);
        assertEquals(0.40, AbilityRules.slowAmount(12, 200), 1e-9);
        assertEquals(0.25, AbilityRules.slowAmount(1, 100), 1e-9);
    }

    @Test
    void lightDurationIsThirtySecondsPlusThirtyPerLevel() {
        assertTrue(AbilityRules.isLightCoal("minecraft:coal"));
        assertFalse(AbilityRules.isLightCoal("minecraft:charcoal"));
        assertFalse(AbilityRules.isLightCoal("minecraft:torch"));
        assertFalse(AbilityRules.isLightCoal("minecraft:soul_torch"));
        assertFalse(AbilityRules.isLightCoal("minecraft:redstone_torch"));
        assertEquals(600, AbilityRules.lightDurationTicks(0));
        assertEquals(1200, AbilityRules.lightDurationTicks(1));
        assertEquals(1800, AbilityRules.lightDurationTicks(2));
        assertEquals(1800, AbilityRules.lightDurationTicks(1.6));
        assertEquals(1200, AbilityRules.lightDurationTicks(1.4));
        assertEquals(14, AbilityRules.LIGHT_LEVEL);
        assertEquals(9, AbilityRules.lightLevel(0));
        assertEquals(9, AbilityRules.lightLevel(599));
        assertEquals(9, AbilityRules.lightLevel(600));
        assertEquals(10, AbilityRules.lightLevel(1200));
        assertEquals(11, AbilityRules.lightLevel(2400));
        assertEquals(14, AbilityRules.lightLevel(6000));
        assertEquals(14, AbilityRules.lightLevel(7200));
        assertEquals(9, AbilityRules.LIGHT_LEVEL_BASE);
    }

    @Test
    void detectOreRanksFamiliesAndClusters() {
        assertEquals("iron", AbilityRules.oreFamily("minecraft:iron_ore"));
        assertEquals("iron", AbilityRules.oreFamily("minecraft:deepslate_iron_ore"));
        assertEquals("gold", AbilityRules.oreFamily("minecraft:nether_gold_ore"));
        assertEquals("quartz", AbilityRules.oreFamily("minecraft:nether_quartz_ore"));
        assertEquals("ancient_debris", AbilityRules.oreFamily("minecraft:ancient_debris"));
        assertEquals(100, AbilityRules.oreValue("minecraft:ancient_debris"));
        assertEquals(90, AbilityRules.oreValue("minecraft:diamond_ore"));
        assertEquals(90, AbilityRules.oreValue("minecraft:deepslate_diamond_ore"));
        assertEquals(80, AbilityRules.oreValue("minecraft:emerald_ore"));
        assertEquals(70, AbilityRules.oreValue("minecraft:gold_ore"));
        assertEquals(70, AbilityRules.oreValue("minecraft:nether_gold_ore"));
        assertEquals(60, AbilityRules.oreValue("minecraft:lapis_ore"));
        assertEquals(50, AbilityRules.oreValue("minecraft:redstone_ore"));
        assertEquals(40, AbilityRules.oreValue("minecraft:iron_ore"));
        assertEquals(30, AbilityRules.oreValue("minecraft:copper_ore"));
        assertEquals(20, AbilityRules.oreValue("minecraft:nether_quartz_ore"));
        assertEquals(10, AbilityRules.oreValue("minecraft:coal_ore"));
        assertEquals(0, AbilityRules.oreValue("minecraft:stone"));
        assertTrue(AbilityRules.isDetectableOre("minecraft:diamond_ore"));
        assertFalse(AbilityRules.isDetectableOre("minecraft:stone"));
        assertEquals("ancient debris", AbilityRules.oreFamilyLabel("ancient_debris"));
        assertEquals("lapis lazuli", AbilityRules.oreFamilyLabel("lapis"));

        AbilityRules.OreDeposit diamond = new AbilityRules.OreDeposit("diamond", 90, 1, 8, 0, 0, 64);
        AbilityRules.OreDeposit hugeCoal = new AbilityRules.OreDeposit("coal", 10, 999, 1, 0, 0, 1);
        assertTrue(diamond.score() > hugeCoal.score());
        assertEquals(diamond, AbilityRules.bestDeposit(List.of(hugeCoal, diamond)));

        List<AbilityRules.OreSample> mixed = List.of(
                new AbilityRules.OreSample("minecraft:iron_ore", 1, 0, 0),
                new AbilityRules.OreSample("minecraft:deepslate_iron_ore", 2, 0, 0),
                new AbilityRules.OreSample("minecraft:coal_ore", 0, 3, 0),
                new AbilityRules.OreSample("minecraft:diamond_ore", 10, 0, 0));
        AbilityRules.OreDeposit best = AbilityRules.bestDeposit(AbilityRules.clusterDeposits(mixed, 0, 0, 0));
        assertEquals("diamond", best.family());
        assertEquals(1, best.size());
        assertEquals(10, best.lookX());

        List<AbilityRules.OreSample> vein = List.of(
                new AbilityRules.OreSample("minecraft:iron_ore", 4, 0, 0),
                new AbilityRules.OreSample("minecraft:iron_ore", 5, 0, 0),
                new AbilityRules.OreSample("minecraft:iron_ore", 6, 0, 0));
        AbilityRules.OreDeposit iron = AbilityRules.bestDeposit(AbilityRules.clusterDeposits(vein, 0, 0, 0));
        assertEquals("iron", iron.family());
        assertEquals(3, iron.size());
        assertEquals(4, iron.lookX());

        List<AbilityRules.OreSample> split = List.of(
                new AbilityRules.OreSample("minecraft:gold_ore", 2, 0, 0),
                new AbilityRules.OreSample("minecraft:gold_ore", 8, 0, 0));
        List<AbilityRules.OreDeposit> golds = AbilityRules.clusterDeposits(split, 0, 0, 0);
        assertEquals(2, golds.size());
        AbilityRules.OreDeposit nearer = AbilityRules.bestDeposit(golds);
        assertEquals(2, nearer.lookX());
        assertEquals(8, AbilityRules.depositDistanceBlocks(new AbilityRules.OreDeposit("diamond", 90, 1, 8, 0, 0, 64)));
        assertEquals(0, AbilityRules.depositDistanceBlocks(null));
        assertEquals(
                "The compass pulls toward iron (roughly 4 blocks away).",
                AbilityRules.detectOreFoundFallback("iron", 4));
    }

    @Test
    void senseEvilRangeAndMessages() {
        assertEquals(0.0, AbilityRules.senseEvilRange(0), 1e-9);
        assertEquals(250.0, AbilityRules.senseEvilRange(1), 1e-9);
        assertEquals(500.0, AbilityRules.senseEvilRange(2), 1e-9);
        assertEquals(600.0, AbilityRules.senseEvilRange(2.4), 1e-9);
        assertTrue(AbilityRules.senseEvilShowsDistance(250, 1));
        assertTrue(AbilityRules.senseEvilShowsDistance(0, 1));
        assertFalse(AbilityRules.senseEvilShowsDistance(251, 1));
        assertTrue(AbilityRules.senseEvilShowsDistance(500, 2));
        assertFalse(AbilityRules.senseEvilShowsDistance(501, 2));
        assertEquals(120, AbilityRules.roughBlocks(119.6));
        assertEquals(0, AbilityRules.roughBlocks(-3));
        assertEquals(
                "You sense an ominous presence ahead...", AbilityRules.senseEvilFoundFallback());
        assertEquals(
                "You sense an ominous presence ahead... (roughly 120 blocks away)",
                AbilityRules.senseEvilFoundNearFallback(120));
        assertTrue(AbilityRules.senseEvilNoneFallback().contains("do not sense"));
        assertTrue(AbilityRules.senseEvilNoneFallback().contains("More exploring"));
        double[] xs = {10, 0, 100};
        double[] ys = {0, 0, 0};
        double[] zs = {0, 8, 0};
        assertEquals(1, AbilityRules.nearestPointIndex(0, 0, 0, xs, ys, zs));
        assertEquals(0, AbilityRules.nearestPointIndex(9, 0, 0, xs, ys, zs));
        assertEquals(-1, AbilityRules.nearestPointIndex(0, 0, 0, new double[0], new double[0], new double[0]));
    }

    @Test
    void remainingCooldownAndLabel() {
        assertEquals(0, AbilityRules.remainingCooldownTicks(100, -1, 2400));
        assertEquals(0, AbilityRules.remainingCooldownTicks(2500, 0, 2400));
        assertEquals(400, AbilityRules.remainingCooldownTicks(2000, 0, 2400));
        assertEquals("1 second", AbilityRules.cooldownLabel(1));
        assertEquals("1 second", AbilityRules.cooldownLabel(20));
        assertEquals("20 seconds", AbilityRules.cooldownLabel(400));
    }
}

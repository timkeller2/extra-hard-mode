package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.WorldGate;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodData;

/** Applies and removes Well Fed bonuses. The level itself lives on the player. */
public final class WellFed {
    private static final AttributeModifier.Operation ADD = AttributeModifier.Operation.ADD_VALUE;

    private WellFed() {}

    public static boolean active(ServerPlayer player) {
        return player.level() instanceof net.minecraft.server.level.ServerLevel level
                && WorldGate.isModuleActive(level, Hunger.ID)
                && !EhmApi.playerBypasses(player)
                && !player.isSpectator();
    }

    public static int level(ServerPlayer player) {
        if (player == null || !active(player)) {
            return 0;
        }
        Integer value = player.getAttachedOrElse(EhmAttachments.EHM_WELL_FED, 0);
        return value == null ? 0 : Math.max(0, value);
    }

    public static void maintain(ServerPlayer player) {
        if (!active(player)) {
            clearModifiers(player);
            clampFood(player, WellFedRules.BASE_FOOD);
            return;
        }
        applyModifiers(player, level(player));
    }

    /** Call after the meal is recorded. Fills new health and hunger. Mana level is not filled. */
    public static int update(ServerPlayer player, List<String> history) {
        int previous = Math.max(0, player.getAttachedOrElse(EhmAttachments.EHM_WELL_FED, 0));
        int next = WellFedRules.nextLevel(history, previous);
        if (next != previous) {
            player.setAttached(EhmAttachments.EHM_WELL_FED, next);
            applyModifiers(player, next);
            shiftBars(player, previous, next);
            announce(player, previous, next);
        } else {
            applyModifiers(player, next);
        }
        return next;
    }

    private static void shiftBars(ServerPlayer player, int previous, int next) {
        int hunger = WellFedRules.foodMax(next) - WellFedRules.foodMax(previous);
        FoodData food = player.getFoodData();
        if (hunger > 0) {
            food.setFoodLevel(food.getFoodLevel() + hunger);
            food.setSaturation(food.getSaturationLevel() + hunger);
        }
        clampFood(player, WellFedRules.foodMax(next));
        int health = WellFedRules.healthBonus(next) - WellFedRules.healthBonus(previous);
        if (health > 0) {
            player.heal(health);
        }
    }

    public static void clampFood(ServerPlayer player, int maxFood) {
        FoodData food = player.getFoodData();
        int max = Math.max(0, maxFood);
        if (food.getFoodLevel() > max) {
            food.setFoodLevel(max);
        }
        if (food.getSaturationLevel() > food.getFoodLevel()) {
            food.setSaturation(food.getFoodLevel());
        }
    }

    private static void announce(ServerPlayer player, int previous, int next) {
        if (next > previous) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.well_fed",
                    "Well Fed %s.",
                    Component.literal(Integer.toString(next))));
            return;
        }
        if (next <= 0) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.well_fed_gone", "You are no longer well fed."));
            return;
        }
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.well_fed_drop",
                "Well Fed drops to %s.",
                Component.literal(Integer.toString(next))));
    }

    private static void applyModifiers(ServerPlayer player, int wellFed) {
        setModifier(player.getAttribute(Attributes.MAX_HEALTH), ExtraHardModeMod.WELL_FED_HEALTH, WellFedRules.healthBonus(wellFed));
        setModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), ExtraHardModeMod.WELL_FED_DAMAGE, WellFedRules.meleeBonus(wellFed));
        setModifier(player.getAttribute(Attributes.LUCK), ExtraHardModeMod.WELL_FED_LUCK, WellFedRules.luck(wellFed) ? 1 : 0);
    }

    private static void clearModifiers(ServerPlayer player) {
        applyModifiers(player, 0);
    }

    private static void setModifier(AttributeInstance attribute, net.minecraft.resources.Identifier id, double amount) {
        if (attribute == null) {
            return;
        }
        if (amount == 0.0) {
            attribute.removeModifier(id);
            return;
        }
        attribute.addOrUpdateTransientModifier(new AttributeModifier(id, amount, ADD));
    }
}

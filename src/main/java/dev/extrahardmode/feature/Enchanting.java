package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.WorldGate;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Limits enchantment-table buttons to the player's enchanting skill.
 * Anvils, grindstones, librarian books, loot, and commands stay vanilla.
 */
public final class Enchanting implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("enchanting");

    /** Requirement captured for the table click now running on this thread. */
    private static final ThreadLocal<Integer> PENDING = new ThreadLocal<>();

    @Override
    public Identifier id() {
        return ID;
    }

    /** Negative hides the inventory icon. The module being off is the hidden case. */
    public static int displayAbility(ServerPlayer player) {
        if (!moduleOn(player)) {
            return -1;
        }
        return ability(player);
    }

    public static int ability(ServerPlayer player) {
        Integer stored = player.getAttachedOrElse(
                EhmAttachments.EHM_ENCHANT_ABILITY, EnchantingRules.DEFAULT_ABILITY);
        if (stored == null) {
            return EnchantingRules.DEFAULT_ABILITY;
        }
        return stored;
    }

    public static boolean moduleOn(ServerPlayer player) {
        return WorldGate.isModuleActive(player.level(), ID);
    }

    /** A bypass, or the module being off, leaves the table alone and does not train. */
    public static boolean applies(ServerPlayer player) {
        return moduleOn(player) && !EhmApi.playerBypasses(player);
    }

    /**
     * Call at the start of an enchantment-table button click.
     *
     * @return true when that button must be refused
     */
    public static boolean deny(ServerPlayer player, int requirement) {
        PENDING.remove();
        if (!applies(player)) {
            return false;
        }
        int skill = ability(player);
        if (EnchantingRules.canEnchant(skill, requirement)) {
            PENDING.set(requirement);
            return false;
        }
        if (requirement > skill) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.enchant_requirement",
                    "That enchantment requires level %s. Your enchanting skill is %s.",
                    Component.literal(Integer.toString(requirement)),
                    Component.literal(Integer.toString(skill))));
            return true;
        }
        return false;
    }

    public static void clearPending() {
        PENDING.remove();
    }

    /** Train only when the table actually enchanted the item for the armed click. */
    public static void onPerformed(ServerPlayer player) {
        Integer requirement = PENDING.get();
        if (requirement == null) {
            return;
        }
        PENDING.remove();
        if (!applies(player)) {
            return;
        }
        int skill = ability(player);
        int roll = player.getRandom().nextInt();
        int next = EnchantingRules.nextAbility(skill, requirement, roll);
        if (next == skill) {
            if (EnchantingRules.trains(skill, requirement)) {
                player.sendSystemMessage(Component.translatableWithFallback(
                        "tougher.message.enchant_skill_stay",
                        "A roll of %s leaves your enchanting skill at %s.",
                        Component.literal(Integer.toString(EnchantingRules.skillDieFace(roll))),
                        Component.literal(Integer.toString(skill))));
            }
            return;
        }
        player.setAttached(EhmAttachments.EHM_ENCHANT_ABILITY, next);
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.enchant_skill",
                "Your enchanting skill is now %s.",
                Component.literal(Integer.toString(next))));
        Achievements.sendMana(player);
    }

    public static void report(ServerPlayer player, Consumer<Component> send) {
        if (!moduleOn(player)) {
            return;
        }
        send.accept(Component.translatableWithFallback(
                "tougher.command.me.enchanting",
                "Enchanting skill: %s. The table only takes a requirement at or below this.",
                Component.literal(Integer.toString(ability(player)))));
    }
}

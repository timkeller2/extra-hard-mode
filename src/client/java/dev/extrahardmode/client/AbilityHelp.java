package dev.extrahardmode.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.extrahardmode.feature.AbilityRules;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;

/** "?" (Shift+/) help for the held mana-ability item. Rebindable in Controls. */
public final class AbilityHelp {
    public static final KeyMapping KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.extrahardmode.ability_help",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            KeyMapping.Category.GAMEPLAY));

    private static long lastShownMs;

    private AbilityHelp() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (KEY.consumeClick()) {
                tryShow(client);
            }
        });
    }

    public static boolean shouldHandle(Minecraft client) {
        if (client == null || client.player == null || client.gui.screen() != null) {
            return false;
        }
        ItemStack held = client.player.getMainHandItem();
        if (held.isEmpty()) {
            return true;
        }
        String itemId = held.typeHolder()
                .unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse("");
        return AbilityRules.abilityForItem(itemId) != null;
    }

    /**
     * @return true if help was shown (or was just shown), so the key should not do something else
     */
    public static boolean tryShow(Minecraft client) {
        if (!shouldHandle(client)) {
            return false;
        }
        ItemStack held = client.player.getMainHandItem();
        if (held.isEmpty()) {
            return showIndex(client);
        }
        String itemId = held.typeHolder()
                .unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse("");
        String ability = AbilityRules.abilityForItem(itemId);
        String fallback = AbilityRules.helpFallback(ability);
        String key = AbilityRules.helpKey(ability);
        if (fallback == null || key == null) {
            return false;
        }
        if (!markShown()) {
            return true;
        }
        client.player.sendSystemMessage(Component.translatableWithFallback(
                AbilityRules.POWER_HELP_KEY, AbilityRules.powerHelpFallback()));
        client.player.sendSystemMessage(Component.translatableWithFallback(key, fallback));
        int hoeBonus = AbilityRules.GROW.equals(ability) ? AbilityRules.growHoeBonus(itemId) : 0;
        if (hoeBonus > 0) {
            client.player.sendSystemMessage(Component.translatableWithFallback(
                    "extrahardmode.ability.grow.help_bonus",
                    "This hoe adds %s extra growth.",
                    Component.literal(AbilityRules.growHoeBonusLabel(hoeBonus))));
        }
        int pickBonus = AbilityRules.POWER_MINE.equals(ability) ? AbilityRules.pickaxeBonus(itemId) : 0;
        if (pickBonus > 0) {
            client.player.sendSystemMessage(Component.translatableWithFallback(
                    "extrahardmode.ability.power_mine.help_bonus",
                    "This pickaxe adds %s extra mining time.",
                    Component.literal(AbilityRules.materialBonusLabel(pickBonus))));
        }
        return true;
    }

    private static boolean showIndex(Minecraft client) {
        if (!markShown()) {
            return true;
        }
        client.player.sendSystemMessage(Component.translatableWithFallback(
                AbilityRules.POWER_HELP_KEY, AbilityRules.powerHelpFallback()));
        List<String> keys = AbilityRules.indexHelpKeys();
        List<String> lines = AbilityRules.indexHelpLines();
        for (int i = 0; i < lines.size(); i++) {
            client.player.sendSystemMessage(Component.translatableWithFallback(keys.get(i), lines.get(i)));
        }
        return true;
    }

    private static boolean markShown() {
        long now = Util.getMillis();
        if (now - lastShownMs < 250L) {
            return false;
        }
        lastShownMs = now;
        return true;
    }
}

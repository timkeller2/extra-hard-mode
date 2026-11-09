package dev.extrahardmode.task;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.config.PlayerSettings;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.feature.ArmorSpeedRules;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/**
 * Identifier-keyed movement-speed modifier. Unarmored walk speed is {@code baseSpeed}
 * (default 0.22, 10% faster than vanilla). Worn armor slows that pace by material.
 */
public final class ArmorWeightTask {
    public static final AttributeModifier.Operation OPERATION = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private ArmorWeightTask() {}

    public static void run(ServerLevel level, WorldConfig world) {
        PlayerSettings player = world.player();
        for (ServerPlayer serverPlayer : level.players()) {
            apply(serverPlayer, player);
        }
    }

    public static void apply(ServerPlayer player, PlayerSettings settings) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        if (!settings.armorEnable() || EhmApi.playerBypasses(player)) {
            speed.removeModifier(ExtraHardModeMod.ARMOR_SLOWDOWN);
            return;
        }
        double slow = ArmorSpeedRules.slowPercent(
                wornArmorIds(player),
                settings.copperIronGoldSlowdownPercent(),
                settings.diamondSlowdownPercent(),
                settings.leatherChainmailSlowdownPercent(),
                settings.heavyExtraSlowdownPercent());
        double amount = ArmorSpeedRules.modifierAmount(settings.baseSpeed(), slow);
        speed.addOrUpdateTransientModifier(new AttributeModifier(ExtraHardModeMod.ARMOR_SLOWDOWN, amount, OPERATION));
    }

    private static List<String> wornArmorIds(ServerPlayer player) {
        List<String> ids = new ArrayList<>(ARMOR_SLOTS.length);
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id != null) {
                ids.add(id.toString());
            }
        }
        return ids;
    }

    public static void clear(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(ExtraHardModeMod.ARMOR_SLOWDOWN);
        }
    }

    public static void clearAll(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            clear(player);
        }
    }
}

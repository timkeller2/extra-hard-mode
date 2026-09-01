package dev.extrahardmode.task;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.config.PlayerSettings;
import dev.extrahardmode.config.WorldConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Identifier-keyed movement-speed modifier. Vanilla walk-speed 0.2 maps to attribute 0.1;
 * unarmored buffs to 0.22 walk-speed; full diamond (20 armor) is a 40% slowdown from that base.
 */
public final class ArmorWeightTask {
    public static final AttributeModifier.Operation OPERATION = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;

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
        double armor = player.getAttributeValue(Attributes.ARMOR);
        double fraction = Math.min(1.0, armor / 20.0);
        // walkSpeed 0.22/0.2 = 1.1x vanilla attribute; then interpolate the configured slowdown.
        double amount = (settings.baseSpeed() / 0.2) * (1.0 - fraction * (settings.fullDiamondSlowdownPercent() / 100.0))
                - 1.0;
        speed.addOrUpdateTransientModifier(new AttributeModifier(ExtraHardModeMod.ARMOR_SLOWDOWN, amount, OPERATION));
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

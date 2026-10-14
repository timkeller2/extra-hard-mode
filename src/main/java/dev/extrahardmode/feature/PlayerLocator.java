package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * Vanilla locator bar, gated per player. Receive range is zero until a golden
 * carrot is eaten and some saturation remains. Transmit stays on, so another
 * player whose bar is also on can see this one. Players with the bar off cannot.
 */
public final class PlayerLocator {
    public static final Identifier RECEIVE_OFF = ExtraHardModeMod.id("locator_receive_off");

    /** Clears a transmit mute left by the previous build. Transient, so a restart drops it too. */
    private static final Identifier TRANSMIT_OFF = ExtraHardModeMod.id("locator_transmit_off");

    private static final AttributeModifier RECEIVE_OFF_MODIFIER = new AttributeModifier(
            RECEIVE_OFF, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    private PlayerLocator() {}

    public static void onFoodEaten(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer) || stack == null || !stack.is(Items.GOLDEN_CARROT)) {
            return;
        }
        if (!(serverPlayer.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, Players.ID)) {
            return;
        }
        if (EhmApi.playerBypasses(serverPlayer)) {
            return;
        }
        float saturation = serverPlayer.getFoodData().getSaturationLevel();
        if (!LocatorBarRules.armed(false, true, saturation)) {
            return;
        }
        serverPlayer.setAttached(EhmAttachments.EHM_LOCATOR_CARROT, Boolean.TRUE);
        refresh(serverPlayer);
    }

    public static void refreshAll(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            refresh(player);
        }
    }

    public static void clearAll(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            clear(player);
        }
    }

    public static void refresh(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, Players.ID)
                || EhmApi.playerBypasses(player)) {
            clear(player);
            return;
        }
        enableGamerule(level);
        boolean wasArmed = Boolean.TRUE.equals(player.getAttached(EhmAttachments.EHM_LOCATOR_CARROT));
        boolean armed = LocatorBarRules.armed(wasArmed, false, player.getFoodData().getSaturationLevel());
        if (armed != wasArmed) {
            player.setAttached(EhmAttachments.EHM_LOCATOR_CARROT, armed);
        }
        if (apply(player, armed)) {
            keepTransmitting(player);
        }
    }

    /** Drop Tougher modifiers so vanilla receive and transmit return. */
    public static void clear(ServerPlayer player) {
        AttributeInstance receive = player.getAttribute(Attributes.WAYPOINT_RECEIVE_RANGE);
        AttributeInstance transmit = player.getAttribute(Attributes.WAYPOINT_TRANSMIT_RANGE);
        boolean changed = silence(receive, RECEIVE_OFF, RECEIVE_OFF_MODIFIER, false);
        changed |= silence(transmit, TRANSMIT_OFF, null, false);
        if (changed) {
            keepTransmitting(player);
        }
    }

    /**
     * @return true when a modifier was added or removed
     */
    private static boolean apply(ServerPlayer player, boolean armed) {
        AttributeInstance receive = player.getAttribute(Attributes.WAYPOINT_RECEIVE_RANGE);
        AttributeInstance transmit = player.getAttribute(Attributes.WAYPOINT_TRANSMIT_RANGE);
        boolean changed = silence(transmit, TRANSMIT_OFF, null, false);
        changed |= silence(receive, RECEIVE_OFF, RECEIVE_OFF_MODIFIER, !armed);
        return changed;
    }

    private static boolean silence(
            AttributeInstance attribute, Identifier id, AttributeModifier modifier, boolean silenced) {
        if (attribute == null || attribute.hasModifier(id) == silenced) {
            return false;
        }
        if (silenced) {
            attribute.addOrUpdateTransientModifier(modifier);
        } else {
            attribute.removeModifier(id);
        }
        return true;
    }

    /**
     * Closing receive calls {@code removePlayer}, which also untracks. Put the player
     * back on the map so other players whose bar is on can still see them.
     */
    private static void keepTransmitting(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        level.getWaypointManager().trackWaypoint(player);
    }

    private static void enableGamerule(ServerLevel level) {
        if (!level.getGameRules().get(GameRules.LOCATOR_BAR)) {
            level.getGameRules().set(GameRules.LOCATOR_BAR, true, level.getServer());
        }
    }
}

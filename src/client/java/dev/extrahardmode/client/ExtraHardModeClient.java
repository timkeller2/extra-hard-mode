package dev.extrahardmode.client;

import dev.extrahardmode.feature.Dragon;
import dev.extrahardmode.network.ClientboundSyncPayload;
import dev.extrahardmode.network.ClientboundToastPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ExtraHardModeClient implements ClientModInitializer {
    private static final SystemToast.SystemToastId TOAST_ID = new SystemToast.SystemToastId(5000L);

    private static volatile ClientboundSyncPayload lastSync;

    public static ClientboundSyncPayload lastSync() {
        return lastSync;
    }

    /** Client destroy-speed / harvest mixins read only {@link ClientboundSyncPayload}. */
    public static boolean denyHardened(Player player, BlockState state) {
        ClientboundSyncPayload sync = lastSync;
        if (sync == null || sync.playerBypass()) {
            return false;
        }
        if (player.hasInfiniteMaterials() || player.isCreative()) {
            return false;
        }
        Identifier blockId = id(state);
        if (blockId == null || !sync.hardenedBlocks().contains(blockId)) {
            return false;
        }
        Identifier toolId = id(player.getMainHandItem());
        return toolId == null || !sync.hardenedPicks().contains(toolId);
    }

    /** Mirrors server {@code BlockItem.place} deny using payload flags only. */
    public static boolean denyOrePlacement(BlockPlaceContext context) {
        ClientboundSyncPayload sync = lastSync;
        if (sync == null || !sync.blockOreNextToStone() || sync.playerBypass()) {
            return false;
        }
        Player player = context.getPlayer();
        if (player != null && (player.hasInfiniteMaterials() || player.isCreative())) {
            return false;
        }
        ItemStack stack = context.getItemInHand();
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        Identifier placed = id(blockItem.getBlock().defaultBlockState());
        if (placed == null || !sync.caveInOres().contains(placed)) {
            return false;
        }
        return touchesHardened(context.getLevel(), context.getClickedPos(), sync);
    }

    public static boolean denyEndBuilding(BlockPlaceContext context) {
        ClientboundSyncPayload sync = lastSync;
        if (sync == null || !sync.noEndBuilding() || sync.playerBypass()) {
            return false;
        }
        Player player = context.getPlayer();
        if (player != null && (player.hasInfiniteMaterials() || player.isCreative())) {
            return false;
        }
        return !Dragon.allowPlaceItem(context.getItemInHand());
    }

    public static void toast(String messageId) {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.execute(() -> showToast(client, messageId));
        }
    }

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ClientboundSyncPayload.TYPE, (payload, context) -> lastSync = payload);
        ClientPlayNetworking.registerGlobalReceiver(ClientboundToastPayload.TYPE, ExtraHardModeClient::onToast);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> lastSync = null);
    }

    private static boolean touchesHardened(Level level, BlockPos pos, ClientboundSyncPayload sync) {
        for (Direction direction : Direction.values()) {
            Identifier neighbor = id(level.getBlockState(pos.relative(direction)));
            if (neighbor != null && sync.hardenedBlocks().contains(neighbor)) {
                return true;
            }
        }
        return false;
    }

    private static Identifier id(BlockState state) {
        return state.typeHolder().unwrapKey().map(key -> key.identifier()).orElse(null);
    }

    private static Identifier id(ItemStack stack) {
        return stack.typeHolder().unwrapKey().map(key -> key.identifier()).orElse(null);
    }

    private static void onToast(ClientboundToastPayload payload, ClientPlayNetworking.Context context) {
        context.client().execute(() -> showToast(context.client(), payload.messageId()));
    }

    private static void showToast(Minecraft client, String messageId) {
        Component title = Component.translatableWithFallback("extrahardmode.toast.title", "Extra Hard Mode");
        Component body = Component.translatableWithFallback(
                "extrahardmode.toast." + messageId,
                toastFallback(messageId));
        SystemToast.addOrUpdate(client.gui.toastManager(), TOAST_ID, title, body);
    }

    private static String toastFallback(String messageId) {
        return switch (messageId) {
            case "enabled_on" -> "Extra Hard Mode is on. /gamerule extrahardmode:enabled";
            case "enabled_off" -> "Extra Hard Mode is off. /gamerule extrahardmode:enabled";
            case "no_placing_ore_against_stone" -> "You can't place ore against stone.";
            case "limited_end_building" -> "Sorry, building here is very limited.";
            case "dragon_fountain_tip" ->
                "Congratulations on defeating the dragon! If you can't reach the fountain, throw an ender pearl at it.";
            default -> messageId;
        };
    }
}

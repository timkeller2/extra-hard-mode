package dev.extrahardmode.client;

import dev.extrahardmode.network.ClientboundSyncPayload;
import dev.extrahardmode.network.ClientboundToastPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
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
        if (sync == null) {
            return false;
        }
        if (player.hasInfiniteMaterials() || player.isCreative()) {
            return false;
        }
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (blockId == null || !sync.hardenedBlocks().contains(blockId)) {
            return false;
        }
        Identifier toolId = BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem());
        return toolId == null || !sync.hardenedPicks().contains(toolId);
    }

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ClientboundSyncPayload.TYPE, (payload, context) -> lastSync = payload);
        ClientPlayNetworking.registerGlobalReceiver(ClientboundToastPayload.TYPE, ExtraHardModeClient::onToast);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> lastSync = null);
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world instanceof ServerLevel) {
                return InteractionResult.PASS;
            }
            ClientboundSyncPayload sync = lastSync;
            if (sync == null) {
                return InteractionResult.PASS;
            }
            if (player.hasInfiniteMaterials() || player.isCreative()) {
                return InteractionResult.PASS;
            }
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof BlockItem blockItem)) {
                return InteractionResult.PASS;
            }
            Identifier placed = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
            if (placed == null || !sync.caveInOres().contains(placed)) {
                return InteractionResult.PASS;
            }
            BlockPos placedAt = hit.getBlockPos().relative(hit.getDirection());
            for (Direction direction : Direction.values()) {
                Identifier neighbor = BuiltInRegistries.BLOCK.getKey(world.getBlockState(placedAt.relative(direction)).getBlock());
                if (neighbor != null && sync.hardenedBlocks().contains(neighbor)) {
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.PASS;
        });
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
            default -> messageId;
        };
    }
}

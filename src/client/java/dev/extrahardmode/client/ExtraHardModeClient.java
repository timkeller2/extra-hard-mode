package dev.extrahardmode.client;

import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.feature.DebugScreenRules;
import dev.extrahardmode.feature.PlacementRules;
import dev.extrahardmode.tag.EhmTags;
import dev.extrahardmode.feature.SeasonAtmosphere;
import dev.extrahardmode.feature.Dragon;
import dev.extrahardmode.feature.DragonRules;
import dev.extrahardmode.feature.Torches;
import dev.extrahardmode.feature.monster.Horses;
import dev.extrahardmode.module.MessageId;
import dev.extrahardmode.network.ClientboundAbilityDurationsPayload;
import dev.extrahardmode.network.ClientboundFlightPayload;
import dev.extrahardmode.network.ClientboundManaPayload;
import dev.extrahardmode.network.ClientboundPowerMinePayload;
import dev.extrahardmode.network.ClientboundSoilLookPayload;
import dev.extrahardmode.network.ClientboundSyncPayload;
import dev.extrahardmode.network.ClientboundToastPayload;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ExtraHardModeClient implements ClientModInitializer {
    private static final Map<String, SystemToast.SystemToastId> TOAST_IDS = new ConcurrentHashMap<>();

    private static volatile ClientboundSyncPayload lastSync;
    private static volatile ClientboundManaPayload lastMana;
    private static volatile ClientboundFlightPayload lastFlight;
    private static volatile ClientboundPowerMinePayload lastPowerMine;
    private static volatile ClientboundAbilityDurationsPayload lastDurations;
    private static volatile ClientboundSoilLookPayload lastSoilLook;

    public static ClientboundSyncPayload lastSync() {
        return lastSync;
    }

    public static ClientboundManaPayload lastMana() {
        return lastMana;
    }

    public static ClientboundFlightPayload lastFlight() {
        return lastFlight;
    }

    public static ClientboundPowerMinePayload lastPowerMine() {
        return lastPowerMine;
    }

    public static ClientboundAbilityDurationsPayload lastDurations() {
        return lastDurations;
    }

    public static ClientboundSoilLookPayload lastSoilLook() {
        return lastSoilLook;
    }

    public static boolean denyOffhandLightSwap(Player player) {
        return offhandLightRestricted(player) && PlacementRules.denyOffhandLight(true, isOffhandLight(player.getMainHandItem()));
    }

    public static boolean denyOffhandPlace(ItemStack stack) {
        Minecraft client = Minecraft.getInstance();
        return offhandLightRestricted(client.player) && PlacementRules.denyOffhandLight(true, isOffhandLight(stack));
    }

    public static boolean isOffhandLight(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(EhmTags.NO_OFFHAND_LIGHT);
    }

    private static boolean offhandLightRestricted(Player player) {
        ClientboundSyncPayload sync = lastSync;
        if (sync == null || sync.playerBypass() || player == null || player.isSpectator()) {
            return false;
        }
        if (player.hasInfiniteMaterials()
                || (sync.extras().creativeBypasses() && player.isCreative())) {
            return false;
        }
        return true;
    }

    /** F3 overlay and F3+ combos. World config wins while connected; otherwise global config. */
    public static boolean f3Allowed() {
        ClientboundSyncPayload sync = lastSync;
        if (sync == null) {
            return ConfigManager.global().f3Enabled();
        }
        return DebugScreenRules.allowed(true, sync.playerBypass(), sync.extras().f3Enabled());
    }

    /**
     * Client prediction for torch Y / soft-surface deny. Payload-only; never reads TOML.
     * Toasts the matching tutorial id when denying.
     */
    public static boolean denyTorchPlacement(BlockPlaceContext context) {
        Torches.DenyReason reason = torchDenyReason(context);
        if (reason == Torches.DenyReason.NONE) {
            return false;
        }
        toast(reason == Torches.DenyReason.Y ? "no_torches_here" : "limited_torch_placement");
        return true;
    }

    public static Torches.DenyReason torchDenyReason(BlockPlaceContext context) {
        ClientboundSyncPayload sync = lastSync;
        if (sync == null || sync.playerBypass()) {
            return Torches.DenyReason.NONE;
        }
        if (!sync.torchYDeny() && !sync.torchSoftDeny()) {
            return Torches.DenyReason.NONE;
        }
        Player player = context.getPlayer();
        if (player != null
                && (player.hasInfiniteMaterials()
                        || (sync.extras().creativeBypasses() && player.isCreative()))) {
            return Torches.DenyReason.NONE;
        }
        ItemStack stack = context.getItemInHand();
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return Torches.DenyReason.NONE;
        }
        return Torches.denyReasonFromPayload(
                sync.torchYDeny(),
                sync.torchNoPlacementUnderY(),
                sync.torchSoftDeny(),
                sync.depthLimitedLights(),
                sync.softTorchSurfaces(),
                context,
                blockItem);
    }

    /**
     * Client prediction for flint-and-steel / fire-charge on a campfire below the torch Y
     * cutoff. Payload-only; never reads TOML.
     */
    public static boolean denyCampfireLightFromPayload(Level level, BlockPos pos, ItemStack stack) {
        ClientboundSyncPayload sync = lastSync;
        if (sync == null || !sync.torchYDeny() || sync.playerBypass()) {
            return false;
        }
        if (stack == null || (!stack.is(Items.FLINT_AND_STEEL) && !stack.is(Items.FIRE_CHARGE))) {
            return false;
        }
        if (!Torches.isCampfire(level.getBlockState(pos))) {
            return false;
        }
        return PlacementRules.denyTorchY(true, sync.torchNoPlacementUnderY(), pos.getY());
    }

    /**
     * Iris handheld lighting: zero torch/campfire held light at and below the torch cutoff.
     * Lanterns, glowstone, and other non-tagged lights still emit.
     */
    public static boolean suppressIrisHeldFlameLight(ItemStack stack) {
        ClientboundSyncPayload sync = lastSync;
        if (sync == null || !sync.torchYDeny() || sync.playerBypass()) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return false;
        }
        if (client.player.hasInfiniteMaterials()
                || (sync.extras().creativeBypasses() && client.player.isCreative())) {
            return false;
        }
        if (!PlacementRules.denyTorchY(true, sync.torchNoPlacementUnderY(), client.player.getBlockY())) {
            return false;
        }
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
        if (blockId != null && sync.depthLimitedLights().contains(blockId)) {
            return true;
        }
        return blockItem.getBlock() instanceof CampfireBlock;
    }

    /** Vanilla block-item luminance; used to keep lanterns when an off-hand torch is suppressed. */
    public static int vanillaHeldBlockLight(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
            return 0;
        }
        return blockItem.getBlock().defaultBlockState().getLightEmission();
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
        return denyEndBuilding(context.getPlayer(), context.getItemInHand());
    }

    public static boolean denyEndBuilding(Player player, ItemStack stack) {
        ClientboundSyncPayload sync = lastSync;
        if (sync == null || !sync.noEndBuilding() || sync.playerBypass()) {
            return false;
        }
        if (player != null && (player.hasInfiniteMaterials() || player.isCreative())) {
            return false;
        }
        boolean empty = stack == null || stack.isEmpty();
        return DragonRules.denyEndUse(
                true, true, false, empty, Dragon.allowPlaceItem(stack), !empty && Dragon.isPlacementItem(stack.getItem()));
    }

    public static void toast(String messageId) {
        toast(messageId, "");
    }

    public static void toast(String messageId, String arg) {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.execute(() -> showToast(client, messageId, arg));
        }
    }

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ClientboundSyncPayload.TYPE, (payload, context) -> {
            lastSync = payload;
            ClientboundSyncPayload.DisplayExtras.SeasonBits season = payload.extras().season();
            SeasonAtmosphere.syncClient(season.antiFarming(), season.lossRate());
        });
        ClientPlayNetworking.registerGlobalReceiver(ClientboundToastPayload.TYPE, ExtraHardModeClient::onToast);
        ClientPlayNetworking.registerGlobalReceiver(
                ClientboundManaPayload.TYPE, (payload, context) -> lastMana = payload);
        ClientPlayNetworking.registerGlobalReceiver(
                ClientboundFlightPayload.TYPE, (payload, context) -> lastFlight = payload);
        ClientPlayNetworking.registerGlobalReceiver(
                ClientboundPowerMinePayload.TYPE, (payload, context) -> lastPowerMine = payload);
        ClientPlayNetworking.registerGlobalReceiver(
                ClientboundAbilityDurationsPayload.TYPE, (payload, context) -> lastDurations = payload);
        ClientPlayNetworking.registerGlobalReceiver(
                ClientboundSoilLookPayload.TYPE, (payload, context) -> lastSoilLook = payload);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            lastSync = null;
            lastMana = null;
            lastFlight = null;
            lastPowerMine = null;
            lastDurations = null;
            lastSoilLook = null;
            SeasonAtmosphere.clearClient();
        });
        ManaHud.register();
        AbilityDurationHud.register();
        SoilLookHud.register();
        AbilityHelp.register();
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (!level.isClientSide() || lastSync == null) {
                return InteractionResult.PASS;
            }
            if (!(entity instanceof AbstractChestedHorse horse)) {
                return InteractionResult.PASS;
            }
            if (Horses.denyChestInteract(
                    horse, player, hand, lastSync.horseBlockChest(), lastSync.horseBlockChestBelowY())) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> denyEndUseClient(player, level, hand));
        UseItemCallback.EVENT.register(ExtraHardModeClient::denyEndUseClient);
    }

    private static InteractionResult denyEndUseClient(Player player, Level level, InteractionHand hand) {
        if (!level.isClientSide()) {
            return InteractionResult.PASS;
        }
        if (!denyEndBuilding(player, player.getItemInHand(hand))) {
            return InteractionResult.PASS;
        }
        toast("limited_end_building");
        return InteractionResult.FAIL;
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
        context.client().execute(() -> showToast(context.client(), payload.messageId(), payload.arg()));
    }

    private static void showToast(Minecraft client, String messageId, String arg) {
        Component title = Component.translatableWithFallback("extrahardmode.toast.title", "Extra Hard Mode");
        Component body = Component.translatableWithFallback(
                "extrahardmode.toast." + messageId, toastFallback(messageId, arg), arg == null ? "" : arg);
        SystemToast.addOrUpdate(client.gui.toastManager(), toastId(messageId), title, body);
    }

    private static SystemToast.SystemToastId toastId(String messageId) {
        return TOAST_IDS.computeIfAbsent(messageId, id -> new SystemToast.SystemToastId(5000L));
    }

    private static String toastFallback(String messageId, String arg) {
        MessageId known = MessageId.byId(messageId);
        String fallback = known != null ? known.fallback() : messageId;
        if (arg != null && !arg.isEmpty() && fallback.contains("%s")) {
            return fallback.formatted(arg);
        }
        return fallback;
    }
}

package dev.extrahardmode.module;

import dev.extrahardmode.command.EhmPermissions;
import net.fabricmc.fabric.api.permission.v1.PermissionNode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.feature.Tutorial;
import dev.extrahardmode.network.EhmNetworking;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.WorldGate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class MsgService {
    private MsgService() {}

    public static void actionBar(ServerPlayer player, PermissionNode<Boolean> silent, String key, String fallback) {
        if (player.checkPermission(silent, false)) {
            return;
        }
        player.sendOverlayMessage(Component.translatableWithFallback(key, fallback));
    }

    public static void lavaFizz(ServerLevel level, BlockPos pos) {
import net.minecraft.world.level.Level;
    private static final long ACTION_BAR_COOLDOWN_MS = 30_000L;
    private static final long TOAST_COOLDOWN_MS = 120_000L;
    private static final int COOLDOWN_CAP = 256;
    private static final Map<String, Long> COOLDOWNS = new ConcurrentHashMap<>();
    public static void send(ServerPlayer player, MessageId id) {
        if (player == null || id == null) {
        switch (id.kind()) {
            case ACTION_BAR -> deny(player, id);
            case TOAST, ONCE -> tutorial(player, id);
            case ANNOUNCE -> {
                broadcast(player.level().getServer(), id, player.getScoreboardName());
                tutorial(player, id);
            }
            case BROADCAST -> broadcast(player.level().getServer(), id, player.getScoreboardName());
    /** Instant deny: action bar, 30s cooldown, honors silent nodes. */
    public static void deny(ServerPlayer player, MessageId id) {
        if (player == null || id == null || silenced(player, id)) {
        if (!cooldownElapsed(player.getUUID(), id.id(), ACTION_BAR_COOLDOWN_MS)) {
        player.sendOverlayMessage(Component.translatableWithFallback(id.messageKey(), id.fallback()));
    /**
     * First-time mechanic toast. Max N (or once) per player, persisted on {@link EhmAttachments#EHM_TUTORIAL}.
     * Skipped when the tutorial module is off. {@code tutorial.maxShows = 0} disables ONCE as well as TOAST.
     */
    public static void tutorial(ServerPlayer player, MessageId id) {
        if (!(player.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, Tutorial.ID)) {
        if (silenced(player, id)) {
        int maxShows = maxShows(id);
        if (maxShows <= 0) {
        Map<String, Integer> counts =
                TutorialCounts.mutableCopy(player.getAttachedOrElse(EhmAttachments.EHM_TUTORIAL, Map.of()));
        if (TutorialCounts.shown(counts, id.id()) >= maxShows) {
        if (!cooldownElapsed(player.getUUID(), "toast:" + id.id(), TOAST_COOLDOWN_MS)) {
        if (!TutorialCounts.tryIncrement(counts, id.id(), maxShows)) {
        player.setAttached(EhmAttachments.EHM_TUTORIAL, counts);
        EhmNetworking.sendToast(player, id.id(), player.getScoreboardName());
    public static void broadcast(MinecraftServer server, MessageId id, String playerName) {
        if (server == null || id == null) {
        String text = id.fallback().contains("%s") && playerName != null
                ? id.fallback().formatted(playerName)
                : id.fallback();
        Component message = Component.translatableWithFallback(id.messageKey(), text, playerName == null ? "" : playerName);
        server.getPlayerList().broadcastSystemMessage(message, false);
        if (!ConfigManager.world(level).torchFizz()) {
        level.playSound(
                null,
                pos,
                SoundEvents.LAVA_EXTINGUISH,
                SoundSource.BLOCKS,
                0.5F,
                2.6F + level.getRandom().nextFloat() * 0.8F);
    }

    public static void noTorchesHere(ServerPlayer player, ServerLevel level, BlockPos pos, boolean fizz) {
        actionBar(
                player,
                EhmPermissions.SILENT_NO_TORCHES_HERE,
                "extrahardmode.message.no_torches_here",
                "There's not enough air flow down here for permanent flames. Use another method to light your way.");
    public static void creeperTntWarning(ServerLevel level, BlockPos pos) {
        if (!ConfigManager.world(level).creeperTntWarning()) {
            return;
        }
        level.playSound(null, pos, SoundEvents.GHAST_WARN, SoundSource.HOSTILE, 1.0F, 1.0F);
    }

        deny(player, MessageId.NO_TORCHES_HERE);
        if (fizz) {
            lavaFizz(level, pos);
        }
    }

    public static void limitedTorchPlacement(ServerPlayer player, ServerLevel level, BlockPos pos, boolean fizz) {
        actionBar(
                player,
                EhmPermissions.SILENT_LIMITED_TORCH_PLACEMENT,
                "extrahardmode.message.limited_torch_placement",
                "It's too soft there to fasten a torch.");
        deny(player, MessageId.LIMITED_TORCH_PLACEMENT);
        if (fizz) {
            lavaFizz(level, pos);
        }
    }

    public static void realisticBuilding(ServerPlayer player) {
        actionBar(
                player,
                EhmPermissions.SILENT_REALISTIC_BUILDING,
                "extrahardmode.message.realistic_building",
                "You can't build while in the air.");
        deny(player, MessageId.REALISTIC_BUILDING);
    }

    public static void realisticBuildingBeneath(ServerPlayer player) {
        deny(player, MessageId.REALISTIC_BUILDING_BENEATH);
    public static void stoneMiningHelp(ServerPlayer player) {
        deny(player, MessageId.STONE_MINING_HELP);
    public static void noPlacingOreAgainstStone(ServerPlayer player) {
        deny(player, MessageId.NO_PLACING_ORE_AGAINST_STONE);
    public static boolean tutorialEnabled(Level level) {
        return level instanceof ServerLevel serverLevel && WorldGate.isModuleActive(serverLevel, Tutorial.ID);
    private static boolean silenced(ServerPlayer player, MessageId id) {
        PermissionNode<Boolean> silent = EhmPermissions.silentNode(id);
        if (silent == null) {
            return false;
        }
        if (player.level() instanceof ServerLevel level && !ConfigManager.world(level).checkPermission()) {
        return player.checkPermission(silent, false);
    static int maxShows(MessageId id) {
        int configured = ConfigManager.global().tutorialMaxShows();
        return switch (id.kind()) {
            case ONCE, ANNOUNCE -> TutorialCounts.effectiveMax(configured, true);
            case TOAST -> TutorialCounts.effectiveMax(configured, false);
            case ACTION_BAR, BROADCAST -> 0;
        };
    private static boolean cooldownElapsed(UUID playerId, String id, long cooldownMs) {
        long now = System.currentTimeMillis();
        if (COOLDOWNS.size() > COOLDOWN_CAP) {
            long cutoff = now - Math.max(ACTION_BAR_COOLDOWN_MS, TOAST_COOLDOWN_MS);
            COOLDOWNS.entrySet().removeIf(entry -> now - entry.getValue() > cutoff);
        String key = playerId + ":" + id;
        Long last = COOLDOWNS.get(key);
        if (last != null && now - last < cooldownMs) {
        COOLDOWNS.put(key, now);
        return true;
    }
}

package dev.extrahardmode.feature;

import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.WorldGate;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Applies the shared life pool, the heart loss below 0 lives, and the win or lose lines. */
public final class Lives {
    private static final int TITLE_FADE_IN = 10;
    private static final int TITLE_STAY = 100;
    private static final int TITLE_FADE_OUT = 20;

    private static final Map<UUID, Integer> PENDING_CREDITS = new ConcurrentHashMap<>();

    private Lives() {}

    public static void clearPending() {
        PENDING_CREDITS.clear();
    }

    /** Grant a missing pool, and keep base health on the life count. */
    public static void maintain(ServerPlayer player) {
        if (!active(player)) {
            return;
        }
        if (EhmApi.playerBypasses(player)) {
            restorePenalty(player);
            return;
        }
        int configured = configured(player);
        if (!LivesRules.enabled(configured)) {
            restorePenalty(player);
            return;
        }
        int lives = currentLives(player, configured);
        applyBaseHealth(player, LivesRules.baseHealth(configured, lives));
    }

    /** True when the players module is on and the overworld life pool is on. */
    public static boolean systemOn(ServerPlayer player) {
        return player != null && active(player) && LivesRules.enabled(configured(player));
    }

    /** Overworld {@code player.death.lives}. */
    public static int configuredLives(ServerPlayer player) {
        return player == null ? LivesRules.DEFAULT_LIVES : configured(player);
    }

    /** Overworld Restore Life Available? setting. It applies only while lives are on. */
    public static boolean restoreAvailable(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        return ConfigManager.world(player.level().getServer().overworld()).player().restoreLifeAvailable();
    }

    /**
     * Lives left, without writing the attachment. Absent means the player still has
     * the starting count, which is the server setting while the pool is on.
     */
    public static int livesOf(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        Integer stored = player.getAttached(EhmAttachments.EHM_LIVES);
        if (stored != null) {
            return stored;
        }
        int configured = configured(player);
        if (!LivesRules.enabled(configured)) {
            return 0;
        }
        return LivesRules.starting(configured);
    }

    /** Gives one life and refreshes base health. A full pool, or a pool that is off, changes nothing. */
    public static void grantLife(ServerPlayer player) {
        if (player == null || !active(player)) {
            return;
        }
        int configured = configured(player);
        int lives = livesOf(player);
        if (!LivesRules.canRestore(lives, configured)) {
            return;
        }
        int next = lives + 1;
        player.setAttached(EhmAttachments.EHM_LIVES, next);
        if (!EhmApi.playerBypasses(player)) {
            applyBaseHealth(player, LivesRules.baseHealth(configured, next));
        }
    }

    public static void onDeath(ServerPlayer player) {
        if (!active(player) || EhmApi.playerBypasses(player)) {
            return;
        }
        int configured = configured(player);
        if (!LivesRules.enabled(configured)) {
            return;
        }
        int before = currentLives(player, configured);
        int after = LivesRules.spend(before);
        if (after != before) {
            player.setAttached(EhmAttachments.EHM_LIVES, after);
        }
        applyBaseHealth(player, LivesRules.baseHealth(configured, after));

        int online = 0;
        int withLives = 0;
        for (ServerPlayer other : player.level().getServer().getPlayerList().getPlayers()) {
            if (EhmApi.playerBypasses(other)) {
                continue;
            }
            online++;
            int lives = other.getUUID().equals(player.getUUID()) ? after : currentLives(other, configured);
            if (lives > 0) {
                withLives++;
            }
        }
        switch (LivesRules.verdict(online, withLives, LivesRules.spentLast(before))) {
            case SOLO_LOSS -> {
                announce(player, "tougher.message.lives_lose", LivesRules.LOSE_SOLO);
                scheduleCredits(player);
            }
            case LAST_SURVIVOR -> {
                for (ServerPlayer other : player.level().getServer().getPlayerList().getPlayers()) {
                    if (EhmApi.playerBypasses(other)) {
                        continue;
                    }
                    int lives = other.getUUID().equals(player.getUUID()) ? after : currentLives(other, configured);
                    if (lives > 0) {
                        announce(other, "tougher.message.lives_win", LivesRules.WIN);
                    } else {
                        announce(other, "tougher.message.lives_lose_many", LivesRules.LOSE_MANY);
                    }
                }
            }
            case NONE -> {}
        }
    }

    public static void flushCredits(MinecraftServer server) {
        if (PENDING_CREDITS.isEmpty()) {
            return;
        }
        int tick = server.getTickCount();
        Iterator<Map.Entry<UUID, Integer>> iterator = PENDING_CREDITS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> pending = iterator.next();
            if (tick < pending.getValue()) {
                continue;
            }
            iterator.remove();
            ServerPlayer player = server.getPlayerList().getPlayer(pending.getKey());
            if (player == null || player.connection == null) {
                continue;
            }
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.WIN_GAME, 0.0F));
        }
    }

    private static void scheduleCredits(ServerPlayer player) {
        int ready = player.level().getServer().getTickCount() + LivesRules.CREDITS_DELAY_TICKS;
        PENDING_CREDITS.put(player.getUUID(), ready);
    }

    private static void announce(ServerPlayer player, String key, String fallback) {
        Component text = Component.translatableWithFallback(key, fallback);
        player.sendSystemMessage(text);
        if (player.connection == null) {
            return;
        }
        player.connection.send(new ClientboundSetTitlesAnimationPacket(TITLE_FADE_IN, TITLE_STAY, TITLE_FADE_OUT));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.empty()));
        player.connection.send(new ClientboundSetTitleTextPacket(text));
    }

    private static int currentLives(ServerPlayer player, int configured) {
        Integer stored = player.getAttached(EhmAttachments.EHM_LIVES);
        if (stored == null) {
            int starting = LivesRules.starting(configured);
            player.setAttached(EhmAttachments.EHM_LIVES, starting);
            return starting;
        }
        return stored;
    }

    /** The overworld file holds the one number for the whole server. */
    private static int configured(ServerPlayer player) {
        return ConfigManager.world(player.level().getServer().overworld()).player().lives();
    }

    private static boolean active(ServerPlayer player) {
        return player.level() instanceof ServerLevel level && WorldGate.isModuleActive(level, Players.ID);
    }

    private static void applyBaseHealth(ServerPlayer player, double desired) {
        AttributeInstance attribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null || attribute.getBaseValue() == desired) {
            return;
        }
        attribute.setBaseValue(desired);
        float max = player.getMaxHealth();
        if (player.getHealth() > max) {
            player.setHealth(max);
        }
    }

    /** Turning the rule off, or bypassing it, raises a lowered base back to 20. A higher base stays. */
    private static void restorePenalty(ServerPlayer player) {
        AttributeInstance attribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null || attribute.getBaseValue() >= LivesRules.VANILLA_BASE_HEALTH) {
            return;
        }
        applyBaseHealth(player, LivesRules.VANILLA_BASE_HEALTH);
    }
}

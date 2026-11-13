package dev.extrahardmode.feature;

import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Applies the shared life pool, the heart loss below 0 lives, and the death count. */
public final class Lives {
    private Lives() {}

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
        recoverFromLevel(player);
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
        tellDeath(player, after);
        recoverFromLevel(player);
    }

    /** The count after this death, before experience buys a life back. */
    private static void tellDeath(ServerPlayer player, int lives) {
        if (lives < 0) {
            int down = -lives;
            if (down == 1) {
                player.sendSystemMessage(Component.translatableWithFallback(
                        "tougher.message.heart_down", LivesRules.HEART_DOWN));
                return;
            }
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.hearts_down",
                    LivesRules.HEARTS_DOWN,
                    Component.literal(Integer.toString(down))));
            return;
        }
        if (lives == 1) {
            player.sendSystemMessage(
                    Component.translatableWithFallback("tougher.message.life_remaining", LivesRules.LIFE_REMAINING));
            return;
        }
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.lives_remaining",
                LivesRules.LIVES_REMAINING,
                Component.literal(Integer.toString(lives))));
    }

    /**
     * While experience level is above 32, each missing life is bought back.
     * The price is 250 plus 250 for every life still below full, and at most the
     * experience the player has. One purchase can spend the whole pool.
     */
    public static void recoverFromLevel(ServerPlayer player) {
        if (player == null || player.isSpectator() || !systemOn(player) || EhmApi.playerBypasses(player)) {
            return;
        }
        int configured = configured(player);
        int gained = 0;
        int spent = 0;
        for (int guard = 0; guard < 64; guard++) {
            if (!LivesRules.aboveRecoveryLevel(player.experienceLevel)) {
                break;
            }
            int lives = livesOf(player);
            int total = TomeRules.totalXp(player.experienceLevel, player.experienceProgress);
            int cost = LivesRules.recoveryCost(lives, configured, total);
            if (cost <= 0) {
                break;
            }
            player.giveExperiencePoints(-cost);
            int left = TomeRules.totalXp(player.experienceLevel, player.experienceProgress);
            if (left >= total) {
                break;
            }
            grantLife(player);
            if (livesOf(player) <= lives) {
                player.giveExperiencePoints(total - left);
                break;
            }
            gained++;
            spent += cost;
        }
        if (gained <= 0) {
            return;
        }
        if (gained == 1) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.life_from_level",
                    "You recover a life for %s experience.",
                    Component.literal(Integer.toString(spent))));
            return;
        }
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.lives_from_level",
                "You recover %s lives for %s experience.",
                Component.literal(Integer.toString(gained)),
                Component.literal(Integer.toString(spent))));
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

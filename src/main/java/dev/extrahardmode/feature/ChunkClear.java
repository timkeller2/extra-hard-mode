package dev.extrahardmode.feature;

import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.ChunkClearData;
import dev.extrahardmode.world.WorldGate;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Prediction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;

/**
 * Counts hostile kills by players in a chunk. A full chunk refuses spawns at
 * and above Y=48, tells nearby players, and marks that chunk once a day.
 */
public final class ChunkClear {
    private ChunkClear() {}

    public static void onDeath(LivingEntity entity, DamageSource source) {
        if (!(entity.level() instanceof ServerLevel level) || !WorldGate.isActive(level)) {
            return;
        }
        if (!(entity instanceof Enemy) || entity instanceof Player) {
            return;
        }
        if (!(source.getEntity() instanceof ServerPlayer killer) || !killer.isAlive() || killer.isSpectator()) {
            return;
        }
        int threshold = ConfigManager.world(level).chunkClearKills();
        if (!ChunkClearRules.enabled(threshold)) {
            return;
        }
        int before = ChunkClearData.of(level).addKill(entity.blockPosition());
        if (ChunkClearRules.justFilled(before, threshold)) {
            rewardNearby(level, entity);
        }
    }

    public static boolean blocksSpawn(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null || pos.getY() < ChunkClearRules.MIN_Y || !WorldGate.isActive(level)) {
            return false;
        }
        int threshold = ConfigManager.world(level).chunkClearKills();
        if (!ChunkClearRules.enabled(threshold)) {
            return false;
        }
        return ChunkClearRules.full(ChunkClearData.of(level).kills(pos), threshold);
    }

    public static void tickLevel(ServerLevel level) {
        if (level == null || level.getGameTime() % 20L != 0L || !WorldGate.isActive(level)) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            tickPeace(player);
        }
    }

    static void tickPeace(ServerPlayer player) {
        if (player == null || !player.isAlive() || player.isSpectator()) {
            return;
        }
        if (player.getBlockY() < ChunkClearRules.MIN_Y) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level) || !WorldGate.isActive(level)) {
            return;
        }
        long today = CropGrowthRules.dayIndex(level.getOverworldClockTime());
        String key = peaceKey(level, player.blockPosition());
        Map<String, Long> heard = player.getAttachedOrElse(EhmAttachments.EHM_CHUNK_PEACE, Map.of());
        if (!ChunkClearRules.peaceDue(key, today, heard)) {
            return;
        }
        int threshold = ConfigManager.world(level).chunkClearKills();
        if (!ChunkClearRules.blocksSpawn(player.getBlockY(), ChunkClearData.of(level).kills(player.blockPosition()), threshold)) {
            return;
        }
        playPeace(player);
        remember(player, key, today);
    }

    private static void rewardNearby(ServerLevel level, LivingEntity slain) {
        long today = CropGrowthRules.dayIndex(level.getOverworldClockTime());
        long chunk = ChunkPos.pack(slain.blockPosition());
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }
            boolean same = ChunkPos.pack(player.blockPosition()) == chunk;
            if (!ChunkClearRules.hearsClearing(same, player.distanceToSqr(slain))) {
                continue;
            }
            player.giveExperiencePoints(ChunkClearRules.REWARD_XP);
            player.getInventory()
                    .placeItemBackInInventory(
                            new ItemStack(Items.EMERALD, ChunkClearRules.REWARD_EMERALDS), Prediction.SERVER_ONLY);
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.chunk_clear", ChunkClearRules.CLEARED_MESSAGE));
            if (player.getBlockY() >= ChunkClearRules.MIN_Y && same) {
                remember(player, peaceKey(level, slain.blockPosition()), today);
            }
        }
    }

    /** Dimension plus packed chunk, so the same column in another world is its own cue. */
    private static String peaceKey(ServerLevel level, BlockPos pos) {
        return level.dimension().identifier() + "/" + ChunkPos.pack(pos);
    }

    private static void remember(ServerPlayer player, String key, long today) {
        Map<String, Long> heard = player.getAttachedOrElse(EhmAttachments.EHM_CHUNK_PEACE, Map.of());
        Map<String, Long> next = new HashMap<>();
        if (heard != null) {
            for (Map.Entry<String, Long> entry : heard.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null || entry.getValue().longValue() != today) {
                    continue;
                }
                next.put(entry.getKey(), today);
            }
        }
        next.put(key, today);
        player.setAttached(EhmAttachments.EHM_CHUNK_PEACE, next);
    }

    private static void playPeace(ServerPlayer player) {
        player.sendOverlayMessage(Component.translatableWithFallback(
                "tougher.message.chunk_peace", ChunkClearRules.PEACE_MESSAGE));
        player.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.2F, 1.4F);
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        level.sendParticles(
                player,
                ParticleTypes.HAPPY_VILLAGER,
                false,
                false,
                player.getX(),
                player.getY() + player.getBbHeight() * 0.5,
                player.getZ(),
                4,
                0.35,
                0.2,
                0.35,
                0.0);
    }
}

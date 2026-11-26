package dev.extrahardmode.feature;

import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.network.ClientboundChunkClearShieldPayload;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.ChunkClearData;
import dev.extrahardmode.world.WorldGate;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/**
 * Counts hostile kills by players in a chunk. A full chunk refuses hostile
 * spawns at and above Y=48. Neutral and friendly mobs still spawn. Reaching
 * the count again clears one uncleared chunk around it, up to nine in all. A synced flag tells
 * the client to show the corner diamond.
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
            celebrate(level, entity.chunkPosition(), entity, true);
        } else if (ChunkClearRules.spreads(before, threshold)) {
            ChunkPos cleared = clearAdjacent(level, entity.blockPosition(), threshold);
            if (cleared != null) {
                celebrate(level, cleared, killer, false);
            }
        }
    }

    /** True when this chunk has reached the kill count, at any height. */
    public static boolean chunkFull(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null || !WorldGate.isActive(level)) {
            return false;
        }
        int threshold = ConfigManager.world(level).chunkClearKills();
        return ChunkClearRules.full(ChunkClearData.of(level).kills(pos), threshold);
    }

    /**
     * True when this block is in a full chunk at Y=48 or above.
     */
    public static boolean peaceful(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null || pos.getY() < ChunkClearRules.MIN_Y || !WorldGate.isActive(level)) {
            return false;
        }
        return ChunkClearRules.full(ChunkClearData.of(level).kills(pos), ConfigManager.world(level).chunkClearKills());
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

    /** True when a full chunk should refuse this mob. Neutral and friendly mobs pass. */
    public static boolean blocksHostile(ServerLevel level, BlockPos pos, EntityType<?> type) {
        return blocksSpawn(level, pos) && hostile(type);
    }

    /**
     * Hostile mobs are the ones peaceful difficulty removes, except neutrals such as
     * endermen and zombified piglins. Friendly mobs are allowed in peaceful already.
     */
    public static boolean hostile(EntityType<?> type) {
        if (type == null || type.isAllowedInPeaceful()) {
            return false;
        }
        Class<?> base = type.getBaseClass();
        return base == null || !NeutralMob.class.isAssignableFrom(base);
    }

    public static void tickLevel(ServerLevel level) {
        if (level == null || level.getGameTime() % 20L != 0L) {
            return;
        }
        boolean active = WorldGate.isActive(level);
        for (ServerPlayer player : level.players()) {
            tickHeld(player, active);
        }
    }

    static void tickHeld(ServerPlayer player, boolean active) {
        if (player == null) {
            return;
        }
        boolean held = false;
        if (active && player.isAlive() && !player.isSpectator() && player.level() instanceof ServerLevel level) {
            held = chunkFull(level, player.blockPosition());
        }
        if (player.getAttachedOrElse(EhmAttachments.EHM_CHUNK_HELD, Boolean.FALSE) != held) {
            player.setAttached(EhmAttachments.EHM_CHUNK_HELD, held);
        }
    }

    /** The neighbor that just became clear, or null when all eight are already full. */
    private static ChunkPos clearAdjacent(ServerLevel level, BlockPos pos, int threshold) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        ChunkClearData data = ChunkClearData.of(level);
        boolean[] neighborFull = new boolean[ChunkClearRules.neighborCount()];
        for (int i = 0; i < neighborFull.length; i++) {
            neighborFull[i] = ChunkClearRules.full(
                    data.kills(ChunkClearRules.neighborChunkX(chunkX, i), ChunkClearRules.neighborChunkZ(chunkZ, i)),
                    threshold);
        }
        int index = ChunkClearRules.firstUncleared(neighborFull);
        if (index < 0) {
            return null;
        }
        int clearedX = ChunkClearRules.neighborChunkX(chunkX, index);
        int clearedZ = ChunkClearRules.neighborChunkZ(chunkZ, index);
        data.raiseTo(clearedX, clearedZ, threshold);
        return new ChunkPos(clearedX, clearedZ);
    }

    /**
     * The chunk that just filled, or the neighbor a later threshold just filled.
     * {@code killRange} measures the reward from the slain mob. A spread measures it from the killer.
     */
    private static void celebrate(ServerLevel level, ChunkPos cleared, Entity focus, boolean killRange) {
        BlockPos origin = cleared.getMiddleBlockPosition(focus.getBlockY() + 1);
        Vec3 at = Vec3.atCenterOf(origin);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 4.0F, 1.15F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.0F);
        ClientboundChunkClearShieldPayload payload = new ClientboundChunkClearShieldPayload(origin);
        double see = (double) ChunkClearRules.SEE_BLOCKS * ChunkClearRules.SEE_BLOCKS;
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }
            boolean reward = killRange
                    ? ChunkClearRules.hearsClearing(
                            player.chunkPosition().equals(focus.chunkPosition()), player.distanceToSqr(focus))
                    : ChunkClearRules.hearsKiller(player.distanceToSqr(focus));
            boolean witness = ChunkClearRules.witnessesClear(
                    player.chunkPosition().x(), player.chunkPosition().z(), cleared.x(), cleared.z());
            if (reward) {
                grant(player);
            }
            if (reward || witness) {
                player.sendSystemMessage(Component.translatableWithFallback(
                        "tougher.message.chunk_clear", ChunkClearRules.CLEARED_MESSAGE));
            }
            if (witness || player.distanceToSqr(at) <= see) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    private static void grant(ServerPlayer player) {
        player.giveExperiencePoints(ChunkClearRules.REWARD_XP);
        player.getInventory()
                .placeItemBackInInventory(
                        new ItemStack(Items.EMERALD, ChunkClearRules.REWARD_EMERALDS), Prediction.SERVER_ONLY);
    }
}

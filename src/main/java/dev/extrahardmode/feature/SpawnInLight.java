package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.EhmTags;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.FluidState;

/**
 * Extra cave spawns on previously visited sections below Y=48, allowing light up
 * to 10. Deep Dark / trial chambers / ancient cities are skipped.
 */
public final class SpawnInLight implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("spawn_in_light");
    public static final SpawnInLight INSTANCE = new SpawnInLight();

    public static final int VISITED_CAP = 4096;
    public static final int ATTEMPT_INTERVAL_TICKS = 200;
    private static final int MIN_PLAYER_DISTANCE_SQ = 24 * 24;

    private SpawnInLight() {}

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void serverTick(ServerLevel level) {
        WorldConfig config = ConfigManager.world(level);
        if (!config.spawnInLightEnable()) {
            return;
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL
                || !level.getGameRules().get(GameRules.SPAWN_MOBS)
                || !level.getGameRules().get(GameRules.SPAWN_MONSTERS)) {
            return;
        }
        boolean attempt = level.getGameTime() % ATTEMPT_INTERVAL_TICKS == 0;
        for (ServerPlayer player : level.players()) {
            recordVisit(player, config);
            if (attempt) {
                trySpawn(level, player, config);
            }
        }
    }

    static void recordVisit(ServerPlayer player, WorldConfig config) {
        if (player.getBlockY() >= config.spawnInLightMaxY()) {
            return;
        }
        long key = SectionPos.asLong(player.blockPosition());
        LongLinkedOpenHashSet visited = player.getAttachedOrCreate(EhmAttachments.EHM_VISITED_SECTIONS);
        if (visited.add(key)) {
            while (visited.size() > VISITED_CAP) {
                visited.removeFirstLong();
            }
            player.setAttached(EhmAttachments.EHM_VISITED_SECTIONS, visited);
        }
    }

    static void trySpawn(ServerLevel level, ServerPlayer player, WorldConfig config) {
        if (config.spawnInLightPercent() <= 0) {
            return;
        }
        LongLinkedOpenHashSet visited = player.getAttachedOrElse(EhmAttachments.EHM_VISITED_SECTIONS, null);
        if (visited == null || visited.isEmpty()) {
            return;
        }
        RandomSource random = level.getRandom();
        if (random.nextInt(100) >= config.spawnInLightPercent()) {
            return;
        }
        long packed = pick(visited, random);
        SectionPos section = SectionPos.of(packed);
        BlockPos pos = new BlockPos(
                section.minBlockX() + random.nextInt(16),
                section.minBlockY() + random.nextInt(16),
                section.minBlockZ() + random.nextInt(16));
        if (pos.getY() >= config.spawnInLightMaxY()) {
            return;
        }
        if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
            return;
        }
        if (EhmTags.noSpawnInLight(level, pos)) {
            return;
        }
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) < MIN_PLAYER_DISTANCE_SQ) {
            return;
        }
        int light = Math.max(level.getBrightness(LightLayer.BLOCK, pos), level.getBrightness(LightLayer.SKY, pos));
        if (light > config.spawnInLightMaxLight()) {
            return;
        }
        EntityType<? extends Monster> type = rollType(random);
        BlockState state = level.getBlockState(pos);
        FluidState fluid = level.getFluidState(pos);
        if (!NaturalSpawner.isValidEmptySpawnBlock(level, pos, state, fluid, type)) {
            return;
        }
        BlockPos floor = pos.below();
        if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
            return;
        }
        if (!Monster.checkAnyLightMonsterSpawnRules(type, level, EntitySpawnReason.NATURAL, pos, random)) {
            return;
        }
        Mob spawned = type.spawn(level, pos, EntitySpawnReason.NATURAL);
        if (spawned != null) {
            spawned.setAttached(EhmAttachments.EHM_SPAWN_PROCESSED, true);
        }
    }

    static EntityType<? extends Monster> rollType(RandomSource random) {
        int roll = random.nextInt(90);
        if (roll < 5) {
            return EntityTypes.SILVERFISH;
        }
        if (roll < 25) {
            return EntityTypes.SKELETON;
        }
        if (roll < 45) {
            return EntityTypes.ZOMBIE;
        }
        if (roll < 65) {
            return EntityTypes.CREEPER;
        }
        return EntityTypes.SPIDER;
    }

    private static long pick(LongLinkedOpenHashSet visited, RandomSource random) {
        int index = random.nextInt(visited.size());
        int i = 0;
        for (long value : visited) {
            if (i++ == index) {
                return value;
            }
        }
        return visited.firstLong();
    }
}

package dev.extrahardmode.feature.monster;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.MonsterConfig;
import dev.extrahardmode.feature.FeatureBus;
import dev.extrahardmode.feature.FeatureModule;
import dev.extrahardmode.module.EntityHelper;
import dev.extrahardmode.module.SpawnReplaceService;
import dev.extrahardmode.world.SpiderWebData;
import dev.extrahardmode.world.WorldGate;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Bonus underground spiders and cobwebs on death. Cave webs persist; Y ≥ 48
 * are cleaned every 100 ticks.
 */
public final class Spiders implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("spiders");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        SpawnReplaceService.register(EntityTypes.ZOMBIE, Spiders::rollBonusSpider);
        bus.listen(ServerLivingEntityEvents.AFTER_DEATH, ID, Spiders::onDeath);
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (level.getGameTime() % MonsterConfig.WEB_CLEANUP_INTERVAL != 0) {
            return;
        }
        cleanupSurfaceWebs(level);
    }

    public static boolean enabled(Level level) {
        return FeatureBus.guard(level, ID);
    }

    public static net.minecraft.world.entity.EntityType<?> rollBonusSpider(
            Mob original, ServerLevel level) {
        if (!WorldGate.isModuleActive(level, ID)) {
            return null;
        }
        if (level.dimension() != Level.OVERWORLD) {
            return null;
        }
        if (original.getY() >= MonsterConfig.SEA_Y) {
            return null;
        }
        int percent = ConfigManager.world(level).monsters().spiderBonusPercent();
        if (!EntityHelper.percent(level.getRandom(), percent)) {
            return null;
        }
        return EntityTypes.SPIDER;
    }

    public static boolean tryBreakWeb(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level) || !enabled(level)) {
            return false;
        }
        if (!(mob instanceof Enemy)) {
            return false;
        }
        BlockPos pos = mob.blockPosition();
        if (!level.getBlockState(pos).is(Blocks.COBWEB)) {
            pos = BlockPos.containing(mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ());
            if (!level.getBlockState(pos).is(Blocks.COBWEB)) {
                return false;
            }
        }
        if (!EntityHelper.percent(level.getRandom(), MonsterConfig.WEB_BREAK_CHANCE)) {
            return false;
        }
        level.destroyBlock(pos, false);
        SpiderWebData.of(level).remove(pos);
        return true;
    }

    private static void onDeath(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof Spider) || !(entity.level() instanceof ServerLevel level) || !enabled(level)) {
            return;
        }
        MonsterConfig config = ConfigManager.world(level).monsters();
        if (!config.spidersDropWeb()) {
            return;
        }
        if (EntityHelper.lootless(entity) && level.getRandom().nextInt(3) != 1) {
            return;
        }
        placeWebs(level, entity);
    }

    static void placeWebs(ServerLevel level, LivingEntity entity) {
        long time = level.getOverworldClockTime();
        BlockPos feet = entity.blockPosition();
        int random1 = Math.floorMod((int) time + feet.getZ(), 9);
        int random2 = Math.floorMod((int) time + feet.getX(), 9);
        BlockPos[] locations = {
            feet.offset(random1, 0, random2),
            feet.offset(-random2, 0, random1 / 2),
            feet.offset(-random1 / 2, 0, -random2),
            feet.offset(random1 / 2, 0, -random2 / 2)
        };
        SpiderWebData data = SpiderWebData.of(level);
        for (BlockPos start : locations) {
            BlockPos web = findWebPos(level, start);
            if (web == null) {
                continue;
            }
            level.setBlock(web, Blocks.COBWEB.defaultBlockState(), 3);
            data.add(web);
        }
        BlockPos atFeet = findWebPos(level, feet);
        if (atFeet != null && level.getBlockState(atFeet).isAir()) {
            level.setBlock(atFeet, Blocks.COBWEB.defaultBlockState(), 3);
            data.add(atFeet);
        }
    }

    private static BlockPos findWebPos(ServerLevel level, BlockPos start) {
        BlockPos pos = start;
        if (!level.getBlockState(pos).isAir()) {
            return null;
        }
        for (int i = 0; i < 5 && pos.getY() > level.getMinY(); i++) {
            BlockPos below = pos.below();
            if (!level.getBlockState(below).isAir()) {
                break;
            }
            pos = below;
        }
        if (level.getBlockState(pos.below()).isAir()) {
            return null;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(direction)).is(Blocks.CACTUS)) {
                return null;
            }
        }
        return pos.immutable();
    }

    static void cleanupSurfaceWebs(ServerLevel level) {
        SpiderWebData data = SpiderWebData.of(level);
        for (BlockPos pos : data.snapshot()) {
            if (pos.getY() < MonsterConfig.CAVE_Y) {
                continue;
            }
            if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.COBWEB)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
            data.remove(pos);
        }
    }
}

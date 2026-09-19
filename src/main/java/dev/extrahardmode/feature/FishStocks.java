package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.tag.EhmTags;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.AABB;

/** Natural fish spawn slowly, one at a time; depleted chunks are slower still. Fishing waits longer. */
public final class FishStocks implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("fish_stocks");

    @Override
    public Identifier id() {
        return ID;
    }

    public static boolean isStockedFish(EntityType<?> type) {
        return type != null && type.builtInRegistryHolder().is(EhmTags.STOCKED_FISH);
    }

    public static boolean isStockedFish(Entity entity) {
        return entity != null && isStockedFish(entity.getType());
    }

    public static int packCount(int vanilla, EntityType<?> type, ServerLevel level) {
        if (!WorldGate.isModuleActive(level, ID) || !isStockedFish(type)) {
            return vanilla;
        }
        return FishStocksRules.PACK_SIZE;
    }

    public static boolean allowNaturalSpawn(ServerLevel level, Entity entity) {
        if (!WorldGate.isModuleActive(level, ID) || !isStockedFish(entity)) {
            return true;
        }
        WorldConfig config = ConfigManager.world(level);
        int count = countSameTypeInChunk(level, entity);
        return FishStocksRules.allowSpawn(
                count,
                config.fishHealthyCount(),
                config.fishHealthyRatePercent(),
                config.fishScarceRatePercent(),
                level.getRandom().nextInt(100));
    }

    public static int scaleBiteWait(ServerLevel level, int ticks) {
        if (!WorldGate.isModuleActive(level, ID)) {
            return ticks;
        }
        return FishStocksRules.scaleWait(ticks, ConfigManager.world(level).fishingWaitMultiplier());
    }

    static int countSameTypeInChunk(ServerLevel level, Entity entity) {
        ChunkAccess chunk = level.getChunk(entity.blockPosition());
        var pos = chunk.getPos();
        AABB box = new AABB(
                pos.getMinBlockX(),
                level.getMinY(),
                pos.getMinBlockZ(),
                pos.getMaxBlockX() + 1,
                level.getMaxY() + 1,
                pos.getMaxBlockZ() + 1);
        EntityType<?> type = entity.getType();
        return level.getEntities(entity, box, other -> other.isAlive() && other.getType() == type).size();
    }
}

package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.world.EhmTags;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;

/** Doubles natural pack size below the cave band. Does not raise the vanilla cap. */
public final class MoreMonsters implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("more_monsters");
    public static final MoreMonsters INSTANCE = new MoreMonsters();

    private MoreMonsters() {}

    @Override
    public Identifier id() {
        return ID;
    }

    public static int scalePackCount(int count, int multiplier) {
        if (multiplier <= 1 || count <= 0) {
            return count;
        }
        long scaled = (long) count * (long) multiplier;
        return scaled > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) scaled;
    }

    public static int maybeScale(int count, MobCategory category, ServerLevel level, BlockPos pos) {
        if (category != MobCategory.MONSTER) {
            return count;
        }
        if (!WorldGate.isModuleActive(level, ID)) {
            return count;
        }
        WorldConfig config = ConfigManager.world(level);
        if (!config.moreMonstersEnable() || pos.getY() >= config.moreMonstersMaxY()) {
            return count;
        }
        if (EhmTags.noExtraPacks(level, pos)) {
            return count;
        }
        return scalePackCount(count, config.moreMonstersMultiplier());
    }
}

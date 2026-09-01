package dev.extrahardmode.feature.monster;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.feature.FeatureBus;
import dev.extrahardmode.feature.FeatureModule;
import dev.extrahardmode.feature.FeatureRegistry;
import dev.extrahardmode.module.SpawnReplaceService;
import dev.extrahardmode.world.WorldGate;
import java.util.function.ToIntFunction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.level.biome.Biome;

/**
 * Bonus biome spawn replacements. Rolls go through {@link SpawnReplaceService} only;
 * Deep Dark / structure skip and WorldGate {@code isActive} live there.
 */
public final class BiomeReplacements {
    public static final Identifier KILLER_BUNNY = ExtraHardModeMod.id("killer_bunny");
    public static final Identifier VINDICATOR = ExtraHardModeMod.id("vindicator");
    public static final Identifier CAVE_SPIDER = ExtraHardModeMod.id("cave_spider");
    public static final Identifier GUARDIANS = ExtraHardModeMod.id("guardians");
    public static final Identifier VEX = ExtraHardModeMod.id("vex");

    public static final TagKey<Biome> VINDICATOR_REPLACE =
            TagKey.create(Registries.BIOME, ExtraHardModeMod.id("vindicator_replace"));
    public static final TagKey<Biome> CAVE_SPIDER_REPLACE =
            TagKey.create(Registries.BIOME, ExtraHardModeMod.id("cave_spider_replace"));
    public static final TagKey<Biome> GUARDIAN_REPLACE =
            TagKey.create(Registries.BIOME, ExtraHardModeMod.id("guardian_replace"));

    private BiomeReplacements() {}

    public static void register(FeatureRegistry features) {
        features.register(module(KILLER_BUNNY, () -> SpawnReplaceService.register(
                EntityTypes.RABBIT, BiomeReplacements::rollKillerBunny)));
        features.register(module(VINDICATOR, () -> SpawnReplaceService.register(
                EntityTypes.SKELETON, BiomeReplacements::rollVindicator)));
        features.register(module(CAVE_SPIDER, () -> SpawnReplaceService.register(
                EntityTypes.SPIDER, BiomeReplacements::rollCaveSpider)));
        features.register(module(GUARDIANS, () -> SpawnReplaceService.register(
                EntityTypes.SQUID, BiomeReplacements::rollGuardian)));
        features.register(module(VEX, () -> SpawnReplaceService.register(
                EntityTypes.BAT, BiomeReplacements::rollVex)));
    }

    /**
     * {@code percent} is 0–100. {@code roll} is exclusive-100 ({@code nextInt(100)}).
     */
    static boolean hitsPercent(int percent, int roll) {
        return percent > 0 && roll < percent;
    }

    static boolean chance(ServerLevel level, int percent) {
        return hitsPercent(percent, level.getRandom().nextInt(100));
    }

    private static FeatureModule module(Identifier id, Runnable bootstrap) {
        return new FeatureModule() {
            @Override
            public Identifier id() {
                return id;
            }

            @Override
            public void bootstrap(FeatureBus bus) {
                bootstrap.run();
            }
        };
    }

    /**
     * Killer bunny is a rabbit variant, not a different {@link EntityType}; mutate in place.
     */
    static EntityType<?> rollKillerBunny(Mob original, ServerLevel level) {
        if (!WorldGate.isModuleActive(level, KILLER_BUNNY)) {
            return null;
        }
        if (!(original instanceof Rabbit rabbit) || rabbit.getVariant() == Rabbit.Variant.EVIL) {
            return null;
        }
        if (!chance(level, ConfigManager.world(level).killerBunnyPercent())) {
            return null;
        }
        rabbit.setComponent(DataComponents.RABBIT_VARIANT, Rabbit.Variant.EVIL);
        rabbit.setBaby(false);
        return null;
    }

    static EntityType<?> rollVindicator(Mob original, ServerLevel level) {
        return typedReplace(
                original, level, VINDICATOR, VINDICATOR_REPLACE, WorldConfig::vindicatorPercent, EntityTypes.VINDICATOR);
    }

    static EntityType<?> rollCaveSpider(Mob original, ServerLevel level) {
        return typedReplace(
                original, level, CAVE_SPIDER, CAVE_SPIDER_REPLACE, WorldConfig::caveSpiderPercent, EntityTypes.CAVE_SPIDER);
    }

    static EntityType<?> rollGuardian(Mob original, ServerLevel level) {
        return typedReplace(
                original, level, GUARDIANS, GUARDIAN_REPLACE, WorldConfig::guardianPercent, EntityTypes.GUARDIAN);
    }

    static EntityType<?> rollVex(Mob original, ServerLevel level) {
        if (!WorldGate.isModuleActive(level, VEX)) {
            return null;
        }
        if (!chance(level, ConfigManager.world(level).vexPercent())) {
            return null;
        }
        return EntityTypes.VEX;
    }

    private static EntityType<?> typedReplace(
            Mob original,
            ServerLevel level,
            Identifier moduleId,
            TagKey<Biome> biomeTag,
            ToIntFunction<WorldConfig> percent,
            EntityType<?> replacement) {
        if (!WorldGate.isModuleActive(level, moduleId)) {
            return null;
        }
        if (!level.getBiome(original.blockPosition()).is(biomeTag)) {
            return null;
        }
        if (!chance(level, percent.applyAsInt(ConfigManager.world(level)))) {
            return null;
        }
        return replacement;
    }
}

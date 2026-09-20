package dev.extrahardmode.feature;

import dev.extrahardmode.world.WorldGate;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.attribute.AmbientParticle;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.modifier.ColorModifier;
import net.minecraft.world.level.Level;
import org.joml.Vector3fc;
import org.joml.Vector4fc;

/**
 * Harsh-season atmosphere. When seasonal crop loss is above
 * {@link CropGrowthRules#BEE_INACTIVE_LOSS_RATE}, vanilla
 * {@code gameplay/bees_stay_in_hive} keeps bees home, and a blight haze
 * (gray sky/fog plus falling spores) marks the dying-plant stretch of the
 * cycle. Rain is not forced: it would water farmland and fight the loss roll.
 */
public final class SeasonAtmosphere {
    static final ColorModifier.BlendToGray SKY_GRAY = new ColorModifier.BlendToGray(0.55F, 0.65F);
    static final ColorModifier.BlendToGray CLOUD_GRAY = new ColorModifier.BlendToGray(0.4F, 0.5F);
    static final float FOG_END_FACTOR = 0.7F;
    static final float BLIGHT_SPORE_PROBABILITY = 0.025F;

    private static volatile boolean clientFarming;
    private static volatile int clientLossRate = 25;

    private SeasonAtmosphere() {}

    public static void syncClient(boolean farmingActive, int lossRate) {
        clientFarming = farmingActive;
        clientLossRate = Math.max(0, lossRate);
    }

    public static void clearClient() {
        clientFarming = false;
        clientLossRate = 25;
    }

    public static boolean beesInactive(Level level) {
        return CropGrowthRules.beesInactive(currentLossRate(level));
    }

    public static boolean blightVisuals(Level level) {
        return beesInactive(level) && level.canHaveWeather();
    }

    public static double currentLossRate(Level level) {
        if (level instanceof ServerLevel server) {
            if (!WorldGate.isModuleActive(server, AntiFarming.ID)) {
                return 0.0;
            }
            return AntiFarming.currentSeasonalLossRate(server);
        }
        if (!clientFarming) {
            return 0.0;
        }
        return CropGrowthRules.seasonalLossRate(
                clientLossRate, CropGrowthRules.dayIndex(level.getOverworldClockTime()));
    }

    public static void addLayers(EnvironmentAttributeSystem.Builder builder, Level level) {
        builder.addTimeBasedLayer(
                EnvironmentAttributes.BEES_STAY_IN_HIVE,
                (value, time) -> beesInactive(level) ? Boolean.TRUE : value);
        builder.addTimeBasedLayer(
                EnvironmentAttributes.SKY_COLOR, (value, time) -> graySkyRgb(level, value, SKY_GRAY));
        builder.addTimeBasedLayer(
                EnvironmentAttributes.FOG_COLOR, (value, time) -> graySkyRgb(level, value, SKY_GRAY));
        builder.addTimeBasedLayer(
                EnvironmentAttributes.CLOUD_COLOR, (value, time) -> graySkyArgb(level, value, CLOUD_GRAY));
        builder.addTimeBasedLayer(EnvironmentAttributes.FOG_END_DISTANCE, (value, time) -> closerFog(level, value));
        builder.addTimeBasedLayer(EnvironmentAttributes.AMBIENT_PARTICLES, (value, time) -> blightSpores(level, value));
    }

    static Vector3fc graySkyRgb(Level level, Vector3fc value, ColorModifier.BlendToGray gray) {
        if (!blightVisuals(level) || value == null) {
            return value;
        }
        return ColorModifier.BLEND_TO_GRAY_RGB.apply(value, gray);
    }

    static Vector4fc graySkyArgb(Level level, Vector4fc value, ColorModifier.BlendToGray gray) {
        if (!blightVisuals(level) || value == null) {
            return value;
        }
        return ColorModifier.BLEND_TO_GRAY_ARGB.apply(value, gray);
    }

    static Float closerFog(Level level, Float value) {
        if (!blightVisuals(level) || value == null) {
            return value;
        }
        return value * FOG_END_FACTOR;
    }

    static List<AmbientParticle> blightSpores(Level level, List<AmbientParticle> current) {
        if (!blightVisuals(level)) {
            return current;
        }
        return withBlightSpores(current);
    }

    static List<AmbientParticle> withBlightSpores(List<AmbientParticle> current) {
        List<AmbientParticle> extra =
                AmbientParticle.of(ParticleTypes.FALLING_SPORE_BLOSSOM, BLIGHT_SPORE_PROBABILITY);
        if (current == null || current.isEmpty()) {
            return extra;
        }
        List<AmbientParticle> merged = new ArrayList<>(current.size() + extra.size());
        merged.addAll(current);
        merged.addAll(extra);
        return List.copyOf(merged);
    }
}

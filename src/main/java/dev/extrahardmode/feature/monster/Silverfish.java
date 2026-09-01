package dev.extrahardmode.feature.monster;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.feature.FeatureBus;
import dev.extrahardmode.feature.FeatureModule;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.WorldGate;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Silverfish cannot enter stone, drop cobble, and emit visibility particles so they
 * stay visible when vanilla buries them in the floor.
 */
public final class Silverfish implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("silverfish");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        bus.listen(ServerLivingEntityEvents.AFTER_DEATH, ID, Silverfish::onAfterDeath);
    }

    public static boolean cantEnterBlocks(ServerLevel level) {
        return WorldGate.isModuleActive(level, ID) && ConfigManager.world(level).silverfishCantEnterBlocks();
    }

    public static void spawnVisibilityParticles(net.minecraft.world.entity.monster.Silverfish fish, ServerLevel level) {
        if (!WorldGate.isModuleActive(level, ID)) {
            return;
        }
        if (!ConfigManager.world(level).silverfishVisibilityParticles()) {
            return;
        }
        level.sendParticles(ParticleTypes.PORTAL, fish.getX(), fish.getY() + 0.2, fish.getZ(), 2, 0.12, 0.12, 0.12, 0.0);
    }

    private static void onAfterDeath(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof net.minecraft.world.entity.monster.Silverfish)) {
            return;
        }
        if (!(entity.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, ID)) {
            return;
        }
        if (Boolean.TRUE.equals(entity.getAttachedOrElse(EhmAttachments.EHM_LOOTLESS, Boolean.FALSE))) {
            return;
        }
        WorldConfig config = ConfigManager.world(level);
        if (!config.silverfishDropCobble()) {
            return;
        }
        entity.drop(new ItemStack(Items.COBBLESTONE), false, false);
    }
}

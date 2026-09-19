package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.world.WorldGate;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.phys.AABB;

public final class AnimalCrowdControl implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("animal_crowd_control");
    private static final int CHECK_INTERVAL = 200;

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        bus.listen(ServerEntityEvents.ENTITY_LOAD, ID, AnimalCrowdControl::onLoad);
    }

    static void onLoad(Entity entity, ServerLevel level) {
        if (!WorldGate.isModuleActive(level, ID)) {
            return;
        }
        if (entity instanceof LivingEntity living) {
            punishIfOvercrowded(level, living);
        }
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (level.getGameTime() % CHECK_INTERVAL != 0) {
            return;
        }
        if (!WorldGate.isModuleActive(level, ID)) {
            return;
        }
        WorldConfig cfg = ConfigManager.world(level);
        if (!cfg.overcrowdEnable()) {
            return;
        }
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof LivingEntity living) {
                punishIfOvercrowded(level, living);
            }
        }
    }

    static void punishIfOvercrowded(ServerLevel level, LivingEntity entity) {
        WorldConfig cfg = ConfigManager.world(level);
        if (!cfg.overcrowdEnable() || !countsTowardCrowd(entity)) {
            return;
        }
        if (crowdCount(level, entity) < cfg.overcrowdThreshold()) {
            return;
        }
        entity.hurtServer(level, level.damageSources().generic(), 1.0F);
        level.sendParticles(
                ParticleTypes.ANGRY_VILLAGER,
                entity.getX(),
                entity.getY() + entity.getBbHeight(),
                entity.getZ(),
                2,
                0.25,
                0.15,
                0.25,
                0.0);
    }

    static boolean countsTowardCrowd(LivingEntity entity) {
        if (!entity.isAlive()
                || entity.hasCustomName()
                || BiomeBosses.isBoss(entity)
                || Inhabitants.isInhabitant(entity)) {
            return false;
        }
        if (entity instanceof AbstractVillager) {
            return true;
        }
        if (!(entity instanceof Animal animal)) {
            return false;
        }
        if (animal instanceof AbstractHorse || animal instanceof Cat || animal instanceof Wolf || animal instanceof Parrot) {
            return false;
        }
        return !(animal instanceof TamableAnimal tamable) || !tamable.isTame();
    }

    /**
     * Villagers count other villagers; animals count other crowding animals.
     * A cow pen next to a hall does not fill the villager quota.
     */
    static int crowdCount(ServerLevel level, LivingEntity entity) {
        AABB box = new AABB(entity.blockPosition()).inflate(1.0);
        if (entity instanceof AbstractVillager) {
            return level.getEntitiesOfClass(AbstractVillager.class, box, AnimalCrowdControl::countsTowardCrowd)
                    .size();
        }
        return level.getEntitiesOfClass(Animal.class, box, AnimalCrowdControl::countsTowardCrowd).size();
    }
}

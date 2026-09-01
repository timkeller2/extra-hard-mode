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
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.wolf.Wolf;
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
        if (!(entity instanceof Animal animal)) {
            return;
        }
        punishIfOvercrowded(level, animal);
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
            if (entity instanceof Animal animal) {
                punishIfOvercrowded(level, animal);
            }
        }
    }

    static void punishIfOvercrowded(ServerLevel level, Animal animal) {
        WorldConfig cfg = ConfigManager.world(level);
        if (!cfg.overcrowdEnable() || !countsTowardCrowd(animal)) {
            return;
        }
        if (crowdCount(level, animal) < cfg.overcrowdThreshold()) {
            return;
        }
        animal.hurtServer(level, level.damageSources().generic(), 1.0F);
        level.sendParticles(
                ParticleTypes.ANGRY_VILLAGER,
                animal.getX(),
                animal.getY() + animal.getBbHeight(),
                animal.getZ(),
                2,
                0.25,
                0.15,
                0.25,
                0.0);
    }

    static boolean countsTowardCrowd(Animal animal) {
        if (!animal.isAlive()) {
            return false;
        }
        if (animal.hasCustomName()) {
            return false;
        }
        if (animal instanceof AbstractHorse || animal instanceof Cat || animal instanceof Wolf || animal instanceof Parrot) {
            return false;
        }
        return !(animal instanceof TamableAnimal tamable) || !tamable.isTame();
    }

    static int crowdCount(ServerLevel level, Animal animal) {
        AABB box = new AABB(animal.blockPosition()).inflate(1.0);
        int count = 0;
        for (Animal other : level.getEntitiesOfClass(Animal.class, box, AnimalCrowdControl::countsTowardCrowd)) {
            count++;
        }
        return count;
    }
}

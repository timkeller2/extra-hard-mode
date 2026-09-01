package dev.extrahardmode.task;

import dev.extrahardmode.api.ExplosionType;
import dev.extrahardmode.config.MonsterConfig;
import dev.extrahardmode.feature.Explosions;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.phys.Vec3;

/**
 * Burning creeper: fireworks, launch, then a custom creeper blast.
 * Tick offsets match original CoolCreeperExplosion (5 between fireworks, then rise, then explode).
 */
public final class CoolCreeperExplosion {
    private static final int TICKS_BETWEEN_FIREWORKS = 5;
    private static final int TICKS_BEFORE_CATAPULT = 3;
    private static final int TICKS_BEFORE_SUICIDE = 8;
    private static final int RISE_STEPS = 10;

    private final ServerLevel level;
    private final Creeper creeper;
    private final Vec3 origin;
    private final int fireworkCount;
    private final double launchSpeed;
    private int age;
    private int explodeAt = Integer.MAX_VALUE;
    private boolean launched;

    public CoolCreeperExplosion(ServerLevel level, Creeper creeper, MonsterConfig config) {
        this.level = level;
        this.creeper = creeper;
        this.origin = creeper.position();
        this.fireworkCount = Math.max(0, config.fireworkCount());
        this.launchSpeed = config.launchSpeed();
        int fireworksEnd = fireworkCount * TICKS_BETWEEN_FIREWORKS;
        int launchAt = fireworksEnd + TICKS_BEFORE_CATAPULT;
        this.explodeAt = launchAt + riseDuration() + TICKS_BEFORE_SUICIDE;
    }

    public boolean tick() {
        if (creeper.isRemoved()) {
            return true;
        }
        age++;
        if (fireworkCount > 0 && age <= fireworkCount * TICKS_BETWEEN_FIREWORKS) {
            if (age % TICKS_BETWEEN_FIREWORKS == 0) {
                spawnFirework();
            }
        }
        int fireworksEnd = fireworkCount * TICKS_BETWEEN_FIREWORKS;
        int launchAt = fireworksEnd + TICKS_BEFORE_CATAPULT;
        if (age >= launchAt && age < launchAt + riseDuration()) {
            launched = true;
            creeper.setTarget(null);
            creeper.setDeltaMovement(creeper.getDeltaMovement().x, launchSpeed, creeper.getDeltaMovement().z);
            creeper.hurtMarked = true;
        }
        if (age < explodeAt) {
            return false;
        }
        explode();
        return true;
    }

    private static int riseDuration() {
        int ticks = 0;
        int gap = 1;
        for (int i = 0; i < RISE_STEPS; i++) {
            ticks += gap;
            gap += i;
        }
        return Math.max(1, ticks);
    }

    private void spawnFirework() {
        ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET);
        IntList colors = new IntArrayList();
        colors.add(DyeColor.LIME.getFireworkColor());
        colors.add(DyeColor.BLACK.getFireworkColor());
        stack.set(
                DataComponents.FIREWORKS,
                new Fireworks(
                        1,
                        List.of(new FireworkExplosion(
                                FireworkExplosion.Shape.CREEPER, colors, new IntArrayList(), true, false))));
        Vec3 pos = creeper.isRemoved() ? origin : creeper.position();
        FireworkRocketEntity rocket = new FireworkRocketEntity(level, pos.x, pos.y, pos.z, stack);
        level.addFreshEntity(rocket);
    }

    private void explode() {
        Vec3 pos = creeper.isAlive() ? creeper.position() : origin;
        if (creeper.isAlive()) {
            Explosions.create(level, pos, ExplosionType.CREEPER, creeper);
            creeper.discard();
        } else {
            Explosions.create(level, pos, ExplosionType.CREEPER, null);
        }
    }
}

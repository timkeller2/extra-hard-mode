package dev.extrahardmode.task;

import dev.extrahardmode.api.ExplosionType;
import dev.extrahardmode.config.MonsterConfig;
import dev.extrahardmode.feature.Explosions;
import dev.extrahardmode.feature.monster.Creepers;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
 * Offsets match original: fireworks every 5 ticks, catapult +3, suicide +8
 * from catapult (not after the rise loop). Still explodes if the creeper dies.
 */
public final class CoolCreeperExplosion {
    public static final int TICKS_BETWEEN_FIREWORKS = 5;
    public static final int TICKS_BEFORE_CATAPULT = 3;
    public static final int TICKS_BEFORE_SUICIDE = 8;

    private final ServerLevel level;
    private final Creeper creeper;
    private final ExplosionType type;
    private Vec3 lastPos;
    private final int fireworkCount;
    private final double launchSpeed;
    private final int launchAt;
    private final int explodeAt;
    private int age;
    private boolean exploded;

    public CoolCreeperExplosion(ServerLevel level, Creeper creeper, MonsterConfig config) {
        this.level = level;
        this.creeper = creeper;
        this.type = creeper.isPowered() ? ExplosionType.CREEPER_CHARGED : ExplosionType.CREEPER;
        this.lastPos = creeper.position();
        this.fireworkCount = Math.max(0, config.fireworkCount());
        this.launchSpeed = config.launchSpeed();
        this.launchAt = explodeDelayTicks(fireworkCount) - TICKS_BEFORE_SUICIDE;
        this.explodeAt = explodeDelayTicks(fireworkCount);
        if (creeper.isAlive()) {
            creeper.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, explodeAt + 5, 0, false, false));
        }
    }

    /** Fireworks*5 + 3 + 8. Original suicide is scheduled from catapult start, not after rise. */
    public static int explodeDelayTicks(int fireworkCount) {
        return Math.max(0, fireworkCount) * TICKS_BETWEEN_FIREWORKS + TICKS_BEFORE_CATAPULT + TICKS_BEFORE_SUICIDE;
    }

    public boolean tick() {
        if (exploded) {
            return true;
        }
        if (!creeper.isRemoved()) {
            lastPos = creeper.position();
        }
        age++;
        if (fireworkCount > 0 && age <= fireworkCount * TICKS_BETWEEN_FIREWORKS) {
            if (age % TICKS_BETWEEN_FIREWORKS == 0) {
                spawnFirework();
            }
        }
        if (!creeper.isRemoved() && age >= launchAt && age < explodeAt) {
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
        Vec3 pos = creeper.isRemoved() ? lastPos : creeper.position();
        FireworkRocketEntity rocket = new FireworkRocketEntity(level, pos.x, pos.y, pos.z, stack);
        level.addFreshEntity(rocket);
    }

    private void explode() {
        if (exploded) {
            return;
        }
        exploded = true;
        Vec3 pos = creeper.isRemoved() ? lastPos : creeper.position();
        Explosions.createFromModule(level, Creepers.ID, pos, type, creeper);
        if (!creeper.isRemoved()) {
            creeper.discard();
        }
    }
}

package dev.extrahardmode.feature;

import com.mojang.math.Transformation;
import dev.extrahardmode.mixin.BlockDisplayAccess;
import dev.extrahardmode.mixin.DisplayAccess;
import dev.extrahardmode.player.EhmAttachments;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.block.Blocks;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * String casts a cobweb that flies in a straight line. A creature or a solid
 * block bursts it, and the burst slows creatures around the landing.
 */
public final class SlowWeb {
    private static final List<Shot> SHOTS = new ArrayList<>();
    private static final Map<UUID, Web> WEBS = new ConcurrentHashMap<>();

    private SlowWeb() {}

    public static void launch(ServerPlayer player, double power) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 direction = look.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : look.normalize();
        Vec3 start = player.getEyePosition().add(direction.scale(0.4));
        Shot shot = new Shot(
                level.dimension().identifier(),
                player.getUUID(),
                player.position(),
                direction,
                power,
                start,
                AbilityRules.slowProjectileRange(power));
        Entity display = spawnCobweb(level, start.x, start.y, start.z, 0.45F);
        if (display != null) {
            shot.displayUuid = display.getUUID();
        }
        SHOTS.add(shot);
    }

    public static void tick(ServerLevel level) {
        if (SHOTS.isEmpty()) {
            return;
        }
        Identifier dimension = level.dimension().identifier();
        Iterator<Shot> iterator = SHOTS.iterator();
        while (iterator.hasNext()) {
            Shot shot = iterator.next();
            if (!dimension.equals(shot.dimension)) {
                continue;
            }
            if (!advance(level, shot)) {
                discardDisplay(level, shot.displayUuid);
                iterator.remove();
            }
        }
    }

    public static void discardLevel(ServerLevel level) {
        Identifier dimension = level.dimension().identifier();
        SHOTS.removeIf(shot -> {
            if (!dimension.equals(shot.dimension)) {
                return false;
            }
            discardDisplay(level, shot.displayUuid);
            return true;
        });
        WEBS.entrySet().removeIf(entry -> {
            if (!dimension.equals(entry.getValue().dimension)) {
                return false;
            }
            discardDisplay(level, entry.getValue().displayUuid);
            return true;
        });
    }

    /** Drops the web on a creature that is no longer in this level. */
    public static void discard(UUID target, ServerLevel level) {
        Web web = WEBS.remove(target);
        if (web == null) {
            return;
        }
        discardDisplay(level, web.displayUuid);
    }

    /** Cobweb block and particles on a slowed creature for as long as the slow lasts. */
    public static void showOn(LivingEntity target, ServerLevel level) {
        if (target == null || level == null) {
            return;
        }
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.45;
        double z = target.getZ();
        Web web = WEBS.get(target.getUUID());
        Entity display = web == null ? null : level.getEntityInAnyDimension(web.displayUuid);
        if (display == null || display.isRemoved() || display.level() != level) {
            if (display != null && !display.isRemoved()) {
                display.discard();
            }
            display = spawnCobweb(level, x, y, z, 0.85F);
            if (display != null) {
                WEBS.put(target.getUUID(), new Web(level.dimension().identifier(), display.getUUID()));
            }
        } else {
            display.snapTo(x, y, z, 0.0F, 0.0F);
        }
        if ((level.getGameTime() + target.getId()) % 3L == 0L) {
            level.sendParticles(
                    ParticleTypes.ITEM_COBWEB, x, y, z, 6, 0.35, 0.45, 0.35, 0.01);
        }
    }

    public static void clearFrom(LivingEntity target) {
        if (target == null) {
            return;
        }
        Web web = WEBS.remove(target.getUUID());
        if (web == null || target.level().getServer() == null) {
            return;
        }
        for (ServerLevel level : target.level().getServer().getAllLevels()) {
            if (level.dimension().identifier().equals(web.dimension)) {
                discardDisplay(level, web.displayUuid);
                return;
            }
        }
    }

    private static boolean advance(ServerLevel level, Shot shot) {
        double step = Math.min(AbilityRules.SLOW_WEB_SPEED, shot.remaining);
        if (step <= 1.0E-4) {
            return false;
        }
        Vec3 from = shot.pos;
        Vec3 to = from.add(shot.direction.scale(step));
        Entity caster = casterIn(level, shot);
        Entity self = shot.displayUuid == null ? null : level.getEntityInAnyDimension(shot.displayUuid);
        Entity clipSource = self != null ? self : caster;
        BlockHitResult blockHit = level.clip(new ClipContext(
                from,
                to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                clipSource != null ? CollisionContext.of(clipSource) : CollisionContext.empty()));
        Vec3 clipEnd = blockHit.getType() == HitResult.Type.MISS ? to : blockHit.getLocation();
        AABB box = new AABB(from, clipEnd).inflate(0.6);
        double reachSqr = shot.remaining * shot.remaining;
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                clipSource,
                from,
                clipEnd,
                box,
                entity -> canHit(entity, shot, self),
                reachSqr);
        boolean entityFirst = entityHit != null
                && (blockHit.getType() != HitResult.Type.BLOCK
                        || from.distanceToSqr(entityHit.getLocation()) <= from.distanceToSqr(blockHit.getLocation()));
        if (entityFirst) {
            burst(level, shot, entityHit.getEntity(), entityHit.getLocation());
            return false;
        }
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            burst(level, shot, null, blockHit.getLocation());
            return false;
        }
        shot.pos = to;
        shot.remaining -= step;
        if (self != null) {
            self.snapTo(to.x, to.y, to.z, 0.0F, 0.0F);
        }
        level.sendParticles(ParticleTypes.ITEM_COBWEB, to.x, to.y, to.z, 2, 0.05, 0.05, 0.05, 0.0);
        return shot.remaining > 1.0E-4;
    }

    private static boolean canHit(Entity entity, Shot shot, Entity self) {
        if (entity == self || !entity.isAlive() || entity.getUUID().equals(shot.caster)) {
            return false;
        }
        return entity instanceof LivingEntity;
    }

    private static void burst(ServerLevel level, Shot shot, Entity direct, Vec3 impact) {
        double radius = AbilityRules.slowBlastRadius(shot.power);
        Vec3 casterPos = casterPosition(level, shot);
        Vec3 targetPoint = direct instanceof LivingEntity living ? living.position() : impact;
        double targetDistance = casterPos.distanceTo(targetPoint);
        AABB box = new AABB(impact, impact).inflate(radius);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (living.distanceToSqr(impact) > radius * radius) {
                continue;
            }
            boolean caster = living.getUUID().equals(shot.caster);
            boolean player = living instanceof Player;
            double fromCaster = living.position().distanceTo(casterPos);
            if (!AbilityRules.slowAffects(caster, player, fromCaster, targetDistance)) {
                continue;
            }
            int duration = AbilityRules.slowDurationTicks(shot.power, living.getMaxHealth());
            if (duration <= 0) {
                continue;
            }
            ManaAbilities.applySlow(living, shot.power, duration, level);
        }
        level.playSound(null, net.minecraft.core.BlockPos.containing(impact), SoundEvents.COBWEB_HIT, SoundSource.PLAYERS, 1.0F, 0.9F);
        level.sendParticles(ParticleTypes.ITEM_COBWEB, impact.x, impact.y, impact.z, 24, 0.6, 0.6, 0.6, 0.04);
        level.sendParticles(
                new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBWEB.defaultBlockState()),
                impact.x,
                impact.y,
                impact.z,
                12,
                0.4,
                0.4,
                0.4,
                0.05);
    }

    private static Entity casterIn(ServerLevel level, Shot shot) {
        Entity caster = level.getServer().getPlayerList().getPlayer(shot.caster);
        if (caster != null && caster.level() == level) {
            return caster;
        }
        return null;
    }

    private static Vec3 casterPosition(ServerLevel level, Shot shot) {
        Entity caster = casterIn(level, shot);
        return caster != null ? caster.position() : shot.origin;
    }

    private static Entity spawnCobweb(ServerLevel level, double x, double y, double z, float scale) {
        Display.BlockDisplay display = EntityTypes.BLOCK_DISPLAY.create(level, EntitySpawnReason.EVENT);
        if (display == null) {
            return null;
        }
        display.snapTo(x, y, z, 0.0F, 0.0F);
        ((BlockDisplayAccess) display).tougher$setBlockState(Blocks.COBWEB.defaultBlockState());
        DisplayAccess access = (DisplayAccess) display;
        access.tougher$setTransformation(new Transformation(
                new Vector3f(-0.5F * scale, -0.5F * scale, -0.5F * scale),
                new Quaternionf(),
                new Vector3f(scale, scale, scale),
                new Quaternionf()));
        access.tougher$setBrightnessOverride(Brightness.FULL_BRIGHT);
        display.setNoGravity(true);
        display.setAttached(EhmAttachments.EHM_SLOW_WEB, true);
        level.addFreshEntity(display);
        return display;
    }

    private static void discardDisplay(ServerLevel level, UUID displayUuid) {
        if (displayUuid == null) {
            return;
        }
        Entity display = level.getEntityInAnyDimension(displayUuid);
        if (display != null) {
            display.discard();
        }
    }

    private static final class Shot {
        final Identifier dimension;
        final UUID caster;
        final Vec3 origin;
        final Vec3 direction;
        final double power;
        Vec3 pos;
        double remaining;
        UUID displayUuid;

        Shot(
                Identifier dimension,
                UUID caster,
                Vec3 origin,
                Vec3 direction,
                double power,
                Vec3 pos,
                double remaining) {
            this.dimension = dimension;
            this.caster = caster;
            this.origin = origin;
            this.direction = direction;
            this.power = power;
            this.pos = pos;
            this.remaining = remaining;
        }
    }

    private record Web(Identifier dimension, UUID displayUuid) {}
}

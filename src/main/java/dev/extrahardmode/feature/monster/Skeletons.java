package dev.extrahardmode.feature.monster;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.api.event.SkeletonDeflectEvent;
import dev.extrahardmode.api.event.SkeletonKnockbackEvent;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.feature.FeatureBus;
import dev.extrahardmode.feature.FeatureModule;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.WorldGate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.skeleton.Bogged;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;

/**
 * Ordered special projectiles for Skeleton and Bogged only (KD-16). First success
 * wins; a 100% earlier entry starves later ones.
 */
public final class Skeletons implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("skeletons");

    public static final String SPECIAL_SNOWBALL = "snowball";
    public static final String SPECIAL_FIREWORK = "firework";
    public static final String SPECIAL_FIREBALL = "fireball";

    private static final AtomicBoolean MIXIN_APPLIED = new AtomicBoolean();
    private static final AtomicBoolean MIXIN_MISSING_WARNED = new AtomicBoolean();

    public enum Special {
        NONE,
        SNOWBALL,
        FIREWORK,
        FIREBALL,
        SILVERFISH
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        bus.listen(ServerLivingEntityEvents.ALLOW_DAMAGE, ID, Skeletons::onAllowDamage);
        bus.listen(ServerLivingEntityEvents.AFTER_DEATH, ID, Skeletons::onAfterDeath);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> server.execute(Skeletons::warnIfMixinMissing));
    }

    /** Mixin injects call this first. Strays, wither skeletons, and Parched are excluded. */
    public static boolean isSpecialShooter(Entity entity) {
        return entity instanceof Skeleton || entity instanceof Bogged;
    }

    public static void markMixinApplied() {
        MIXIN_APPLIED.set(true);
    }

    public static void warnIfMixinMissing() {
        if (MIXIN_APPLIED.get()) {
            return;
        }
        if (MIXIN_MISSING_WARNED.compareAndSet(false, true)) {
            ExtraHardModeMod.LOGGER.warn(
                    "EHM AbstractSkeleton performRangedAttack inject did not apply; skeleton special projectiles disabled. Lithium or another mixin may have replaced performRangedAttack.");
        }
    }

    public static boolean percentChance(RandomSource random, int percent) {
        if (percent <= 0) {
            return false;
        }
        if (percent >= 100) {
            return true;
        }
        return random.nextInt(100) < percent;
    }

    public static Special roll(AbstractSkeleton skeleton, WorldConfig config) {
        RandomSource random = skeleton.getRandom();
        if (config.skeletonSnowballEnable() && percentChance(random, config.skeletonSnowballPercent())) {
            return Special.SNOWBALL;
        }
        if (config.skeletonFireworkEnable() && percentChance(random, config.skeletonFireworkPercent())) {
            return Special.FIREWORK;
        }
        if (config.skeletonFireballEnable() && percentChance(random, config.skeletonFireballPercent())) {
            return Special.FIREBALL;
        }
        if (config.skeletonSilverfishEnable()
                && skeleton.getTarget() instanceof Player
                && percentChance(random, config.skeletonSilverfishPercent())) {
            return Special.SILVERFISH;
        }
        return Special.NONE;
    }

    /**
     * Replaces or augments the vanilla arrow. Silverfish skips adding the arrow when
     * live/total limits allow; other specials keep the arrow and spawn a visual extra.
     */
    public static Projectile replaceShot(
            AbstractSkeleton skeleton,
            Projectile arrow,
            ServerLevel level,
            ItemStack stack,
            double x,
            double y,
            double z,
            float velocity,
            float inaccuracy) {
        WorldConfig config = ConfigManager.world(level);
        Special special = roll(skeleton, config);
        if (special == Special.SILVERFISH && spawnSilverfish(skeleton, level, config, x, y, z, velocity)) {
            return arrow;
        }
        Projectile spawned = Projectile.spawnProjectileUsingShoot(arrow, level, stack, x, y, z, velocity, inaccuracy);
        applySpecial(skeleton, spawned, special, level);
        return spawned;
    }

    public static boolean tryDeflect(AbstractArrow arrow, Entity target) {
        if (!isSpecialShooter(target) || !(target instanceof AbstractSkeleton skeleton)) {
            return false;
        }
        if (!(skeleton.level() instanceof ServerLevel level)) {
            return false;
        }
        if (!WorldGate.isModuleActive(level, ID)) {
            return false;
        }
        WorldConfig config = ConfigManager.world(level);
        int percent = config.skeletonDeflectArrowsPercent();
        if (percent <= 0) {
            return false;
        }
        ServerPlayer shooter = arrow.getOwner() instanceof ServerPlayer player ? player : null;
        SkeletonDeflectEvent event = new SkeletonDeflectEvent(shooter, skeleton, arrow, percent);
        if (!percentChance(skeleton.getRandom(), percent)) {
            event.cancel();
        }
        SkeletonDeflectEvent.EVENT.invoker().onSkeletonDeflect(event);
        return !event.isCanceled();
    }

    public static void applyOnHit(LivingEntity victim, AbstractArrow arrow, WorldConfig config) {
        String special = arrow.getAttached(EhmAttachments.EHM_SKELETON_SPECIAL);
        if (special == null) {
            return;
        }
        Entity owner = arrow.getOwner();
        if (!isSpecialShooter(owner) || !(owner instanceof AbstractSkeleton skeleton)) {
            return;
        }
        switch (special) {
            case SPECIAL_SNOWBALL -> victim.addEffect(
                    new MobEffectInstance(MobEffects.BLINDNESS, config.skeletonSnowballBlindTicks(), 0), skeleton);
            case SPECIAL_FIREWORK -> applyKnockback(victim, skeleton, arrow, config);
            case SPECIAL_FIREBALL -> victim.igniteForTicks(config.skeletonFireballFireTicks());
            default -> {
            }
        }
    }

    private static boolean onAllowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, ID)) {
            return true;
        }
        Entity direct = source.getDirectEntity();
        if (!(direct instanceof AbstractArrow arrow)) {
            return true;
        }
        if (tryDeflect(arrow, entity)) {
            return false;
        }
        if (entity instanceof Player) {
            applyOnHit(entity, arrow, ConfigManager.world(level));
        }
        return true;
    }

    private static void onAfterDeath(LivingEntity entity, DamageSource source) {
        if (!isSpecialShooter(entity) || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (!WorldGate.isModuleActive(level, ID)) {
            return;
        }
        if (!ConfigManager.world(level).skeletonKillSilverfishOnDeath()) {
            return;
        }
        UUID owner = entity.getUUID();
        for (net.minecraft.world.entity.monster.Silverfish minion : liveMinions(level, owner)) {
            minion.discard();
        }
    }

    private static void applySpecial(AbstractSkeleton skeleton, Projectile spawned, Special special, ServerLevel level) {
        switch (special) {
            case SNOWBALL -> {
                spawned.setAttached(EhmAttachments.EHM_SKELETON_SPECIAL, SPECIAL_SNOWBALL);
                Snowball snowball = new Snowball(level, skeleton, new ItemStack(Items.SNOWBALL));
                snowball.setDeltaMovement(spawned.getDeltaMovement());
                level.addFreshEntity(snowball);
            }
            case FIREWORK -> {
                spawned.setAttached(EhmAttachments.EHM_SKELETON_SPECIAL, SPECIAL_FIREWORK);
                FireworkRocketEntity rocket = new FireworkRocketEntity(
                        level, spawned.getX(), spawned.getY(), spawned.getZ(), new ItemStack(Items.FIREWORK_ROCKET));
                rocket.setOwner(skeleton);
                rocket.setDeltaMovement(spawned.getDeltaMovement());
                level.addFreshEntity(rocket);
            }
            case FIREBALL -> {
                spawned.setAttached(EhmAttachments.EHM_SKELETON_SPECIAL, SPECIAL_FIREBALL);
                SmallFireball fireball = new SmallFireball(level, skeleton, spawned.getDeltaMovement());
                fireball.setPos(spawned.position());
                level.addFreshEntity(fireball);
            }
            default -> {
            }
        }
    }

    private static boolean spawnSilverfish(
            AbstractSkeleton skeleton, ServerLevel level, WorldConfig config, double x, double y, double z, float velocity) {
        UUID owner = skeleton.getUUID();
        if (liveMinions(level, owner).size() >= config.skeletonSilverfishMaxAtOnce()) {
            return false;
        }
        int total = skeleton.getAttachedOrElse(EhmAttachments.EHM_SILVERFISH_SPAWNED, 0);
        if (total >= config.skeletonSilverfishMaxTotal()) {
            return false;
        }
        net.minecraft.world.entity.monster.Silverfish fish =
                EntityTypes.SILVERFISH.create(level, EntitySpawnReason.EVENT);
        if (fish == null) {
            return false;
        }
        Vec3 vel = new Vec3(x, y, z);
        if (vel.lengthSqr() > 1.0E-7) {
            vel = vel.normalize().scale(velocity * 0.25);
        }
        fish.snapTo(skeleton.getX(), skeleton.getY() + 1.5, skeleton.getZ(), skeleton.getYRot(), skeleton.getXRot());
        fish.setDeltaMovement(vel);
        fish.setTarget(skeleton.getTarget());
        fish.setAttached(EhmAttachments.EHM_SILVERFISH_OWNER, owner);
        fish.setAttached(EhmAttachments.EHM_LOOTLESS, true);
        fish.skipDropExperience();
        if (!level.addFreshEntity(fish)) {
            return false;
        }
        skeleton.setAttached(EhmAttachments.EHM_SILVERFISH_SPAWNED, total + 1);
        return true;
    }

    private static List<net.minecraft.world.entity.monster.Silverfish> liveMinions(ServerLevel level, UUID owner) {
        List<net.minecraft.world.entity.monster.Silverfish> found = new ArrayList<>();
        level.getEntities(
                EntityTypeTest.forClass(net.minecraft.world.entity.monster.Silverfish.class),
                fish -> owner.equals(fish.getAttached(EhmAttachments.EHM_SILVERFISH_OWNER)),
                found);
        return found;
    }

    private static void applyKnockback(
            LivingEntity victim, AbstractSkeleton skeleton, AbstractArrow arrow, WorldConfig config) {
        Vec3 velocity = arrow.getDeltaMovement().scale(config.skeletonFireworkKnockback());
        SkeletonKnockbackEvent event =
                new SkeletonKnockbackEvent(victim, skeleton, velocity, config.skeletonFireworkPercent());
        SkeletonKnockbackEvent.EVENT.invoker().onSkeletonKnockback(event);
        if (event.isCanceled()) {
            return;
        }
        victim.setDeltaMovement(event.velocity());
        victim.hurtMarked = true;
    }
}

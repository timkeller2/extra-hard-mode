package dev.extrahardmode.feature.monster;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.MonsterConfig;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.feature.FeatureBus;
import dev.extrahardmode.feature.FeatureModule;
import dev.extrahardmode.module.EntityHelper;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.task.RespawnZombieTask;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Zombie slowness on hit, reanimate, and ±20% speed with inverse damage and scale.
 * No villager / reinforcement / on-fire reanimate. Copies {@code EHM_REANIMATE_COUNT}
 * so chance decays.
 */
public final class Zombies implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("zombies");
    public static final Identifier SPEED_MOD = ExtraHardModeMod.id("zombie_speed");
    public static final Identifier DAMAGE_MOD = ExtraHardModeMod.id("zombie_damage");
    public static final Identifier SCALE_MOD = ExtraHardModeMod.id("zombie_scale");
    public static final Identifier HEAD_SPEED_MOD = ExtraHardModeMod.id("zombie_head_speed");
    public static final Identifier HEAD_DAMAGE_MOD = ExtraHardModeMod.id("zombie_head_damage");
    private static final DustParticleOptions HEAD_SPARK = new DustParticleOptions(0xFF2020, 0.7F);

    private static final Map<Identifier, ArrayDeque<RespawnZombieTask>> PENDING = new ConcurrentHashMap<>();

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        bus.listen(ServerLivingEntityEvents.AFTER_DAMAGE, ID, Zombies::onAfterDamage);
        bus.listen(ServerLivingEntityEvents.AFTER_DEATH, ID, Zombies::onDeath);
        bus.listen(PlayerBlockBreakEvents.BEFORE, ID, Zombies::onSkullBreak);
        bus.listen(ServerEntityEvents.ENTITY_LOAD, ID, Zombies::onLoad);
    }

    @Override
    public void onWorldUnload(ServerLevel level) {
        PENDING.remove(level.dimension().identifier());
    }

    @Override
    public void serverTick(ServerLevel level) {
        ArrayDeque<RespawnZombieTask> queue = PENDING.get(level.dimension().identifier());
        if (queue == null || queue.isEmpty()) {
            return;
        }
        int snapshot = queue.size();
        for (int i = 0; i < snapshot && !queue.isEmpty(); i++) {
            RespawnZombieTask task = queue.pollFirst();
            if (task == null) {
                break;
            }
            if (!task.tick()) {
                queue.addLast(task);
            }
        }
    }

    public static boolean enabled(Level level) {
        return FeatureBus.guard(level, ID);
    }

    /** {@code p/n} after incrementing the death count. First death n=1 → 50%. */
    public static int reanimateChance(int percent, int respawnCount) {
        int n = Math.max(1, respawnCount);
        return (int) ((1.0D / n) * percent);
    }

    /** 3–8 seconds, matching {@code nextInt(6)+3}. {@code nextInt6} is 0–5. */
    public static int reanimateDelayTicks(int nextInt6) {
        return (3 + Math.floorMod(nextInt6, 6)) * 20;
    }

    /** 3–8 seconds, matching {@code nextInt(6)+3}. */
    public static int reanimateDelayTicks(RandomSource random) {
        return reanimateDelayTicks(random.nextInt(6));
    }

    public static boolean isOrdinaryZombie(Entity entity) {
        return entity instanceof Zombie
                && !(entity instanceof ZombieVillager)
                && entity.getType() == EntityTypes.ZOMBIE;
    }

    public static void stampReinforcement(Mob mob, EntitySpawnReason reason) {
        if (reason != EntitySpawnReason.REINFORCEMENT) {
            return;
        }
        if (!(mob instanceof Zombie)) {
            return;
        }
        EntityHelper.markIgnored(mob);
    }

    /**
     * Permanent ±20% movement-speed roll with inverse attack damage and scale.
     * Skips biome bosses. Chunk reload keeps the existing roll; scale is filled
     * in if an older zombie already has speed but not size.
     */
    public static void applySpeedVariance(Entity entity) {
        if (!isOrdinaryZombie(entity) || !(entity instanceof Mob mob)) {
            return;
        }
        if (!(mob.level() instanceof ServerLevel level) || !enabled(level)) {
            return;
        }
        if (Boolean.TRUE.equals(mob.getAttachedOrElse(EhmAttachments.EHM_BIOME_BOSS, Boolean.FALSE))) {
            return;
        }
        if (Boolean.TRUE.equals(mob.getAttachedOrElse(EhmAttachments.EHM_HEAD_ZOMBIE, Boolean.FALSE))) {
            grantHeadReturn(mob);
        }
        AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance damage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance scale = mob.getAttribute(Attributes.SCALE);
        if (speed == null || damage == null) {
            return;
        }
        if (speed.hasModifier(SPEED_MOD) || damage.hasModifier(DAMAGE_MOD)) {
            applyScaleFromSpeed(speed, scale);
            return;
        }
        double speedDelta = ZombieRules.speedDelta(mob.getRandom().nextDouble());
        addVariance(speed, SPEED_MOD, speedDelta);
        addVariance(damage, DAMAGE_MOD, ZombieRules.damageDelta(speedDelta));
        addVariance(scale, SCALE_MOD, ZombieRules.scaleDelta(speedDelta));
    }

    private static void applyScaleFromSpeed(AttributeInstance speed, AttributeInstance scale) {
        if (scale == null || scale.hasModifier(SCALE_MOD) || !speed.hasModifier(SPEED_MOD)) {
            return;
        }
        AttributeModifier existing = speed.getModifier(SPEED_MOD);
        if (existing == null) {
            return;
        }
        addVariance(scale, SCALE_MOD, ZombieRules.scaleDelta(existing.amount()));
    }

    /**
     * A zombie that rose from a head. Half health is set by the respawn task.
     * Damage is 30% higher and speed is 20% higher than the speed roll already gave it.
     */
    public static void grantHeadReturn(Mob mob) {
        if (!isOrdinaryZombie(mob)) {
            return;
        }
        mob.setAttached(EhmAttachments.EHM_HEAD_ZOMBIE, Boolean.TRUE);
        AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance damage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        addTotal(speed, HEAD_SPEED_MOD, ZombieRules.HEAD_SPEED_BONUS);
        addTotal(damage, HEAD_DAMAGE_MOD, ZombieRules.HEAD_DAMAGE_BONUS);
    }

    /** Red sparkle at both hands while a head-risen zombie is alive. */
    public static void sparkleHands(Zombie zombie, ServerLevel level) {
        if (zombie == null || level == null || !zombie.isAlive() || !enabled(level)) {
            return;
        }
        if (!Boolean.TRUE.equals(zombie.getAttachedOrElse(EhmAttachments.EHM_HEAD_ZOMBIE, Boolean.FALSE))) {
            return;
        }
        for (ZombieRules.Hand hand : ZombieRules.hands(zombie.getYRot(), zombie.getBbHeight())) {
            double x = zombie.getX() + hand.x();
            double y = zombie.getY() + hand.y();
            double z = zombie.getZ() + hand.z();
            level.sendParticles(HEAD_SPARK, x, y, z, 1, 0.04, 0.05, 0.04, 0.012);
            level.sendParticles(
                    ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 1.0F, 0.15F, 0.12F),
                    x,
                    y,
                    z,
                    1,
                    0.02,
                    0.03,
                    0.02,
                    0.0);
        }
    }

    private static void addTotal(AttributeInstance instance, Identifier id, double amount) {
        if (instance == null || instance.hasModifier(id)) {
            return;
        }
        instance.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void addVariance(AttributeInstance instance, Identifier id, double amount) {
        if (instance == null || instance.hasModifier(id)) {
            return;
        }
        instance.addPermanentModifier(
                new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    private static void onLoad(Entity entity, ServerLevel level) {
        if (!enabled(level)) {
            return;
        }
        applySpeedVariance(entity);
    }

    private static void onAfterDamage(
            LivingEntity entity, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level) || !enabled(level)) {
            return;
        }
        if (EhmApi.playerBypasses(player)) {
            return;
        }
        Entity damager = source.getEntity();
        if (damager instanceof Projectile projectile) {
            damager = projectile.getOwner();
        }
        if (!isOrdinaryZombie(damager)) {
            return;
        }
        WorldConfig world = ConfigManager.world(level);
        MonsterConfig config = world.monsters();
        if (!config.zombiesSlowPlayers()) {
            return;
        }
        applySlow(player, config);
    }

    static void applySlow(ServerPlayer player, MonsterConfig config) {
        var type = config.slowEffectHolder();
        int amplifier = config.slowAmplifier();
        if (config.slowStack() && player.hasEffect(type)) {
            MobEffectInstance current = player.getEffect(type);
            int next = current == null ? amplifier : current.getAmplifier() + 1;
            amplifier = Math.min(next, config.slowStackMax());
            player.removeEffect(type);
        }
        player.addEffect(config.slowEffect(amplifier));
    }

    private static void onDeath(LivingEntity entity, DamageSource source) {
        if (!isOrdinaryZombie(entity) || !(entity.level() instanceof ServerLevel level) || !enabled(level)) {
            return;
        }
        if (EntityHelper.ignored(entity)) {
            return;
        }
        if (entity.getRemainingFireTicks() >= 1 || entity.isOnFire()) {
            return;
        }
        MonsterConfig config = ConfigManager.world(level).monsters();
        int count = EntityHelper.reanimateCount(entity) + 1;
        int chance = reanimateChance(config.reanimatePercent(), count);
        if (!EntityHelper.percent(level.getRandom(), chance)) {
            return;
        }
        BlockPos skullPos = null;
        if (config.placeSkulls()) {
            skullPos = tryPlaceSkull(level, entity.blockPosition());
        }
        UUID targetId = null;
        if (entity instanceof Mob mob && mob.getTarget() instanceof ServerPlayer target && target.isAlive()) {
            targetId = target.getUUID();
        }
        RespawnZombieTask task = new RespawnZombieTask(
                level,
                entity.position(),
                targetId,
                skullPos,
                count,
                reanimateDelayTicks(level.getRandom()));
        PENDING.computeIfAbsent(level.dimension().identifier(), id -> new ArrayDeque<>()).addLast(task);
    }

    private static BlockPos tryPlaceSkull(ServerLevel level, BlockPos origin) {
        if (level.getFluidState(origin).is(FluidTags.WATER) || level.getBlockState(origin).is(Blocks.WATER)) {
            return null;
        }
        BlockPos pos = origin;
        if (!level.getBlockState(pos).isAir()) {
            pos = pos.above();
            if (!level.getBlockState(pos).isAir()) {
                return null;
            }
        }
        BlockState skull = Blocks.ZOMBIE_HEAD.defaultBlockState();
        if (skull.hasProperty(BlockStateProperties.ROTATION_16)) {
            skull = skull.setValue(BlockStateProperties.ROTATION_16, level.getRandom().nextInt(16));
        }
        level.setBlock(pos, skull, 3);
        return pos.immutable();
    }

    private static boolean onSkullBreak(
            Level world, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (!(world instanceof ServerLevel level) || !enabled(level)) {
            return true;
        }
        if (!state.is(Blocks.ZOMBIE_HEAD)) {
            return true;
        }
        ArrayDeque<RespawnZombieTask> queue = PENDING.get(level.dimension().identifier());
        if (queue == null || queue.isEmpty()) {
            return true;
        }
        MonsterConfig config = ConfigManager.world(level).monsters();
        Iterator<RespawnZombieTask> iterator = queue.iterator();
        while (iterator.hasNext()) {
            RespawnZombieTask task = iterator.next();
            if (!pos.equals(task.skullPos())) {
                continue;
            }
            task.cancel();
            iterator.remove();
            if (!EntityHelper.percent(level.getRandom(), config.skullDropPercent())) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                return false;
            }
            return true;
        }
        return true;
    }
}

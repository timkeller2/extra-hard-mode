package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.player.DamageTracker;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.EhmTags;
import dev.extrahardmode.world.WorldGate;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;

/**
 * Blocks grinder loot/XP: unnatural floors, environmental damage, no path to a
 * player, or standing in water. Trial spawners are never touched.
 */
public final class AntiGrinder implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("anti_grinder");
    public static final AntiGrinder INSTANCE = new AntiGrinder();

    private static final double PATH_RANGE = 16.0;

    private AntiGrinder() {}

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        bus.listen(ServerLivingEntityEvents.AFTER_DAMAGE, ID, AntiGrinder::onAfterDamage);
    }

    public static boolean onCheckSpawnRules(Mob mob, ServerLevel level, EntitySpawnReason reason) {
        if (!WorldGate.isModuleActive(level, ID) || !ConfigManager.world(level).inhibitGrinders()) {
            return true;
        }
        if (reason == EntitySpawnReason.TRIAL_SPAWNER
                || reason == EntitySpawnReason.REINFORCEMENT
                || reason == EntitySpawnReason.SPAWN_ITEM_USE
                || reason == EntitySpawnReason.PATROL
                || reason == EntitySpawnReason.COMMAND) {
            return true;
        }
        if (isTrialSpawned(mob) || !(mob instanceof Enemy) || isExempt(mob)) {
            return true;
        }
        if (reason == EntitySpawnReason.SPAWNER) {
            markLootless(mob);
            return true;
        }
        if (!isNaturalFloor(level, mob.blockPosition())) {
            if (reason == EntitySpawnReason.NATURAL) {
                return false;
            }
            mob.setAttached(EhmAttachments.EHM_UNNATURAL_SPAWN, true);
            markLootless(mob);
        }
        return true;
    }

    public static boolean shouldBlockDrops(LivingEntity entity, ServerLevel level) {
        if (!WorldGate.isModuleActive(level, ID) || !ConfigManager.world(level).inhibitGrinders()) {
            return false;
        }
        if (isTrialSpawned(entity) || !(entity instanceof Enemy) || isExempt(entity)) {
            return false;
        }
        if (Boolean.TRUE.equals(entity.getAttachedOrElse(EhmAttachments.EHM_LOOTLESS, Boolean.FALSE))) {
            return true;
        }
        if (Boolean.TRUE.equals(entity.getAttachedOrElse(EhmAttachments.EHM_UNNATURAL_SPAWN, Boolean.FALSE))) {
            return true;
        }
        DamageTracker tracker = entity.getAttachedOrElse(EhmAttachments.EHM_DAMAGE_TRACKER, DamageTracker.EMPTY);
        if (tracker.mostlyEnvironmental()) {
            return true;
        }
        if (entity.isInWater() && !entity.is(EntityTypeTags.AQUATIC)) {
            return true;
        }
        if (entity instanceof Mob mob && !canPathToPlayer(mob, level)) {
            return true;
        }
        return false;
    }

    static void onAfterDamage(
            LivingEntity entity, DamageSource source, float dealt, float original, boolean blocked) {
        if (isTrialSpawned(entity) || !(entity instanceof Enemy) || isExempt(entity)) {
            return;
        }
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (!WorldGate.isModuleActive(level, ID) || !ConfigManager.world(level).inhibitGrinders()) {
            return;
        }
        float amount = dealt > 0.0F ? dealt : original;
        if (amount <= 0.0F) {
            return;
        }
        DamageTracker tracker = entity.getAttachedOrElse(EhmAttachments.EHM_DAMAGE_TRACKER, DamageTracker.EMPTY);
        entity.setAttached(
                EhmAttachments.EHM_DAMAGE_TRACKER,
                isEnvironmental(source) ? tracker.addEnvironmental(amount) : tracker.addPlayer(amount));
    }

    public static boolean isNaturalFloor(ServerLevel level, BlockPos feet) {
        BlockState floor = level.getBlockState(feet.below());
        if (floor.isAir()) {
            return true;
        }
        return floor.is(EhmTags.NATURAL_SPAWN_BLOCKS);
    }

    public static void markLootless(LivingEntity entity) {
        entity.setAttached(EhmAttachments.EHM_LOOTLESS, true);
    }

    public static void markTrialSpawned(LivingEntity entity) {
        entity.setAttached(EhmAttachments.EHM_TRIAL_SPAWNED, true);
    }

    public static boolean isTrialSpawned(LivingEntity entity) {
        return Boolean.TRUE.equals(entity.getAttachedOrElse(EhmAttachments.EHM_TRIAL_SPAWNED, Boolean.FALSE));
    }

    private static boolean isEnvironmental(DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker instanceof TamableAnimal) {
            return true;
        }
        if (attacker instanceof Player) {
            return false;
        }
        if (attacker instanceof LivingEntity) {
            return false;
        }
        if (source.is(DamageTypeTags.IS_PROJECTILE) || source.is(DamageTypeTags.IS_PLAYER_ATTACK)) {
            return false;
        }
        return true;
    }

    private static boolean canPathToPlayer(Mob mob, ServerLevel level) {
        Player nearest = level.getNearestPlayer(mob, PATH_RANGE);
        if (nearest == null) {
            return false;
        }
        Path path = mob.getNavigation().createPath(nearest, 0);
        return path != null && path.canReach();
    }

    private static boolean isExempt(LivingEntity entity) {
        return entity instanceof WitherBoss
                || entity instanceof EnderDragon
                || entity instanceof Guardian
                || Boolean.TRUE.equals(entity.getAttachedOrElse(EhmAttachments.EHM_BIOME_BOSS, Boolean.FALSE));
    }
}

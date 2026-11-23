package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.module.TutorialCounts;
import dev.extrahardmode.player.DamageTracker;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.EhmTags;
import dev.extrahardmode.world.WorldGate;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
 * player, or standing in water. A monster spawner still drops for each player's
 * first configured kills from that spawner, and an unnatural floor does not
 * veto those kills. Trial spawners are never touched. Natural mobs still are.
 */
public final class AntiGrinder implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("anti_grinder");
    public static final AntiGrinder INSTANCE = new AntiGrinder();

    private static final double PATH_RANGE = 16.0;
    /** Set while a minecart spawner's tick is running, so the cart stays one spawner. */
    private static final ThreadLocal<UUID> MINECART_SPAWNER = new ThreadLocal<>();
    /** Spawner key for mobs created during {@code BaseSpawner.serverTick}. */
    private static final ThreadLocal<String> SPAWNER_KEY = new ThreadLocal<>();

    private AntiGrinder() {}

    public static void beginMinecartSpawner(UUID id) {
        if (id == null) {
            MINECART_SPAWNER.remove();
            return;
        }
        MINECART_SPAWNER.set(id);
    }

    public static void endMinecartSpawner() {
        MINECART_SPAWNER.remove();
    }

    public static void beginSpawnerTick(ServerLevel level, BlockPos pos) {
        UUID cart = MINECART_SPAWNER.get();
        if (cart != null) {
            SPAWNER_KEY.set(SpawnerLootRules.minecartKey(cart.toString()));
            return;
        }
        if (level == null || pos == null) {
            SPAWNER_KEY.remove();
            return;
        }
        Identifier dimension = level.dimension().identifier();
        SPAWNER_KEY.set(SpawnerLootRules.blockKey(dimension == null ? "" : dimension.toString(), pos.asLong()));
    }

    public static void endSpawnerTick() {
        SPAWNER_KEY.remove();
    }

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
            // A spawner tick that we did not see stays fully lootless.
            String key = SPAWNER_KEY.get();
            if (key == null || key.isEmpty()) {
                markLootless(mob);
            } else {
                stampSpawnerMob(mob, key);
            }
            return true;
        }
        if (!isNaturalFloor(level, mob.blockPosition())) {
            if (reason == EntitySpawnReason.NATURAL) {
                return false;
            }
            if (SpawnerLootRules.unnaturalFloorBlocksLoot(isTrialSpawned(mob), hasSpawnerId(mob))) {
                mob.setAttached(EhmAttachments.EHM_UNNATURAL_SPAWN, true);
                markLootless(mob);
            }
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
        boolean spawner = hasSpawnerId(entity);
        boolean unnatural = Boolean.TRUE.equals(entity.getAttachedOrElse(EhmAttachments.EHM_UNNATURAL_SPAWN, Boolean.FALSE));
        boolean lootless = Boolean.TRUE.equals(entity.getAttachedOrElse(EhmAttachments.EHM_LOOTLESS, Boolean.FALSE));
        // The floor mark also sets lootless. A monster spawner's kill cap is the veto instead.
        if (lootless && !(spawner && unnatural)) {
            return true;
        }
        if (SpawnerLootRules.unnaturalFloorBlocksLoot(false, spawner) && unnatural) {
            return true;
        }
        int gate = spawnerGate(entity, level);
        if (gate == SpawnerLootRules.GATE_BLOCK) {
            return true;
        }
        if (gate == SpawnerLootRules.GATE_ALLOW || gate == SpawnerLootRules.GATE_COUNTED) {
            return false;
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

    /**
     * Called when a spawner tick adds a mob. Spawners with custom rules never call
     * {@code checkSpawnRules}, so this is what still ties those mobs to the spawner.
     */
    public static void noteAddedDuringSpawnerTick(Entity entity) {
        String key = SPAWNER_KEY.get();
        if (key == null || key.isEmpty() || entity == null) {
            return;
        }
        stampSpawnerMob(entity, key);
        for (Entity passenger : entity.getPassengers()) {
            stampSpawnerMob(passenger, key);
        }
    }

    private static void stampSpawnerMob(Entity entity, String key) {
        if (!(entity instanceof Mob mob) || !(mob instanceof Enemy) || isExempt(mob) || isTrialSpawned(mob)) {
            return;
        }
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        if (!WorldGate.isModuleActive(level, ID) || !ConfigManager.world(level).inhibitGrinders()) {
            return;
        }
        if (Boolean.TRUE.equals(mob.getAttachedOrElse(EhmAttachments.EHM_LOOTLESS, Boolean.FALSE))) {
            return;
        }
        if (mob.getAttached(EhmAttachments.EHM_SPAWNER_ID) != null) {
            return;
        }
        mob.setAttached(EhmAttachments.EHM_SPAWNER_ID, key);
    }

    /**
     * Decides whether this spawner mob's death drops loot. The result is cached on the mob
     * so the loot roll and the experience roll agree, and the kill is counted only once.
     */
    public static int spawnerGate(LivingEntity entity, ServerLevel level) {
        if (entity == null || level == null) {
            return SpawnerLootRules.GATE_NONE;
        }
        if (!WorldGate.isModuleActive(level, ID) || !ConfigManager.world(level).inhibitGrinders()) {
            return SpawnerLootRules.GATE_NONE;
        }
        String id = entity.getAttached(EhmAttachments.EHM_SPAWNER_ID);
        if (id == null || id.isEmpty()) {
            return SpawnerLootRules.GATE_NONE;
        }
        Integer cached = entity.getAttached(EhmAttachments.EHM_SPAWNER_GATE);
        if (cached != null && cached != SpawnerLootRules.GATE_NONE) {
            return cached;
        }
        int gate = SpawnerLootRules.GATE_BLOCK;
        int allowance = ConfigManager.world(level).spawnerLootKills();
        if (entity.getKillCredit() instanceof ServerPlayer player
                && SpawnerLootRules.dropsLoot(spawnerKills(player, id), allowance)) {
            gate = SpawnerLootRules.GATE_ALLOW;
        }
        entity.setAttached(EhmAttachments.EHM_SPAWNER_GATE, gate);
        if (gate == SpawnerLootRules.GATE_BLOCK) {
            markLootless(entity);
        }
        return gate;
    }

    /** Counts this player kill once the loot table is actually rolling. */
    public static void countSpawnerKill(LivingEntity entity) {
        if (entity == null || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (spawnerGate(entity, level) != SpawnerLootRules.GATE_ALLOW) {
            return;
        }
        String id = entity.getAttached(EhmAttachments.EHM_SPAWNER_ID);
        if (!(entity.getKillCredit() instanceof ServerPlayer player) || id == null || id.isEmpty()) {
            entity.setAttached(EhmAttachments.EHM_SPAWNER_GATE, SpawnerLootRules.GATE_BLOCK);
            markLootless(entity);
            return;
        }
        int kills = spawnerKills(player, id);
        int allowance = ConfigManager.world(level).spawnerLootKills();
        if (!SpawnerLootRules.dropsLoot(kills, allowance)) {
            entity.setAttached(EhmAttachments.EHM_SPAWNER_GATE, SpawnerLootRules.GATE_BLOCK);
            markLootless(entity);
            return;
        }
        putSpawnerKills(player, id, kills + 1);
        entity.setAttached(EhmAttachments.EHM_SPAWNER_GATE, SpawnerLootRules.GATE_COUNTED);
    }

    private static int spawnerKills(ServerPlayer player, String id) {
        Map<String, Integer> map = player.getAttachedOrElse(EhmAttachments.EHM_SPAWNER_KILLS, Map.of());
        return Math.max(0, map.getOrDefault(id, 0));
    }

    private static void putSpawnerKills(ServerPlayer player, String id, int kills) {
        Map<String, Integer> map = TutorialCounts.mutableCopy(player.getAttachedOrElse(EhmAttachments.EHM_SPAWNER_KILLS, Map.of()));
        if (kills <= 0) {
            map.remove(id);
        } else {
            map.put(id, kills);
        }
        player.setAttached(EhmAttachments.EHM_SPAWNER_KILLS, map);
    }

    public static void markTrialSpawned(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        entity.setAttached(EhmAttachments.EHM_TRIAL_SPAWNED, true);
    }

    /**
     * Marks a trial-spawner mob and anything riding it.
     * Cave spiders replace {@code finalizeSpawn} and never call the mob method, so the
     * spawn-reason stamp there does not reach them.
     */
    public static void markTrialGroup(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        markTrialSpawned(living);
        for (Entity passenger : entity.getPassengers()) {
            if (passenger instanceof LivingEntity rider) {
                markTrialSpawned(rider);
            }
        }
    }

    public static boolean hasSpawnerId(Entity entity) {
        if (entity == null) {
            return false;
        }
        String id = entity.getAttached(EhmAttachments.EHM_SPAWNER_ID);
        return id != null && !id.isEmpty();
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

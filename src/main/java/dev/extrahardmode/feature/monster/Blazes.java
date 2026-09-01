package dev.extrahardmode.feature.monster;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.api.ExplosionType;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.feature.FeatureBus;
import dev.extrahardmode.feature.FeatureModule;
import dev.extrahardmode.module.SpawnReplaceService;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.task.CreateExplosionTask;
import dev.extrahardmode.world.WorldGate;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * Near-bedrock overworld blazes, bonus nether blazes with magma cubes, fire on
 * hit, nether split, overworld death blast. Magma cubes live here (original).
 */
public final class Blazes implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("blazes");
    public static final int DEFAULT_NEAR_BEDROCK_MAX_Y = -56;
    public static final int DEFAULT_NEAR_BEDROCK_PERCENT = 50;
    public static final int DEFAULT_BONUS_NETHER_PERCENT = 20;
    public static final int DEFAULT_NETHER_SPLIT_PERCENT = 25;
    public static final int DEFAULT_MAGMA_WITH_BLAZE_PERCENT = 100;

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        SpawnReplaceService.register(EntityTypes.SKELETON, Blazes::rollOverworldSkeleton);
        SpawnReplaceService.register(EntityTypes.ZOMBIFIED_PIGLIN, Blazes::rollNetherBlaze);
        bus.listen(ServerLivingEntityEvents.ALLOW_DAMAGE, ID, Blazes::onAllowDamage);
        bus.listen(ServerLivingEntityEvents.AFTER_DAMAGE, ID, Blazes::onAfterDamage);
        bus.listen(ServerLivingEntityEvents.AFTER_DEATH, ID, Blazes::onAfterDeath);
        bus.listen(LootTableEvents.MODIFY_DROPS, ID, Blazes::modifyDrops);
    }

    public static boolean percentChance(RandomSource random, int percent) {
        return percentChance(percent, percent <= 0 || percent >= 100 ? 0 : random.nextInt(100));
    }

    /** {@code roll} is {@code nextInt(100)}; ignored when percent is 0 or 100. */
    public static boolean percentChance(int percent, int roll) {
        if (percent <= 0) {
            return false;
        }
        if (percent >= 100) {
            return true;
        }
        return roll < percent;
    }

    public static boolean shouldReplaceNearBedrock(int blockY, boolean enable, int maxY) {
        if (!enable || maxY == Integer.MIN_VALUE) {
            return false;
        }
        return blockY <= maxY;
    }

    /** Original decay: each split generation is {@code base / generation}. */
    public static int splitPercent(int basePercent, int nextGeneration) {
        if (basePercent <= 0 || nextGeneration <= 0) {
            return 0;
        }
        return (int) (1.0D / nextGeneration * basePercent);
    }

    static EntityType<?> rollOverworldSkeleton(Mob original, ServerLevel level) {
        if (!WorldGate.isModuleActive(level, ID)) {
            return null;
        }
        if (level.dimension() != Level.OVERWORLD) {
            return null;
        }
        WorldConfig config = ConfigManager.world(level);
        if (!shouldReplaceNearBedrock(
                original.getBlockY(), config.blazeNearBedrockEnable(), config.blazeNearBedrockMaxY())) {
            return null;
        }
        if (!percentChance(original.getRandom(), config.blazeNearBedrockPercent())) {
            return null;
        }
        return EntityTypes.BLAZE;
    }

    static EntityType<?> rollNetherBlaze(Mob original, ServerLevel level) {
        if (!WorldGate.isModuleActive(level, ID)) {
            return null;
        }
        if (level.dimension() != Level.NETHER) {
            return null;
        }
        if (inFortress(level, original.blockPosition())) {
            return null;
        }
        WorldConfig config = ConfigManager.world(level);
        if (!percentChance(original.getRandom(), config.blazeBonusNetherPercent())) {
            return null;
        }
        maybeSpawnMagmaCube(original, level, config);
        return EntityTypes.BLAZE;
    }

    static boolean inFortress(ServerLevel level, BlockPos pos) {
        return level.structureManager()
                .getStructureWithPieceAt(pos, holder -> holder.is(BuiltinStructures.FORTRESS))
                .isValid();
    }

    static void maybeSpawnMagmaCube(Mob original, ServerLevel level, WorldConfig config) {
        if (!percentChance(original.getRandom(), config.magmaSpawnWithNetherBlazePercent())) {
            return;
        }
        Entity spawned = EntityTypes.MAGMA_CUBE.spawn(level, original.blockPosition(), EntitySpawnReason.EVENT);
        if (spawned instanceof MagmaCube cube) {
            cube.setSize(1, true);
            cube.snapTo(original.getX(), original.getY(), original.getZ(), original.getYRot(), original.getXRot());
            cube.setAttached(EhmAttachments.EHM_SPAWN_PROCESSED, true);
        }
    }

    private static boolean onAllowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof MagmaCube cube)) {
            return true;
        }
        if (!(entity.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, ID)) {
            return true;
        }
        if (!ConfigManager.world(level).magmaGrowIntoBlazesOnDamage()) {
            return true;
        }
        if (cube.isRemoved() || cube.isDeadOrDying()) {
            return true;
        }
        if (Boolean.TRUE.equals(cube.getAttachedOrElse(EhmAttachments.EHM_IGNORE, Boolean.FALSE))) {
            return true;
        }
        growIntoBlaze(cube, level);
        return false;
    }

    public static void growIntoBlaze(MagmaCube cube, ServerLevel level) {
        Vec3 origin = cube.position();
        cube.discard();
        new CreateExplosionTask(level, origin, ExplosionType.MAGMACUBE_FIRE, null, 0).run();
        Entity spawned = EntityTypes.BLAZE.spawn(level, BlockPos.containing(origin.x, origin.y + 2.0, origin.z), EntitySpawnReason.EVENT);
        if (spawned != null) {
            spawned.snapTo(origin.x, origin.y + 2.0, origin.z, cube.getYRot(), cube.getXRot());
            spawned.setAttached(EhmAttachments.EHM_SPAWN_PROCESSED, true);
        }
    }

    private static void onAfterDamage(
            LivingEntity entity, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked) {
        if (!(entity instanceof Blaze blaze) || blocked) {
            return;
        }
        if (!(entity.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, ID)) {
            return;
        }
        if (!ConfigManager.world(level).blazeDropFireOnDamage()) {
            return;
        }
        dropFire(blaze, level);
    }

    public static boolean shouldDropFire(float health, float maxHealth) {
        return maxHealth > 0.0F && health > maxHealth / 2.0F;
    }

    static void dropFire(Blaze blaze, ServerLevel level) {
        if (!shouldDropFire(blaze.getHealth(), blaze.getMaxHealth())) {
            return;
        }
        BlockPos below = blaze.blockPosition().below();
        for (int i = 0; i < 50; i++) {
            if (!level.getBlockState(below).isAir()) {
                break;
            }
            below = below.below();
        }
        BlockPos firePos = below.above();
        BlockState under = level.getBlockState(below);
        if (!level.getBlockState(firePos).isAir() || under.isAir() || under.liquid()) {
            return;
        }
        if (below.getY() <= level.getMinY()) {
            return;
        }
        level.setBlock(firePos, BaseFireBlock.getState(level, firePos), 3);
    }

    private static void onAfterDeath(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof Blaze blaze) || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (!WorldGate.isModuleActive(level, ID)) {
            return;
        }
        WorldConfig config = ConfigManager.world(level);
        if (level.dimension() == Level.OVERWORLD && config.explosions().custom(ExplosionType.OVERWORLD_BLAZE)) {
            new CreateExplosionTask(level, blaze.position(), ExplosionType.OVERWORLD_BLAZE, blaze, 0).run();
        }
        if (level.dimension() == Level.NETHER) {
            trySplit(blaze, level, config);
        }
    }

    static void trySplit(Blaze blaze, ServerLevel level, WorldConfig config) {
        int generation = blaze.getAttachedOrElse(EhmAttachments.EHM_BLAZE_SPLIT, 0) + 1;
        int chance = splitPercent(config.blazeNetherSplitPercent(), generation);
        if (!percentChance(blaze.getRandom(), chance)) {
            return;
        }
        boolean lootless = Boolean.TRUE.equals(blaze.getAttachedOrElse(EhmAttachments.EHM_LOOTLESS, Boolean.FALSE));
        spawnSplit(level, blaze, generation, lootless, new Vec3(1.0, 0.0, 1.0));
        spawnSplit(level, blaze, generation, lootless, new Vec3(-1.0, 0.0, -1.0));
    }

    private static void spawnSplit(ServerLevel level, Blaze parent, int generation, boolean lootless, Vec3 velocity) {
        Entity spawned = EntityTypes.BLAZE.spawn(level, parent.blockPosition(), EntitySpawnReason.EVENT);
        if (!(spawned instanceof Blaze child)) {
            return;
        }
        child.snapTo(parent.getX(), parent.getY(), parent.getZ(), parent.getYRot(), parent.getXRot());
        child.setDeltaMovement(velocity);
        child.setAttached(EhmAttachments.EHM_SPAWN_PROCESSED, true);
        child.setAttached(EhmAttachments.EHM_BLAZE_SPLIT, generation);
        if (lootless) {
            child.setAttached(EhmAttachments.EHM_LOOTLESS, true);
        }
    }

    private static void modifyDrops(
            net.minecraft.core.Holder<net.minecraft.world.level.storage.loot.LootTable> table,
            LootContext context,
            java.util.List<ItemStack> drops) {
        ServerLevel level = context.getLevel();
        if (!WorldGate.isModuleActive(level, ID)) {
            return;
        }
        Entity entity = context.getOptionalParameter(LootContextParams.THIS_ENTITY);
        if (!(entity instanceof Blaze blaze)) {
            return;
        }
        if (Boolean.TRUE.equals(blaze.getAttachedOrElse(EhmAttachments.EHM_LOOTLESS, Boolean.FALSE))) {
            drops.clear();
            return;
        }
        WorldConfig config = ConfigManager.world(level);
        if (level.dimension() == Level.OVERWORLD && config.blazeBlockOverworldDrops()) {
            drops.clear();
            return;
        }
        if (level.dimension() == Level.NETHER && config.blazeBonusLoot()) {
            drops.add(new ItemStack(Items.GUNPOWDER, 2));
            drops.add(new ItemStack(Items.BLAZE_ROD, 1));
        }
    }
}

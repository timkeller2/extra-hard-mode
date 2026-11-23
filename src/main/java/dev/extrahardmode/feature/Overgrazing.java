package dev.extrahardmode.feature;

import com.mojang.math.Transformation;
import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.mixin.DisplayAccess;
import dev.extrahardmode.mixin.ItemDisplayAccess;
import dev.extrahardmode.module.EntityHelper;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.WorldGate;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.pathfinder.Path;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Livestock eat a nearby grass block, turning it to dirt, the way a sheep does.
 * Sheep also keep their own grazing, which regrows wool. An animal that cannot
 * find a grass block may eat short grass from a reachable chest, then breeding
 * food, or starve. Milking a cow waits one day and adds one extra meal.
 */
public final class Overgrazing implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("overgrazing");
    private static final Map<Identifier, HerdSchedule> SCHEDULES = new HashMap<>();

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void onWorldUnload(ServerLevel level) {
        SCHEDULES.remove(level.dimension().identifier());
    }

    /**
     * The entity list is read once a minute. Each animal then runs on its own
     * tick inside that minute, so a herd does not pathfind on the census tick.
     */
    @Override
    public void serverTick(ServerLevel level) {
        Identifier dimension = level.dimension().identifier();
        if (!WorldGate.isModuleActive(level, ID)) {
            SCHEDULES.remove(dimension);
            return;
        }
        WorldConfig config = ConfigManager.world(level);
        if (!config.overgrazingEnable()) {
            SCHEDULES.remove(dimension);
            return;
        }
        long now = level.getGameTime();
        HerdSchedule schedule = SCHEDULES.computeIfAbsent(dimension, id -> new HerdSchedule());
        drain(level, schedule, now, config);
        if (schedule.enrolledAt < 0L || now - schedule.enrolledAt >= OvergrazingRules.CHECK_INTERVAL_TICKS) {
            enroll(level, schedule, now);
            drain(level, schedule, now, config);
        }
        drainMilkHunger(level, schedule, now, config);
    }

    private static void enroll(ServerLevel level, HerdSchedule schedule, long now) {
        schedule.enrolledAt = now;
        schedule.pending.clear();
        schedule.cursor = 0;
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Animal animal && isLivestock(animal)) {
                schedule.pending.add(new ScheduledCheck(
                        OvergrazingRules.nextCheckTick(now, animal.getId()), animal.getUUID()));
                noteMilkHunger(schedule, animal, now);
            }
        }
        schedule.pending.sort(Comparator.comparingLong(ScheduledCheck::runAt));
    }

    private static void drain(ServerLevel level, HerdSchedule schedule, long now, WorldConfig config) {
        while (schedule.cursor < schedule.pending.size()
                && schedule.pending.get(schedule.cursor).runAt() <= now) {
            ScheduledCheck check = schedule.pending.get(schedule.cursor++);
            if (level.getEntity(check.animalId()) instanceof Animal animal) {
                tickAnimal(level, animal, config);
            }
        }
    }

    private record ScheduledCheck(long runAt, UUID animalId) {}

    private static final class HerdSchedule {
        private long enrolledAt = -1L;
        private final List<ScheduledCheck> pending = new ArrayList<>();
        private int cursor;
        /** Extra-meal time for each cow milked while this dimension is loaded. */
        private final Map<UUID, Long> milkHunger = new HashMap<>();
    }

    /**
     * True when the client must not turn this bucket into milk. The ready time is
     * synced onto the cow. The server still decides whether the milk happens.
     */
    public static boolean blocksPredictedMilk(AbstractCow cow, Player player, InteractionHand hand) {
        if (cow == null || cow.isBaby() || !holdingBucket(player, hand)) {
            return false;
        }
        long readyAt = cow.getAttachedOrElse(EhmAttachments.EHM_MILK_READY_AT, 0L);
        return CowMilkRules.onCooldown(cow.level().getGameTime(), readyAt);
    }

    /**
     * True when this bucket use on an adult cow must not give milk. The cooldown
     * applies to every adult, including a named cow that does not overgraze.
     */
    public static boolean denyMilk(ServerLevel level, AbstractCow cow, Player player, InteractionHand hand) {
        if (!milkRules(level) || cow.isBaby() || !holdingBucket(player, hand)) {
            return false;
        }
        long readyAt = cow.getAttachedOrElse(EhmAttachments.EHM_MILK_READY_AT, 0L);
        return CowMilkRules.onCooldown(level.getGameTime(), readyAt);
    }

    /** True when this use is a bucket on an adult cow and the milk may proceed. */
    public static boolean milkAttempt(ServerLevel level, AbstractCow cow, Player player, InteractionHand hand) {
        return milkRules(level) && !cow.isBaby() && holdingBucket(player, hand);
    }

    /**
     * Starts the one-day cooldown. A cow the graze already feeds also gets one
     * extra meal a few seconds later. That meal does not move the usual timer.
     */
    public static void afterMilk(ServerLevel level, AbstractCow cow) {
        if (!milkRules(level) || cow.isBaby()) {
            return;
        }
        long now = level.getGameTime();
        cow.setAttached(EhmAttachments.EHM_MILK_READY_AT, CowMilkRules.nextReady(now));
        if (!isLivestock(cow)) {
            return;
        }
        long hungerAt = CowMilkRules.hungerAt(now, level.getRandom().nextInt());
        cow.setAttached(EhmAttachments.EHM_MILK_HUNGER_AT, hungerAt);
        HerdSchedule schedule = SCHEDULES.computeIfAbsent(level.dimension().identifier(), id -> new HerdSchedule());
        schedule.milkHunger.put(cow.getUUID(), hungerAt);
    }

    private static boolean milkRules(ServerLevel level) {
        return WorldGate.isModuleActive(level, ID) && ConfigManager.world(level).overgrazingEnable();
    }

    private static boolean holdingBucket(Player player, InteractionHand hand) {
        return player.getItemInHand(hand).is(Items.BUCKET);
    }

    private static void noteMilkHunger(HerdSchedule schedule, Animal animal, long now) {
        long hungerAt = animal.getAttachedOrElse(EhmAttachments.EHM_MILK_HUNGER_AT, 0L);
        if (hungerAt <= 0L) {
            return;
        }
        if (CowMilkRules.hungerStale(now, hungerAt)) {
            animal.setAttached(EhmAttachments.EHM_MILK_HUNGER_AT, 0L);
            schedule.milkHunger.remove(animal.getUUID());
            return;
        }
        schedule.milkHunger.put(animal.getUUID(), hungerAt);
    }

    /**
     * Extra meal after milking. It tries grass, then the same chest food, and
     * stops. It does not roll the usual chest chance or the starve chance, and
     * it does not write the usual bite time.
     */
    private static void drainMilkHunger(ServerLevel level, HerdSchedule schedule, long now, WorldConfig config) {
        if (schedule.milkHunger.isEmpty()) {
            return;
        }
        List<UUID> due = new ArrayList<>();
        for (Map.Entry<UUID, Long> entry : schedule.milkHunger.entrySet()) {
            if (CowMilkRules.hungerDue(now, entry.getValue())) {
                due.add(entry.getKey());
            }
        }
        for (UUID id : due) {
            Long scheduled = schedule.milkHunger.get(id);
            if (scheduled == null) {
                continue;
            }
            if (!(level.getEntity(id) instanceof Animal animal)) {
                if (CowMilkRules.hungerStale(now, scheduled)) {
                    schedule.milkHunger.remove(id);
                }
                continue;
            }
            schedule.milkHunger.remove(id);
            long stored = animal.getAttachedOrElse(EhmAttachments.EHM_MILK_HUNGER_AT, 0L);
            animal.setAttached(EhmAttachments.EHM_MILK_HUNGER_AT, 0L);
            if (stored <= 0L || CowMilkRules.hungerStale(now, stored) || !isLivestock(animal)) {
                continue;
            }
            if (!level.shouldTickBlocksAt(animal.blockPosition())) {
                continue;
            }
            eatBecauseMilked(level, animal, config);
        }
    }

    private static void eatBecauseMilked(ServerLevel level, Animal animal, WorldConfig config) {
        if (tryEatGrass(level, animal)) {
            return;
        }
        tryEatBreedingFood(level, animal, config.overgrazingChestRange());
    }

    static void tickAnimal(ServerLevel level, Animal animal, WorldConfig config) {
        if (!isLivestock(animal)) {
            return;
        }
        // Grass spreads only on a block random tick. Loaded chunks past simulation
        // distance still hold these animals, and eating there strips a pasture
        // that cannot regrow until the player comes back.
        if (!level.shouldTickBlocksAt(animal.blockPosition())) {
            return;
        }
        int interval = OvergrazingRules.eatIntervalTicks(
                isSmallLivestock(animal), config.overgrazingLargeBlocks(), config.overgrazingSmallBlocks());
        long now = level.getGameTime();
        long scheduled = animal.getAttachedOrElse(EhmAttachments.EHM_OVERGRAZE_AT, 0L);
        if (!OvergrazingRules.isDue(now, scheduled)) {
            if (OvergrazingRules.scheduledSoon(now, scheduled, interval)) {
                return;
            }
            animal.setAttached(
                    EhmAttachments.EHM_OVERGRAZE_AT,
                    OvergrazingRules.firstCheckAt(now, interval, level.getRandom().nextInt()));
            return;
        }
        if (OvergrazingRules.staleBite(now, scheduled)) {
            animal.setAttached(
                    EhmAttachments.EHM_OVERGRAZE_AT,
                    OvergrazingRules.firstCheckAt(now, interval, level.getRandom().nextInt()));
            return;
        }
        animal.setAttached(EhmAttachments.EHM_OVERGRAZE_AT, OvergrazingRules.reschedule(now, interval));
        if (tryEatGrass(level, animal)) {
            return;
        }
        if (!OvergrazingRules.lookInChests(level.getRandom().nextInt(100), config.overgrazingLookChance())) {
            return;
        }
        if (tryEatBreedingFood(level, animal, config.overgrazingChestRange())) {
            return;
        }
        if (OvergrazingRules.starve(level.getRandom().nextInt(100))) {
            starve(level, animal);
        }
    }

    static boolean isLivestock(Animal animal) {
        if (!animal.isAlive()
                || animal.isBaby()
                || animal.hasCustomName()
                || BiomeBosses.isBoss(animal)
                || Inhabitants.isInhabitant(animal)) {
            return false;
        }
        if (animal instanceof AbstractHorse
                || animal instanceof Bee
                || animal instanceof Cat
                || animal instanceof Wolf
                || animal instanceof Parrot) {
            return false;
        }
        return !(animal instanceof TamableAnimal tamable) || !tamable.isTame();
    }

    static boolean isSmallLivestock(Animal animal) {
        return animal instanceof Chicken || animal instanceof Rabbit;
    }

    static boolean tryEatGrass(ServerLevel level, Animal animal) {
        BlockPos grass = nearestReachableGrass(level, animal);
        if (grass == null) {
            return false;
        }
        eatGrassBlock(level, animal, grass);
        return true;
    }

    /** Closest grass block inside the graze radius that pathfinding can reach. */
    static BlockPos nearestReachableGrass(ServerLevel level, Animal animal) {
        int range = OvergrazingRules.DEFAULT_GRASS_RANGE;
        double rangeSq = (double) range * range;
        BlockPos origin = animal.blockPosition();
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos pos :
                BlockPos.betweenClosed(origin.offset(-range, -range, -range), origin.offset(range, range, range))) {
            if (!level.isLoaded(pos) || !level.getBlockState(pos).is(Blocks.GRASS_BLOCK)) {
                continue;
            }
            if (animal.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > rangeSq) {
                continue;
            }
            candidates.add(pos.immutable());
        }
        candidates.sort(Comparator.comparingDouble(
                pos -> animal.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)));
        int pathChecks = 0;
        for (BlockPos pos : candidates) {
            if (pathChecks >= OvergrazingRules.MAX_PATH_CHECKS) {
                break;
            }
            pathChecks++;
            if (canPathTo(animal, pos)) {
                return pos;
            }
        }
        return null;
    }

    /**
     * Turns the grass block into dirt, with the same particles sheep use. Sheep
     * do not regrow wool here; their own eat goal still does that.
     */
    static void eatGrassBlock(ServerLevel level, Animal animal, BlockPos pos) {
        if (level.getGameRules().get(GameRules.MOB_GRIEFING)) {
            BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
            level.levelEvent(2001, pos, Block.getId(grass));
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
            SoundType sound = grass.getSoundType();
            level.playSound(
                    null, pos, sound.getBreakSound(), SoundSource.BLOCKS, sound.getVolume(), sound.getPitch());
        }
        level.broadcastEntityEvent(animal, (byte) 10);
        animal.gameEvent(GameEvent.EAT);
        level.playSound(
                null,
                animal.blockPosition(),
                SoundEvents.GENERIC_EAT.value(),
                SoundSource.NEUTRAL,
                0.8F,
                1.0F + level.getRandom().nextFloat() * 0.2F);
    }

    static boolean tryEatBreedingFood(ServerLevel level, Animal animal, int chestRange) {
        LongOpenHashSet cached = new LongOpenHashSet(animal.getAttachedOrElse(
                EhmAttachments.EHM_OVERGRAZE_CHESTS, new LongOpenHashSet()));
        pruneChests(level, animal, cached, chestRange);
        if (eatFromCached(level, animal, cached)) {
            animal.setAttached(EhmAttachments.EHM_OVERGRAZE_CHESTS, cached);
            return true;
        }
        scanChests(level, animal, cached, chestRange);
        animal.setAttached(EhmAttachments.EHM_OVERGRAZE_CHESTS, cached);
        return eatFromCached(level, animal, cached);
    }

    static void pruneChests(ServerLevel level, Animal animal, LongOpenHashSet cached, int chestRange) {
        double rangeSq = (double) chestRange * chestRange;
        LongOpenHashSet keep = new LongOpenHashSet();
        cached.forEach((long packed) -> {
            BlockPos pos = BlockPos.of(packed);
            if (!level.isLoaded(pos)) {
                keep.add(packed);
                return;
            }
            if (!isStorage(level.getBlockEntity(pos))) {
                return;
            }
            if (animal.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > rangeSq) {
                return;
            }
            if (canPathTo(animal, pos)) {
                keep.add(packed);
            }
        });
        cached.clear();
        cached.addAll(keep);
    }

    static void scanChests(ServerLevel level, Animal animal, LongOpenHashSet cached, int chestRange) {
        BlockPos origin = animal.blockPosition();
        int minCx = origin.getX() - chestRange >> 4;
        int maxCx = origin.getX() + chestRange >> 4;
        int minCz = origin.getZ() - chestRange >> 4;
        int maxCz = origin.getZ() + chestRange >> 4;
        double rangeSq = (double) chestRange * chestRange;
        int pathChecks = 0;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(cx, cz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!isStorage(be)) {
                        continue;
                    }
                    BlockPos pos = be.getBlockPos();
                    if (cached.contains(pos.asLong())) {
                        continue;
                    }
                    if (animal.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > rangeSq) {
                        continue;
                    }
                    if (pathChecks >= OvergrazingRules.MAX_PATH_CHECKS) {
                        return;
                    }
                    pathChecks++;
                    if (canPathTo(animal, pos)) {
                        cached.add(pos.asLong());
                    }
                }
            }
        }
    }

    static boolean eatFromCached(ServerLevel level, Animal animal, LongOpenHashSet cached) {
        List<Container> containers = new ArrayList<>();
        List<Integer> slotIndexes = new ArrayList<>();
        List<Boolean> storedGrass = new ArrayList<>();
        List<Boolean> breedingFood = new ArrayList<>();
        for (long packed : cached) {
            BlockPos pos = BlockPos.of(packed);
            if (!level.isLoaded(pos)) {
                continue;
            }
            Container container = containerAt(level, pos);
            if (container == null) {
                continue;
            }
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                boolean grass = stack.is(Items.SHORT_GRASS);
                boolean breeding = animal.isFood(stack);
                if (!grass && !breeding) {
                    continue;
                }
                containers.add(container);
                slotIndexes.add(slot);
                storedGrass.add(grass);
                breedingFood.add(breeding);
            }
        }
        int chosen = OvergrazingRules.preferredChestIndex(flags(storedGrass), flags(breedingFood));
        if (chosen < 0) {
            return false;
        }
        Container container = containers.get(chosen);
        ItemStack stack = container.getItem(slotIndexes.get(chosen));
        if (stack.isEmpty()) {
            return false;
        }
        ItemStack shown = stack.copyWithCount(1);
        stack.shrink(1);
        container.setItem(slotIndexes.get(chosen), stack);
        container.setChanged();
        spawnEatenFood(level, animal, shown);
        return true;
    }

    static boolean[] flags(List<Boolean> values) {
        boolean[] flags = new boolean[values.size()];
        for (int i = 0; i < values.size(); i++) {
            flags[i] = values.get(i);
        }
        return flags;
    }

    static boolean isStorage(BlockEntity be) {
        return be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity;
    }

    static Container containerAt(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock chest) {
            return ChestBlock.getContainer(chest, state, level, pos, true);
        }
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof Container container ? container : null;
    }

    static boolean canPathTo(Animal animal, BlockPos pos) {
        Path path = animal.getNavigation().createPath(pos, 1);
        return path != null && path.canReach();
    }

    static void spawnEatenFood(ServerLevel level, Animal animal, ItemStack food) {
        Display.ItemDisplay display = EntityTypes.ITEM_DISPLAY.create(level, EntitySpawnReason.EVENT);
        if (display == null) {
            return;
        }
        display.snapTo(
                animal.getX(),
                animal.getY() + animal.getBbHeight() * 0.6,
                animal.getZ(),
                animal.getYRot(),
                0.0F);
        ((ItemDisplayAccess) display).tougher$setItemStack(food);
        ((ItemDisplayAccess) display).tougher$setItemTransform(ItemDisplayContext.GROUND);
        DisplayAccess access = (DisplayAccess) display;
        access.tougher$setBillboardConstraints(Display.BillboardConstraints.CENTER);
        access.tougher$setTransformation(new Transformation(
                new Vector3f(),
                new Quaternionf(),
                new Vector3f(0.45F, 0.45F, 0.45F),
                new Quaternionf()));
        display.setAttached(EhmAttachments.EHM_FLOAT_TEXT, OvergrazingRules.FOOD_FLOAT_TICKS);
        level.addFreshEntity(display);
        level.playSound(
                null,
                animal.blockPosition(),
                SoundEvents.GENERIC_EAT.value(),
                SoundSource.NEUTRAL,
                0.8F,
                1.0F + level.getRandom().nextFloat() * 0.2F);
    }

    static void starve(ServerLevel level, Animal animal) {
        EntityHelper.markLootless(animal);
        animal.hurtServer(level, level.damageSources().starve(), Float.MAX_VALUE);
        if (animal.isAlive()) {
            animal.kill(level);
        }
    }
}

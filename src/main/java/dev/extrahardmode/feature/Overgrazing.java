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
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
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
import net.minecraft.world.entity.animal.chicken.Chicken;
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
 * food, or starve.
 */
public final class Overgrazing implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("overgrazing");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (level.getGameTime() % OvergrazingRules.TICK_STRIDE != 0L) {
            return;
        }
        if (!WorldGate.isModuleActive(level, ID)) {
            return;
        }
        WorldConfig config = ConfigManager.world(level);
        if (!config.overgrazingEnable()) {
            return;
        }
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Animal animal) {
                tickAnimal(level, animal, config);
            }
        }
    }

    static void tickAnimal(ServerLevel level, Animal animal, WorldConfig config) {
        if (!isLivestock(animal)) {
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
        if (animal instanceof AbstractHorse || animal instanceof Cat || animal instanceof Wolf || animal instanceof Parrot) {
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

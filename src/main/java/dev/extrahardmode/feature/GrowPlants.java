package dev.extrahardmode.feature;

import dev.extrahardmode.mixin.StemBlockAccess;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

/** One-stage plant growth for the Let it grow ability. */
public final class GrowPlants {
    private static final int COLUMN_MAX = 3;

    private GrowPlants() {}

    public static List<BlockPos> findGrowable(ServerLevel level, BlockPos origin, int range) {
        List<BlockPos> found = new ArrayList<>();
        BlockPos min = origin.offset(-range, -range, -range);
        BlockPos max = origin.offset(range, range, range);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (canGrow(level, pos.immutable())) {
                found.add(pos.immutable());
            }
        }
        return found;
    }

    public static int apply(ServerLevel level, BlockPos origin, int range, int times) {
        return apply(level, origin, range, times, Set.of());
    }

    public static int apply(ServerLevel level, BlockPos origin, int range, int times, Set<BlockPos> exclude) {
        List<BlockPos> growable = findGrowable(level, origin, range);
        if (exclude != null && !exclude.isEmpty()) {
            growable.removeIf(exclude::contains);
        }
        List<BlockPos> targets = AbilityRules.pickDistinctRandom(
                growable, times, bound -> level.getRandom().nextInt(bound));
        int grown = 0;
        for (BlockPos pos : targets) {
            if (growOneStage(level, pos)) {
                level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos, 0);
                AntiFarming.onLetItGrow(level, pos);
                grown++;
            }
        }
        return grown;
    }

    /**
     * Grow {@code target} by {@code stages}. Leftover stages after it matures go
     * to the nearest other growable plant within {@code range} of the target,
     * repeating until stages are spent.
     */
    public static int applyFocused(ServerLevel level, BlockPos target, int range, int stages, Set<BlockPos> grown) {
        int remaining = Math.max(0, stages);
        int applied = 0;
        Set<BlockPos> used = new HashSet<>();
        BlockPos current = target;
        if (!canGrow(level, current)) {
            used.add(current);
            current = nearestGrowable(level, target, range, used);
        }
        while (remaining > 0 && current != null) {
            int usedHere = growStages(level, current, remaining);
            if (usedHere > 0) {
                applied += usedHere;
                remaining -= usedHere;
                grown.add(current);
                AntiFarming.onLetItGrow(level, current);
            }
            used.add(current);
            if (remaining <= 0) {
                break;
            }
            current = nearestGrowable(level, target, range, used);
        }
        return applied;
    }

    static int growStages(ServerLevel level, BlockPos pos, int stages) {
        int grown = 0;
        for (int i = 0; i < stages; i++) {
            if (!growOneStage(level, pos)) {
                break;
            }
            level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos, 0);
            grown++;
        }
        return grown;
    }

    static BlockPos nearestGrowable(ServerLevel level, BlockPos origin, int range, Set<BlockPos> exclude) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : findGrowable(level, origin, range)) {
            if (exclude != null && exclude.contains(pos)) {
                continue;
            }
            double dist = pos.distSqr(origin);
            if (best == null || dist < bestDist) {
                best = pos;
                bestDist = dist;
            }
        }
        return best;
    }

    static boolean isPlant(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof SugarCaneBlock || block instanceof CactusBlock) {
            return true;
        }
        if (state.hasProperty(BlockStateProperties.STAGE)) {
            return true;
        }
        if (block instanceof CropBlock) {
            return true;
        }
        return ageProperty(state) != null;
    }

    static boolean canGrow(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof SugarCaneBlock || block instanceof CactusBlock) {
            return canGrowColumn(level, pos, state, block);
        }
        if (block instanceof StemBlock) {
            int age = state.getValue(StemBlock.AGE);
            if (age < StemBlock.MAX_AGE) {
                return true;
            }
            return canGrowStemFruit(level, pos, (StemBlock) block);
        }
        if (state.hasProperty(BlockStateProperties.STAGE)
                && state.getValue(BlockStateProperties.STAGE) == 0) {
            return true;
        }
        if (block instanceof CropBlock crop) {
            return !crop.isMaxAge(state);
        }
        IntegerProperty age = ageProperty(state);
        if (age == null) {
            return false;
        }
        return state.getValue(age) < maxAge(age);
    }

    static boolean growOneStage(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof SugarCaneBlock || block instanceof CactusBlock) {
            return growColumn(level, pos, state, block);
        }
        if (block instanceof StemBlock stem) {
            int age = state.getValue(StemBlock.AGE);
            if (age < StemBlock.MAX_AGE) {
                int next = age + 1;
                if (!StemBlock.AGE.getPossibleValues().contains(next)) {
                    return false;
                }
                level.setBlock(pos, state.setValue(StemBlock.AGE, next), Block.UPDATE_CLIENTS);
                return true;
            }
            return growStemFruit(level, pos, stem);
        }
        if (state.hasProperty(BlockStateProperties.STAGE) && state.getValue(BlockStateProperties.STAGE) == 0) {
            level.setBlock(pos, state.setValue(BlockStateProperties.STAGE, 1), Block.UPDATE_CLIENTS);
            return true;
        }
        if (block instanceof CropBlock crop && !crop.isMaxAge(state)) {
            level.setBlock(pos, crop.getStateForAge(crop.getAge(state) + 1), Block.UPDATE_CLIENTS);
            return true;
        }
        IntegerProperty age = ageProperty(state);
        if (age == null) {
            return false;
        }
        int current = state.getValue(age);
        int next = current + 1;
        if (!age.getPossibleValues().contains(next)) {
            return false;
        }
        level.setBlock(pos, state.setValue(age, next), Block.UPDATE_CLIENTS);
        return true;
    }

    static boolean canGrowStemFruit(ServerLevel level, BlockPos pos, StemBlock stem) {
        StemBlockAccess access = (StemBlockAccess) stem;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos fruitPos = pos.relative(direction);
            if (level.getBlockState(fruitPos).isAir()
                    && level.getBlockState(fruitPos.below()).is(access.tougher$fruitSupportBlocks())) {
                return true;
            }
        }
        return false;
    }

    static boolean growStemFruit(ServerLevel level, BlockPos pos, StemBlock stem) {
        StemBlockAccess access = (StemBlockAccess) stem;
        Block fruit = resolveBlock(level, access.tougher$fruit());
        Block attached = resolveBlock(level, access.tougher$attachedStem());
        if (fruit == null || attached == null) {
            return false;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos fruitPos = pos.relative(direction);
            if (!level.getBlockState(fruitPos).isAir()
                    || !level.getBlockState(fruitPos.below()).is(access.tougher$fruitSupportBlocks())) {
                continue;
            }
            BlockState attachedState = attached.defaultBlockState();
            if (attachedState.hasProperty(AttachedStemBlock.FACING)) {
                attachedState = attachedState.setValue(AttachedStemBlock.FACING, direction);
            }
            level.setBlock(fruitPos, fruit.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(pos, attachedState, Block.UPDATE_CLIENTS);
            AntiFarming.onStemFruitAppeared(level, pos);
            return true;
        }
        return false;
    }

    private static Block resolveBlock(ServerLevel level, ResourceKey<Block> key) {
        if (key == null) {
            return null;
        }
        Optional<Holder.Reference<Block>> holder = level.registryAccess().lookupOrThrow(Registries.BLOCK).get(key);
        return holder.map(Holder.Reference::value).orElse(null);
    }

    private static boolean canGrowColumn(ServerLevel level, BlockPos pos, BlockState state, Block block) {
        if (!level.getBlockState(pos.above()).isAir()) {
            return false;
        }
        if (columnHeight(level, pos, block) >= COLUMN_MAX) {
            return false;
        }
        IntegerProperty age = ageProperty(state);
        return age != null;
    }

    private static boolean growColumn(ServerLevel level, BlockPos pos, BlockState state, Block block) {
        if (!canGrowColumn(level, pos, state, block)) {
            return false;
        }
        IntegerProperty age = ageProperty(state);
        if (age == null) {
            return false;
        }
        int current = state.getValue(age);
        int max = maxAge(age);
        if (current >= max) {
            level.setBlock(pos.above(), block.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(pos, state.setValue(age, 0), Block.UPDATE_CLIENTS);
            return true;
        }
        level.setBlock(pos, state.setValue(age, current + 1), Block.UPDATE_CLIENTS);
        return true;
    }

    private static int columnHeight(ServerLevel level, BlockPos pos, Block block) {
        int height = 1;
        BlockPos below = pos.below();
        while (level.getBlockState(below).is(block) && height < COLUMN_MAX + 1) {
            height++;
            below = below.below();
        }
        return height;
    }

    static IntegerProperty ageProperty(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if ("age".equals(property.getName()) && property instanceof IntegerProperty age) {
                return age;
            }
        }
        return null;
    }

    static int maxAge(IntegerProperty age) {
        int max = 0;
        for (int value : age.getPossibleValues()) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }
}

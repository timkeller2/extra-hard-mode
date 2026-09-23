package dev.extrahardmode.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

/** Beds placed in the Nether or the End are removed. They do not explode. */
public final class UnsafeBeds {
    private UnsafeBeds() {}

    public static boolean vanishes(boolean netherOrEnd, boolean bed) {
        return netherOrEnd && bed;
    }

    public static boolean hostileDimension(Level level) {
        return level != null && (level.dimension() == Level.NETHER || level.dimension() == Level.END);
    }

    public static void remove(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof AbstractBedBlock)) {
            return;
        }
        BlockPos other = otherHalf(pos, state);
        level.removeBlock(pos, false);
        if (level.getBlockState(other).getBlock() instanceof AbstractBedBlock) {
            level.removeBlock(other, false);
        }
    }

    static BlockPos otherHalf(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(AbstractBedBlock.FACING);
        BedPart part = state.getValue(AbstractBedBlock.PART);
        return part == BedPart.FOOT ? pos.relative(facing) : pos.relative(facing.getOpposite());
    }
}

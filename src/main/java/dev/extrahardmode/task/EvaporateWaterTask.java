package dev.extrahardmode.task;

import dev.extrahardmode.feature.Water;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * Converts a leftover source at a marked pos to flowing LEVEL=1.
 * Never un-waterlogs (upstream slab bug).
 */
public final class EvaporateWaterTask implements Runnable {
    private final ServerLevel level;
    private final BlockPos pos;

    public EvaporateWaterTask(ServerLevel level, BlockPos pos) {
        this.level = level;
        this.pos = pos.immutable();
    }

    @Override
    public void run() {
        convertIfSource(level, pos);
    }

    public static void convertIfSource(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)) {
            return;
        }
        if (!(state.getBlock() instanceof LiquidBlock)) {
            return;
        }
        FluidState fluid = state.getFluidState();
        if (fluid.isEmpty() || !fluid.getType().isSame(Fluids.WATER) || !fluid.isSource()) {
            return;
        }
        level.setBlock(pos, Water.flowingLevel1(), Block.UPDATE_ALL);
    }
}

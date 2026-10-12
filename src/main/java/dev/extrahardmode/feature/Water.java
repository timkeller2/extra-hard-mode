package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.world.WorldGate;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public final class Water implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("water_sources");
    private static final int MARK_TTL_TICKS = 40;
    private static final Map<Identifier, Long2LongOpenHashMap> MARKS = new ConcurrentHashMap<>();

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        bus.listen(UseBlockCallback.EVENT, ID, Water::onUseBlock);
    }

    static InteractionResult onUseBlock(
            Player player, Level level, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !WorldGate.isModuleActive(server, ID)) {
            return InteractionResult.PASS;
        }
        if (!enabled(server)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer && EhmApi.playerBypasses(serverPlayer)) {
            return InteractionResult.PASS;
        }
        Item item = player.getItemInHand(hand).getItem();
        if (item != Items.KELP && item != Items.SEAGRASS) {
            return InteractionResult.PASS;
        }
        BlockPos clicked = hit.getBlockPos();
        BlockPos placed = clicked.relative(hit.getDirection());
        if (isMarked(server, clicked) || isMarked(server, placed)) {
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onWorldUnload(ServerLevel level) {
        MARKS.remove(level.dimension().identifier());
    }

    @Override
    public void serverTick(ServerLevel level) {
        Long2LongOpenHashMap marks = MARKS.get(level.dimension().identifier());
        if (marks == null || marks.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Long2LongMap.Entry> it = marks.long2LongEntrySet().iterator();
        while (it.hasNext()) {
            Long2LongMap.Entry entry = it.next();
            if (entry.getLongValue() <= now) {
                it.remove();
            }
        }
    }

    public static boolean enabled(ServerLevel level) {
        return restricts(level, Fluids.WATER);
    }

    public static boolean restricts(Level level, Fluid fluid) {
        if (!(isWater(fluid) || isLava(fluid))) {
            return false;
        }
        if (level instanceof ServerLevel server) {
            if (!WorldGate.isModuleActive(server, ID)) {
                return false;
            }
            return isLava(fluid)
                    ? ConfigManager.world(server).bucketsDontMoveLavaSources()
                    : ConfigManager.world(server).bucketsDontMoveSources();
        }
        return level.isClientSide();
    }

    /**
     * Server: WorldGate + toggle. Client: rewrite so the first predicted block is LEVEL=1 (no source flicker).
     */
    public static boolean rewritePlacement(Level level) {
        return restricts(level, Fluids.WATER);
    }

    /** A real source is allowed when the cell cannot flow and does not touch a source. */
    public static boolean allowsContainedSource(Level level, BlockPos pos, Fluid fluid) {
        if (level == null || pos == null || fluid == null) {
            return false;
        }
        boolean adjacentSource = false;
        boolean downClosed = false;
        boolean northClosed = false;
        boolean southClosed = false;
        boolean eastClosed = false;
        boolean westClosed = false;
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            BlockState state = level.getBlockState(neighbor);
            FluidState fluidState = state.getFluidState();
            if (fluidState.isSource()) {
                adjacentSource = true;
            }
            if (direction == Direction.UP) {
                continue;
            }
            boolean closed = !canFlowInto(level, neighbor, state, fluid);
            if (direction == Direction.DOWN) {
                downClosed = closed;
            } else if (direction == Direction.NORTH) {
                northClosed = closed;
            } else if (direction == Direction.SOUTH) {
                southClosed = closed;
            } else if (direction == Direction.EAST) {
                eastClosed = closed;
            } else if (direction == Direction.WEST) {
                westClosed = closed;
            }
        }
        BlockState here = level.getBlockState(pos);
        return SourceMoveRules.allowsSource(
                downClosed,
                northClosed,
                southClosed,
                eastClosed,
                westClosed,
                adjacentSource,
                isReceptacle(level, pos, here, fluid));
    }

    /** True when this fluid could enter the neighbor, so that side is not a seal. */
    private static boolean canFlowInto(Level level, BlockPos pos, BlockState state, Fluid fluid) {
        if (!state.getFluidState().isEmpty()) {
            return true;
        }
        if (state.getBlock() instanceof LiquidBlockContainer container
                && container.canPlaceLiquid(null, level, pos, state, fluid)) {
            return true;
        }
        return state.is(BlockTags.WASHED_AWAY_BY_FLUIDS);
    }

    /**
     * A waterloggable block, including one this placement just filled. Cauldrons are not containers;
     * their own interaction still accepts a source.
     */
    private static boolean isReceptacle(Level level, BlockPos pos, BlockState state, Fluid fluid) {
        if (!(state.getBlock() instanceof LiquidBlockContainer container)) {
            return false;
        }
        if (container.canPlaceLiquid(null, level, pos, state, fluid)) {
            return true;
        }
        FluidState fluidState = state.getFluidState();
        return fluidState.isSource() && fluid.isSame(fluidState.getType());
    }

    public static void mark(ServerLevel level, BlockPos pos) {
        Long2LongOpenHashMap marks =
                MARKS.computeIfAbsent(level.dimension().identifier(), id -> new Long2LongOpenHashMap());
        marks.put(pos.asLong(), level.getGameTime() + MARK_TTL_TICKS);
    }

    public static boolean isMarked(ServerLevel level, BlockPos pos) {
        Long2LongOpenHashMap marks = MARKS.get(level.dimension().identifier());
        if (marks == null) {
            return false;
        }
        long expiry = marks.get(pos.asLong());
        return expiry != 0 && expiry > level.getGameTime();
    }

    public static BlockState flowingLevel1() {
        return flowingBlock(Fluids.WATER);
    }

    public static BlockState flowingBlock(Fluid fluid) {
        BlockState state = isLava(fluid) ? Blocks.LAVA.defaultBlockState() : Blocks.WATER.defaultBlockState();
        return state.setValue(LiquidBlock.LEVEL, 1);
    }

    public static FluidState flowingFluid() {
        return flowingFluid(Fluids.WATER);
    }

    public static FluidState flowingFluid(Fluid fluid) {
        return (isLava(fluid) ? Fluids.FLOWING_LAVA : Fluids.FLOWING_WATER).getFlowing(7, false);
    }

    public static boolean isWater(Fluid fluid) {
        return fluid != null && (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER || fluid.isSame(Fluids.WATER));
    }

    public static boolean isLava(Fluid fluid) {
        return fluid != null && (fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA || fluid.isSame(Fluids.LAVA));
    }

    public static Fluid fluidOfBucket(Item item) {
        if (isWaterBucket(item)) {
            return Fluids.WATER;
        }
        if (item == Items.LAVA_BUCKET) {
            return Fluids.LAVA;
        }
        return null;
    }

    public static boolean isWaterBucket(Item item) {
        return item == Items.WATER_BUCKET
                || item == Items.COD_BUCKET
                || item == Items.SALMON_BUCKET
                || item == Items.TROPICAL_FISH_BUCKET
                || item == Items.PUFFERFISH_BUCKET
                || item == Items.AXOLOTL_BUCKET
                || item == Items.TADPOLE_BUCKET;
    }
}

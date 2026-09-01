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
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
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
        return WorldGate.isModuleActive(level, ID) && ConfigManager.world(level).bucketsDontMoveSources();
    }

    /**
     * Server: WorldGate + toggle. Client: rewrite so the first predicted block is LEVEL=1 (no source flicker).
     */
    public static boolean rewritePlacement(Level level) {
        return level instanceof ServerLevel server
                ? WorldGate.isModuleActive(server, ID) && ConfigManager.world(server).bucketsDontMoveSources()
                : level.isClientSide();
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
        return Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1);
    }

    public static FluidState flowingFluid() {
        return Fluids.FLOWING_WATER.getFlowing(7, false);
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

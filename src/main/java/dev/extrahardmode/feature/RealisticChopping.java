package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.tag.EhmTags;
import dev.extrahardmode.task.FallingLogsTask;
import dev.extrahardmode.world.WorldGate;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class RealisticChopping implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("realistic_chopping");

    private static final TagKey<Block> OAK_LOGS = mcBlockTag("oak_logs");
    private static final TagKey<Block> SPRUCE_LOGS = mcBlockTag("spruce_logs");
    private static final TagKey<Block> BIRCH_LOGS = mcBlockTag("birch_logs");
    private static final TagKey<Block> JUNGLE_LOGS = mcBlockTag("jungle_logs");
    private static final TagKey<Block> ACACIA_LOGS = mcBlockTag("acacia_logs");
    private static final TagKey<Block> DARK_OAK_LOGS = mcBlockTag("dark_oak_logs");
    private static final TagKey<Block> MANGROVE_LOGS = mcBlockTag("mangrove_logs");
    private static final TagKey<Block> CHERRY_LOGS = mcBlockTag("cherry_logs");
    private static final TagKey<Block> PALE_OAK_LOGS = mcBlockTag("pale_oak_logs");

    private static final List<TagKey<Block>> WOOD_TAGS = List.of(
            OAK_LOGS,
            SPRUCE_LOGS,
            BIRCH_LOGS,
            JUNGLE_LOGS,
            ACACIA_LOGS,
            DARK_OAK_LOGS,
            MANGROVE_LOGS,
            CHERRY_LOGS,
            PALE_OAK_LOGS);

    private static TagKey<Block> mcBlockTag(String path) {
        return TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace(path));
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        bus.listen(PlayerBlockBreakEvents.AFTER, ID, RealisticChopping::onBreak);
    }

    public static boolean enabled(Level level) {
        return FeatureBus.guard(level, ID)
                && level instanceof ServerLevel serverLevel
                && ConfigManager.world(serverLevel).betterTreeFelling();
    }

    /** GameTests call this after the broken log is already air. */
    public static void tryFell(ServerLevel level, Player player, BlockPos broken, BlockState brokenState) {
        if (!WorldGate.isModuleActive(level, ID)) {
            return;
        }
        if (!ConfigManager.world(level).betterTreeFelling()) {
            return;
        }
        if (!brokenState.is(EhmTags.FELLABLE_LOGS)) {
            return;
        }
        if (player instanceof ServerPlayer serverPlayer && EhmApi.playerBypasses(serverPlayer)) {
            return;
        }
        if (player.hasInfiniteMaterials() || player.isCreative()) {
            return;
        }
        TagKey<Block> wood = matchingVanillaLogTag(brokenState);
        List<BlockPos> logs = collectLogs(level, broken, brokenState, wood);
        if (logs.size() < TreeFellLimits.MIN_LOGS) {
            return;
        }
        if (countAdjacentLeaves(level, logs, wood) < TreeFellLimits.MIN_ADJACENT_LEAVES) {
            return;
        }
        FallingLogsTask.enqueue(level, logs, broken);
    }

    public static TagKey<Block> matchingVanillaLogTag(BlockState state) {
        for (TagKey<Block> tag : WOOD_TAGS) {
            if (state.is(tag)) {
                return tag;
            }
        }
        return null;
    }

    public static boolean sameWood(BlockState origin, BlockState other, TagKey<Block> wood) {
        if (wood != null) {
            return other.is(wood);
        }
        return other.getBlock() == origin.getBlock();
    }

    public static boolean matchingLeaves(BlockState state, TagKey<Block> wood) {
        if (wood == null) {
            return false;
        }
        if (wood.equals(OAK_LOGS)) {
            return state.is(Blocks.OAK_LEAVES)
                    || state.is(Blocks.AZALEA_LEAVES)
                    || state.is(Blocks.FLOWERING_AZALEA_LEAVES);
        }
        if (wood.equals(SPRUCE_LOGS)) {
            return state.is(Blocks.SPRUCE_LEAVES);
        }
        if (wood.equals(BIRCH_LOGS)) {
            return state.is(Blocks.BIRCH_LEAVES);
        }
        if (wood.equals(JUNGLE_LOGS)) {
            return state.is(Blocks.JUNGLE_LEAVES);
        }
        if (wood.equals(ACACIA_LOGS)) {
            return state.is(Blocks.ACACIA_LEAVES);
        }
        if (wood.equals(DARK_OAK_LOGS)) {
            return state.is(Blocks.DARK_OAK_LEAVES);
        }
        if (wood.equals(MANGROVE_LOGS)) {
            return state.is(Blocks.MANGROVE_LEAVES);
        }
        if (wood.equals(CHERRY_LOGS)) {
            return state.is(Blocks.CHERRY_LEAVES);
        }
        if (wood.equals(PALE_OAK_LOGS)) {
            return state.is(Blocks.PALE_OAK_LEAVES);
        }
        return false;
    }

    static List<BlockPos> collectLogs(
            ServerLevel level,
            BlockPos origin,
            BlockState originState,
            TagKey<Block> wood) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        LongOpenHashSet seen = new LongOpenHashSet();
        List<BlockPos> logs = new ArrayList<>();
        queue.add(origin);
        seen.add(origin.asLong());
        logs.add(origin);
        while (!queue.isEmpty() && logs.size() < TreeFellLimits.MAX_LOGS) {
            BlockPos current = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (!seen.add(next.asLong())) {
                    continue;
                }
                if (!TreeFellLimits.inBounds(
                        origin.getX(), origin.getY(), origin.getZ(), next.getX(), next.getY(), next.getZ())) {
                    continue;
                }
                BlockState state = level.getBlockState(next);
                if (!state.is(EhmTags.FELLABLE_LOGS) || !sameWood(originState, state, wood)) {
                    continue;
                }
                logs.add(next);
                queue.add(next);
                if (logs.size() >= TreeFellLimits.MAX_LOGS) {
                    break;
                }
            }
        }
        return logs;
    }

    static int countAdjacentLeaves(
            ServerLevel level, List<BlockPos> logs, TagKey<Block> wood) {
        LongOpenHashSet logSet = new LongOpenHashSet(logs.size());
        for (BlockPos log : logs) {
            logSet.add(log.asLong());
        }
        LongOpenHashSet counted = new LongOpenHashSet();
        int leaves = 0;
        for (BlockPos log : logs) {
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = log.relative(direction);
                if (logSet.contains(neighbor.asLong()) || !counted.add(neighbor.asLong())) {
                    continue;
                }
                if (matchingLeaves(level.getBlockState(neighbor), wood)) {
                    leaves++;
                }
            }
        }
        return leaves;
    }

    private static void onBreak(Level world, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (!(world instanceof ServerLevel level)) {
            return;
        }
        if (!WorldGate.isModuleActive(level, ID)) {
            return;
        }
        tryFell(level, player, pos, state);
    }
}

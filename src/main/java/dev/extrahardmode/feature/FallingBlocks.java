package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.GlobalConfig;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.module.PhysicsBudget;
import dev.extrahardmode.module.PhysicsQueue;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.tag.EhmTags;
import dev.extrahardmode.world.PhysicsSkip;
import dev.extrahardmode.world.WorldGate;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class FallingBlocks implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("falling_blocks");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        bus.listen(PlayerBlockBreakEvents.AFTER, ID, FallingBlocks::onBreak);
    }

    public static boolean enabled(Level level) {
        return FeatureBus.guard(level, ID)
                && level instanceof ServerLevel serverLevel
                && ConfigManager.world(serverLevel).fallingEnable();
    }

    public static void onPlaced(BlockPlaceContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!enabled(level)) {
            return;
        }
        BlockPos placed = context.getClickedPos();
        enqueueIfFalling(level, placed, placed, 0, false);
    }

    public static void onRemoved(FallingBlockEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        PhysicsQueue.of(level).markLanded(entity);
        RealisticChopping.dropUnsupportedLogs(level, entity.blockPosition());
        if (!Boolean.TRUE.equals(entity.getAttachedOrElse(EhmAttachments.EHM_OURS, Boolean.FALSE))) {
            return;
        }
        if (!enabled(level) && !CaveIns.enabled(level)) {
            return;
        }
        WorldConfig config = ConfigManager.world(level);
        BlockPos pos = entity.blockPosition();
        BlockPos chainOrigin = chainOrigin(entity, pos);
        boolean caveIn = Boolean.TRUE.equals(entity.getAttachedOrElse(EhmAttachments.EHM_CAVE_IN_CHAIN, Boolean.FALSE));
        if (config.fallingBreakTorches()) {
            breakColumn(level, pos);
        }
        if (config.fallingCascade()) {
            cascade(level, pos, chainOrigin, caveIn);
        }
    }

    /**
     * A hole just opened. Adjacent loose blocks, and stone when this fall is a cave-in,
     * are queued a few ticks later. The walk stops 50 blocks from {@code chainOrigin}.
     */
    public static void onBeganFalling(ServerLevel level, BlockPos pos, BlockPos chainOrigin, boolean caveIn) {
        RealisticChopping.dropUnsupportedLogs(level, pos);
        BlockPos origin = chainOrigin == null ? pos : chainOrigin;
        if (caveIn) {
            CaveIns.spread(level, pos, origin);
        }
        if (!ConfigManager.world(level).fallingCascade()) {
            return;
        }
        if (!enabled(level) && !CaveIns.enabled(level)) {
            return;
        }
        enqueueAround(level, pos, origin, PhysicsChainRules.HOP_DELAY_TICKS, caveIn);
    }

    public static void applyConfiguredDamage(FallingBlockEntity entity, double fallDistance) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (!appliesFallDamage(entity)) {
            return;
        }
        if (!PhysicsBudget.farEnoughToHurt(fallDistance)) {
            return;
        }
        float amount = ConfigManager.world(level).fallingDamage();
        if (amount <= 0.0F) {
            return;
        }
        var source = entity.damageSources().fallingBlock(entity);
        var targets = EntitySelector.NO_CREATIVE_OR_SPECTATOR.and(EntitySelector.LIVING_ENTITY_STILL_ALIVE);
        for (var hit : level.getEntities(entity, entity.getBoundingBox(), targets)) {
            hit.hurtServer(level, source, amount);
        }
    }

    /**
     * Extra damage only for {@code #extra_falling}, cave-in cobble/cobbled_deepslate, or EHM-spawned
     * fellable logs. Anvils, pointed dripstone, and sulfur spikes stay vanilla.
     */
    public static boolean appliesFallDamage(FallingBlockEntity entity) {
        BlockState state = entity.getBlockState();
        if (isVanillaFallDamage(state)) {
            return false;
        }
        boolean ours = Boolean.TRUE.equals(entity.getAttachedOrElse(EhmAttachments.EHM_OURS, Boolean.FALSE));
        if (ours && state.is(EhmTags.FELLABLE_LOGS) && RealisticChopping.enabled(entity.level())) {
            return true;
        }
        boolean tagged = isCaveInProduct(state) || state.is(EhmTags.EXTRA_FALLING);
        if (ours) {
            return tagged && (CaveIns.enabled(entity.level()) || enabled(entity.level()));
        }
        return enabled(entity.level()) && state.is(EhmTags.EXTRA_FALLING);
    }

    public static boolean isCaveInProduct(BlockState state) {
        return state.is(Blocks.COBBLESTONE) || state.is(Blocks.COBBLED_DEEPSLATE);
    }

    public static boolean isVanillaFallDamage(BlockState state) {
        return state.is(BlockTags.ANVIL) || state.is(Blocks.POINTED_DRIPSTONE) || state.is(Blocks.SULFUR_SPIKE);
    }

    public static BlockState fallingState(WorldConfig config, BlockState state) {
        if (config.fallingTurnGrassToDirt() && turnsToDirt(state)) {
            return Blocks.DIRT.defaultBlockState();
        }
        return PhysicsQueue.unwaterlog(state);
    }

    private static boolean turnsToDirt(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM) || state.is(Blocks.PODZOL);
    }

    private static void onBreak(Level world, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (!(world instanceof ServerLevel level)) {
            return;
        }
        if (!WorldGate.isModuleActive(level, ID)) {
            return;
        }
        if (!ConfigManager.world(level).fallingEnable()) {
            return;
        }
        enqueueAround(level, pos, pos, 0, false);
    }

    private static void enqueueAround(
            ServerLevel level, BlockPos pos, BlockPos origin, int delayTicks, boolean caveIn) {
        GlobalConfig global = ConfigManager.global();
        int remaining = global.maxFloodFillPerConversion();
        for (Direction direction : Direction.values()) {
            if (remaining <= 0) {
                break;
            }
            BlockPos neighbor = pos.relative(direction);
            if (!PhysicsChainRules.withinRange(
                    origin.getX(), origin.getY(), origin.getZ(),
                    neighbor.getX(), neighbor.getY(), neighbor.getZ())) {
                continue;
            }
            if (enqueueIfFalling(level, neighbor, origin, delayTicks, caveIn)) {
                remaining--;
            }
        }
    }

    private static void cascade(ServerLevel level, BlockPos pos, BlockPos origin, boolean caveIn) {
        if (!enabled(level)) {
            return;
        }
        enqueueAround(level, pos, origin, PhysicsChainRules.HOP_DELAY_TICKS, caveIn);
    }

    private static boolean enqueueIfFalling(
            ServerLevel level, BlockPos pos, BlockPos origin, int delayTicks, boolean caveIn) {
        if (PhysicsSkip.notReady(level, pos) || PhysicsSkip.never(level, pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (state.is(EhmTags.PHYSICS_PROTECTED) || !state.is(EhmTags.EXTRA_FALLING)) {
            return false;
        }
        if (!PhysicsQueue.canFall(level, pos)) {
            return false;
        }
        WorldConfig config = ConfigManager.world(level);
        PhysicsQueue.of(level)
                .enqueueFalling(level, pos, state, fallingState(config, state), delayTicks, origin, caveIn);
        return true;
    }

    private static BlockPos chainOrigin(FallingBlockEntity entity, BlockPos fallback) {
        Long packed = entity.getAttached(EhmAttachments.EHM_CHAIN_ORIGIN);
        if (packed == null) {
            return fallback;
        }
        return BlockPos.of(packed);
    }

    private static void breakColumn(ServerLevel level, BlockPos start) {
        BlockPos current = start;
        for (int y = start.getY(); y > level.getMinY(); y--) {
            BlockState below = level.getBlockState(current.below());
            if (FallingBlock.isFree(below) && !below.isAir()) {
                level.destroyBlock(current.below(), true);
            } else if (!FallingBlock.isFree(below)) {
                break;
            }
            current = current.below();
        }
    }
}

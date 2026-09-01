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
        enqueueIfFalling(level, context.getClickedPos());
    }

    public static void onRemoved(FallingBlockEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        PhysicsQueue.of(level).markLanded(entity);
        if (!Boolean.TRUE.equals(entity.getAttachedOrElse(EhmAttachments.EHM_OURS, Boolean.FALSE))) {
            return;
        }
        if (!enabled(level) && !CaveIns.enabled(level)) {
            return;
        }
        WorldConfig config = ConfigManager.world(level);
        BlockPos pos = entity.blockPosition();
        if (config.fallingBreakTorches()) {
            breakColumn(level, pos);
        }
        if (config.fallingCascade()) {
            cascade(level, pos);
        }
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
     * Extra damage only for {@code #extra_falling} or cave-in cobble/cobbled_deepslate we spawned.
     * Anvils, pointed dripstone, and sulfur spikes stay vanilla.
     */
    public static boolean appliesFallDamage(FallingBlockEntity entity) {
        BlockState state = entity.getBlockState();
        if (isVanillaFallDamage(state)) {
            return false;
        }
        boolean tagged = isCaveInProduct(state) || state.is(EhmTags.EXTRA_FALLING);
        boolean ours = Boolean.TRUE.equals(entity.getAttachedOrElse(EhmAttachments.EHM_OURS, Boolean.FALSE));
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
        enqueueAround(level, pos);
    }

    private static void enqueueAround(ServerLevel level, BlockPos origin) {
        GlobalConfig global = ConfigManager.global();
        int remaining = global.maxFloodFillPerConversion();
        for (Direction direction : Direction.values()) {
            if (remaining <= 0) {
                break;
            }
            if (enqueueIfFalling(level, origin.relative(direction))) {
                remaining--;
            }
        }
    }

    private static void cascade(ServerLevel level, BlockPos origin) {
        if (!enabled(level)) {
            return;
        }
        enqueueAround(level, origin);
    }

    private static boolean enqueueIfFalling(ServerLevel level, BlockPos pos) {
        if (PhysicsSkip.never(level, pos)) {
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
        PhysicsQueue.of(level).enqueueFalling(level, pos, state, fallingState(config, state));
        return true;
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

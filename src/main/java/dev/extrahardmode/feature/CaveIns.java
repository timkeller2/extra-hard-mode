package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.module.PhysicsQueue;
import dev.extrahardmode.tag.EhmTags;
import dev.extrahardmode.world.PhysicsSkip;
import dev.extrahardmode.world.WorldGate;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class CaveIns implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("cave_ins");
    public static final float COPPER_NEIGHBOR_CHANCE = 0.5F;

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        bus.listen(PlayerBlockBreakEvents.AFTER, ID, CaveIns::onBreak);
    }

    public static boolean enabled(Level level) {
        return FeatureBus.guard(level, ID)
                && level instanceof ServerLevel serverLevel
                && ConfigManager.world(serverLevel).caveInsEnable();
    }

    /** GameTests and explosion modules call this after the ore is already air. */
    public static void onOreBroken(ServerLevel level, BlockPos pos, BlockState broken) {
        if (!enabled(level)) {
            return;
        }
        if (!broken.is(EhmTags.CAVE_IN_ORES)) {
            return;
        }
        if (PhysicsSkip.never(level, pos)) {
            return;
        }
        WorldConfig config = ConfigManager.world(level);
        boolean copper = isCopperOre(broken);
        boolean ancientDebris = broken.is(Blocks.ANCIENT_DEBRIS);
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (PhysicsSkip.never(level, neighbor)) {
                continue;
            }
            if (copper && level.getRandom().nextFloat() >= COPPER_NEIGHBOR_CHANCE) {
                continue;
            }
            BlockState from = level.getBlockState(neighbor);
            if (from.is(EhmTags.PHYSICS_PROTECTED)) {
                continue;
            }
            if (ancientDebris && !from.is(EhmTags.HARDENED)) {
                continue;
            }
            BlockState to = soften(config, from);
            if (to == null) {
                continue;
            }
            PhysicsQueue.of(level).enqueueConvert(level, neighbor, from, to, config.caveInsApplyPhysics());
        }
    }

    public static BlockState soften(WorldConfig config, BlockState from) {
        Identifier fromId = BuiltInRegistries.BLOCK.getKey(from.getBlock());
        if (fromId == null) {
            return null;
        }
        Identifier toId = config.softenTo(fromId);
        if (toId == null) {
            return null;
        }
        var toBlock = BuiltInRegistries.BLOCK.getValue(toId);
        if (toBlock == null || toBlock == Blocks.AIR) {
            return null;
        }
        return toBlock.defaultBlockState();
    }

    private static boolean isCopperOre(BlockState state) {
        return state.is(Blocks.COPPER_ORE) || state.is(Blocks.DEEPSLATE_COPPER_ORE);
    }

    private static void onBreak(Level world, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (!(world instanceof ServerLevel level)) {
            return;
        }
        if (!WorldGate.isModuleActive(level, ID)) {
            return;
        }
        onOreBroken(level, pos, state);
    }
}

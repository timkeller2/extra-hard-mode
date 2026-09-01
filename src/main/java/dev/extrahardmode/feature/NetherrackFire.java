package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.world.WorldGate;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Breaking netherrack may start fire in the now-empty cell, sitting on remaining
 * netherrack below. The netherrack drop is not cancelled (AFTER).
 */
public final class NetherrackFire implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("netherrack_fire");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        bus.listen(PlayerBlockBreakEvents.AFTER, ID, NetherrackFire::onBreak);
    }

    static void onBreak(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!WorldGate.isModuleActive(serverLevel, ID) || EhmApi.playerBypasses(serverPlayer)) {
            return;
        }
        if (!state.is(Blocks.NETHERRACK)) {
            return;
        }
        tryIgnite(serverLevel, serverPlayer, pos);
    }

    /**
     * AFTER-break: {@code pos} is the empty cell. Fire only if the block below is
     * still netherrack (eternal-fire support), matching original netherrack-on-netherrack.
     */
    public static boolean tryIgnite(ServerLevel level, @Nullable ServerPlayer player, BlockPos pos) {
        if (!WorldGate.isModuleActive(level, ID)) {
            return false;
        }
        if (player != null && EhmApi.playerBypasses(player)) {
            return false;
        }
        int percent = ConfigManager.world(level).netherrackFirePercent();
        if (percent <= 0 || level.getRandom().nextInt(100) >= percent) {
            return false;
        }
        if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.below()).is(Blocks.NETHERRACK)) {
            return false;
        }
        BlockState fire = BaseFireBlock.getState(level, pos);
        if (BaseFireBlock.canBePlacedAt(level, pos, Direction.UP) || fire.canSurvive(level, pos)) {
            return level.setBlock(pos, fire, 3);
        }
        return false;
    }
}

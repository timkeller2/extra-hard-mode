package dev.extrahardmode.feature.monster;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.api.event.EndermanTeleportPlayerEvent;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.feature.FeatureModule;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.WorldGate;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

/**
 * Endermen teleport the player onto them when the player cheeses a 2-high roof
 * or otherwise blocks LoS in a close fight.
 */
public final class Endermen implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("endermen");
    public static final int RANGE_BLOCKS = 8;
    public static final int COOLDOWN_TICKS = 60;
    public static final int MIN_DEST_Y = 3;
    public static final int MAX_HEIGHT_DIFF = 10;

    @Override
    public Identifier id() {
        return ID;
    }

    public static void tick(EnderMan enderman, ServerLevel level) {
        if (!WorldGate.isModuleActive(level, ID)) {
            return;
        }
        if (!enderman.isAlive() || enderman.isRemoved()) {
            return;
        }
        if (!ConfigManager.world(level).endermenTeleportPlayers()) {
            return;
        }
        LivingEntity target = enderman.getTarget();
        if (!(target instanceof ServerPlayer player) || player.isRemoved()) {
            return;
        }
        if (EhmApi.playerBypasses(player) || player.isPassenger()) {
            return;
        }
        if (enderman.distanceToSqr(player) > RANGE_BLOCKS * RANGE_BLOCKS) {
            return;
        }
        if (!isCheese(level, enderman, player)) {
            return;
        }
        long now = level.getGameTime();
        long last = player.getAttachedOrElse(EhmAttachments.EHM_ENDERMAN_TP_TICK, 0L);
        if (onCooldown(now, last)) {
            return;
        }
        BlockPos landing = findLanding(level, enderman, player);
        if (landing == null) {
            return;
        }
        Vec3 dest = Vec3.atBottomCenterOf(landing);
        EndermanTeleportPlayerEvent event = new EndermanTeleportPlayerEvent(player, enderman, dest);
        EndermanTeleportPlayerEvent.EVENT.invoker().onEndermanTeleportPlayer(event);
        if (event.isCanceled() || event.teleportTo() == null) {
            return;
        }
        Vec3 to = event.teleportTo();
        level.playSound(
                enderman,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.HOSTILE,
                1.0F,
                1.0F);
        player.teleportTo(level, to.x, to.y, to.z, Set.of(), player.getYRot(), player.getXRot(), true);
        player.resetFallDistance();
        player.setAttached(EhmAttachments.EHM_ENDERMAN_TP_TICK, now);
        level.playSound(
                enderman, to.x, to.y, to.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    public static boolean onCooldown(long now, long last) {
        return now - last < COOLDOWN_TICKS;
    }

    static boolean isTwoHighRoof(boolean losBlocked, boolean above1Blocks, boolean above2Blocks) {
        return losBlocked || above1Blocks || above2Blocks;
    }

    static boolean isCheese(ServerLevel level, EnderMan enderman, ServerPlayer player) {
        BlockPos feet = player.blockPosition();
        return isTwoHighRoof(
                !enderman.hasLineOfSight(player),
                blocksPath(level, feet.above(1)),
                blocksPath(level, feet.above(2)));
    }

    static BlockPos findLanding(ServerLevel level, EnderMan enderman, ServerPlayer player) {
        BlockPos origin = enderman.blockPosition();
        if (isSafeLanding(level, origin, player)) {
            return origin;
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos beside = origin.relative(dir);
            if (isSafeLanding(level, beside, player)) {
                return beside;
            }
        }
        return null;
    }

    static boolean isSafeLanding(ServerLevel level, BlockPos feet, ServerPlayer player) {
        if (feet.getY() < MIN_DEST_Y) {
            return false;
        }
        if (Math.abs(player.getBlockY() - feet.getY()) > MAX_HEIGHT_DIFF) {
            return false;
        }
        if (!level.getBlockState(feet).isAir() || !level.getBlockState(feet.above()).isAir()) {
            return false;
        }
        if (!level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty()) {
            return false;
        }
        BlockState below = level.getBlockState(feet.below());
        if (!blocksPath(level, feet.below())) {
            return false;
        }
        return !below.getFluidState().is(Fluids.WATER);
    }

    private static boolean blocksPath(ServerLevel level, BlockPos pos) {
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }
}

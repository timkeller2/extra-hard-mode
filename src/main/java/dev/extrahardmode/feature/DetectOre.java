package dev.extrahardmode.feature;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Scan, cluster, and look-at for the Detect ore mana ability. */
public final class DetectOre {
    private static final float LODESTONE_REACH = 5.0F;

    private DetectOre() {}

    public static AbilityRules.OreDeposit findBest(ServerPlayer player, int range) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos origin = player.blockPosition();
        List<AbilityRules.OreSample> samples = new ArrayList<>();
        int reach = Math.max(0, range);
        BlockPos min = origin.offset(-reach, -reach, -reach);
        BlockPos max = origin.offset(reach, reach, reach);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            String id = blockId(state);
            if (AbilityRules.isDetectableOre(id)) {
                samples.add(new AbilityRules.OreSample(id, pos.getX(), pos.getY(), pos.getZ()));
            }
        }
        return AbilityRules.bestDeposit(
                AbilityRules.clusterDeposits(samples, origin.getX(), origin.getY(), origin.getZ()));
    }

    public static void lookAt(ServerPlayer player, AbilityRules.OreDeposit deposit) {
        Vec3 eye = player.getEyePosition();
        double dx = deposit.lookX() + 0.5 - eye.x;
        double dy = deposit.lookY() + 0.5 - eye.y;
        double dz = deposit.lookZ() + 0.5 - eye.z;
        float yaw = AbilityRules.lookYaw(dx, dz);
        float pitch = AbilityRules.lookPitch(dx, dy, dz);
        ServerLevel level = (ServerLevel) player.level();
        player.teleportTo(level, player.getX(), player.getY(), player.getZ(), Set.of(), yaw, pitch, true);
    }

    /** Lodestone compass binding still needs the vanilla right-click. */
    public static boolean targetingLodestone(ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0F).scale(LODESTONE_REACH));
        BlockHitResult hit = player.level()
                .clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        return player.level().getBlockState(hit.getBlockPos()).is(Blocks.LODESTONE);
    }

    static String blockId(BlockState state) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id == null ? "" : id.toString();
    }
}

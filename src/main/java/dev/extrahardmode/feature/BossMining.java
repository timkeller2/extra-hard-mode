package dev.extrahardmode.feature;

import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.BiomeBossData;
import dev.extrahardmode.world.WorldGate;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

/**
 * Biome bosses break the block in front of them when walking will not reach their player.
 * Break time matches an iron pickaxe with no haste, fatigue, or water penalty.
 */
public final class BossMining {
    /** Vanilla {@code destroyBlock} neighbor-update limit. */
    private static final int DESTROY_LIMIT = 512;
    private static ItemStack ironPickaxe;

    private BossMining() {}

    public static void ensureGoals(ServerLevel level) {
        if (!WorldGate.isModuleActive(level, BiomeBosses.ID)) {
            return;
        }
        BiomeBossData data = BiomeBossData.of(level);
        for (BossFamily family : BossFamily.values()) {
            Optional<UUID> living = data.state(family).living();
            if (living.isEmpty()) {
                continue;
            }
            if (!(level.getEntity(living.get()) instanceof Mob mob) || !mob.isAlive() || !BiomeBosses.isBoss(mob)) {
                continue;
            }
            if (Boolean.TRUE.equals(mob.getAttachedOrElse(EhmAttachments.EHM_BOSS_MINE_GOAL, Boolean.FALSE))) {
                continue;
            }
            mob.getGoalSelector().addGoal(2, new BossMineGoal(mob));
            mob.setAttached(EhmAttachments.EHM_BOSS_MINE_GOAL, Boolean.TRUE);
        }
    }

    public static void stop(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        Long packed = mob.getAttached(EhmAttachments.EHM_BOSS_MINE_POS);
        if (packed != null) {
            level.destroyBlockProgress(mob.getId(), BlockPos.of(packed), -1);
        }
        mob.removeAttached(EhmAttachments.EHM_BOSS_MINE_POS);
        mob.removeAttached(EhmAttachments.EHM_BOSS_MINE_PROGRESS);
    }

    static boolean needsToDig(Mob mob, Player target, long now) {
        if (mob.getNavigation().isStuck()) {
            return true;
        }
        if (mob.hasLineOfSight(target) && mob.distanceToSqr(target) <= 9.0) {
            return false;
        }
        Long last = mob.getAttached(EhmAttachments.EHM_BOSS_PATH_CHECK);
        Boolean reaches = mob.getAttached(EhmAttachments.EHM_BOSS_PATH_REACHES);
        if (last != null && reaches != null && now - last < BiomeBossesRules.BOSS_PATH_RECHECK_TICKS) {
            return !reaches;
        }
        Path fresh = mob.getNavigation().createPath(target, 0);
        boolean ok = fresh != null && fresh.canReach();
        mob.setAttached(EhmAttachments.EHM_BOSS_PATH_CHECK, now);
        mob.setAttached(EhmAttachments.EHM_BOSS_PATH_REACHES, ok);
        return !ok;
    }

    static void mine(ServerLevel level, Mob mob, Player target) {
        BlockPos pos = obstructing(level, mob, target);
        if (pos == null) {
            stop(mob);
            return;
        }
        Vec3 center = Vec3.atCenterOf(pos);
        mob.getLookControl().setLookAt(center);
        if (!inReach(mob, pos)) {
            stop(mob);
            if (mob.getNavigation().isDone()) {
                Vec3 eye = mob.getEyePosition();
                Vec3 toward = center.subtract(eye);
                double length = toward.length();
                Vec3 stand = length <= 1.2 ? center : eye.add(toward.scale((length - 1.2) / length));
                mob.getNavigation().moveTo(stand.x, stand.y, stand.z, 1.0);
            }
            return;
        }
        BlockState state = level.getBlockState(pos);
        ItemStack pick = ironPickaxe();
        if (!breakable(state, level, pos)) {
            stop(mob);
            return;
        }
        boolean correct = !state.requiresCorrectToolForDrops() || pick.isCorrectToolForDrops(state);
        float added = BiomeBossesRules.toolDestroyProgress(
                state.getDestroySpeed(level, pos), pick.getDestroySpeed(state), correct);
        if (added <= 0.0F) {
            stop(mob);
            return;
        }
        long packed = pos.asLong();
        Long previous = mob.getAttached(EhmAttachments.EHM_BOSS_MINE_POS);
        float progress = mob.getAttachedOrElse(EhmAttachments.EHM_BOSS_MINE_PROGRESS, 0.0F);
        if (previous == null || previous.longValue() != packed) {
            if (previous != null) {
                level.destroyBlockProgress(mob.getId(), BlockPos.of(previous), -1);
            }
            progress = 0.0F;
            mob.setAttached(EhmAttachments.EHM_BOSS_MINE_POS, packed);
        }
        progress += added;
        if (level.getGameTime() % 4L == 0L) {
            SoundType sound = state.getSoundType();
            level.playSound(
                    null,
                    center.x,
                    center.y,
                    center.z,
                    sound.getHitSound(),
                    SoundSource.BLOCKS,
                    (sound.getVolume() + 1.0F) / 8.0F,
                    sound.getPitch());
            mob.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT);
        }
        if (progress < 1.0F) {
            mob.setAttached(EhmAttachments.EHM_BOSS_MINE_PROGRESS, progress);
            level.destroyBlockProgress(mob.getId(), pos, Math.min(9, (int) (progress * 10.0F)));
            return;
        }
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        if (correct) {
            Block.dropResources(state, level, pos, blockEntity, mob, pick);
        }
        level.destroyBlockProgress(mob.getId(), pos, -1);
        level.destroyBlock(pos, false, mob, DESTROY_LIMIT);
        mob.removeAttached(EhmAttachments.EHM_BOSS_MINE_POS);
        mob.removeAttached(EhmAttachments.EHM_BOSS_MINE_PROGRESS);
    }

    static BlockPos obstructing(ServerLevel level, Mob mob, Player target) {
        BlockPos body = bodyBlock(level, mob, target);
        if (body != null) {
            return body;
        }
        BlockHitResult hit = level.clip(new ClipContext(
                mob.getEyePosition(),
                target.getEyePosition(),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                mob));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!breakable(state, level, pos)) {
            return null;
        }
        return pos;
    }

    static BlockPos bodyBlock(ServerLevel level, Mob mob, Player target) {
        Vec3 delta = target.position().subtract(mob.position());
        double length = delta.length();
        if (length < 1.0E-4) {
            return null;
        }
        double step = Math.min(1.05, length);
        Vec3 forward = delta.scale(step / length);
        AABB box = mob.getBoundingBox().expandTowards(forward.x, Math.min(0.5, Math.max(-0.5, forward.y)), forward.z);
        BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
        BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = level.getBlockState(pos);
            if (!breakable(state, level, pos) || state.getCollisionShape(level, pos).isEmpty()) {
                continue;
            }
            double dist = pos.distToCenterSqr(target.getX(), target.getY(), target.getZ());
            if (dist < bestDist) {
                bestDist = dist;
                best = pos.immutable();
            }
        }
        return best;
    }

    static boolean inReach(Mob mob, BlockPos pos) {
        AABB block = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0);
        return mob.getBoundingBox().inflate(1.0).intersects(block);
    }

    static boolean breakable(BlockState state, ServerLevel level, BlockPos pos) {
        if (state.isAir() || state.liquid()) {
            return false;
        }
        if (!level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }
        return state.getDestroySpeed(level, pos) >= 0.0F;
    }

    private static ItemStack ironPickaxe() {
        if (ironPickaxe == null || ironPickaxe.isEmpty()) {
            ironPickaxe = new ItemStack(Items.IRON_PICKAXE);
        }
        return ironPickaxe;
    }

    static final class BossMineGoal extends Goal {
        private final Mob mob;

        BossMineGoal(Mob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return hunting();
        }

        @Override
        public boolean canContinueToUse() {
            return hunting();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            BossMining.stop(mob);
        }

        @Override
        public void tick() {
            if (!(mob.level() instanceof ServerLevel level)) {
                return;
            }
            if (mob.getTarget() instanceof Player target) {
                mine(level, mob, target);
            }
        }

        private boolean hunting() {
            if (!(mob.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, BiomeBosses.ID)) {
                return false;
            }
            if (!BiomeBosses.isBoss(mob) || !mob.isAlive()) {
                return false;
            }
            if (!(mob.getTarget() instanceof ServerPlayer target)
                    || !target.isAlive()
                    || target.isSpectator()
                    || target.isCreative()
                    || target.level() != level) {
                return false;
            }
            if (!needsToDig(mob, target, level.getGameTime())) {
                return false;
            }
            return obstructing(level, mob, target) != null;
        }
    }
}

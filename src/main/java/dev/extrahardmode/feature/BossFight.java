package dev.extrahardmode.feature;

import dev.extrahardmode.network.ClientboundBossRayPayload;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.BiomeBossData;
import dev.extrahardmode.world.WorldGate;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Leap and green ray for a living biome boss. Mining stays in {@link BossMining}.
 * The pull is also applied on the client, because the local player is the one who moves.
 * Both sides write the same absolute velocity, so the step is not added twice.
 */
public final class BossFight {
    private BossFight() {}

    public static void tick(ServerLevel level) {
        if (level == null || !WorldGate.isModuleActive(level, BiomeBosses.ID)) {
            return;
        }
        BiomeBossData data = BiomeBossData.of(level);
        long now = level.getGameTime();
        for (BossFamily family : BossFamily.values()) {
            Optional<UUID> living = data.state(family).living();
            if (living.isEmpty()) {
                continue;
            }
            if (!(level.getEntity(living.get()) instanceof Mob mob) || !mob.isAlive() || !BiomeBosses.isBoss(mob)) {
                continue;
            }
            keepJumpSafe(mob, now);
            tryJump(mob, now);
            tryRay(level, mob, now);
            pullTarget(level, mob, now);
        }
    }

    private static void keepJumpSafe(Mob mob, long now) {
        Long until = mob.getAttached(EhmAttachments.EHM_BOSS_JUMP_SAFE_UNTIL);
        if (until == null) {
            return;
        }
        if (now < until) {
            mob.resetFallDistance();
            return;
        }
        mob.removeAttached(EhmAttachments.EHM_BOSS_JUMP_SAFE_UNTIL);
    }

    private static void tryJump(Mob mob, long now) {
        ServerPlayer target = fightTarget(mob);
        if (target == null) {
            return;
        }
        long last = mob.getAttachedOrElse(EhmAttachments.EHM_BOSS_JUMP_TICK, 0L);
        if (!BiomeBossesRules.spitReady(now, last, BiomeBossesRules.BOSS_JUMP_INTERVAL_TICKS)) {
            if (last <= 0L) {
                mob.setAttached(EhmAttachments.EHM_BOSS_JUMP_TICK, now);
            }
            return;
        }
        boolean hovering = mob.getType() == EntityTypes.BLAZE;
        boolean supported = mob.onGround() || mob.isInWater() || mob.isInLava();
        if (!BiomeBossesRules.canLeap(mob.isPassenger(), supported, hovering)) {
            return;
        }
        mob.setAttached(EhmAttachments.EHM_BOSS_JUMP_TICK, now);
        double vy = BiomeBossesRules.jumpVelocity(BiomeBossesRules.BOSS_JUMP_HEIGHT);
        int airTicks = BiomeBossesRules.jumpAirTicks(vy);
        double dx = target.getX() - mob.getX();
        double dz = target.getZ() - mob.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double leap = BiomeBossesRules.jumpLeapDistance(horizontal);
        double vx = 0.0;
        double vz = 0.0;
        if (leap > 0.0 && horizontal > 1.0E-6) {
            double speed = BiomeBossesRules.jumpHorizontalVelocity(leap, airTicks);
            vx = dx / horizontal * speed;
            vz = dz / horizontal * speed;
        }
        mob.setDeltaMovement(vx, vy, vz);
        mob.syncVelocity = true;
        mob.needsSync = true;
        mob.setOnGround(false);
        mob.resetFallDistance();
        mob.setAttached(
                EhmAttachments.EHM_BOSS_JUMP_SAFE_UNTIL, now + BiomeBossesRules.BOSS_JUMP_FALL_SAFE_TICKS);
        mob.getLookControl().setLookAt(target);
    }

    private static void tryRay(ServerLevel level, Mob mob, long now) {
        if (!BiomeBossesRules.belowHalfHealth(mob.getHealth(), mob.getMaxHealth())) {
            return;
        }
        ServerPlayer target = fightTarget(mob);
        if (target == null) {
            return;
        }
        double range = BiomeBossesRules.BOSS_RAY_RANGE;
        if (mob.distanceToSqr(target) > range * range) {
            return;
        }
        long last = mob.getAttachedOrElse(EhmAttachments.EHM_BOSS_RAY_TICK, 0L);
        if (!BiomeBossesRules.spitReady(now, last, BiomeBossesRules.BOSS_RAY_INTERVAL_TICKS)) {
            if (last <= 0L) {
                mob.setAttached(EhmAttachments.EHM_BOSS_RAY_TICK, now);
            }
            return;
        }
        mob.setAttached(EhmAttachments.EHM_BOSS_RAY_TICK, now);
        mob.getLookControl().setLookAt(target);
        level.playSound(
                null,
                mob.getX(),
                mob.getY(),
                mob.getZ(),
                SoundEvents.GUARDIAN_ATTACK,
                SoundSource.HOSTILE,
                1.0F,
                1.0F);
        Vec3 from = mob.getEyePosition();
        Vec3 to = target.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(
                from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
        if (hit.getType() == HitResult.Type.BLOCK) {
            clearPull(level, mob);
            Vec3 blocked = hit.getLocation();
            sendRay(
                    level,
                    mob,
                    -1,
                    blocked.x,
                    blocked.y,
                    blocked.z,
                    now + BiomeBossesRules.BOSS_RAY_MISS_TICKS);
            return;
        }
        long until = now + BiomeBossesRules.BOSS_RAY_PULL_TICKS;
        mob.setAttached(EhmAttachments.EHM_BOSS_PULL_UNTIL, until);
        mob.setAttached(EhmAttachments.EHM_BOSS_PULL_TARGET, target.getStringUUID());
        mob.setAttached(EhmAttachments.EHM_BOSS_RAY_SEND, now);
        sendRay(level, mob, target.getId(), to.x, to.y, to.z, until);
    }

    private static void pullTarget(ServerLevel level, Mob mob, long now) {
        Long until = mob.getAttached(EhmAttachments.EHM_BOSS_PULL_UNTIL);
        if (until == null) {
            return;
        }
        if (now >= until) {
            clearPull(level, mob);
            return;
        }
        String raw = mob.getAttached(EhmAttachments.EHM_BOSS_PULL_TARGET);
        UUID id = parseId(raw);
        if (id == null) {
            clearPull(level, mob);
            return;
        }
        if (!(level.getEntity(id) instanceof ServerPlayer player)
                || !player.isAlive()
                || player.isSpectator()
                || player.isCreative()
                || player.isPassenger()
                || player.level() != level) {
            clearPull(level, mob);
            return;
        }
        long lastSend = mob.getAttachedOrElse(EhmAttachments.EHM_BOSS_RAY_SEND, 0L);
        if (now - lastSend >= BiomeBossesRules.BOSS_RAY_REFRESH_TICKS) {
            Vec3 eye = player.getEyePosition();
            mob.setAttached(EhmAttachments.EHM_BOSS_RAY_SEND, now);
            sendRay(level, mob, player.getId(), eye.x, eye.y, eye.z, until);
        }
        double[] step = BiomeBossesRules.pullStep(
                mob.getX() - player.getX(),
                mob.getY() + mob.getBbHeight() * 0.5 - player.getY(),
                mob.getZ() - player.getZ());
        if (step[0] == 0.0 && step[1] == 0.0 && step[2] == 0.0) {
            return;
        }
        player.setDeltaMovement(step[0], step[1], step[2]);
        player.syncVelocity = true;
        player.needsSync = true;
    }

    private static void sendRay(
            ServerLevel level, Mob mob, int targetId, double x, double y, double z, long untilTick) {
        ClientboundBossRayPayload payload =
                new ClientboundBossRayPayload(mob.getId(), targetId, x, y, z, untilTick);
        double range = BiomeBossesRules.BOSS_RAY_VIEW_BLOCKS;
        double rangeSq = range * range;
        for (ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(mob) <= rangeSq) {
                ServerPlayNetworking.send(viewer, payload);
            }
        }
    }

    private static ServerPlayer fightTarget(Mob mob) {
        if (!(mob.getTarget() instanceof ServerPlayer player)) {
            return null;
        }
        if (!player.isAlive() || player.isSpectator() || player.isCreative() || player.level() != mob.level()) {
            return null;
        }
        return player;
    }

    private static void clearPull(ServerLevel level, Mob mob) {
        Long until = mob.getAttached(EhmAttachments.EHM_BOSS_PULL_UNTIL);
        mob.removeAttached(EhmAttachments.EHM_BOSS_PULL_UNTIL);
        mob.removeAttached(EhmAttachments.EHM_BOSS_PULL_TARGET);
        mob.removeAttached(EhmAttachments.EHM_BOSS_RAY_SEND);
        if (until != null && level.getGameTime() < until) {
            sendRay(level, mob, -1, mob.getX(), mob.getY(), mob.getZ(), level.getGameTime());
        }
    }

    private static UUID parseId(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

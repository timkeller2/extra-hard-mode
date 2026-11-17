package dev.extrahardmode.client;

import dev.extrahardmode.feature.BiomeBossesRules;
import dev.extrahardmode.network.ClientboundBossRayPayload;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

/**
 * Green copy of the guardian beam. A ray aimed at the local player also sets their velocity,
 * because that player is the one who decides where they move.
 */
public final class BossRayRenderer {
    private static final RenderType BEAM = RenderTypes.entityCutout(
            Identifier.withDefaultNamespace("textures/entity/guardian/guardian_beam.png"));
    private static final int RED = 40;
    private static final int GREEN = 220;
    private static final int BLUE = 70;
    private static final Map<Integer, Ray> RAYS = new ConcurrentHashMap<>();

    private BossRayRenderer() {}

    public static void register() {
        LevelRenderEvents.COLLECT_SUBMITS.register(BossRayRenderer::collect);
        ClientTickEvents.END_CLIENT_TICK.register(BossRayRenderer::tick);
    }

    public static void accept(ClientboundBossRayPayload payload) {
        if (payload == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.level != null && client.level.getGameTime() >= payload.untilTick()) {
            RAYS.remove(payload.bossId());
            return;
        }
        RAYS.put(
                payload.bossId(),
                new Ray(payload.bossId(), payload.targetId(), payload.x(), payload.y(), payload.z(), payload.untilTick()));
    }

    public static void clear() {
        RAYS.clear();
    }

    static void tick(Minecraft client) {
        if (client == null || client.player == null || client.level == null) {
            return;
        }
        long now = client.level.getGameTime();
        LocalPlayer player = client.player;
        Iterator<Ray> rays = RAYS.values().iterator();
        while (rays.hasNext()) {
            Ray ray = rays.next();
            if (now >= ray.untilTick) {
                rays.remove();
                continue;
            }
            if (!(client.level.getEntity(ray.bossId) instanceof LivingEntity boss) || !boss.isAlive()) {
                rays.remove();
                continue;
            }
            if (ray.targetId != player.getId() || !player.isAlive() || player.isSpectator() || player.isCreative()
                    || player.isPassenger()) {
                continue;
            }
            double[] step = BiomeBossesRules.pullStep(
                    boss.getX() - player.getX(),
                    boss.getY() + boss.getBbHeight() * 0.5 - player.getY(),
                    boss.getZ() - player.getZ());
            if (step[0] == 0.0 && step[1] == 0.0 && step[2] == 0.0) {
                continue;
            }
            player.setDeltaMovement(step[0], step[1], step[2]);
        }
    }

    static void collect(net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext context) {
        if (RAYS.isEmpty() || context.poseStack() == null || context.levelState() == null) {
            return;
        }
        CameraRenderState camera = context.levelState().cameraRenderState;
        if (camera == null || !camera.initialized || camera.pos == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        SubmitNodeCollector collector = context.submitNodeCollector();
        if (collector == null) {
            return;
        }
        long now = minecraft.level.getGameTime();
        float partial = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Vec3 cam = camera.pos;
        for (Ray ray : RAYS.values()) {
            if (now >= ray.untilTick) {
                continue;
            }
            if (!(minecraft.level.getEntity(ray.bossId) instanceof LivingEntity boss) || !boss.isAlive()) {
                continue;
            }
            Vec3 from = boss.getEyePosition(partial);
            Vec3 to = end(minecraft, ray, partial);
            Vec3 direction = to.subtract(from);
            if (direction.lengthSqr() < 1.0E-6) {
                continue;
            }
            PoseStack pose = context.poseStack();
            pose.pushPose();
            pose.translate(from.x - cam.x, from.y - cam.y, from.z - cam.z);
            float attackTime = (float) (now % 80L) + partial;
            renderBeam(pose, collector, direction, attackTime, attackTime * 0.5F % 1.0F);
            pose.popPose();
        }
    }

    private static Vec3 end(Minecraft minecraft, Ray ray, float partial) {
        if (ray.targetId >= 0 && minecraft.level != null) {
            Entity target = minecraft.level.getEntity(ray.targetId);
            if (target != null) {
                return target.getEyePosition(partial);
            }
        }
        return new Vec3(ray.x, ray.y, ray.z);
    }

    /** Same ribbon as {@code GuardianRenderer}, tinted green. */
    private static void renderBeam(PoseStack pose, SubmitNodeCollector collector, Vec3 direction, float attackTime, float phase) {
        float length = (float) (direction.length() + 1.0);
        Vec3 normal = direction.normalize();
        float pitch = (float) Math.acos(normal.y) * 57.295776F;
        float yaw = (1.5707964F - (float) Math.atan2(normal.z, normal.x)) * 57.295776F;
        pose.rotateDegrees(Axis.YP, yaw);
        pose.rotateDegrees(Axis.XP, pitch);
        float spin = attackTime * 0.05F * -1.5F;
        float outer = 0.2F;
        float inner = 0.282F;
        float x0 = Mth.cos(spin + 3.1415927F) * outer;
        float z0 = Mth.sin(spin + 3.1415927F) * outer;
        float x1 = Mth.cos(spin) * outer;
        float z1 = Mth.sin(spin) * outer;
        float x2 = Mth.cos(spin + 1.5707964F) * outer;
        float z2 = Mth.sin(spin + 1.5707964F) * outer;
        float x3 = Mth.cos(spin + 4.712389F) * outer;
        float z3 = Mth.sin(spin + 4.712389F) * outer;
        float ix0 = Mth.cos(spin + 2.3561945F) * inner;
        float iz0 = Mth.sin(spin + 2.3561945F) * inner;
        float ix1 = Mth.cos(spin + 0.7853982F) * inner;
        float iz1 = Mth.sin(spin + 0.7853982F) * inner;
        float ix2 = Mth.cos(spin + 5.4977875F) * inner;
        float iz2 = Mth.sin(spin + 5.4977875F) * inner;
        float ix3 = Mth.cos(spin + 3.926991F) * inner;
        float iz3 = Mth.sin(spin + 3.926991F) * inner;
        float near = -1.0F + phase;
        float far = near + length * 2.5F;
        float flash = Mth.floor(attackTime) % 2 == 0 ? 0.5F : 0.0F;
        collector.submitCustomGeometry(pose, BEAM, (matrix, buffer) -> {
            ribbon(buffer, matrix, x0, z0, x1, z1, length, far, near);
            ribbon(buffer, matrix, x2, z2, x3, z3, length, far, near);
            vertex(buffer, matrix, ix0, length, iz0, 0.5F, flash + 0.5F);
            vertex(buffer, matrix, ix1, length, iz1, 1.0F, flash + 0.5F);
            vertex(buffer, matrix, ix2, length, iz2, 1.0F, flash);
            vertex(buffer, matrix, ix3, length, iz3, 0.5F, flash);
        });
    }

    private static void ribbon(
            VertexConsumer buffer,
            PoseStack.Pose matrix,
            float x0,
            float z0,
            float x1,
            float z1,
            float length,
            float far,
            float near) {
        vertex(buffer, matrix, x0, length, z0, 0.4999F, far);
        vertex(buffer, matrix, x0, 0.0F, z0, 0.4999F, near);
        vertex(buffer, matrix, x1, 0.0F, z1, 0.0F, near);
        vertex(buffer, matrix, x1, length, z1, 0.0F, far);
    }

    private static void vertex(
            VertexConsumer buffer, PoseStack.Pose matrix, float x, float y, float z, float u, float v) {
        buffer.addVertex(matrix, x, y, z)
                .setColor(RED, GREEN, BLUE, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT)
                .setNormal(matrix, 0.0F, 1.0F, 0.0F);
    }

    private record Ray(int bossId, int targetId, double x, double y, double z, long untilTick) {}
}

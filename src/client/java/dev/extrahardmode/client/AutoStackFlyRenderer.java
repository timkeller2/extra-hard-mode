package dev.extrahardmode.client;

import dev.extrahardmode.feature.AutoStackRules;
import dev.extrahardmode.network.ClientboundAutoStackFlyPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/** Ground-item stacks that fly from a player into the chest that received them. */
public final class AutoStackFlyRenderer {
    private static final int STAGGER_TICKS = 1;
    private static final int MAX_STAGGER = 6;
    private static final List<Visual> FLIGHTS = new ArrayList<>();
    private static final RandomSource RANDOM = RandomSource.create();

    private AutoStackFlyRenderer() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                ClientboundAutoStackFlyPayload.TYPE,
                (payload, context) -> Minecraft.getInstance().execute(() -> accept(payload)));
        LevelRenderEvents.COLLECT_SUBMITS.register(AutoStackFlyRenderer::collect);
        ClientTickEvents.END_CLIENT_TICK.register(AutoStackFlyRenderer::tick);
    }

    public static void clear() {
        FLIGHTS.clear();
    }

    static void accept(ClientboundAutoStackFlyPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (payload == null || minecraft.level == null || payload.flights() == null) {
            return;
        }
        long born = minecraft.level.getGameTime();
        int index = 0;
        for (ClientboundAutoStackFlyPayload.Flight flight : payload.flights()) {
            if (flight == null || flight.chest() == null || flight.stack() == null || flight.stack().isEmpty()) {
                continue;
            }
            int delay = Math.min(MAX_STAGGER, index * STAGGER_TICKS);
            FLIGHTS.add(new Visual(
                    payload.playerId(), flight.stack().copy(), flight.chest(), born, delay, new ItemClusterRenderState()));
            index++;
        }
    }

    static void tick(Minecraft client) {
        if (client == null || client.level == null || client.player == null) {
            FLIGHTS.clear();
            return;
        }
        long now = client.level.getGameTime();
        FLIGHTS.removeIf(visual -> now > visual.born + visual.delay + AutoStackRules.FLY_TICKS);
    }

    static void collect(net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext context) {
        if (FLIGHTS.isEmpty() || context.poseStack() == null || context.levelState() == null) {
            return;
        }
        CameraRenderState camera = context.levelState().cameraRenderState;
        if (camera == null || !camera.initialized || camera.pos == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        SubmitNodeCollector collector = context.submitNodeCollector();
        if (minecraft.level == null || minecraft.getItemModelResolver() == null || collector == null) {
            return;
        }
        long now = minecraft.level.getGameTime();
        float partial = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Vec3 cam = camera.pos;
        PoseStack pose = context.poseStack();
        for (Visual visual : FLIGHTS) {
            float age = now - visual.born + partial - visual.delay;
            if (age < 0.0f || age > AutoStackRules.FLY_TICKS) {
                continue;
            }
            Entity entity = minecraft.level.getEntity(visual.playerId);
            if (entity == null) {
                continue;
            }
            Vec3 start = entity.getPosition(partial).add(0.0, entity.getBbHeight() * 0.55, 0.0);
            Vec3 end = chestCenter(minecraft, visual.chest);
            float progress = AutoStackRules.flyProgress(age);
            double x = AutoStackRules.flyCoord(start.x, end.x, progress);
            double y = AutoStackRules.flyCoord(start.y, end.y, progress) + AutoStackRules.flyArc(age);
            double z = AutoStackRules.flyCoord(start.z, end.z, progress);
            visual.state.extractItemGroupRenderState(entity, visual.stack, minecraft.getItemModelResolver());
            if (visual.state.item.isEmpty()) {
                continue;
            }
            pose.pushPose();
            pose.translate(x - cam.x, y - cam.y, z - cam.z);
            float lift = (float) -visual.state.item.getModelBoundingBox().minY + 0.0625F;
            pose.translate(0.0, lift, 0.0);
            pose.rotateDegrees(Axis.YP, (age + visual.delay) * 18.0F);
            int light = LightCoordsUtil.max(
                    LightCoordsUtil.getLightCoords(minecraft.level, BlockPos.containing(x, y, z)),
                    LightCoordsUtil.pack(6, 0));
            ItemEntityRenderer.submitMultipleFromCount(pose, collector, light, visual.state, RANDOM);
            pose.popPose();
        }
    }

    private static Vec3 chestCenter(Minecraft minecraft, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        if (minecraft.level == null) {
            return center;
        }
        BlockState state = minecraft.level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock) || !state.hasProperty(ChestBlock.TYPE)) {
            return center;
        }
        if (state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return center;
        }
        return center.add(Vec3.atCenterOf(ChestBlock.getConnectedBlockPos(pos, state))).scale(0.5);
    }

    private record Visual(
            int playerId, ItemStack stack, BlockPos chest, long born, int delay, ItemClusterRenderState state) {}
}

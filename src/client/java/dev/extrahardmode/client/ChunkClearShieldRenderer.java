package dev.extrahardmode.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.extrahardmode.feature.ChunkClearRules;
import dev.extrahardmode.network.ClientboundChunkClearShieldPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** A large shield that rises from a chunk the moment the alliance clears it. */
public final class ChunkClearShieldRenderer {
    /** Big enough to read as a shield over the chunk, not a dropped item. */
    private static final float SCALE = 5.0F;
    private static final List<Visual> SHIELDS = new ArrayList<>();

    private ChunkClearShieldRenderer() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(
                ClientboundChunkClearShieldPayload.TYPE,
                (payload, context) -> Minecraft.getInstance().execute(() -> accept(payload)));
        LevelRenderEvents.COLLECT_SUBMITS.register(ChunkClearShieldRenderer::collect);
        ClientTickEvents.END_CLIENT_TICK.register(ChunkClearShieldRenderer::tick);
    }

    public static void clear() {
        SHIELDS.clear();
    }

    static void accept(ClientboundChunkClearShieldPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (payload == null || payload.origin() == null || minecraft.level == null) {
            return;
        }
        ItemStack shield = new ItemStack(Items.SHIELD);
        ItemStackRenderState state = new ItemStackRenderState();
        if (minecraft.player != null && minecraft.getItemModelResolver() != null) {
            minecraft.getItemModelResolver()
                    .updateForLiving(state, shield, ItemDisplayContext.FIXED, minecraft.player);
        }
        SHIELDS.add(new Visual(payload.origin(), minecraft.level.getGameTime(), shield, state));
    }

    static void tick(Minecraft client) {
        if (client == null || client.level == null || client.player == null) {
            SHIELDS.clear();
            return;
        }
        long now = client.level.getGameTime();
        SHIELDS.removeIf(visual -> now > visual.born + ChunkClearRules.RISE_TICKS);
    }

    static void collect(net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext context) {
        if (SHIELDS.isEmpty() || context.poseStack() == null || context.levelState() == null) {
            return;
        }
        CameraRenderState camera = context.levelState().cameraRenderState;
        if (camera == null || !camera.initialized || camera.pos == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        SubmitNodeCollector collector = context.submitNodeCollector();
        if (minecraft.level == null || minecraft.player == null || collector == null) {
            return;
        }
        if (minecraft.getItemModelResolver() == null) {
            return;
        }
        long now = minecraft.level.getGameTime();
        float partial = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Vec3 cam = camera.pos;
        PoseStack pose = context.poseStack();
        for (Visual visual : SHIELDS) {
            float age = now - visual.born + partial;
            if (age < 0.0F || age > ChunkClearRules.RISE_TICKS) {
                continue;
            }
            if (visual.state.isEmpty()) {
                minecraft.getItemModelResolver()
                        .updateForLiving(visual.state, visual.shield, ItemDisplayContext.FIXED, minecraft.player);
            }
            if (visual.state.isEmpty()) {
                continue;
            }
            double y = visual.origin.getY() + ChunkClearRules.riseHeight(age);
            int light = LightCoordsUtil.max(
                    LightCoordsUtil.getLightCoords(minecraft.level, BlockPos.containing(
                            visual.origin.getX(), y, visual.origin.getZ())),
                    LightCoordsUtil.pack(15, 15));
            pose.pushPose();
            pose.translate(visual.origin.getX() + 0.5 - cam.x, y - cam.y, visual.origin.getZ() + 0.5 - cam.z);
            pose.rotateDegrees(Axis.YP, age * 3.0F);
            pose.scale(SCALE, SCALE, SCALE);
            visual.state.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }

    private static final class Visual {
        private final BlockPos origin;
        private final long born;
        private final ItemStack shield;
        private final ItemStackRenderState state;

        private Visual(BlockPos origin, long born, ItemStack shield, ItemStackRenderState state) {
            this.origin = origin;
            this.born = born;
            this.shield = shield;
            this.state = state;
        }
    }
}

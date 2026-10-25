package dev.extrahardmode.client;

import dev.extrahardmode.feature.AchievementFloatRules;
import dev.extrahardmode.network.ClientboundAchievementFloatPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** Small brown count that drifts down and left from a sneak preview or a kill. */
public final class AchievementFloater {
    private static final List<Floater> FLOATERS = new ArrayList<>();

    private AchievementFloater() {}

    public static void register() {
        LevelRenderEvents.COLLECT_SUBMITS.register(AchievementFloater::collect);
    }

    public static void spawn(ClientboundAchievementFloatPayload payload) {
        if (payload == null || payload.remaining() <= 0) {
            return;
        }
        FLOATERS.add(new Floater(payload.x(), payload.y(), payload.z(), payload.remaining(), Util.getMillis()));
    }

    public static void clear() {
        FLOATERS.clear();
    }

    static void collect(net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext context) {
        if (FLOATERS.isEmpty()) {
            return;
        }
        long now = Util.getMillis();
        FLOATERS.removeIf(floater -> !AchievementFloatRules.alive(now - floater.bornMillis));
        if (FLOATERS.isEmpty() || context.poseStack() == null || context.levelState() == null) {
            return;
        }
        CameraRenderState camera = context.levelState().cameraRenderState;
        if (camera == null || !camera.initialized || camera.pos == null || camera.orientation == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        SubmitNodeCollector collector = context.submitNodeCollector();
        if (font == null || collector == null) {
            return;
        }
        Vec3 cam = camera.pos;
        Quaternionf facing = camera.orientation;
        for (Floater floater : FLOATERS) {
            float drift = AchievementFloatRules.driftBlocks(now - floater.bornMillis);
            String text = Integer.toString(floater.remaining);
            var pose = context.poseStack();
            pose.pushPose();
            pose.translate(floater.x - cam.x, floater.y - cam.y, floater.z - cam.z);
            pose.rotate(facing);
            pose.translate(-drift, -drift, 0.0);
            pose.scale(AchievementFloatRules.TEXT_SCALE, -AchievementFloatRules.TEXT_SCALE, AchievementFloatRules.TEXT_SCALE);
            collector.submitText(
                    pose,
                    -font.width(text) / 2.0f,
                    0.0f,
                    Component.literal(text).getVisualOrderText(),
                    true,
                    Font.DisplayMode.SEE_THROUGH,
                    LightCoordsUtil.FULL_BRIGHT,
                    AchievementFloatRules.COLOR,
                    0,
                    0);
            pose.popPose();
        }
    }

    private record Floater(double x, double y, double z, int remaining, long bornMillis) {}
}

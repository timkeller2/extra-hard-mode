package dev.extrahardmode.client;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.feature.AbilityDurationRules;
import dev.extrahardmode.feature.CouncilMissionRules;
import dev.extrahardmode.network.ClientboundCouncilBountyPayload;
import dev.extrahardmode.network.ClientboundWiseLessonPayload;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** Hunt lines stacked above council bounties, just over the ability icons. */
public final class WiseLessonHud {
    public static final Identifier ELEMENT_ID = ExtraHardModeMod.id("wise_lesson");
    public static final int COLOR = 0xFFD6E2C5;

    private WiseLessonHud() {}

    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, ELEMENT_ID, WiseLessonHud::extract);
    }

    static void extract(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta) {
        ClientboundWiseLessonPayload lesson = ExtraHardModeClient.lastWiseLesson();
        if (lesson == null || !lesson.visible()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator() || minecraft.font == null) {
            return;
        }
        List<String> lines = lesson.lines();
        int bountyCount = 0;
        ClientboundCouncilBountyPayload bounty = ExtraHardModeClient.lastCouncilBounty();
        if (bounty != null && bounty.visible()) {
            bountyCount = bounty.lines().size();
        }
        int total = bountyCount + lines.size();
        int x = AbilityDurationRules.originX();
        int lineHeight = minecraft.font.lineHeight;
        for (int i = 0; i < lines.size(); i++) {
            int y = CouncilMissionRules.bountyHudLineY(graphics.guiHeight(), lineHeight, bountyCount + i, total);
            graphics.text(minecraft.font, lines.get(i), x, y, COLOR, true);
        }
    }
}

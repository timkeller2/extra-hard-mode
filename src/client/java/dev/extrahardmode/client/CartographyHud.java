package dev.extrahardmode.client;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.feature.AbilityDurationRules;
import dev.extrahardmode.feature.CouncilMissionRules;
import dev.extrahardmode.network.ClientboundCartographyPayload;
import dev.extrahardmode.network.ClientboundCouncilBountyPayload;
import dev.extrahardmode.network.ClientboundMinerLeadPayload;
import dev.extrahardmode.network.ClientboundWiseLessonPayload;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** Map marks stacked above old-miner leads, wise-teacher hunts, and council bounties. */
public final class CartographyHud {
    public static final Identifier ELEMENT_ID = ExtraHardModeMod.id("cartography");
    public static final int COLOR = 0xFF7EB6D9;

    private CartographyHud() {}

    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, ELEMENT_ID, CartographyHud::extract);
    }

    static void extract(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta) {
        ClientboundCartographyPayload marks = ExtraHardModeClient.lastCartography();
        if (marks == null || !marks.visible()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator() || minecraft.font == null) {
            return;
        }
        List<String> lines = marks.lines();
        int below = 0;
        ClientboundCouncilBountyPayload bounty = ExtraHardModeClient.lastCouncilBounty();
        if (bounty != null && bounty.visible()) {
            below += bounty.lines().size();
        }
        ClientboundWiseLessonPayload lesson = ExtraHardModeClient.lastWiseLesson();
        if (lesson != null && lesson.visible()) {
            below += lesson.lines().size();
        }
        ClientboundMinerLeadPayload leads = ExtraHardModeClient.lastMinerLead();
        if (leads != null && leads.visible()) {
            below += leads.lines().size();
        }
        int total = below + lines.size();
        int x = AbilityDurationRules.originX();
        int lineHeight = minecraft.font.lineHeight;
        for (int i = 0; i < lines.size(); i++) {
            int y = CouncilMissionRules.bountyHudLineY(graphics.guiHeight(), lineHeight, below + i, total);
            graphics.text(minecraft.font, lines.get(i), x, y, COLOR, true);
        }
    }
}

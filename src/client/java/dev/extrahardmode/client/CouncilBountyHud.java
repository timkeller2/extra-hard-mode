package dev.extrahardmode.client;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.feature.AbilityDurationRules;
import dev.extrahardmode.feature.CouncilMissionRules;
import dev.extrahardmode.network.ClientboundCouncilBountyPayload;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** Bottom-left bounty counter, drawn above the mana ability duration icons. */
public final class CouncilBountyHud {
    public static final Identifier ELEMENT_ID = ExtraHardModeMod.id("council_bounty");
    public static final int COLOR = 0xFFE8D56A;

    private CouncilBountyHud() {}

    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, ELEMENT_ID, CouncilBountyHud::extract);
    }

    static void extract(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta) {
        ClientboundCouncilBountyPayload bounty = ExtraHardModeClient.lastCouncilBounty();
        if (bounty == null || !bounty.visible()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator() || minecraft.font == null) {
            return;
        }
        String text = CouncilMissionRules.hudLabel(bounty.label(), bounty.kills(), bounty.target());
        int x = AbilityDurationRules.originX();
        int y = CouncilMissionRules.bountyHudY(graphics.guiHeight(), minecraft.font.lineHeight);
        graphics.text(minecraft.font, text, x, y, COLOR, true);
    }
}

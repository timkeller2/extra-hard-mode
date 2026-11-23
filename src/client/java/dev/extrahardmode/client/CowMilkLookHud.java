package dev.extrahardmode.client;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.feature.CowMilkRules;
import dev.extrahardmode.network.ClientboundSyncPayload;
import dev.extrahardmode.player.EhmAttachments;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.phys.EntityHitResult;

/** Remaining milk wait on the cow or mooshroom under the crosshair. */
public final class CowMilkLookHud {
    public static final Identifier ELEMENT_ID = ExtraHardModeMod.id("cow_milk_look");
    private static final int OFFSET_Y = 12;
    private static final int COLOR = 0xFFFFC14A;

    private CowMilkLookHud() {}

    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, ELEMENT_ID, CowMilkLookHud::extract);
    }

    static void extract(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta) {
        ClientboundSyncPayload sync = ExtraHardModeClient.lastSync();
        if (sync != null && !sync.worldActive()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator() || minecraft.font == null) {
            return;
        }
        if (!(minecraft.hitResult instanceof EntityHitResult hit)) {
            return;
        }
        if (!(hit.getEntity() instanceof AbstractCow cow)) {
            return;
        }
        int remaining = CowMilkRules.remainingTicks(
                cow.level().getGameTime(), cow.getAttachedOrElse(EhmAttachments.EHM_MILK_READY_AT, 0L));
        if (remaining <= 0) {
            return;
        }
        graphics.centeredText(
                minecraft.font,
                CowMilkRules.remainingLabel(remaining),
                graphics.guiWidth() / 2,
                graphics.guiHeight() / 2 + OFFSET_Y,
                COLOR);
    }
}

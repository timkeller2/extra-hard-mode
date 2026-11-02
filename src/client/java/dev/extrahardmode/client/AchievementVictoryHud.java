package dev.extrahardmode.client;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.feature.AchievementFloatRules;
import dev.extrahardmode.feature.AchievementRules;
import dev.extrahardmode.network.ClientboundAchievementScorePayload;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

/** Top-left achievement victory counter, and the credits that follow a win. */
public final class AchievementVictoryHud {
    public static final Identifier ELEMENT_ID = ExtraHardModeMod.id("achievement_victory");
    /** Half the normal HUD font, tucked into the top-left corner. */
    static final float TEXT_SCALE = 0.5F;
    static final int MARGIN = 2;

    private static volatile ClientboundAchievementScorePayload score;

    private AchievementVictoryHud() {}

    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, ELEMENT_ID, AchievementVictoryHud::extract);
    }

    public static void onScore(ClientboundAchievementScorePayload payload) {
        score = payload;
    }

    public static void clear() {
        score = null;
    }

    public static void showCredits() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        minecraft.execute(() -> openCredits(minecraft));
    }

    private static void openCredits(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.gui.screen() instanceof WinScreen) {
            return;
        }
        boolean[] finished = {false};
        minecraft.gui.setScreen(new WinScreen(false, () -> finishCredits(minecraft, finished)));
    }

    private static void finishCredits(Minecraft minecraft, boolean[] finished) {
        if (finished[0]) {
            return;
        }
        finished[0] = true;
        if (minecraft.gui.screen() instanceof WinScreen) {
            minecraft.gui.setScreen(null);
        }
    }

    static void extract(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta) {
        ClientboundAchievementScorePayload current = score;
        if (current == null || !current.visible()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator() || minecraft.font == null) {
            return;
        }
        String text = AchievementRules.victoryProgress(current.earned(), current.required());
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        try {
            pose.translate(MARGIN, MARGIN);
            pose.scale(TEXT_SCALE, TEXT_SCALE);
            graphics.text(minecraft.font, text, 0, 0, AchievementFloatRules.COLOR, true);
        } finally {
            pose.popMatrix();
        }
    }
}

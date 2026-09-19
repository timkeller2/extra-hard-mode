package dev.extrahardmode.client;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.feature.ManaHudRules;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/**
 * Heart-style mana crystals: black containers, cyan full/half fill, packed then wrapped.
 */
public final class ManaHud {
    public static final Identifier ELEMENT_ID = ExtraHardModeMod.id("mana");
    public static final Identifier CONTAINER = ExtraHardModeMod.id("hud/mana_container");
    public static final Identifier FULL = ExtraHardModeMod.id("hud/mana_full");
    public static final Identifier HALF = ExtraHardModeMod.id("hud/mana_half");

    private ManaHud() {}

    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.ARMOR_BAR, ELEMENT_ID, ManaHud::extract);
        HudStatusBarHeightRegistry.addLeft(ELEMENT_ID, ManaHud::statusBarHeight);
    }

    static int statusBarHeight(Player player) {
        if (!shouldRender(player)) {
            return 0;
        }
        return ManaHudRules.layout(ManaHudRules.crystalCount(manaLevel(), currentMana())).height();
    }

    static void extract(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !shouldRender(minecraft.player)) {
            return;
        }
        int level = manaLevel();
        float current = currentMana();
        int crystals = ManaHudRules.crystalCount(level, current);
        if (crystals <= 0) {
            return;
        }
        ManaHudRules.Layout layout = ManaHudRules.layout(crystals);
        int left = graphics.guiWidth() / 2 - 91;
        int top = graphics.guiHeight() - HudStatusBarHeightRegistry.getHeight(ELEMENT_ID);
        for (int index = 0; index < crystals; index++) {
            int x = left + ManaHudRules.offsetX(index, layout);
            int y = top + ManaHudRules.offsetY(index, layout);
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED, CONTAINER, x, y, ManaHudRules.CRYSTAL_SIZE, ManaHudRules.CRYSTAL_SIZE);
            Identifier fill =
                    switch (ManaHudRules.fill(index, current)) {
                        case FULL -> FULL;
                        case HALF -> HALF;
                        case EMPTY -> null;
                    };
            if (fill != null) {
                graphics.blitSprite(
                        RenderPipelines.GUI_TEXTURED, fill, x, y, ManaHudRules.CRYSTAL_SIZE, ManaHudRules.CRYSTAL_SIZE);
            }
        }
    }

    static boolean shouldRender(Player player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (player == null || player.isSpectator()) {
            return false;
        }
        if (minecraft.gameMode == null || !minecraft.gameMode.canHurtPlayer()) {
            return false;
        }
        return ManaHudRules.crystalCount(manaLevel(), currentMana()) > 0;
    }

    private static int manaLevel() {
        var mana = ExtraHardModeClient.lastMana();
        return mana == null ? 0 : Math.max(0, mana.manaLevel());
    }

    private static float currentMana() {
        var mana = ExtraHardModeClient.lastMana();
        return mana == null ? 0.0F : Math.max(0.0F, mana.currentMana());
    }
}

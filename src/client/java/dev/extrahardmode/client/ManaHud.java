package dev.extrahardmode.client;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.feature.AbilityRules;
import dev.extrahardmode.feature.HudCapacityRules;
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
 * Heart-style mana crystals: black containers, cyan fill in eighths, packed then wrapped.
 * Crystals gained over about ten minutes are drawn faintly, in the same eighths.
 */
public final class ManaHud {
    public static final Identifier ELEMENT_ID = ExtraHardModeMod.id("mana");
    public static final Identifier CONTAINER = ExtraHardModeMod.id("hud/mana_container");
    public static final Identifier FULL = ExtraHardModeMod.id("hud/mana_full");

    private ManaHud() {}

    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.ARMOR_BAR, ELEMENT_ID, ManaHud::extract);
        HudStatusBarHeightRegistry.addLeft(ELEMENT_ID, ManaHud::statusBarHeight);
    }

    static int statusBarHeight(Player player) {
        if (!shouldRender(player)) {
            return 0;
        }
        int level = manaLevel();
        float current = currentMana();
        double preview = projected(player, level, current);
        int crystals = ManaHudRules.drawnCrystalCount(level, current, preview);
        return ManaHudRules.layout(crystals).height();
    }

    static void extract(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !shouldRender(minecraft.player)) {
            return;
        }
        int level = manaLevel();
        float current = currentMana();
        double preview = projected(minecraft.player, level, current);
        int solid = ManaHudRules.crystalCount(level, current);
        int crystals = ManaHudRules.drawnCrystalCount(level, current, preview);
        if (crystals <= 0) {
            return;
        }
        int solidPoints = ManaHudRules.displayedPoints(level, current);
        int previewPoints = ManaHudRules.displayedPoints(level, preview);
        ManaHudRules.Layout layout = ManaHudRules.layout(crystals);
        int left = graphics.guiWidth() / 2 - 91;
        int top = graphics.guiHeight() - HudStatusBarHeightRegistry.getHeight(ELEMENT_ID);
        for (int index = 0; index < crystals; index++) {
            int x = left + ManaHudRules.offsetX(index, layout);
            int y = top + ManaHudRules.offsetY(index, layout);
            boolean extra = index >= solid;
            boolean halfCapacity = HudCapacityRules.halfShadow(extra ? previewPoints : solidPoints, index);
            if (halfCapacity) {
                graphics.enableScissor(x, y, x + ManaHudRules.HALF_WIDTH, y + ManaHudRules.CRYSTAL_SIZE);
            }
            if (extra) {
                graphics.blitSprite(
                        RenderPipelines.GUI_TEXTURED,
                        CONTAINER,
                        x,
                        y,
                        ManaHudRules.CRYSTAL_SIZE,
                        ManaHudRules.CRYSTAL_SIZE,
                        ManaHudRules.PREVIEW_ALPHA);
            } else {
                graphics.blitSprite(
                        RenderPipelines.GUI_TEXTURED,
                        CONTAINER,
                        x,
                        y,
                        ManaHudRules.CRYSTAL_SIZE,
                        ManaHudRules.CRYSTAL_SIZE);
            }
            if (halfCapacity) {
                graphics.disableScissor();
            }
            if (!extra) {
                blitEighths(graphics, x, y, ManaHudRules.eighths(index, current), 1.0F);
            }
            if (level <= 0) {
                continue;
            }
            Integer ghost = ManaHudRules.previewEighths(index, current, preview);
            if (ghost == null) {
                continue;
            }
            blitEighths(graphics, x, y, ghost, ManaHudRules.PREVIEW_ALPHA);
        }
    }

    /** Bottom slice of the full crystal. Eight eighths is the whole sprite. */
    private static void blitEighths(GuiGraphicsExtractor graphics, int x, int y, int eighths, float alpha) {
        int height = ManaHudRules.eighthHeight(eighths);
        if (height <= 0) {
            return;
        }
        boolean clip = height < ManaHudRules.CRYSTAL_SIZE;
        if (clip) {
            int top = y + ManaHudRules.CRYSTAL_SIZE - height;
            graphics.enableScissor(x, top, x + ManaHudRules.CRYSTAL_SIZE, y + ManaHudRules.CRYSTAL_SIZE);
        }
        if (alpha < 1.0F) {
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    FULL,
                    x,
                    y,
                    ManaHudRules.CRYSTAL_SIZE,
                    ManaHudRules.CRYSTAL_SIZE,
                    alpha);
        } else {
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    FULL,
                    x,
                    y,
                    ManaHudRules.CRYSTAL_SIZE,
                    ManaHudRules.CRYSTAL_SIZE);
        }
        if (clip) {
            graphics.disableScissor();
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

    private static double projected(Player player, int level, float current) {
        double saturation = player == null ? 0.0 : player.getFoodData().getSaturationLevel();
        return ManaHudRules.previewMana(level, current, saturation, batteryMultiplier());
    }

    /**
     * Active only while the duration payload still lists Battery.
     * Power comes from the ready icon. A held quartz block shows that icon without being active.
     */
    private static double batteryMultiplier() {
        boolean active = false;
        var durations = ExtraHardModeClient.lastDurations();
        if (durations != null) {
            for (var effect : durations.effects()) {
                if (AbilityRules.BATTERY.equals(effect.ability()) && effect.remainingTicks() > 0) {
                    active = true;
                    break;
                }
            }
        }
        boolean powerKnown = false;
        double power = 0.0;
        if (active) {
            var ready = ExtraHardModeClient.lastAbilityReady();
            if (ready != null) {
                for (var entry : ready.abilities()) {
                    if (AbilityRules.BATTERY.equals(entry.ability())) {
                        powerKnown = true;
                        power = entry.power();
                        break;
                    }
                }
            }
        }
        return ManaHudRules.previewBatteryMultiplier(active, powerKnown, power);
    }
}

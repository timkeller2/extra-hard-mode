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
 * Heart-style mana crystals: black containers, cyan full/half fill, packed then wrapped.
 * Crystals gained over about ten minutes are drawn faintly, with the same half and full sprites.
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
                Identifier fill =
                        switch (ManaHudRules.fill(index, current)) {
                            case FULL -> FULL;
                            case HALF -> HALF;
                            case EMPTY -> null;
                        };
                if (fill != null) {
                    graphics.blitSprite(
                            RenderPipelines.GUI_TEXTURED,
                            fill,
                            x,
                            y,
                            ManaHudRules.CRYSTAL_SIZE,
                            ManaHudRules.CRYSTAL_SIZE);
                }
            }
            if (level <= 0) {
                continue;
            }
            ManaHudRules.Fill ghost = ManaHudRules.previewFill(index, current, preview);
            if (ghost != ManaHudRules.Fill.HALF && ghost != ManaHudRules.Fill.FULL) {
                continue;
            }
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    ghost == ManaHudRules.Fill.FULL ? FULL : HALF,
                    x,
                    y,
                    ManaHudRules.CRYSTAL_SIZE,
                    ManaHudRules.CRYSTAL_SIZE,
                    ManaHudRules.PREVIEW_ALPHA);
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

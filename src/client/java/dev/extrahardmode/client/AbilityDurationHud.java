package dev.extrahardmode.client;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.feature.AbilityDurationRules;
import dev.extrahardmode.feature.AbilityRules;
import dev.extrahardmode.network.ClientboundAbilityDurationsPayload;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Stacked duration bars for flight, light, Iron Heart, and power mining. */
public final class AbilityDurationHud {
    public static final net.minecraft.resources.Identifier ELEMENT_ID = ExtraHardModeMod.id("ability_durations");
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    private static final int ROW_GAP = 6;

    private AbilityDurationHud() {}

    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, ELEMENT_ID, AbilityDurationHud::extract);
    }

    static void extract(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta) {
        ClientboundAbilityDurationsPayload payload = ExtraHardModeClient.lastDurations();
        if (payload == null || payload.effects().isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator()) {
            return;
        }
        long time = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        int x = graphics.guiWidth() / 2 - BAR_WIDTH / 2;
        int y = graphics.guiHeight() / 2 + 16;
        for (ClientboundAbilityDurationsPayload.Entry effect : payload.effects()) {
            if (effect.remainingTicks() <= 0 || effect.maxTicks() <= 0) {
                continue;
            }
            drawBar(graphics, effect, x, y, time);
            y += BAR_HEIGHT + ROW_GAP;
        }
    }

    static void drawBar(
            GuiGraphicsExtractor graphics,
            ClientboundAbilityDurationsPayload.Entry effect,
            int x,
            int y,
            long time) {
        boolean auto = effect.autoContinue();
        float alpha = AbilityDurationRules.alpha(effect.remainingTicks(), auto);
        boolean showFill = AbilityDurationRules.visible(effect.remainingTicks(), auto, time);
        int filled = Math.max(1, (int) (BAR_WIDTH * (effect.remainingTicks() / (float) effect.maxTicks())));
        filled = Math.min(BAR_WIDTH, filled);
        graphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, AbilityDurationRules.argb(0x000000, alpha));
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, AbilityDurationRules.argb(trackRgb(effect.ability()), alpha));
        if (showFill) {
            graphics.fill(
                    x, y, x + filled, y + BAR_HEIGHT, AbilityDurationRules.argb(fillRgb(effect.ability()), alpha));
        }
        graphics.item(icon(effect.ability()), x - 20, y - 6);
    }

    static int trackRgb(String ability) {
        if (AbilityRules.POWER_MINE.equals(ability)) {
            return 0x1A2430;
        }
        if (AbilityRules.LIGHT.equals(ability)) {
            return 0x2A2610;
        }
        if (AbilityRules.IRON_HEART.equals(ability)) {
            return 0x22262C;
        }
        return 0x2A2418;
    }

    static int fillRgb(String ability) {
        if (AbilityRules.POWER_MINE.equals(ability)) {
            return 0x6EC1E4;
        }
        if (AbilityRules.LIGHT.equals(ability)) {
            return 0xFFF2A8;
        }
        if (AbilityRules.IRON_HEART.equals(ability)) {
            return 0xC5CDD6;
        }
        return 0xE8D56A;
    }

    static ItemStack icon(String ability) {
        if (AbilityRules.POWER_MINE.equals(ability)) {
            return new ItemStack(Items.IRON_PICKAXE);
        }
        if (AbilityRules.LIGHT.equals(ability)) {
            return new ItemStack(Items.COAL);
        }
        if (AbilityRules.IRON_HEART.equals(ability)) {
            return new ItemStack(Items.IRON_INGOT);
        }
        return new ItemStack(Items.FEATHER);
    }
}

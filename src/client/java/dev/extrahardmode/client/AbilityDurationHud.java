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

/** Bottom-left duration icons with a short bar under each for flight, light, Iron Heart, power mining, and Master Builder. */
public final class AbilityDurationHud {
    public static final net.minecraft.resources.Identifier ELEMENT_ID = ExtraHardModeMod.id("ability_durations");

    private AbilityDurationHud() {}

    public static void register() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, ELEMENT_ID, AbilityDurationHud::extract);
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
        int originX = AbilityDurationRules.originX();
        int originY = AbilityDurationRules.originY(graphics.guiHeight());
        int index = 0;
        for (ClientboundAbilityDurationsPayload.Entry effect : payload.effects()) {
            if (effect.remainingTicks() <= 0 || effect.maxTicks() <= 0) {
                continue;
            }
            drawBar(graphics, effect, AbilityDurationRules.iconX(originX, index), originY, time);
            index++;
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
        int barY = AbilityDurationRules.barY(y);
        int barBottom = barY + AbilityDurationRules.BAR_HEIGHT + 2 * AbilityDurationRules.BAR_OUTLINE;
        int innerLeft = x + AbilityDurationRules.BAR_OUTLINE;
        int innerTop = barY + AbilityDurationRules.BAR_OUTLINE;
        int innerRight = x + AbilityDurationRules.BAR_WIDTH - AbilityDurationRules.BAR_OUTLINE;
        int innerBottom = barY + AbilityDurationRules.BAR_OUTLINE + AbilityDurationRules.BAR_HEIGHT;
        int filled = AbilityDurationRules.filledWidth(effect.remainingTicks(), effect.maxTicks());
        graphics.item(icon(effect.ability()), x, y);
        graphics.fill(
                x,
                barY,
                x + AbilityDurationRules.BAR_WIDTH,
                barBottom,
                AbilityDurationRules.argb(0x000000, alpha));
        graphics.fill(
                innerLeft,
                innerTop,
                innerRight,
                innerBottom,
                AbilityDurationRules.argb(trackRgb(effect.ability()), alpha));
        if (showFill && filled > 0) {
            graphics.fill(
                    innerLeft,
                    innerTop,
                    innerLeft + filled,
                    innerBottom,
                    AbilityDurationRules.argb(fillRgb(effect.ability()), alpha));
        }
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
        if (AbilityRules.MASTER_BUILDER.equals(ability)) {
            return 0x2A2824;
        }
        if (AbilityRules.DIAMOND_SKIN.equals(ability)) {
            return 0x12343A;
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
        if (AbilityRules.MASTER_BUILDER.equals(ability)) {
            return 0xC4B8A0;
        }
        if (AbilityRules.DIAMOND_SKIN.equals(ability)) {
            return 0x5CFFF0;
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
        if (AbilityRules.MASTER_BUILDER.equals(ability)) {
            return new ItemStack(Items.STONE_BRICKS);
        }
        if (AbilityRules.DIAMOND_SKIN.equals(ability)) {
            return new ItemStack(Items.DIAMOND);
        }
        return new ItemStack(Items.FEATHER);
    }
}

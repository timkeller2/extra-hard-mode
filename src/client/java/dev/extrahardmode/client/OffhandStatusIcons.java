package dev.extrahardmode.client;

import dev.extrahardmode.feature.OffhandStatusIconRules;
import dev.extrahardmode.network.ClientboundSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix3x2fStack;

/**
 * Mana crystal, Well Fed chicken, and enchanting skill above the offhand slot.
 * Mana and Well Fed are omitted at 0. The crystal uses the mana HUD sprite. The
 * chicken uses the hunger-bar drumstick. The skill uses an enchantment table.
 */
public final class OffhandStatusIcons {
    private static final Identifier FOOD_EMPTY = Identifier.withDefaultNamespace("hud/food_empty");
    private static final Identifier FOOD_FULL = Identifier.withDefaultNamespace("hud/food_full");
    private static final ItemStack ENCHANTING_TABLE = new ItemStack(Items.ENCHANTING_TABLE);
    private static final int NUMBER = 0xFFFFFFFF;

    private OffhandStatusIcons() {}

    public static void draw(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int leftPos, int topPos) {
        if (graphics == null || !visible()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.font == null) {
            return;
        }
        var mana = ExtraHardModeClient.lastMana();
        int wellFed = mana == null ? 0 : mana.wellFed();
        int enchantAbility = mana == null ? -1 : mana.enchantAbility();
        double capacity = ExtraHardModeClient.lastManaLearning();
        boolean showCapacity = OffhandStatusIconRules.showCapacity(capacity);
        boolean showWellFed = OffhandStatusIconRules.showWellFed(wellFed);
        boolean showEnchant = OffhandStatusIconRules.showEnchant(enchantAbility);
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;
        String manaLabel = showCapacity
                ? OffhandStatusIconRules.manaLabel(ExtraHardModeClient.lastBaseManaLevel(), capacity)
                : null;
        String wellFedLabel = showWellFed ? OffhandStatusIconRules.wellFedLabel(wellFed) : null;
        String enchantLabel = showEnchant ? OffhandStatusIconRules.enchantLabel(enchantAbility) : null;
        float numberScale = OffhandStatusIconRules.numberScale(
                manaLabel == null ? 0 : minecraft.font.width(manaLabel),
                wellFedLabel == null ? 0 : minecraft.font.width(wellFedLabel),
                enchantLabel == null ? 0 : minecraft.font.width(enchantLabel));
        int crystalRow = OffhandStatusIconRules.crystalRow(showCapacity, showWellFed);
        if (crystalRow >= 0 && manaLabel != null) {
            int x = OffhandStatusIconRules.iconX();
            int y = OffhandStatusIconRules.rowY(crystalRow);
            blit(graphics, ManaHud.CONTAINER, x, y);
            blit(graphics, ManaHud.FULL, x, y);
            drawNumber(graphics, minecraft.font, manaLabel, x, y, numberScale);
            if (OffhandStatusIconRules.hit(x, y, localX, localY)) {
                tooltip(graphics, minecraft.font, "tougher.tooltip.mana_learning", "Mana %s", manaLabel, mouseX, mouseY);
            }
        }
        int chickenRow = OffhandStatusIconRules.chickenRow(showCapacity, showWellFed);
        if (chickenRow >= 0 && wellFedLabel != null) {
            int x = OffhandStatusIconRules.iconX();
            int y = OffhandStatusIconRules.rowY(chickenRow);
            blit(graphics, FOOD_EMPTY, x, y);
            blit(graphics, FOOD_FULL, x, y);
            drawNumber(graphics, minecraft.font, wellFedLabel, x, y, numberScale);
            if (OffhandStatusIconRules.hit(x, y, localX, localY)) {
                tooltip(
                        graphics,
                        minecraft.font,
                        "tougher.tooltip.well_fed_level",
                        "Well Fed %s",
                        wellFedLabel,
                        mouseX,
                        mouseY);
            }
        }
        int enchantRow = OffhandStatusIconRules.enchantRow(showCapacity, showWellFed, showEnchant);
        if (enchantRow >= 0 && enchantLabel != null) {
            int x = OffhandStatusIconRules.iconX();
            int y = OffhandStatusIconRules.rowY(enchantRow);
            graphics.item(ENCHANTING_TABLE, x, y);
            drawNumber(graphics, minecraft.font, enchantLabel, x, y, numberScale);
            if (OffhandStatusIconRules.hit(x, y, localX, localY)) {
                tooltip(
                        graphics,
                        minecraft.font,
                        "tougher.tooltip.enchant_ability",
                        "Enchanting skill %s",
                        enchantLabel,
                        mouseX,
                        mouseY);
            }
        }
    }

    private static boolean visible() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator()) {
            return false;
        }
        ClientboundSyncPayload sync = ExtraHardModeClient.lastSync();
        return sync != null && sync.worldActive();
    }

    private static void blit(GuiGraphicsExtractor graphics, Identifier sprite, int x, int y) {
        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED, sprite, x, y, OffhandStatusIconRules.ICON, OffhandStatusIconRules.ICON);
    }

    private static void drawNumber(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, float scale) {
        int width = Math.max(1, font.width(text));
        float drawWidth = width * scale;
        float drawHeight = font.lineHeight * scale;
        float tx = x + OffhandStatusIconRules.ICON - drawWidth;
        float ty = y + OffhandStatusIconRules.ICON - drawHeight;
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        try {
            pose.translate(tx, ty);
            pose.scale(scale, scale);
            graphics.text(font, text, 0, 0, NUMBER, true);
        } finally {
            pose.popMatrix();
        }
    }

    private static void tooltip(
            GuiGraphicsExtractor graphics,
            Font font,
            String key,
            String fallback,
            String label,
            int mouseX,
            int mouseY) {
        graphics.setTooltipForNextFrame(
                font,
                Component.translatableWithFallback(key, fallback, Component.literal(label)),
                mouseX,
                mouseY);
    }
}

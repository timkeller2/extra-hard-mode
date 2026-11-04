package dev.extrahardmode.client;

import dev.extrahardmode.feature.FoodCraving;
import dev.extrahardmode.feature.FoodCravingRules;
import dev.extrahardmode.feature.Hunger;
import dev.extrahardmode.network.ClientboundFoodCravingPayload;
import dev.extrahardmode.network.ClientboundSyncPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Small craving mark in the top-right corner of a food icon, plus one tooltip line. */
public final class FoodCravingOutline {
    private static final int MARK = 5;

    private FoodCravingOutline() {}

    public static void draw(GuiGraphicsExtractor graphics, Slot slot) {
        if (graphics == null || slot == null || !slot.hasItem()) {
            return;
        }
        drawOnItem(graphics, slot.getItem(), slot.x, slot.y);
    }

    /** {@code x} and {@code y} are the top-left of the 16-pixel item icon. */
    public static void drawOnItem(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        if (graphics == null) {
            return;
        }
        FoodCraving craving = craving(stack);
        if (craving == null || craving == FoodCraving.NORMAL) {
            return;
        }
        int left = x + 16 - MARK;
        int top = y;
        switch (craving) {
            case VERY_SICK -> icon(graphics, left, top, 0xFFE6E6E6, 0xFF141414, "#...#", ".#.#.", "..#..", ".#.#.", "#...#");
            case SICK -> icon(graphics, left, top, 0xFF3A2418, 0xFFC4956A, ".....", "#...#", ".#.#.", "..#..", ".....");
            case SLIGHT -> icon(graphics, left, top, 0xFF1A242C, 0xFFB7D0E0, ".....", "..#..", ".#.#.", ".....", ".....");
            case GRACE -> icon(graphics, left, top, 0xFF1A242C, 0xFFB7D0E0, "..#..", ".#.#.", "#...#", ".....", ".....");
            case CRAVING -> icon(graphics, left, top, 0xFF102418, 0xFF7DCEA0, "..#..", ".#.#.", "#...#", ".....", ".....");
            case GREAT -> icon(graphics, left, top, 0xFF2A220C, 0xFFF0C14A, "..#..", ".#.#.", "#...#", ".....", ".....");
            case NORMAL -> {
                // No mark.
            }
        }
    }

    /** Item tooltip plus the craving line. Used for shop offers, which do not go through a slot. */
    public static void showTooltip(GuiGraphicsExtractor graphics, Font font, ItemStack stack, int mouseX, int mouseY) {
        if (graphics == null || font == null || stack == null) {
            return;
        }
        List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), stack));
        Component craving = tooltip(stack);
        if (craving != null) {
            lines.add(craving);
        }
        graphics.setTooltipForNextFrame(
                font, lines, stack.getTooltipImage(), mouseX, mouseY, stack.get(DataComponents.TOOLTIP_STYLE), true);
    }

    public static Component tooltip(ItemStack stack) {
        FoodCraving craving = craving(stack);
        String key = tooltipKey(craving);
        if (key == null) {
            return null;
        }
        return Component.translatableWithFallback(key, tooltipFallback(craving))
                .withStyle(style -> style.withColor(tooltipRgb(craving)));
    }

    static FoodCraving craving(ItemStack stack) {
        if (!visible() || stack == null || stack.isEmpty() || stack.get(DataComponents.FOOD) == null) {
            return FoodCraving.NORMAL;
        }
        ClientboundFoodCravingPayload payload = ExtraHardModeClient.lastFoodCraving();
        return FoodCravingRules.classify(payload.history(), payload.wellFed(), Hunger.itemId(stack));
    }

    static boolean visible() {
        ClientboundFoodCravingPayload payload = ExtraHardModeClient.lastFoodCraving();
        if (payload == null || !payload.active()) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator()) {
            return false;
        }
        ClientboundSyncPayload sync = ExtraHardModeClient.lastSync();
        if (sync != null && !sync.worldActive()) {
            return false;
        }
        return sync == null || !sync.extras().creativeBypasses() || !minecraft.player.isCreative();
    }

    private static void icon(
            GuiGraphicsExtractor graphics, int x, int y, int plate, int ink, String... rows) {
        graphics.fill(x, y, x + MARK, y + MARK, plate);
        for (int row = 0; row < rows.length && row < MARK; row++) {
            String line = rows[row];
            for (int col = 0; col < line.length() && col < MARK; col++) {
                if (line.charAt(col) == '#') {
                    graphics.fill(x + col, y + row, x + col + 1, y + row + 1, ink);
                }
            }
        }
    }

    private static String tooltipKey(FoodCraving craving) {
        return switch (craving) {
            case VERY_SICK -> "tougher.tooltip.food_very_sick";
            case SICK -> "tougher.tooltip.food_sick";
            case SLIGHT -> "tougher.tooltip.food_slight";
            case GRACE -> "tougher.tooltip.food_grace";
            case CRAVING -> "tougher.tooltip.food_craving";
            case GREAT -> "tougher.tooltip.food_great";
            case NORMAL -> null;
        };
    }

    private static String tooltipFallback(FoodCraving craving) {
        return switch (craving) {
            case VERY_SICK -> "Very sick of this";
            case SICK -> "Sick of this";
            case SLIGHT -> "Slight craving";
            case GRACE -> "Within grace";
            case CRAVING -> "Craving";
            case GREAT -> "Great craving";
            case NORMAL -> "";
        };
    }

    /** Readable on the dark tooltip. Black text would disappear there. */
    private static int tooltipRgb(FoodCraving craving) {
        return switch (craving) {
            case VERY_SICK -> 0xB4B4B4;
            case SICK -> 0xC4956A;
            case SLIGHT -> 0xB7D0E0;
            case GRACE -> 0xB7D0E0;
            case CRAVING -> 0x7DCEA0;
            case GREAT -> 0xF0C14A;
            case NORMAL -> 0xFFFFFF;
        };
    }
}

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

/** Tiny craving mark in the top-right corner of a food icon, plus one tooltip line. */
public final class FoodCravingOutline {
    private static final int MARK = 5;
    /** The 5-pixel glyph is drawn at half size so it stays in the corner. */
    private static final float MARK_SCALE = 0.5F;
    private static final int DIGIT_WIDTH = 3;
    private static final int DIGIT_GAP = 1;
    /** Five rows of three bits. The high bit of each row is the left pixel. */
    private static final int[] DIGIT_BITS = {
        0b111_101_101_101_111,
        0b010_110_010_010_111,
        0b111_001_111_100_111,
        0b111_001_111_001_111,
        0b101_101_111_001_001,
        0b111_100_111_001_111,
        0b111_100_111_101_111,
        0b111_001_001_001_001,
        0b111_101_111_101_111,
        0b111_101_111_001_111,
    };

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
        var pose = graphics.pose();
        pose.pushMatrix();
        try {
            pose.translate(x + 16.0F, y);
            pose.scale(MARK_SCALE, MARK_SCALE);
            switch (craving) {
                case VERY_SICK -> icon(graphics, -MARK, 0, 0xFFE6E6E6, 0xFF141414, "#...#", ".#.#.", "..#..", ".#.#.", "#...#");
                case SICK -> icon(graphics, -MARK, 0, 0xFF3A2418, 0xFFC4956A, ".....", "#...#", ".#.#.", "..#..", ".....");
                case SLIGHT, GRACE -> mealsAgoNumber(graphics, mealsAgo(stack));
                case MAINTAIN -> icon(graphics, -MARK, 0, 0xFF0A280A, 0xFF55FF55, ".....", ".....", "#####", ".....", ".....");
                case GREAT -> icon(graphics, -MARK, 0, 0xFF0A280A, 0xFF55FF55, "..#..", ".#.#.", "#...#", ".....", ".....");
                case NORMAL -> {
                    // No mark.
                }
            }
        } finally {
            pose.popMatrix();
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
        if (craving == FoodCraving.SLIGHT || craving == FoodCraving.GRACE) {
            return Component.translatableWithFallback(
                            key,
                            tooltipFallback(craving),
                            Component.literal(FoodCravingRules.mealsAgoPhrase(mealsAgo(stack))))
                    .withStyle(style -> style.withColor(tooltipRgb(craving)));
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

    /** Yellow meals-ago count, the same height as the half-size down mark it replaces. */
    private static void mealsAgoNumber(GuiGraphicsExtractor graphics, int mealsAgo) {
        if (mealsAgo <= 0) {
            return;
        }
        String digits = Integer.toString(mealsAgo);
        int width = digits.length() * DIGIT_WIDTH + (digits.length() - 1) * DIGIT_GAP;
        int x = -width;
        graphics.fill(x, 0, x + width, MARK, 0xFF2E2808);
        for (int i = 0; i < digits.length(); i++) {
            int value = digits.charAt(i) - '0';
            if (value >= 0 && value < DIGIT_BITS.length) {
                digit(graphics, x + i * (DIGIT_WIDTH + DIGIT_GAP), DIGIT_BITS[value]);
            }
        }
    }

    private static void digit(GuiGraphicsExtractor graphics, int x, int bits) {
        for (int row = 0; row < MARK; row++) {
            int rowBits = (bits >> ((MARK - 1 - row) * DIGIT_WIDTH)) & 0b111;
            for (int col = 0; col < DIGIT_WIDTH; col++) {
                if (((rowBits >> (DIGIT_WIDTH - 1 - col)) & 1) != 0) {
                    graphics.fill(x + col, row, x + col + 1, row + 1, 0xFFFFE14A);
                }
            }
        }
    }

    private static int mealsAgo(ItemStack stack) {
        ClientboundFoodCravingPayload payload = ExtraHardModeClient.lastFoodCraving();
        if (payload == null || stack == null) {
            return 0;
        }
        return FoodCravingRules.mealsAgo(payload.history(), Hunger.itemId(stack));
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
            case MAINTAIN -> "tougher.tooltip.food_maintain";
            case GREAT -> "tougher.tooltip.food_great";
            case NORMAL -> null;
        };
    }

    private static String tooltipFallback(FoodCraving craving) {
        return switch (craving) {
            case VERY_SICK -> "Very sick of this";
            case SICK -> "Sick of this";
            case SLIGHT -> "Slight craving, %s";
            case GRACE -> "Within grace, %s";
            case MAINTAIN -> "Maintaining";
            case GREAT -> "Craving";
            case NORMAL -> "";
        };
    }

    /** Readable on the dark tooltip. Black text would disappear there. */
    private static int tooltipRgb(FoodCraving craving) {
        return switch (craving) {
            case VERY_SICK -> 0xB4B4B4;
            case SICK -> 0xC4956A;
            case SLIGHT -> 0xFFE14A;
            case GRACE -> 0xFFE14A;
            case MAINTAIN -> 0x55FF55;
            case GREAT -> 0x55FF55;
            case NORMAL -> 0xFFFFFF;
        };
    }
}

package dev.extrahardmode.client;

import dev.extrahardmode.feature.FoodCraving;
import dev.extrahardmode.feature.FoodCravingRules;
import dev.extrahardmode.feature.Hunger;
import dev.extrahardmode.network.ClientboundFoodCravingPayload;
import dev.extrahardmode.network.ClientboundSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

/** Thin craving frame around a food slot, plus one tooltip line that names it. */
public final class FoodCravingOutline {
    /** One scaled pixel, so the frame is a little under a full GUI pixel. */
    private static final float STROKE = 0.8F;
    /** 16 GUI pixels divided by {@link #STROKE}. */
    private static final int SPAN = 20;

    private FoodCravingOutline() {}

    public static void draw(GuiGraphicsExtractor graphics, Slot slot) {
        if (graphics == null || slot == null || !slot.hasItem()) {
            return;
        }
        int color = FoodCravingRules.outlineArgb(craving(slot.getItem()));
        if (color == 0) {
            return;
        }
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        try {
            pose.translate(slot.x, slot.y);
            pose.scale(STROKE, STROKE);
            graphics.fill(0, 0, SPAN, 1, color);
            graphics.fill(0, SPAN - 1, SPAN, SPAN, color);
            graphics.fill(0, 1, 1, SPAN - 1, color);
            graphics.fill(SPAN - 1, 1, SPAN, SPAN - 1, color);
        } finally {
            pose.popMatrix();
        }
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

    private static String tooltipKey(FoodCraving craving) {
        return switch (craving) {
            case VERY_SICK -> "tougher.tooltip.food_very_sick";
            case SICK -> "tougher.tooltip.food_sick";
            case SLIGHT -> "tougher.tooltip.food_slight";
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
            case CRAVING -> 0x7DCEA0;
            case GREAT -> 0xF0C14A;
            case NORMAL -> 0xFFFFFF;
        };
    }
}

package dev.extrahardmode.client;

import dev.extrahardmode.feature.FoodCravingRules;
import dev.extrahardmode.feature.WellFedRunIconRules;
import dev.extrahardmode.network.ClientboundFoodCravingPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Oldest unique meal of the Well Fed run, drawn above the offhand slot. */
public final class WellFedRunIcon {
    private WellFedRunIcon() {}

    public static void draw(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int leftPos, int topPos) {
        if (graphics == null || !FoodCravingOutline.visible()) {
            return;
        }
        ClientboundFoodCravingPayload payload = ExtraHardModeClient.lastFoodCraving();
        if (payload == null || payload.wellFed() <= 0) {
            return;
        }
        String foodId = FoodCravingRules.oldestUniqueInRun(payload.history(), payload.wellFed());
        ItemStack stack = stack(foodId);
        if (stack.isEmpty()) {
            return;
        }
        int x = WellFedRunIconRules.iconX();
        int y = WellFedRunIconRules.iconY();
        graphics.item(stack, x, y);
        FoodCravingOutline.drawOnItem(graphics, stack, x, y);
        if (WellFedRunIconRules.hit(mouseX - leftPos, mouseY - topPos)) {
            Minecraft minecraft = Minecraft.getInstance();
            FoodCravingOutline.showTooltip(graphics, minecraft.font, stack, mouseX, mouseY);
        }
    }

    static ItemStack stack(String foodId) {
        Identifier id = foodId == null ? null : Identifier.tryParse(foodId);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(BuiltInRegistries.ITEM.getValue(id));
    }
}

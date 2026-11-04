package dev.extrahardmode.mixin.client;

import dev.extrahardmode.client.FoodCravingOutline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Craving marks on the trade list. Those icons are drawn with {@code fakeItem},
 * not through a container slot. Player shops, villagers, and residents all use this screen.
 * Positions match MerchantScreen.extractContents in 26.3: seven visible rows, 20 pixels apart.
 * {@code leftPos} and {@code topPos} belong to the container screen, so this mixin extends it.
 */
@Mixin(net.minecraft.client.gui.screens.inventory.MerchantScreen.class)
public abstract class MerchantFoodCravingMixin extends AbstractContainerScreen<MerchantMenu> {
    private static final int VISIBLE_ROWS = 7;
    private static final int ROW_STEP = 20;
    /** Top of the 16-pixel icon on the first visible row, relative to the screen. */
    private static final int FIRST_ITEM_Y = 19;
    private static final int COST_A_X = 10;
    private static final int COST_B_X = 40;
    private static final int RESULT_X = 73;

    @Shadow
    private int scrollOff;

    private MerchantFoodCravingMixin(MerchantMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 0, 0);
    }

    @Inject(method = "extractContents", at = @At("TAIL"))
    private void tougher$shopFoodMarks(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (graphics == null) {
            return;
        }
        MerchantMenu merchant = this.getMenu();
        if (merchant == null) {
            return;
        }
        MerchantOffers offers = merchant.getOffers();
        if (offers == null || offers.isEmpty()) {
            return;
        }
        int scroll = Math.max(0, this.scrollOff);
        boolean window = offers.size() > VISIBLE_ROWS;
        int shown = 0;
        for (int index = 0; index < offers.size() && shown < VISIBLE_ROWS; index++) {
            if (window && (index < scroll || index >= scroll + VISIBLE_ROWS)) {
                continue;
            }
            int y = this.topPos + FIRST_ITEM_Y + shown * ROW_STEP;
            MerchantOffer offer = offers.get(index);
            FoodCravingOutline.drawOnItem(graphics, offer.getCostA(), this.leftPos + COST_A_X, y);
            ItemStack costB = offer.getCostB();
            if (costB != null && !costB.isEmpty()) {
                FoodCravingOutline.drawOnItem(graphics, costB, this.leftPos + COST_B_X, y);
            }
            FoodCravingOutline.drawOnItem(graphics, offer.getResult(), this.leftPos + RESULT_X, y);
            shown++;
        }
    }
}

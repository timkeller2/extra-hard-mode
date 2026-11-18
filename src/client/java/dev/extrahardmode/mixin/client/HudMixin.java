package dev.extrahardmode.mixin.client;

import dev.extrahardmode.client.ExtraHardModeClient;
import dev.extrahardmode.client.FoodCravingOutline;
import dev.extrahardmode.client.SaturationOutline;
import dev.extrahardmode.client.SmiteCrosshair;
import dev.extrahardmode.feature.HudCapacityRules;
import dev.extrahardmode.feature.SaturationOutlineRules;
import dev.extrahardmode.feature.WellFedRules;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudMixin {
    private static final Identifier FOOD_EMPTY = Identifier.withDefaultNamespace("hud/food_empty");
    private static final Identifier FOOD_HALF = Identifier.withDefaultNamespace("hud/food_half");
    private static final Identifier FOOD_FULL = Identifier.withDefaultNamespace("hud/food_full");
    private static final Identifier FOOD_EMPTY_HUNGER = Identifier.withDefaultNamespace("hud/food_empty_hunger");
    private static final Identifier FOOD_HALF_HUNGER = Identifier.withDefaultNamespace("hud/food_half_hunger");
    private static final Identifier FOOD_FULL_HUNGER = Identifier.withDefaultNamespace("hud/food_full_hunger");

    private int tougher$heartIndex;
    private int tougher$maxPoints;
    private boolean tougher$clipContainer;
    /** True when this food bar was drawn here, so the vanilla return does not draw it again. */
    private boolean tougher$customFood;

    @Redirect(
            method = "extractCrosshair",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
                    ordinal = 0))
    private void tougher$goldSmiteCrosshair(
            GuiGraphicsExtractor graphics,
            RenderPipeline pipeline,
            Identifier sprite,
            int x,
            int y,
            int width,
            int height) {
        graphics.blitSprite(pipeline, sprite, x, y, width, height, SmiteCrosshair.color(Minecraft.getInstance()));
    }

    @Inject(method = "extractHearts", at = @At("HEAD"))
    private void tougher$startHearts(
            GuiGraphicsExtractor graphics,
            Player player,
            int x,
            int y,
            int rowHeight,
            int regen,
            float health,
            int current,
            int display,
            int absorption,
            boolean blink,
            CallbackInfo ci) {
        tougher$maxPoints = Mth.ceil(health);
        int slots = Mth.ceil(health / 2.0F);
        int absorb = Mth.ceil(absorption / 2.0F);
        tougher$heartIndex = slots + absorb - 1;
    }

    @ModifyArg(
            method = "extractHearts",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Hud;extractHeart(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Hud$HeartType;IIZZZ)V",
                    ordinal = 0),
            index = 6)
    private boolean tougher$halfHeartContainer(boolean half) {
        int index = tougher$heartIndex;
        tougher$heartIndex--;
        tougher$clipContainer = half || HudCapacityRules.halfShadow(tougher$maxPoints, index);
        return tougher$clipContainer;
    }

    @Redirect(
            method = "extractHeart",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private void tougher$clipHalfHeart(
            GuiGraphicsExtractor graphics,
            RenderPipeline pipeline,
            Identifier sprite,
            int x,
            int y,
            int width,
            int height) {
        boolean clip = tougher$clipContainer && sprite != null && sprite.getPath().contains("container");
        if (clip) {
            graphics.enableScissor(x, y, x + (width + 1) / 2, y + height);
        }
        graphics.blitSprite(pipeline, sprite, x, y, width, height);
        if (clip) {
            graphics.disableScissor();
            tougher$clipContainer = false;
        }
    }

    @Inject(method = "extractFood", at = @At("HEAD"), cancellable = true)
    private void tougher$wellFedFood(
            GuiGraphicsExtractor graphics, Player player, int y, int right, CallbackInfo ci) {
        tougher$customFood = false;
        if (player == null) {
            return;
        }
        var mana = ExtraHardModeClient.lastMana();
        int wellFed = mana == null ? 0 : Math.max(0, mana.wellFed());
        int maxFood = WellFedRules.foodMax(wellFed);
        if (maxFood <= WellFedRules.BASE_FOOD) {
            return;
        }
        ci.cancel();
        tougher$customFood = true;
        tougher$drawFoodBar(graphics, player, y, right, maxFood);
    }

    /**
     * Vanilla food drawing never reaches its return once the extra rows take over,
     * so the outline is drawn with those rows. On the normal bar it is drawn here
     * when AppleSkin is absent, then the drumsticks are drawn again over the gold.
     */
    @Inject(method = "extractFood", at = @At("RETURN"))
    private void tougher$saturationOutline(
            GuiGraphicsExtractor graphics, Player player, int y, int right, CallbackInfo ci) {
        if (tougher$customFood || player == null || FabricLoader.getInstance().isModLoaded("appleskin")) {
            return;
        }
        if (!(player.getFoodData().getSaturationLevel() > 0.0F)) {
            return;
        }
        tougher$drawFoodBar(graphics, player, y, right, WellFedRules.BASE_FOOD);
    }

    private void tougher$drawFoodBar(
            GuiGraphicsExtractor graphics, Player player, int y, int right, int maxFood) {
        int food = player.getFoodData().getFoodLevel();
        boolean hungry = player.hasEffect(MobEffects.HUNGER);
        Identifier empty = hungry ? FOOD_EMPTY_HUNGER : FOOD_EMPTY;
        Identifier halfSprite = hungry ? FOOD_HALF_HUNGER : FOOD_HALF;
        Identifier full = hungry ? FOOD_FULL_HUNGER : FOOD_FULL;
        float saturation = player.getFoodData().getSaturationLevel();
        int icons = (maxFood + 1) / 2;
        for (int index = 0; index < icons; index++) {
            int x = SaturationOutlineRules.iconX(right, index);
            int iconY = SaturationOutlineRules.iconY(y, index);
            SaturationOutline.draw(graphics, full, x, iconY, SaturationOutlineRules.coverage(saturation, index));
            if (HudCapacityRules.halfShadow(maxFood, index)) {
                graphics.enableScissor(x, iconY, x + 5, iconY + 9);
            }
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, empty, x, iconY, 9, 9);
            if (HudCapacityRules.halfShadow(maxFood, index)) {
                graphics.disableScissor();
            }
            int point = index * 2 + 1;
            Identifier fill = food > point ? full : food == point ? halfSprite : null;
            if (fill != null) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, fill, x, iconY, 9, 9);
            }
        }
    }

    @Inject(method = "extractSlot", at = @At("TAIL"))
    private void tougher$hotbarFoodCraving(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            DeltaTracker deltaTracker,
            Player player,
            ItemStack stack,
            int seed,
            CallbackInfo ci) {
        FoodCravingOutline.drawOnItem(graphics, stack, x, y);
    }
}

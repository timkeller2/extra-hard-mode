package dev.extrahardmode.mixin.client;

import dev.extrahardmode.client.ExtraHardModeClient;
import dev.extrahardmode.feature.HudCapacityRules;
import dev.extrahardmode.feature.WellFedRules;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
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
        int food = player.getFoodData().getFoodLevel();
        boolean hungry = player.hasEffect(MobEffects.HUNGER);
        Identifier empty = hungry ? FOOD_EMPTY_HUNGER : FOOD_EMPTY;
        Identifier halfSprite = hungry ? FOOD_HALF_HUNGER : FOOD_HALF;
        Identifier full = hungry ? FOOD_FULL_HUNGER : FOOD_FULL;
        int icons = (maxFood + 1) / 2;
        for (int index = 0; index < icons; index++) {
            int row = index / 10;
            int column = index % 10;
            int x = right - column * 8 - 9;
            int iconY = y - row * 10;
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
}

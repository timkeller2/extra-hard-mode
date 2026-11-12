package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Hunger;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A slice is eaten on the cake block, including a candle cake, so it never reaches
 * {@code FoodProperties.onConsume}. One slice is the food {@code minecraft:cake}.
 */
@Mixin(CakeBlock.class)
public abstract class CakeBlockMixin {
    @Inject(method = "eat", at = @At("HEAD"))
    private static void tougher$rememberCake(
            LevelAccessor level,
            BlockPos pos,
            BlockState state,
            Player player,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (player != null && player.canEat(false)) {
            Hunger.rememberMeal(player);
        }
    }

    @Inject(method = "eat", at = @At("RETURN"))
    private static void tougher$recordCake(
            LevelAccessor level,
            BlockPos pos,
            BlockState state,
            Player player,
            CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult result = cir.getReturnValue();
        if (player == null || result == null || !result.consumesAction()) {
            return;
        }
        Hunger.onFoodEaten(player, new ItemStack(Items.CAKE));
    }
}

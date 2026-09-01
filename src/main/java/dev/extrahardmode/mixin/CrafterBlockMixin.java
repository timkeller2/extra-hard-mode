package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.MoreTnt;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CrafterBlock.class)
public abstract class CrafterBlockMixin {
    @Inject(method = "dispenseFrom", at = @At("HEAD"))
    private void extrahardmode$captureCraftLevel(BlockState state, ServerLevel level, BlockPos pos, CallbackInfo ci) {
        MoreTnt.captureCraftLevel(level);
    }

    @ModifyArg(
            method = "dispenseFrom",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/CrafterBlock;dispenseItem(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/CrafterBlockEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/item/crafting/RecipeHolder;)V"),
            index = 3)
    private ItemStack extrahardmode$tntCount(ItemStack stack) {
        return MoreTnt.adjustCaptured(stack);
    }

    @Inject(method = "dispenseFrom", at = @At("RETURN"))
    private void extrahardmode$clearCraftLevel(BlockState state, ServerLevel level, BlockPos pos, CallbackInfo ci) {
        MoreTnt.clearCraftLevel();
    }
}

package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Torches;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CampfireBlockEntity.class)
public abstract class CampfireBlockEntityMixin {
    @Inject(method = "cookTick", at = @At("HEAD"), cancellable = true)
    private static void ehm$extinguishBelowCutoff(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            CampfireBlockEntity campfire,
            RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> check,
            CallbackInfo ci) {
        if (!WorldGate.isModuleActive(level, Torches.ID)) {
            return;
        }
        if (!Torches.denyCampfireAt(level, pos) || !state.getValue(CampfireBlock.LIT)) {
            return;
        }
        CampfireBlock.dowse(null, level, pos, state);
        level.setBlock(pos, state.setValue(CampfireBlock.LIT, false), 3);
        ci.cancel();
    }
}

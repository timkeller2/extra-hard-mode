package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.CopperChests;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChestBlock.class)
public abstract class CopperChestOpenMixin {
    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void tougher$copperChestOwner(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit,
            CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult result = CopperChests.onOpen(state, level, pos, player);
        if (result != InteractionResult.PASS) {
            cir.setReturnValue(result);
        }
    }
}

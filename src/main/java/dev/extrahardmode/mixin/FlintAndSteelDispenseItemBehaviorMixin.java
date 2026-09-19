package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Torches;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.FlintAndSteelDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlintAndSteelDispenseItemBehavior.class)
public abstract class FlintAndSteelDispenseItemBehaviorMixin {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    private void ehm$denyCampfireLight(BlockSource source, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        ServerLevel level = source.level();
        if (!WorldGate.isModuleActive(level, Torches.ID)) {
            return;
        }
        Direction facing = source.state().getValue(DispenserBlock.FACING);
        BlockPos pos = source.pos().relative(facing);
        if (!Torches.isCampfire(level.getBlockState(pos))) {
            return;
        }
        if (Torches.tryDenyCampfireLight(level, pos, null)) {
            ((FlintAndSteelDispenseItemBehavior) (Object) this).setSuccess(false);
            cir.setReturnValue(stack);
        }
    }
}

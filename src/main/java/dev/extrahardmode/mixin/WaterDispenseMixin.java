package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Water;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DispenserBlock.class)
public abstract class WaterDispenseMixin {
    @Inject(method = "getDispenseMethod", at = @At("RETURN"), cancellable = true)
    private void ehm$wrapWaterBucket(Level level, ItemStack stack, CallbackInfoReturnable<DispenseItemBehavior> cir) {
        if (!(level instanceof ServerLevel server) || !WorldGate.isModuleActive(server, Water.ID)) {
            return;
        }
        Fluid fluid = Water.fluidOfBucket(stack.getItem());
        if (fluid == null || !Water.restricts(server, fluid)) {
            return;
        }
        DispenseItemBehavior original = cir.getReturnValue();
        if (original == null) {
            return;
        }
        cir.setReturnValue((BlockSource source, ItemStack item) -> {
            ServerLevel world = source.level();
            Fluid dispensed = Water.fluidOfBucket(item.getItem());
            if (dispensed == null || !Water.restricts(world, dispensed)) {
                return original.dispense(source, item);
            }
            ItemStack result = original.dispense(source, item);
            if (result.isEmpty() || result.getItem() == Items.BUCKET) {
                Direction facing = source.state().getValue(DispenserBlock.FACING);
                BlockPos target = source.pos().relative(facing);
                if (!Water.allowsContainedSource(world, target, dispensed)) {
                    Water.mark(world, target);
                }
            }
            return result;
        });
    }
}

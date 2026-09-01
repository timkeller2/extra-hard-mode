package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Water;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DispenserBlock.class)
public abstract class WaterDispenseMixin {
    @Inject(method = "getDispenseMethod", at = @At("RETURN"), cancellable = true)
    private void ehm$wrapWaterBucket(
            Level level, ItemStack stack, CallbackInfoReturnable<DispenseItemBehavior> cir) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (!WorldGate.isModuleActive(server, Water.ID)) {
            return;
        }
        if (!Water.enabled(server) || !Water.isWaterBucket(stack.getItem())) {
            return;
        }
        DispenseItemBehavior original = cir.getReturnValue();
        if (original == null) {
            return;
        }
        cir.setReturnValue((BlockSource source, ItemStack item) -> {
            ItemStack result = original.dispense(source, item);
            ServerLevel world = source.level();
            if (WorldGate.isModuleActive(world, Water.ID) && Water.enabled(world)) {
                Direction facing = source.state().getValue(DispenserBlock.FACING);
                BlockPos target = source.pos().relative(facing);
                Water.convertPlacedWater(world, target);
            }
            return result;
        });
    }
}

package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Water;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public abstract class BucketItemMixin {
    @Shadow
    public abstract Fluid getContent();

    @Inject(method = "emptyContents", at = @At("RETURN"))
    private void ehm$placeFlowingWater(
            LivingEntity entity,
            Level level,
            BlockPos pos,
            BlockHitResult hit,
            CallbackInfoReturnable<Boolean> cir) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (!WorldGate.isModuleActive(server, Water.ID)) {
            return;
        }
        if (!cir.getReturnValue()) {
            return;
        }
        Fluid content = getContent();
        if (content != Fluids.WATER && !content.isSame(Fluids.WATER)) {
            return;
        }
        Water.convertPlacedWater(server, pos);
    }

    @Redirect(
            method = "use",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/BucketPickup;pickupBlock(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack ehm$cancelMarkedPickup(
            BucketPickup pickup, LivingEntity entity, LevelAccessor accessor, BlockPos pos, BlockState state) {
        if (accessor instanceof ServerLevel server
                && WorldGate.isModuleActive(server, Water.ID)
                && Water.enabled(server)
                && Water.isMarked(server, pos)) {
            return ItemStack.EMPTY;
        }
        return pickup.pickupBlock(entity, accessor, pos, state);
    }
}

package dev.extrahardmode.mixin;

import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.feature.BedBreakRules;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A survival break that would pick a bed up destroys it instead. */
@Mixin(Block.class)
public abstract class BedPickupMixin {
    @Inject(
            method =
                    "playerDestroy(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/item/ItemStack;)V",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/block/Block;dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V"),
            cancellable = true)
    private void tougher$destroyBedOnPickup(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            BlockState state,
            BlockEntity blockEntity,
            ItemStack tool,
            CallbackInfo ci) {
        if (!WorldGate.isActive(level)) {
            return;
        }
        if (!(state.getBlock() instanceof AbstractBedBlock)) {
            return;
        }
        if (!BedBreakRules.dropsNothing(
                ConfigManager.world(level).destroyBedOnPickup(), EhmApi.playerBypasses(player))) {
            return;
        }
        ci.cancel();
    }
}

package dev.extrahardmode.mixin;

import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.feature.BedBreakRules;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A player break that would pick a bed up destroys it instead.
 *
 * <p>The loot table drops a bed only for the head half. Breaking the foot removes the head through
 * {@code Level.destroyBlock}, which calls this method with no player. Cancelling {@code playerDestroy}
 * on the half that was clicked misses that drop.
 */
@Mixin(Block.class)
public abstract class BedPickupMixin {
    @Inject(
            method =
                    "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"),
            cancellable = true)
    private static void tougher$destroyBedOnPickup(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockEntity blockEntity,
            Entity entity,
            ItemStack tool,
            CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel) || !WorldGate.isActive(serverLevel)) {
            return;
        }
        if (!(state.getBlock() instanceof AbstractBedBlock)) {
            return;
        }
        boolean bypass = entity instanceof ServerPlayer player && EhmApi.playerBypasses(player);
        if (!BedBreakRules.dropsNothing(ConfigManager.world(serverLevel).destroyBedOnPickup(), bypass)) {
            return;
        }
        ci.cancel();
    }
}

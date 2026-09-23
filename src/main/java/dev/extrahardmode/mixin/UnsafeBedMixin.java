package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.UnsafeBeds;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.StrawBedBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Right-clicking a bed that is already in the Nether or the End removes it instead of exploding. */
@Mixin({BedBlock.class, StrawBedBlock.class})
public abstract class UnsafeBedMixin {
    @Inject(method = "destroyOnUse", at = @At("HEAD"), cancellable = true)
    private void tougher$deleteInsteadOfExplode(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (!UnsafeBeds.hostileDimension(level)) {
            return;
        }
        UnsafeBeds.remove(level, pos);
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}

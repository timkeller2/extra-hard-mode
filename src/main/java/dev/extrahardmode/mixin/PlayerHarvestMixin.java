package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.FeatureBus;
import dev.extrahardmode.feature.HardenedStone;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerHarvestMixin {
    @Inject(method = "hasCorrectToolForDrops", at = @At("HEAD"), cancellable = true)
    private void extrahardmode$hardenedHarvest(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        if (!FeatureBus.guard(self.level(), HardenedStone.ID)) {
            return;
        }
        if (HardenedStone.shouldDenyHarvest(self, state)) {
            cir.setReturnValue(false);
        }
    }
}

package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.CopperChests;
import dev.extrahardmode.feature.OwnedHoppers;
import java.util.List;
import net.minecraft.server.network.FilteredText;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SignBlockEntity.class)
public abstract class CopperChestSignMixin {
    @Inject(method = "updateSignText", at = @At("HEAD"), cancellable = true)
    private void tougher$copperChestSign(Player player, SignTextSlot slot, List<FilteredText> lines, CallbackInfo ci) {
        SignBlockEntity sign = (SignBlockEntity) (Object) this;
        if (CopperChests.denySignEdit(sign, player) || OwnedHoppers.denySignEdit(sign, player)) {
            ci.cancel();
        }
    }
}

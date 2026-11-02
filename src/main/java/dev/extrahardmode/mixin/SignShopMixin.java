package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.FeatureBus;
import dev.extrahardmode.feature.Inhabitants;
import dev.extrahardmode.feature.PlayerShops;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SignBlockEntity.class)
public abstract class SignShopMixin {
    @Inject(method = "updateSignText", at = @At("RETURN"))
    private void tougher$playerShopSign(Player player, SignTextSlot slot, List<FilteredText> lines, CallbackInfo ci) {
        if (!FeatureBus.guard(player == null ? null : player.level(), Inhabitants.ID)) {
            return;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerShops.onSignEdited((SignBlockEntity) (Object) this, serverPlayer);
        }
    }
}

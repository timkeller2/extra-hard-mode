package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Torches;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FireChargeItem.class)
public abstract class FireChargeItemMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void ehm$denyCampfireLight(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel) || !WorldGate.isModuleActive(serverLevel, Torches.ID)) {
            return;
        }
        BlockPos pos = context.getClickedPos();
        if (!Torches.isCampfire(level.getBlockState(pos))) {
            return;
        }
        ServerPlayer player = context.getPlayer() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        if (Torches.tryDenyCampfireLight(serverLevel, pos, player)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}

package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.AntiFarming;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HoeItem.class)
public abstract class HoeItemMixin {
    @Inject(method = "useOn", at = @At("RETURN"))
    private void ehm$tillSoilModifier(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(context.getLevel() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, AntiFarming.ID)) {
            return;
        }
        InteractionResult result = cir.getReturnValue();
        if (result == null || !result.consumesAction()) {
            return;
        }
        BlockPos pos = context.getClickedPos();
        if (!level.getBlockState(pos).is(Blocks.FARMLAND)) {
            return;
        }
        AntiFarming.onSoilTilled(level, pos, context.getItemInHand());
    }
}

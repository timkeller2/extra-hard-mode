package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.AntiFarming;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class HoeItemMixin {
    /** The hoe that started this use. Survives the stack breaking before useOn returns. */
    private static final ThreadLocal<Item> TILL_HOE = new ThreadLocal<>();

    @Inject(method = "useOn", at = @At("HEAD"))
    private void ehm$rememberHoe(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(context.getLevel() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, AntiFarming.ID)) {
            TILL_HOE.remove();
            return;
        }
        ItemStack hand = context.getItemInHand();
        if (hand.is(ItemTags.HOES)) {
            TILL_HOE.set(hand.getItem());
        } else {
            TILL_HOE.remove();
        }
    }

    @Inject(method = "useOn", at = @At("RETURN"))
    private void ehm$tillSoilModifier(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(context.getLevel() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, AntiFarming.ID)) {
            TILL_HOE.remove();
            return;
        }
        Item remembered = TILL_HOE.get();
        TILL_HOE.remove();
        ItemStack hand = context.getItemInHand();
        ItemStack hoe = hand.is(ItemTags.HOES) ? hand : remembered == null ? ItemStack.EMPTY : new ItemStack(remembered);
        if (!hoe.is(ItemTags.HOES)) {
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
        AntiFarming.onSoilTilled(level, pos, hoe);
    }
}

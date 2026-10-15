package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Inhabitants;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractVillager.class)
public abstract class InhabitantTradeMixin {
    @Inject(method = "notifyTrade", at = @At("RETURN"))
    private void tougher$residentTradeXp(MerchantOffer offer, CallbackInfo ci) {
        if ((Object) this instanceof Villager villager) {
            Inhabitants.onTrade(villager);
        }
    }
}

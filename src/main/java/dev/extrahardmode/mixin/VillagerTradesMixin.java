package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.FeatureBus;
import dev.extrahardmode.feature.VillagerNerf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerTradesMixin {
    @Inject(method = "updateTrades", at = @At("RETURN"))
    private void extrahardmode$nerfGeneratedTrades(ServerLevel level, CallbackInfo ci) {
        if (!FeatureBus.guard((Level) level, VillagerNerf.ID)) {
            return;
        }
        VillagerNerf.apply((Villager) (Object) this);
    }

    @Inject(method = "startTrading", at = @At("HEAD"))
    private void extrahardmode$nerfExistingTrades(Player player, CallbackInfo ci) {
        if (!FeatureBus.guard(((Villager) (Object) this).level(), VillagerNerf.ID)) {
            return;
        }
        VillagerNerf.apply((Villager) (Object) this);
    }
}

package dev.extrahardmode.mixin;

import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.feature.monster.Horses;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractHorse.class)
public abstract class AbstractHorseMixin {
    @Inject(method = "openCustomInventoryScreen", at = @At("HEAD"), cancellable = true)
    private void ehm$blockChestBelowCave(Player player, CallbackInfo ci) {
        AbstractHorse self = (AbstractHorse) (Object) this;
        if (!(self.level() instanceof ServerLevel level)) {
            return;
        }
        if (!WorldGate.isModuleActive(level, Horses.ID)) {
            return;
        }
        if (!(self instanceof AbstractChestedHorse horse) || !horse.hasChest()) {
            return;
        }
        if (player instanceof ServerPlayer serverPlayer && EhmApi.playerBypasses(serverPlayer)) {
            return;
        }
        if (Horses.shouldBlockChest(horse, ConfigManager.world(level).horseBlockChestBelowY())) {
            ci.cancel();
        }
    }
}

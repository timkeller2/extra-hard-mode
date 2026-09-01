package dev.extrahardmode.mixin;

import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.feature.monster.Horses;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractMountInventoryMenu.class)
public abstract class AbstractMountInventoryMenuMixin {
    @Shadow
    @Final
    protected LivingEntity mount;

    @Inject(method = "stillValid", at = @At("HEAD"), cancellable = true)
    private void ehm$closeChestBelowCave(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (!(mount.level() instanceof ServerLevel level)) {
            return;
        }
        if (!WorldGate.isModuleActive(level, Horses.ID)) {
            return;
        }
        if (!(mount instanceof AbstractChestedHorse horse) || !horse.hasChest()) {
            return;
        }
        if (player instanceof ServerPlayer serverPlayer && EhmApi.playerBypasses(serverPlayer)) {
            return;
        }
        if (Horses.shouldBlockChest(horse, ConfigManager.world(level))) {
            cir.setReturnValue(false);
        }
    }
}

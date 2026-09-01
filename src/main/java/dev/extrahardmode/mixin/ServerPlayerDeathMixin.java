package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Players;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDeathMixin {
    @Inject(method = "die", at = @At("HEAD"))
    private void extrahardmode$forfeitInventory(DamageSource source, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        Level level = player.level();
        if (!(level instanceof ServerLevel serverLevel) || !WorldGate.isModuleActive(serverLevel, Players.ID)) {
            return;
        }
        Players.onDeath(player);
    }
}

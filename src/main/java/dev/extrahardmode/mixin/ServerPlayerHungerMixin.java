package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Hunger;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerHungerMixin {
    @Unique
    private float ehm$lastSyncedSaturation = Float.NaN;

    @Inject(method = "checkMovementStatistics", at = @At("HEAD"))
    private void extrahardmode$movingHunger(double dx, double dy, double dz, CallbackInfo ci) {
        Hunger.noteMovement((ServerPlayer) (Object) this, dx, dy, dz);
    }

    /**
     * Vanilla only sends saturation when it hits 0 (or food/health changes). Extra
     * Hard Mode drains saturation in the background, so HUD mods keep showing the
     * last high value until the bar vanishes at once.
     */
    @Inject(method = "doTick", at = @At("TAIL"))
    private void ehm$syncSaturation(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (!(player.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, Hunger.ID)) {
            return;
        }
        if (player.connection == null) {
            return;
        }
        FoodData food = player.getFoodData();
        float saturation = food.getSaturationLevel();
        if (!Float.isNaN(this.ehm$lastSyncedSaturation)
                && Float.compare(this.ehm$lastSyncedSaturation, saturation) == 0) {
            return;
        }
        this.ehm$lastSyncedSaturation = saturation;
        player.connection.send(new ClientboundSetHealthPacket(player.getHealth(), food.getFoodLevel(), saturation));
    }
}

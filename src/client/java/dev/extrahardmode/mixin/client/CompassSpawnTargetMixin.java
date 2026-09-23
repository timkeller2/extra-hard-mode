package dev.extrahardmode.mixin.client;

import dev.extrahardmode.client.ExtraHardModeClient;
import dev.extrahardmode.feature.CompassRules;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Spawn-compass target. Sneaking points at the player's bed or respawn anchor. */
@Mixin(targets = "net.minecraft.client.renderer.item.properties.numeric.CompassAngleState$CompassTarget$3")
public class CompassSpawnTargetMixin {
    @Inject(method = "get", at = @At("RETURN"), cancellable = true)
    private void tougher$personalSpawn(
            ClientLevel level, ItemStack stack, ItemOwner owner, CallbackInfoReturnable<GlobalPos> cir) {
        if (level == null || owner == null) {
            return;
        }
        LivingEntity living = owner.asLivingEntity();
        boolean local = living instanceof Player player && player.isLocalPlayer();
        boolean sneaking = living instanceof Player player && player.isShiftKeyDown();
        GlobalPos spawn = ExtraHardModeClient.compassSpawn();
        boolean sameDimension = spawn != null && spawn.dimension().equals(level.dimension());
        if (CompassRules.pointAtPersonalSpawn(sneaking, local, spawn != null, sameDimension)) {
            cir.setReturnValue(spawn);
        }
    }
}

package dev.extrahardmode.mixin;

import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LootlessMixin {
    @Inject(
            method =
                    "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;ZLnet/minecraft/resources/ResourceKey;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void ehm$skipLootless(
            ServerLevel level, DamageSource source, boolean causedByPlayer, ResourceKey<LootTable> table, CallbackInfo ci) {
        if (!WorldGate.isActive(level)) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        if (Boolean.TRUE.equals(self.getAttachedOrElse(EhmAttachments.EHM_LOOTLESS, Boolean.FALSE))) {
            ci.cancel();
        }
    }
}

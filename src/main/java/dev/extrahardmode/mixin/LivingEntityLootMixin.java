package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Dragon;
import dev.extrahardmode.feature.FeatureBus;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityLootMixin {
    @Inject(
            method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;ZLnet/minecraft/resources/ResourceKey;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void extrahardmode$lootlessMinions(
            ServerLevel level,
            DamageSource source,
            boolean causedByPlayer,
            ResourceKey<LootTable> lootTable,
            CallbackInfo ci) {
        if (!FeatureBus.guard((Level) level, Dragon.ID)) {
            return;
        }
        if (Dragon.skipLoot((LivingEntity) (Object) this)) {
            ci.cancel();
        }
    }
}

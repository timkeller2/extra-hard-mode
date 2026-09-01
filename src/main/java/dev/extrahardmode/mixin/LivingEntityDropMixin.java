package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.FeatureBus;
import dev.extrahardmode.feature.monster.Creepers;
import dev.extrahardmode.feature.monster.Zombies;
import dev.extrahardmode.module.EntityHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDropMixin {
    @Inject(method = "dropFromLootTable", at = @At("HEAD"), cancellable = true)
    private void extrahardmode$skipLootless(ServerLevel level, DamageSource source, boolean playerKill, CallbackInfo ci) {
        if (!FeatureBus.guard((Level) level, lootlessModule((LivingEntity) (Object) this))) {
            return;
        }
        if (EntityHelper.lootless((LivingEntity) (Object) this)) {
            ci.cancel();
        }
    }

    private static Identifier lootlessModule(LivingEntity entity) {
        return entity instanceof Creeper ? Creepers.ID : Zombies.ID;
    }
}

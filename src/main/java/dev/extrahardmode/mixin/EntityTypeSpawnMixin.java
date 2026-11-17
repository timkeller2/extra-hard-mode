package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.CopperGolems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A copper golem from a player's spawn egg remembers that player. A dispenser egg does not. */
@Mixin(EntityType.class)
public abstract class EntityTypeSpawnMixin {
    @Inject(
            method =
                    "spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;ZZ)Lnet/minecraft/world/entity/Entity;",
            at = @At("RETURN"))
    private void tougher$stampCopperGolem(
            ServerLevel level,
            ItemStack stack,
            LivingEntity user,
            BlockPos pos,
            EntitySpawnReason reason,
            boolean alignPosition,
            boolean invertY,
            CallbackInfoReturnable<Entity> cir) {
        CopperGolems.stampSummoned(cir.getReturnValue(), user);
    }
}

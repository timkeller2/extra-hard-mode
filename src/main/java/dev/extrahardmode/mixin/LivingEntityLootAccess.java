package dev.extrahardmode.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityLootAccess {
    @Invoker("shouldDropLoot")
    boolean tougher$shouldDropLoot(ServerLevel level);
}

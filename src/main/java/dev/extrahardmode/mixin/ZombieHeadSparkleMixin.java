package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.monster.Zombies;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Red sparkle on the hands of a zombie that rose from a head. */
@Mixin(Zombie.class)
public abstract class ZombieHeadSparkleMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void tougher$headSparkle(CallbackInfo ci) {
        Zombie zombie = (Zombie) (Object) this;
        if (zombie.level() instanceof ServerLevel level) {
            Zombies.sparkleHands(zombie, level);
        }
    }
}

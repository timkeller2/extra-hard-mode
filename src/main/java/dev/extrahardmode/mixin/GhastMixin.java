package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.monster.Ghasts;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.Ghast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Ghast.class)
public abstract class GhastMixin {
    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float ehm$scaleArrowDamage(float amount, ServerLevel level, DamageSource source) {
        if (!WorldGate.isModuleActive(level, Ghasts.ID)) {
            return amount;
        }
        return Ghasts.scaleIncoming((Ghast) (Object) this, source, amount);
    }
}

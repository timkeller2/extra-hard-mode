package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.SeasonAtmosphere;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnvironmentAttributeSystem.class)
public abstract class EnvironmentAttributeSystemMixin {
    @Inject(method = "addDynamicLayers", at = @At("RETURN"))
    private static void ehm$harshSeason(
            EnvironmentAttributeSystem.Builder builder, Level level, CallbackInfo ci) {
        SeasonAtmosphere.addLayers(builder, level);
    }
}

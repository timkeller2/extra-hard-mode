package dev.extrahardmode.mixin;

import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.feature.AntiFarming;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.chicken.Chicken;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Chicken.class)
public abstract class ChickenMixin {
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 6000))
    private int ehm$constructorEggTime(int vanilla) {
        return scaledEggTime(vanilla);
    }

    @ModifyConstant(method = "aiStep", constant = @Constant(intValue = 6000))
    private int ehm$laidEggTime(int vanilla) {
        return scaledEggTime(vanilla);
    }

    @Unique
    private int scaledEggTime(int vanilla) {
        Chicken self = (Chicken) (Object) this;
        if (!(self.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, AntiFarming.ID)) {
            return vanilla;
        }
        return AntiFarming.eggLayTime(vanilla, ConfigManager.world(level).eggLayTimeMultiplier());
    }
}

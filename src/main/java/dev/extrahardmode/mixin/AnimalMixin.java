package dev.extrahardmode.mixin;

import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.feature.AntiFarming;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Animal.class)
public abstract class AnimalMixin {
    @Inject(method = "finalizeSpawnChildFromBreeding", at = @At("RETURN"))
    private void ehm$longerBreedCooldown(
            ServerLevel level, Animal partner, AgeableMob child, CallbackInfo ci) {
        if (!WorldGate.isModuleActive(level, AntiFarming.ID)) {
            return;
        }
        int cooldown = AntiFarming.breedCooldownTicks(
                AntiFarming.VANILLA_BREED_COOLDOWN_TICKS,
                ConfigManager.world(level).animalBreedCooldownMultiplier());
        ((Animal) (Object) this).setAge(cooldown);
        if (partner != null) {
            partner.setAge(cooldown);
        }
    }
}

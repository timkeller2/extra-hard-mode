package dev.extrahardmode.mixin;

import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.feature.AntiFarming;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Sheep.class)
public abstract class SheepMixin {
    @Inject(method = "ate", at = @At("TAIL"))
    private void ehm$regrowWhite(CallbackInfo ci) {
        if (!(((Sheep) (Object) this).level() instanceof ServerLevel level)
                || !WorldGate.isModuleActive(level, AntiFarming.ID)) {
            return;
        }
        if (ConfigManager.world(level).sheepWhiteWool()) {
            ((Sheep) (Object) this).setColor(DyeColor.WHITE);
        }
    }

    @Inject(
            method = "getBreedOffspring(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/AgeableMob;)Lnet/minecraft/world/entity/animal/sheep/Sheep;",
            at = @At("RETURN"))
    private void ehm$breedWhite(ServerLevel level, AgeableMob other, CallbackInfoReturnable<Sheep> cir) {
        if (!WorldGate.isModuleActive(level, AntiFarming.ID)) {
            return;
        }
        if (!ConfigManager.world(level).sheepWhiteWool()) {
            return;
        }
        Sheep baby = cir.getReturnValue();
        if (baby != null) {
            baby.setColor(DyeColor.WHITE);
        }
    }
}

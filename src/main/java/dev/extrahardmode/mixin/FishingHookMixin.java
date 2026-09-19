package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.FishStocks;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin {
    @Redirect(
            method = "catchingFish",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/util/Mth;nextInt(Lnet/minecraft/util/RandomSource;II)I"))
    private int ehm$longerWaitBetweenBites(RandomSource random, int min, int max) {
        int value = Mth.nextInt(random, min, max);
        if (min != 100 || max != 600) {
            return value;
        }
        FishingHook self = (FishingHook) (Object) this;
        Level level = self.level();
        if (!(level instanceof ServerLevel serverLevel) || !WorldGate.isModuleActive(serverLevel, FishStocks.ID)) {
            return value;
        }
        return FishStocks.scaleBiteWait(serverLevel, value);
    }
}

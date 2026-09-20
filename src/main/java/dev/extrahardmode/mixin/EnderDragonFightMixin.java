package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Dragon;
import dev.extrahardmode.feature.FeatureBus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderDragonFight.class)
public abstract class EnderDragonFightMixin {
    @Inject(method = "setDragonKilled", at = @At("HEAD"))
    private void tougher$dragonLoot(EnderDragon dragon, CallbackInfo ci) {
        ServerLevel level = ((EnderDragonFightAccess) this).tougher$level();
        if (level == null || !FeatureBus.guard((Level) level, Dragon.ID)) {
            return;
        }
        Dragon.onDragonKilled(dragon);
    }
}

package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.Inhabitants;
import dev.extrahardmode.feature.ResidentCombatRules;
import dev.extrahardmode.feature.ResidentDefense;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Zombie.class)
public abstract class ZombieVillagerConvertMixin {
    @Inject(method = "convertVillagerToZombieVillager", at = @At("HEAD"), cancellable = true)
    private void tougher$residentsResist(
            ServerLevel level, Villager villager, CallbackInfoReturnable<Boolean> cir) {
        if (!Inhabitants.isInhabitant(villager)) {
            return;
        }
        if (!ResidentCombatRules.canZombify(ResidentDefense.healthBeforeHit(villager))) {
            cir.setReturnValue(false);
        }
    }
}

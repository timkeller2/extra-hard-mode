package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.AntiGrinder;
import dev.extrahardmode.feature.WiseLessons;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Monster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Monster.class)
public abstract class MonsterLootMixin {
    @Inject(method = "shouldDropLoot", at = @At("HEAD"), cancellable = true)
    private void ehm$antiGrinderLoot(ServerLevel level, CallbackInfoReturnable<Boolean> cir) {
        Monster monster = (Monster) (Object) this;
        if (WorldGate.isActive(level) && WiseLessons.isLessonMob(monster)) {
            cir.setReturnValue(true);
            return;
        }
        if (!WorldGate.isModuleActive(level, AntiGrinder.ID)) {
            return;
        }
        if (AntiGrinder.shouldBlockDrops(monster, level)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "shouldDropExperience", at = @At("HEAD"), cancellable = true)
    private void ehm$antiGrinderXp(CallbackInfoReturnable<Boolean> cir) {
        Monster monster = (Monster) (Object) this;
        if (!(monster.level() instanceof ServerLevel level)) {
            return;
        }
        if (WorldGate.isActive(level) && WiseLessons.isLessonMob(monster)) {
            cir.setReturnValue(true);
            return;
        }
        if (!WorldGate.isModuleActive(level, AntiGrinder.ID)) {
            return;
        }
        if (AntiGrinder.shouldBlockDrops(monster, level)) {
            cir.setReturnValue(false);
        }
    }
}

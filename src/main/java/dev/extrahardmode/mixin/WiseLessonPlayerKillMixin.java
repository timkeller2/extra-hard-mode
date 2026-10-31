package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.WiseLessons;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemKilledByPlayerCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A lesson creature's table treats every death as a player kill, so the rolls are not empty. */
@Mixin(LootItemKilledByPlayerCondition.class)
public abstract class WiseLessonPlayerKillMixin {
    @Inject(method = "test(Lnet/minecraft/world/level/storage/loot/LootContext;)Z", at = @At("HEAD"), cancellable = true)
    private void ehm$lessonCountsAsPlayerKill(LootContext context, CallbackInfoReturnable<Boolean> cir) {
        Entity entity = context.getOptional(LootContextParams.THIS_ENTITY);
        if (!(entity instanceof LivingEntity living) || !(living.level() instanceof ServerLevel level)) {
            return;
        }
        if (!WorldGate.isActive(level) || !WiseLessons.isLessonMob(living)) {
            return;
        }
        cir.setReturnValue(true);
    }
}

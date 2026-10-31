package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.WiseLessonRules;
import dev.extrahardmode.feature.WiseLessons;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lesson creatures drop twice and grant triple experience, including kills with no player blow.
 * The guard is set before the inner calls so those rolls are not doubled again.
 */
@Mixin(LivingEntity.class)
public abstract class WiseLessonLootMixin {
    private static final ThreadLocal<Boolean> DOUBLING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(
            method =
                    "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;ZLnet/minecraft/resources/ResourceKey;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void ehm$doubleLessonLoot(
            ServerLevel level, DamageSource source, boolean causedByPlayer, ResourceKey<LootTable> table, CallbackInfo ci) {
        if (Boolean.TRUE.equals(DOUBLING.get())) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        if (level == null || !WorldGate.isActive(level) || !WiseLessons.isLessonMob(self)) {
            return;
        }
        ci.cancel();
        DOUBLING.set(Boolean.TRUE);
        try {
            self.dropFromLootTable(level, source, true, table);
            self.dropFromLootTable(level, source, true, table);
        } finally {
            DOUBLING.set(Boolean.FALSE);
        }
    }

    @Inject(method = "dropExperience", at = @At("HEAD"), cancellable = true)
    private void ehm$lessonExperience(ServerLevel level, Entity killer, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (level == null || !WorldGate.isActive(level) || !WiseLessons.isLessonMob(self) || self.wasExperienceConsumed()) {
            return;
        }
        int xp = self.getExperienceReward(level, killer);
        if (xp > 0) {
            ExperienceOrb.award(level, self.position(), xp);
        }
        ci.cancel();
    }

    @Inject(method = "getExperienceReward", at = @At("RETURN"), cancellable = true)
    private void ehm$tripleLessonXp(ServerLevel level, Entity attacker, CallbackInfoReturnable<Integer> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (level == null || !WorldGate.isActive(level) || !WiseLessons.isLessonMob(self)) {
            return;
        }
        cir.setReturnValue(WiseLessonRules.scaledXp(cir.getReturnValueI()));
    }
}

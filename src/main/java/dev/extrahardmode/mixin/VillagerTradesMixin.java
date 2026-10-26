package dev.extrahardmode.mixin;

import dev.extrahardmode.feature.AntiFarming;
import dev.extrahardmode.feature.CropGrowthRules;
import dev.extrahardmode.feature.FeatureBus;
import dev.extrahardmode.feature.FletcherTrades;
import dev.extrahardmode.feature.Inhabitants;
import dev.extrahardmode.feature.JobClaimRules;
import dev.extrahardmode.feature.JobClaims;
import dev.extrahardmode.feature.LibrarianBooks;
import dev.extrahardmode.feature.VillagerNerf;
import dev.extrahardmode.feature.VillagerRestockRules;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Villager.class)
public abstract class VillagerTradesMixin {
    @Shadow
    private long lastRestockGameTime;

    @Shadow
    private long lastRestockCheckDay;

    @Shadow
    protected abstract boolean allowedToRestock();

    @Shadow
    protected abstract boolean needsToRestock();

    @Shadow
    protected abstract void resetNumberOfRestocks();

    @Inject(method = "updateTrades", at = @At("HEAD"), cancellable = true)
    private void tougher$skipInhabitantTrades(ServerLevel level, CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        if (Inhabitants.isInhabitant(self)) {
            Inhabitants.ensureOffers(self);
            ci.cancel();
        }
    }

    @Inject(method = "updateTrades", at = @At("RETURN"))
    private void tougher$nerfGeneratedTrades(ServerLevel level, CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        if (Inhabitants.isInhabitant(self)) {
            return;
        }
        if (WorldGate.isActive(level)) {
            LibrarianBooks.apply(self);
            FletcherTrades.apply(self);
        }
        if (FeatureBus.guard((Level) level, VillagerNerf.ID)) {
            VillagerNerf.apply(self);
        }
    }

    /** Blocks an unemployed villager from taking a job that already has its share nearby. */
    @Inject(method = "setVillagerData", at = @At("HEAD"), cancellable = true)
    private void tougher$limitJobClaims(VillagerData data, CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        if (!(self.level() instanceof ServerLevel level) || !WorldGate.isActive(level)) {
            return;
        }
        if (Inhabitants.isInhabitant(self) || data == null) {
            return;
        }
        VillagerData current = self.getVillagerData();
        if (current.profession().equals(data.profession())) {
            return;
        }
        if (!current.profession().is(VillagerProfession.NONE)) {
            return;
        }
        if (!JobClaimRules.isClaimedJob(
                data.profession().is(VillagerProfession.NONE), data.profession().is(VillagerProfession.NITWIT))) {
            return;
        }
        if (!JobClaims.blocked(level, self, data.profession())) {
            return;
        }
        self.releasePoi(MemoryModuleType.JOB_SITE);
        self.getBrain().eraseMemory(MemoryModuleType.JOB_SITE);
        ci.cancel();
    }

    @Inject(method = "mobInteract", at = @At("HEAD"))
    private void tougher$residentShopReady(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Villager self = (Villager) (Object) this;
        if (Inhabitants.isInhabitant(self) && !self.level().isClientSide()) {
            Inhabitants.prepareTrade(self);
        }
    }

    @Inject(method = "startTrading", at = @At("HEAD"))
    private void tougher$nerfExistingTrades(Player player, CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        if (Inhabitants.isInhabitant(self)) {
            Inhabitants.onStartTrading(self, player);
            return;
        }
        if (self.level() instanceof ServerLevel serverLevel && WorldGate.isActive(serverLevel)) {
            LibrarianBooks.apply(self);
            FletcherTrades.apply(self);
        }
        if (FeatureBus.guard(self.level(), VillagerNerf.ID)) {
            VillagerNerf.apply(self);
        }
    }

    @Inject(method = "canBreed", at = @At("HEAD"), cancellable = true)
    private void tougher$noInhabitantBreed(CallbackInfoReturnable<Boolean> cir) {
        if (Inhabitants.isInhabitant((Villager) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @ModifyConstant(method = "allowedToRestock", constant = @Constant(longValue = 2400L))
    private long ehm$betweenRestocks(long vanilla) {
        Villager self = (Villager) (Object) this;
        if (!(self.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, AntiFarming.ID)) {
            return vanilla;
        }
        return VillagerRestockRules.scaleTicks(vanilla);
    }

    @Inject(method = "shouldRestock", at = @At("HEAD"), cancellable = true)
    private void ehm$slowRestock(ServerLevel level, CallbackInfoReturnable<Boolean> cir) {
        Villager self = (Villager) (Object) this;
        if (Inhabitants.isInhabitant(self)) {
            cir.setReturnValue(false);
            return;
        }
        if (!WorldGate.isModuleActive(level, AntiFarming.ID)) {
            return;
        }
        long now = self.level().getGameTime();
        long day = CropGrowthRules.dayIndex(level.getOverworldClockTime());
        boolean overdue = now > lastRestockGameTime + VillagerRestockRules.catchUpTicks();
        boolean periodElapsed = VillagerRestockRules.restockPeriodElapsed(day, lastRestockCheckDay);
        lastRestockCheckDay = day;
        if (overdue || periodElapsed) {
            lastRestockGameTime = now;
            resetNumberOfRestocks();
        }
        cir.setReturnValue(allowedToRestock() && needsToRestock());
    }
}

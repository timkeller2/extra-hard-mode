package dev.extrahardmode.feature;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.block.state.BlockState;

/** Turns the villager who worked a removed job block into a nitwit. */
public final class JobSiteLoss {
    private JobSiteLoss() {}

    public static void onBlockChange(ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState) {
        if (level == null || pos == null || !workBlockLost(oldState, newState)) {
            return;
        }
        BlockPos at = pos.immutable();
        for (Villager villager : level.getEntities(EntityTypes.VILLAGER, Villager::isAlive)) {
            if (claims(level, villager, at)) {
                makeNitwit(level, villager);
            }
        }
    }

    /** A loaded villager whose remembered work block is no longer a job site. */
    public static void nitwitIfWorkBlockMissing(ServerLevel level, Villager villager) {
        if (level == null || villager == null || !canBecomeNitwit(villager)) {
            return;
        }
        Optional<GlobalPos> memory = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
        if (memory.isEmpty() || !memory.get().dimension().equals(level.dimension())) {
            return;
        }
        BlockPos at = memory.get().pos();
        if (!level.isLoaded(at)) {
            return;
        }
        if (stillJobSite(level.getBlockState(at))) {
            return;
        }
        makeNitwit(level, villager);
    }

    static boolean workBlockLost(BlockState oldState, BlockState newState) {
        if (oldState == null || newState == null) {
            return false;
        }
        Optional<Holder<PoiType>> oldSite = PoiTypes.forState(oldState);
        if (oldSite.isEmpty() || !oldSite.get().is(PoiTypeTags.ACQUIRABLE_JOB_SITE)) {
            return false;
        }
        return JobSiteLossRules.workBlockLost(true, oldSite.equals(PoiTypes.forState(newState)));
    }

    private static boolean stillJobSite(BlockState state) {
        Optional<Holder<PoiType>> site = PoiTypes.forState(state);
        return site.isPresent() && site.get().is(PoiTypeTags.ACQUIRABLE_JOB_SITE);
    }

    private static boolean claims(ServerLevel level, Villager villager, BlockPos pos) {
        Optional<GlobalPos> memory = villager.getBrain().getMemory(MemoryModuleType.JOB_SITE);
        if (memory.isEmpty()) {
            return false;
        }
        GlobalPos site = memory.get();
        return site.dimension().equals(level.dimension()) && site.pos().equals(pos);
    }

    private static boolean canBecomeNitwit(Villager villager) {
        var profession = villager.getVillagerData().profession();
        return JobSiteLossRules.becomesNitwit(
                Inhabitants.isInhabitant(villager),
                profession.is(VillagerProfession.NONE),
                profession.is(VillagerProfession.NITWIT));
    }

    private static void makeNitwit(ServerLevel level, Villager villager) {
        if (!canBecomeNitwit(villager)) {
            return;
        }
        villager.setVillagerData(villager.getVillagerData()
                .withProfession(level.registryAccess(), VillagerProfession.NITWIT));
        villager.releasePoi(MemoryModuleType.JOB_SITE);
        villager.releasePoi(MemoryModuleType.POTENTIAL_JOB_SITE);
        villager.refreshBrain(level);
        level.playSound(null, villager.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }
}

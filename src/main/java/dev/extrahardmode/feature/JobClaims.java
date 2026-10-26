package dev.extrahardmode.feature;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.phys.AABB;

/** Counts living villagers around someone about to take a job. Residents count. */
public final class JobClaims {
    private JobClaims() {}

    public static boolean blocked(ServerLevel level, Villager self, Holder<VillagerProfession> job) {
        if (level == null || self == null || job == null) {
            return false;
        }
        AABB box = self.getBoundingBox().inflate(JobClaimRules.RADIUS);
        int villagers = 0;
        int already = 0;
        boolean sawSelf = false;
        for (Villager other : level.getEntitiesOfClass(Villager.class, box, Villager::isAlive)) {
            if (!JobClaimRules.within(self.distanceToSqr(other))) {
                continue;
            }
            if (other == self) {
                sawSelf = true;
            }
            villagers++;
            if (other != self && other.getVillagerData().profession().equals(job)) {
                already++;
            }
        }
        if (!sawSelf) {
            villagers++;
        }
        return JobClaimRules.claimBlocked(already, villagers);
    }
}

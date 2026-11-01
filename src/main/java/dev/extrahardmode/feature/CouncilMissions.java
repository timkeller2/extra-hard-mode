package dev.extrahardmode.feature;

import dev.extrahardmode.mixin.PhantomAccess;
import dev.extrahardmode.module.SpawnReplaceService;
import dev.extrahardmode.network.ClientboundCouncilBountyPayload;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.InhabitantData;
import dev.extrahardmode.world.WorldGate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Per-player council kill bounties. Each council member can hold one, up to five at a time.
 */
public final class CouncilMissions {
    /** Same offset Phantom.finalizeSpawn uses, so a lifted phantom circles the air and not the ground. */
    private static final int PHANTOM_ANCHOR_ABOVE = 5;
    private static final int PHANTOM_LIFT_MIN = 6;
    private static final int PHANTOM_LIFT_MAX = 24;

    private static boolean spawnReplacementRegistered;
    /** Server thread only. Set by the roll and consumed by afterSpawn or cancelSpawn. */
    private static UUID pendingPlayer;
    private static String pendingGiver;
    private static String pendingMob;

    private CouncilMissions() {}

    /** One registration. Each open bounty is a separate 10% roll, and the first hit is the only swap. */
    public static void registerSpawnReplacement() {
        if (spawnReplacementRegistered) {
            return;
        }
        spawnReplacementRegistered = true;
        SpawnReplaceService.registerAny(new SpawnReplaceService.ReplaceFn() {
            @Override
            public EntityType<?> roll(Mob original, ServerLevel level) {
                return rollSpawnReplacement(original, level);
            }

            @Override
            public void afterSpawn(Mob original, Entity spawned) {
                finishSpawnReplacement(original, spawned);
            }

            @Override
            public void cancelSpawn() {
                clearPendingReplacement();
            }
        });
    }

    public static List<CouncilMissionRules.Mission> boardOf(ServerPlayer player) {
        List<CouncilMissionRules.Mission> stored = player.getAttached(EhmAttachments.EHM_COUNCIL_MISSIONS);
        List<CouncilMissionRules.Mission> board = new ArrayList<>();
        if (stored != null) {
            board.addAll(stored);
        }
        if (!board.isEmpty()) {
            return board;
        }
        CouncilMissionRules.Mission legacy = player.getAttached(EhmAttachments.EHM_COUNCIL_MISSION);
        if (!CouncilMissionRules.worthMigrating(legacy)) {
            return board;
        }
        board.add(legacy);
        player.setAttached(EhmAttachments.EHM_COUNCIL_MISSION, CouncilMissionRules.NONE);
        player.setAttached(EhmAttachments.EHM_COUNCIL_MISSIONS, new ArrayList<>(board));
        return board;
    }

    static void save(ServerPlayer player, List<CouncilMissionRules.Mission> board) {
        player.setAttached(EhmAttachments.EHM_COUNCIL_MISSIONS, new ArrayList<>(board));
        sendHud(player, board);
    }

    public static void sendHud(ServerPlayer player) {
        sendHud(player, boardOf(player));
    }

    static void sendHud(ServerPlayer player, List<CouncilMissionRules.Mission> board) {
        ServerPlayNetworking.send(player, ClientboundCouncilBountyPayload.from(CouncilMissionRules.hudLines(board)));
    }

    public static void tick(ServerPlayer player, long today) {
        List<CouncilMissionRules.Mission> board = boardOf(player);
        List<CouncilMissionRules.Mission> next = new ArrayList<>();
        boolean changed = false;
        for (CouncilMissionRules.Mission mission : board) {
            if (!CouncilMissionRules.expired(mission, today)) {
                next.add(mission);
                continue;
            }
            expire(player, mission);
            next.add(mission.clearHunt());
            changed = true;
        }
        if (changed) {
            save(player, next);
        }
    }

    /**
     * Bounty creature for this natural hostile spawn, or null to leave it. Friendly categories
     * (animals, fish, squid, bats) are never swapped. Uses the nearest survival player within
     * {@link CouncilMissionRules#BOUNTY_SPAWN_NEAR_BLOCKS}. The daily count is not saved here.
     * {@link #finishSpawnReplacement} saves it once the replacement is alive.
     */
    public static EntityType<?> rollSpawnReplacement(Mob mob, ServerLevel level) {
        clearPendingReplacement();
        if (mob == null || level == null || !WorldGate.isModuleActive(level, Inhabitants.ID)) {
            return null;
        }
        if (!CouncilMissionRules.bountyCanReplace(mob.getType().getCategory().getName())) {
            return null;
        }
        ServerPlayer player = nearestHunter(level, mob);
        if (player == null) {
            return null;
        }
        long today = AntiFarming.overworldDay(level);
        List<CouncilMissionRules.Mission> open = new ArrayList<>();
        for (CouncilMissionRules.Mission mission : boardOf(player)) {
            if (mission.active() && !CouncilMissionRules.expired(mission.assignedDay(), today)) {
                open.add(mission);
            }
        }
        if (open.isEmpty()) {
            return null;
        }
        List<String> ids = new ArrayList<>();
        boolean[] capped = new boolean[open.size()];
        for (int i = 0; i < open.size(); i++) {
            CouncilMissionRules.Mission mission = open.get(i);
            ids.add(mission.mobId());
            capped[i] = CouncilMissionRules.spawnCapped(
                    mission.spawnCountDay(), mission.spawnReplacements(), today);
        }
        Identifier spawning = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        String spawningId = spawning == null ? "" : spawning.toString();
        int[] rolls = new int[ids.size()];
        var random = level.getRandom();
        for (int i = 0; i < rolls.length; i++) {
            rolls[i] = random.nextInt(100);
        }
        int index = CouncilMissionRules.bountyReplacementIndex(ids, spawningId, rolls, capped);
        if (index < 0) {
            return null;
        }
        Identifier id = Identifier.tryParse(ids.get(index));
        if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            return null;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
        if (type == mob.getType()) {
            return null;
        }
        CouncilMissionRules.Mission chosen = open.get(index);
        pendingPlayer = player.getUUID();
        pendingGiver = chosen.giverId() == null ? "" : chosen.giverId();
        pendingMob = chosen.mobId() == null ? "" : chosen.mobId();
        return type;
    }

    /**
     * Counts the replacement only after it is in the world, then keeps it. A monster between 32 and
     * 128 blocks despawns after about a minute, which was deleting the day's three swaps before a
     * flying player reached them. A foot-level phantom mostly shows wing spores, so it is lifted.
     */
    private static void finishSpawnReplacement(Mob original, Entity spawned) {
        UUID playerId = pendingPlayer;
        String giverId = pendingGiver == null ? "" : pendingGiver;
        String mobId = pendingMob == null ? "" : pendingMob;
        clearPendingReplacement();
        if (!(spawned instanceof Mob replacement) || !replacement.isAlive()) {
            return;
        }
        if (!(original.level() instanceof ServerLevel level)) {
            return;
        }
        if (playerId != null) {
            ServerPlayer counted = level.getServer().getPlayerList().getPlayer(playerId);
            if (counted != null) {
                recordReplacement(counted, giverId, mobId, AntiFarming.overworldDay(level));
            }
        }
        replacement.setPersistenceRequired();
        AttributeInstance follow = replacement.getAttribute(Attributes.FOLLOW_RANGE);
        if (follow != null) {
            follow.setBaseValue(CouncilMissionRules.BOUNTY_SPAWN_NEAR_BLOCKS);
        }
        ServerPlayer hunter = nearestHunter(level, replacement);
        if (hunter != null) {
            replacement.setTarget(hunter);
        }
        if (replacement instanceof Phantom phantom) {
            liftPhantom(level, phantom);
        }
    }

    private static void recordReplacement(ServerPlayer player, String giverId, String mobId, long today) {
        List<CouncilMissionRules.Mission> board = boardOf(player);
        List<CouncilMissionRules.Mission> next = new ArrayList<>();
        boolean counted = false;
        for (CouncilMissionRules.Mission mission : board) {
            String id = mission.giverId() == null ? "" : mission.giverId();
            if (!counted
                    && mission.active()
                    && !CouncilMissionRules.expired(mission.assignedDay(), today)
                    && id.equals(giverId)
                    && mobId.equals(mission.mobId())
                    && !CouncilMissionRules.spawnCapped(
                            mission.spawnCountDay(), mission.spawnReplacements(), today)) {
                next.add(mission.withReplacement(today));
                counted = true;
            } else {
                next.add(mission);
            }
        }
        if (counted) {
            save(player, next);
        }
    }

    /** First open column from 6 to 24 blocks up. The anchor moves with it so the circle does not dive. */
    private static void liftPhantom(ServerLevel level, Phantom phantom) {
        int base = phantom.getBlockY();
        int top = Math.min(base + PHANTOM_LIFT_MAX, level.getMaxY() - 2);
        int x = phantom.getBlockX();
        int z = phantom.getBlockZ();
        for (int y = base + PHANTOM_LIFT_MIN; y <= top; y++) {
            BlockPos feet = new BlockPos(x, y, z);
            if (!level.isLoaded(feet)) {
                return;
            }
            if (level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir()) {
                phantom.snapTo(phantom.getX(), y, phantom.getZ(), phantom.getYRot(), phantom.getXRot());
                ((PhantomAccess) phantom).tougher$setAnchorPoint(feet.above(PHANTOM_ANCHOR_ABOVE));
                return;
            }
        }
    }

    private static void clearPendingReplacement() {
        pendingPlayer = null;
        pendingGiver = null;
        pendingMob = null;
    }

    private static ServerPlayer nearestHunter(ServerLevel level, Mob mob) {
        double limit = (double) CouncilMissionRules.BOUNTY_SPAWN_NEAR_BLOCKS * CouncilMissionRules.BOUNTY_SPAWN_NEAR_BLOCKS;
        ServerPlayer nearest = null;
        double best = limit;
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || Achievements.skipPlayer(player)) {
                continue;
            }
            double distance = player.distanceToSqr(mob);
            if (distance > limit) {
                continue;
            }
            if (nearest == null || distance < best) {
                best = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    public static void onKill(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof Enemy) || entity instanceof Player) {
            return;
        }
        Entity attacker = source.getEntity();
        if (!(attacker instanceof ServerPlayer player) || Achievements.skipPlayer(player)) {
            return;
        }
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (typeId == null) {
            return;
        }
        long today = AntiFarming.overworldDay(level);
        String mobId = typeId.toString();
        List<CouncilMissionRules.Mission> board = boardOf(player);
        List<CouncilMissionRules.Mission> next = new ArrayList<>();
        boolean changed = false;
        for (CouncilMissionRules.Mission mission : board) {
            if (CouncilMissionRules.expired(mission, today)) {
                expire(player, mission);
                next.add(mission.clearHunt());
                changed = true;
                continue;
            }
            if (!mission.active() || !mission.mobId().equals(mobId)) {
                next.add(mission);
                continue;
            }
            int kills = mission.kills() + 1;
            if (kills >= mission.target()) {
                next.add(finish(player, level, mission.withKills(kills)));
            } else {
                next.add(mission.withKills(kills));
            }
            changed = true;
        }
        if (changed) {
            save(player, next);
        }
    }

    public static void onTalk(ServerPlayer player, ServerLevel level, InhabitantData.Home home) {
        if (Achievements.skipPlayer(player)) {
            return;
        }
        long today = AntiFarming.overworldDay(level);
        String giverId = home == null || home.id() == null ? "" : home.id();
        String giverName = home == null || home.name() == null || home.name().isEmpty() ? "The council" : home.name();
        List<CouncilMissionRules.Mission> board =
                new ArrayList<>(CouncilMissionRules.claimLegacy(boardOf(player), giverId, giverName));
        boolean changed = !board.equals(boardOf(player));
        boolean thisOneExpired = false;
        List<CouncilMissionRules.Mission> kept = new ArrayList<>();
        for (CouncilMissionRules.Mission mission : board) {
            if (!CouncilMissionRules.expired(mission, today)) {
                kept.add(mission);
                continue;
            }
            String id = mission.giverId() == null ? "" : mission.giverId();
            if (id.equals(giverId)) {
                thisOneExpired = true;
            }
            expire(player, mission);
            kept.add(mission.clearHunt());
            changed = true;
        }
        board = kept;
        CouncilMissionRules.Mission slot = CouncilMissionRules.findGiver(board, giverId);
        if (slot != null && slot.awaitingTurnIn()) {
            board = new ArrayList<>(payEmeralds(player, level, giverName, board, slot));
            changed = true;
            slot = CouncilMissionRules.findGiver(board, giverId);
        }
        if (slot != null && slot.active()) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.council_mission_progress",
                    "Your bounty: %s / %s %s.",
                    Component.literal(Integer.toString(slot.kills())),
                    Component.literal(Integer.toString(slot.target())),
                    Component.literal(slot.label())));
            if (changed) {
                save(player, board);
            }
            return;
        }
        if (thisOneExpired) {
            if (changed) {
                save(player, board);
            }
            return;
        }
        if (slot != null && !CouncilMissionRules.canTakeNext(slot, today)) {
            int wait = CouncilMissionRules.daysUntilNextMission(slot.assignedDay(), today);
            if (wait > 0) {
                player.sendSystemMessage(Component.translatableWithFallback(
                        "tougher.message.council_mission_wait",
                        "Another bounty will be ready in %s.",
                        Component.literal(wait == 1 ? "1 Minecraft day" : wait + " Minecraft days")));
            }
            if (changed) {
                save(player, board);
            }
            return;
        }
        if (CouncilMissionRules.atCap(board)) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "tougher.message.council_mission_cap",
                    "You already have 5 bounties. Finish one before taking another."));
            if (changed) {
                save(player, board);
            }
            return;
        }
        assign(player, level, board, giverId, giverName, home == null ? InhabitantRules.MIN_SCORE : home.score(), today);
    }

    static void assign(
            ServerPlayer player,
            ServerLevel level,
            List<CouncilMissionRules.Mission> board,
            String giverId,
            String giverName,
            int houseScore,
            long today) {
        CouncilMissionRules.Mission prior = CouncilMissionRules.findGiver(board, giverId);
        CouncilMissionRules.Mission next = CouncilMissionRules.assign(
                prior == null ? CouncilMissionRules.NONE : prior,
                giverId,
                giverName,
                houseScore,
                today,
                level.getRandom().nextInt(),
                level.getRandom().nextInt());
        save(player, CouncilMissionRules.put(board, next));
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.council_mission_new",
                "%s wants you to slay %s %s. You have 14 Minecraft days.",
                Component.literal(giverName),
                Component.literal(Integer.toString(next.target())),
                Component.literal(next.label())));
    }

    static CouncilMissionRules.Mission finish(
            ServerPlayer player, ServerLevel level, CouncilMissionRules.Mission mission) {
        int xp = CouncilMissionRules.completionXp(mission.hp(), mission.target(), mission.houseScore());
        int emeralds = CouncilMissionRules.emeraldReward(xp);
        CouncilMissionRules.Mission done = mission.asCompleted(xp, emeralds);
        player.giveExperiencePoints(xp);
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.council_mission_complete",
                "Bounty complete! +%s experience.",
                Component.literal(Integer.toString(xp))));
        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                SoundSource.PLAYERS,
                0.8F,
                1.1F);
        return done;
    }

    static List<CouncilMissionRules.Mission> payEmeralds(
            ServerPlayer player,
            ServerLevel level,
            String giverName,
            List<CouncilMissionRules.Mission> board,
            CouncilMissionRules.Mission mission) {
        int emeralds = Math.max(0, mission.pendingEmeralds());
        if (emeralds > 0) {
            giveEmeralds(player, emeralds);
        }
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.council_mission_payout",
                "%s pays you %s emeralds for the completed bounty.",
                Component.literal(giverName),
                Component.literal(Integer.toString(emeralds))));
        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS,
                0.9F,
                1.0F);
        level.playSound(
                null, player.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);
        return CouncilMissionRules.put(board, mission.clearHunt());
    }

    static void expire(ServerPlayer player, CouncilMissionRules.Mission mission) {
        String label = mission.label() == null || mission.label().isEmpty() ? "your quarry" : mission.label();
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.council_mission_expired",
                "The bounty on %s has expired.",
                Component.literal(label)));
    }

    static void giveEmeralds(ServerPlayer player, int amount) {
        int left = Math.max(0, amount);
        while (left > 0) {
            int n = Math.min(64, left);
            ItemStack stack = new ItemStack(Items.EMERALD, n);
            player.getInventory().placeItemBackInInventory(stack, net.minecraft.util.Prediction.SERVER_ONLY);
            left -= n;
        }
    }
}

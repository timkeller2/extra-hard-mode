package dev.extrahardmode.feature;

import dev.extrahardmode.network.ClientboundCouncilBountyPayload;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.InhabitantData;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Per-player council kill bounties. Each council member can hold one, up to five at a time.
 */
public final class CouncilMissions {
    private CouncilMissions() {}

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

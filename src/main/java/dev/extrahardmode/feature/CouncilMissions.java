package dev.extrahardmode.feature;

import dev.extrahardmode.network.ClientboundCouncilBountyPayload;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.InhabitantData;
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
 * Per-player council kill bounties: assign on talk, track kills, pay XP then emeralds.
 */
public final class CouncilMissions {
    private CouncilMissions() {}

    public static CouncilMissionRules.Mission missionOf(ServerPlayer player) {
        CouncilMissionRules.Mission stored = player.getAttached(EhmAttachments.EHM_COUNCIL_MISSION);
        return stored == null ? CouncilMissionRules.NONE : stored;
    }

    public static void setMission(ServerPlayer player, CouncilMissionRules.Mission mission) {
        player.setAttached(
                EhmAttachments.EHM_COUNCIL_MISSION, mission == null ? CouncilMissionRules.NONE : mission);
        sendHud(player);
    }

    public static void sendHud(ServerPlayer player) {
        ServerPlayNetworking.send(player, ClientboundCouncilBountyPayload.from(missionOf(player)));
    }

    public static void tick(ServerPlayer player, long today) {
        CouncilMissionRules.Mission mission = missionOf(player);
        if (!CouncilMissionRules.expired(mission, today)) {
            return;
        }
        expire(player, mission);
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
        if (!Achievements.lootableKill(entity, level)) {
            return;
        }
        Identifier typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (typeId == null) {
            return;
        }
        CouncilMissionRules.Mission mission = missionOf(player);
        long today = AntiFarming.overworldDay(level);
        if (CouncilMissionRules.expired(mission, today)) {
            expire(player, mission);
            return;
        }
        if (!mission.active() || !mission.mobId().equals(typeId.toString())) {
            return;
        }
        int kills = mission.kills() + 1;
        if (kills >= mission.target()) {
            complete(player, level, mission.withKills(kills));
            return;
        }
        setMission(player, mission.withKills(kills));
    }

    public static void onTalk(ServerPlayer player, ServerLevel level, InhabitantData.Home home) {
        if (Achievements.skipPlayer(player)) {
            return;
        }
        long today = AntiFarming.overworldDay(level);
        CouncilMissionRules.Mission mission = missionOf(player);
        if (CouncilMissionRules.expired(mission, today)) {
            expire(player, mission);
            mission = missionOf(player);
        }
        if (mission.awaitingTurnIn()) {
            payEmeralds(player, level, home, mission);
            mission = missionOf(player);
        }
        if (mission.active()) {
            player.sendSystemMessage(Component.translatableWithFallback(
                    "extrahardmode.message.council_mission_progress",
                    "Your bounty: %s / %s %s.",
                    Component.literal(Integer.toString(mission.kills())),
                    Component.literal(Integer.toString(mission.target())),
                    Component.literal(mission.label())));
            return;
        }
        assign(player, level, home, today);
    }

    static void assign(ServerPlayer player, ServerLevel level, InhabitantData.Home home, long today) {
        CouncilMissionRules.Mission next = CouncilMissionRules.assign(
                missionOf(player),
                home == null ? InhabitantRules.MIN_SCORE : home.score(),
                today,
                level.getRandom().nextInt(),
                level.getRandom().nextInt());
        setMission(player, next);
        String who = home == null || home.name().isEmpty() ? "The council" : home.name();
        player.sendSystemMessage(Component.translatableWithFallback(
                "extrahardmode.message.council_mission_new",
                "%s wants you to slay %s %s. You have 7 Minecraft days.",
                Component.literal(who),
                Component.literal(Integer.toString(next.target())),
                Component.literal(next.label())));
    }

    static void complete(ServerPlayer player, ServerLevel level, CouncilMissionRules.Mission mission) {
        int xp = CouncilMissionRules.completionXp(mission.hp(), mission.target(), mission.houseScore());
        int emeralds = CouncilMissionRules.emeraldReward(xp);
        CouncilMissionRules.Mission done = mission.asCompleted(xp, emeralds);
        player.giveExperiencePoints(xp);
        setMission(player, done);
        player.sendSystemMessage(Component.translatableWithFallback(
                "extrahardmode.message.council_mission_complete",
                "Bounty complete! +%s experience.",
                Component.literal(Integer.toString(xp))));
        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                SoundSource.PLAYERS,
                0.8F,
                1.1F);
    }

    static void payEmeralds(
            ServerPlayer player, ServerLevel level, InhabitantData.Home home, CouncilMissionRules.Mission mission) {
        int emeralds = Math.max(0, mission.pendingEmeralds());
        if (emeralds > 0) {
            giveEmeralds(player, emeralds);
        }
        setMission(player, mission.clearHunt());
        String who = home == null || home.name().isEmpty() ? "The council" : home.name();
        player.sendSystemMessage(Component.translatableWithFallback(
                "extrahardmode.message.council_mission_payout",
                "%s pays you %s emeralds for the completed bounty.",
                Component.literal(who),
                Component.literal(Integer.toString(emeralds))));
        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS,
                0.9F,
                1.0F);
        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.VILLAGER_YES,
                SoundSource.NEUTRAL,
                1.0F,
                1.0F);
    }

    static void expire(ServerPlayer player, CouncilMissionRules.Mission mission) {
        String label = mission.label() == null || mission.label().isEmpty() ? "your quarry" : mission.label();
        setMission(player, mission.clearHunt());
        player.sendSystemMessage(Component.translatableWithFallback(
                "extrahardmode.message.council_mission_expired",
                "The bounty on %s has expired.",
                Component.literal(label)));
    }

    static void giveEmeralds(ServerPlayer player, int amount) {
        int left = Math.max(0, amount);
        while (left > 0) {
            int n = Math.min(64, left);
            ItemStack stack = new ItemStack(Items.EMERALD, n);
            player.getInventory().placeItemBackInInventory(stack);
            left -= n;
        }
    }
}

package dev.extrahardmode.feature;

import dev.extrahardmode.network.ClientboundCartographyPayload;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.InhabitantData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Per-player map marks from cartographers. One cartographer can mark for several players. */
public final class Cartographers {
    private static final Map<UUID, String> LAST_HUD = new ConcurrentHashMap<>();

    private Cartographers() {}

    public static void clearHud(ServerPlayer player) {
        if (player != null) {
            LAST_HUD.remove(player.getUUID());
        }
    }

    public static void tick(ServerPlayer player, ServerLevel level, long today) {
        if (player == null || level == null) {
            return;
        }
        List<CartographyRules.Mission> board = boardOf(player);
        if (board.isEmpty()) {
            sendHud(player, board, today);
            return;
        }
        String dimension = level.dimension().identifier().toString();
        int px = player.getBlockX();
        int pz = player.getBlockZ();
        boolean changed = false;
        for (int i = 0; i < board.size(); i++) {
            CartographyRules.Mission mission = board.get(i);
            if (!mission.active()) {
                continue;
            }
            boolean here = dimension.equals(mission.dimension());
            if (here && CartographyRules.arrived(px, pz, mission.x(), mission.z())) {
                board.set(i, finish(player, level, mission, today));
                changed = true;
                continue;
            }
            if (mission.expireDay() >= 0L && today >= mission.expireDay()) {
                board.set(i, mission.expired());
                tellExpired(player, mission.giverName());
                changed = true;
            }
        }
        if (changed) {
            save(player, board, today);
        } else {
            sendHud(player, board, today);
        }
    }

    public static void onTalk(ServerPlayer player, ServerLevel level, InhabitantData.Home home, Villager villager) {
        if (player == null || level == null || home == null || villager == null) {
            return;
        }
        long today = AntiFarming.overworldDay(level);
        String id = villager.getUUID().toString();
        String name = home.name() == null || home.name().isBlank() ? "Cartographer" : home.name().trim();
        List<CartographyRules.Mission> board = boardOf(player);
        int index = indexOf(board, id);
        CartographyRules.Mission mission =
                (index < 0 ? CartographyRules.Mission.blank(id, name) : board.get(index)).named(id, name);
        String dimension = level.dimension().identifier().toString();
        if (mission.active()) {
            boolean here = dimension.equals(mission.dimension());
            if (here && CartographyRules.arrived(player.getBlockX(), player.getBlockZ(), mission.x(), mission.z())) {
                mission = finish(player, level, mission, today);
                put(board, index, mission);
                save(player, board, today);
                return;
            } else if (mission.expireDay() >= 0L && today >= mission.expireDay()) {
                tellExpired(player, mission.giverName());
                mission = mission.expired();
            } else if (!here) {
                tell(
                        player,
                        "tougher.cartography.elsewhere",
                        "%s: That place is in another dimension.",
                        Component.literal(mission.giverName()));
                put(board, index, mission);
                save(player, board, today);
                return;
            } else {
                tellMark(player, mission);
                put(board, index, mission);
                save(player, board, today);
                return;
            }
        }
        if (mission.nextDay() > today) {
            tellWait(player, mission.giverName(), today, mission.nextDay());
            put(board, index, mission);
            save(player, board, today);
            return;
        }
        mission = assign(level, home, villager, mission, today, dimension);
        tellMark(player, mission);
        put(board, index, mission);
        save(player, board, today);
    }

    private static CartographyRules.Mission assign(
            ServerLevel level,
            InhabitantData.Home home,
            Villager villager,
            CartographyRules.Mission mission,
            long today,
            String dimension) {
        int dice = CartographyRules.threeD6(
                level.getRandom().nextInt(), level.getRandom().nextInt(), level.getRandom().nextInt());
        double swing = level.getRandom().nextDouble() * 2.0 - 1.0;
        int distance = CartographyRules.distance(home.score(), mission.successes(), dice, swing);
        int angle = level.getRandom().nextInt(360);
        int[] point = CartographyRules.place(villager.getBlockX(), villager.getBlockZ(), distance, angle);
        return mission.assigned(point[0], point[1], distance, dimension, today);
    }

    private static CartographyRules.Mission finish(
            ServerPlayer player, ServerLevel level, CartographyRules.Mission mission, long today) {
        int xp = CartographyRules.experience(mission.distance());
        int emeralds = CartographyRules.emeralds(mission.distance());
        if (xp > 0) {
            player.giveExperiencePoints(xp);
        }
        if (emeralds > 0) {
            giveEmeralds(player, emeralds);
        }
        CartographyRules.Mission done = mission.completed(today);
        tellReward(player, xp, emeralds);
        tellWait(player, done.giverName(), today, done.nextDay());
        level.playSound(
                null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.9F, 1.0F);
        level.playSound(null, player.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);
        return done;
    }

    private static void giveEmeralds(ServerPlayer player, int amount) {
        int left = Math.max(0, amount);
        while (left > 0) {
            int n = Math.min(64, left);
            player.getInventory()
                    .placeItemBackInInventory(
                            new ItemStack(Items.EMERALD, n), net.minecraft.util.Prediction.SERVER_ONLY);
            left -= n;
        }
    }

    private static List<CartographyRules.Mission> boardOf(ServerPlayer player) {
        List<CartographyRules.Mission> stored = player.getAttached(EhmAttachments.EHM_CARTOGRAPHY);
        return stored == null ? new ArrayList<>() : new ArrayList<>(stored);
    }

    private static void save(ServerPlayer player, List<CartographyRules.Mission> board, long today) {
        player.setAttached(EhmAttachments.EHM_CARTOGRAPHY, new ArrayList<>(board));
        sendHud(player, board, today);
    }

    private static void sendHud(ServerPlayer player, List<CartographyRules.Mission> board, long today) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        List<String> lines = CartographyRules.hudLines(
                board,
                level.dimension().identifier().toString(),
                player.getBlockX(),
                player.getBlockZ(),
                today,
                level.getOverworldClockTime());
        String key = String.join("\n", lines);
        String previous = LAST_HUD.put(player.getUUID(), key);
        if (key.equals(previous)) {
            return;
        }
        ServerPlayNetworking.send(player, ClientboundCartographyPayload.from(lines));
    }

    private static int indexOf(List<CartographyRules.Mission> board, String giverId) {
        for (int i = 0; i < board.size(); i++) {
            if (giverId.equals(board.get(i).giverId())) {
                return i;
            }
        }
        return -1;
    }

    private static void put(List<CartographyRules.Mission> board, int index, CartographyRules.Mission mission) {
        if (index < 0) {
            board.add(mission);
        } else {
            board.set(index, mission);
        }
    }

    private static void tellMark(ServerPlayer player, CartographyRules.Mission mission) {
        String direction = CartographyRules.direction(
                mission.x() - player.getBlockX(), mission.z() - player.getBlockZ());
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.cartography.mark",
                "%s: %s",
                Component.literal(mission.giverName()),
                Component.literal(direction)));
    }

    private static void tellReward(ServerPlayer player, int xp, int emeralds) {
        if (emeralds == 1) {
            tell(
                    player,
                    "tougher.cartography.found_one",
                    "You reach the place. +%s experience and 1 emerald.",
                    Component.literal(Integer.toString(xp)));
            return;
        }
        if (emeralds > 1) {
            tell(
                    player,
                    "tougher.cartography.found",
                    "You reach the place. +%s experience and %s emeralds.",
                    Component.literal(Integer.toString(xp)),
                    Component.literal(Integer.toString(emeralds)));
            return;
        }
        tell(
                player,
                "tougher.cartography.found_xp",
                "You reach the place. +%s experience.",
                Component.literal(Integer.toString(xp)));
    }

    private static void tellWait(ServerPlayer player, String name, long today, long nextDay) {
        long left = Math.max(0L, nextDay - today);
        Component who = Component.literal(name == null || name.isBlank() ? "Cartographer" : name);
        if (left <= 1L) {
            tell(player, "tougher.cartography.wait_one", "%s: Come back in 1 Minecraft day.", who);
            return;
        }
        tell(
                player,
                "tougher.cartography.wait",
                "%s: Come back in %s Minecraft days.",
                who,
                Component.literal(Long.toString(left)));
    }

    private static void tellExpired(ServerPlayer player, String name) {
        tell(
                player,
                "tougher.cartography.expired",
                "%s: That mark has expired.",
                Component.literal(name == null || name.isBlank() ? "Cartographer" : name));
    }

    private static void tell(ServerPlayer player, String key, String fallback, Component... args) {
        player.sendSystemMessage(Component.translatableWithFallback(key, fallback, (Object[]) args));
    }
}

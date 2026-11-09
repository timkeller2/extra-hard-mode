package dev.extrahardmode.feature;

import dev.extrahardmode.network.ClientboundMinerLeadPayload;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.InhabitantData;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/** Per-player ore leads from old miners. One miner can lead several players at once. */
public final class OldMiners {
    private static final Map<UUID, String> LAST_HUD = new ConcurrentHashMap<>();

    private OldMiners() {}

    public static void clearHud(ServerPlayer player) {
        if (player != null) {
            LAST_HUD.remove(player.getUUID());
        }
    }

    public static void tick(ServerPlayer player, ServerLevel level, long today) {
        if (player == null || level == null) {
            return;
        }
        List<OldMinerRules.Lead> board = boardOf(player);
        if (board.isEmpty()) {
            sendHud(player, board, today);
            return;
        }
        String dimension = level.dimension().identifier().toString();
        int px = player.getBlockX();
        int py = player.getBlockY();
        int pz = player.getBlockZ();
        boolean changed = false;
        for (int i = 0; i < board.size(); i++) {
            OldMinerRules.Lead lead = board.get(i);
            if (!lead.active()) {
                continue;
            }
            boolean here = dimension.equals(lead.dimension());
            if (here && OldMinerRules.arrived(px, py, pz, lead.x(), lead.y(), lead.z())) {
                board.set(i, lead.completed());
                tellFound(player, lead.ore());
                changed = true;
                continue;
            }
            if (lead.expireDay() >= 0L && today >= lead.expireDay()) {
                board.set(i, lead.expired());
                changed = true;
            }
        }
        if (changed) {
            save(player, board, today);
        } else {
            sendHud(player, board, today);
        }
    }

    public static void onTalk(
            ServerPlayer player, ServerLevel level, InhabitantData.Home home, UUID minerId) {
        if (player == null || level == null || home == null || minerId == null) {
            return;
        }
        long today = AntiFarming.overworldDay(level);
        String id = minerId.toString();
        String name = home.name() == null || home.name().isBlank() ? "Miner" : home.name().trim();
        List<OldMinerRules.Lead> board = boardOf(player);
        int index = indexOf(board, id);
        OldMinerRules.Lead lead = (index < 0 ? OldMinerRules.Lead.blank(id, name) : board.get(index)).named(id, name);
        String dimension = level.dimension().identifier().toString();
        if (lead.active()) {
            boolean here = dimension.equals(lead.dimension());
            if (here && OldMinerRules.arrived(player.getBlockX(), player.getBlockY(), player.getBlockZ(), lead.x(), lead.y(), lead.z())) {
                tellFound(player, lead.ore());
                lead = lead.completed();
            } else if (lead.expireDay() >= 0L && today >= lead.expireDay()) {
                lead = lead.expired();
            } else if (!here) {
                tell(
                        player,
                        "tougher.miner.elsewhere",
                        "%s: %s is in another dimension.",
                        Component.literal(lead.minerName()),
                        Component.literal(OldMinerRules.oreTitle(lead.ore())));
                put(board, index, lead);
                save(player, board, today);
                return;
            } else {
                tellLead(player, lead, player.getBlockX(), player.getBlockY(), player.getBlockZ());
                put(board, index, lead);
                save(player, board, today);
                return;
            }
        }
        if (lead.nextDay() > today) {
            tellWait(player, today, lead.nextDay());
            put(board, index, lead);
            save(player, board, today);
            return;
        }
        if (OldMinerRules.offerCount(home.score(), lead.successes()) <= 0) {
            tell(player, "tougher.miner.small", "This house is too small for me to name an ore.");
            put(board, index, lead);
            save(player, board, today);
            return;
        }
        lead = assign(player, level, home, lead, today, dimension);
        if (lead.active()
                && dimension.equals(lead.dimension())
                && OldMinerRules.arrived(
                        player.getBlockX(), player.getBlockY(), player.getBlockZ(), lead.x(), lead.y(), lead.z())) {
            tellFound(player, lead.ore());
            lead = lead.completed();
        }
        put(board, index, lead);
        save(player, board, today);
    }

    private static OldMinerRules.Lead assign(
            ServerPlayer player,
            ServerLevel level,
            InhabitantData.Home home,
            OldMinerRules.Lead lead,
            long today,
            String dimension) {
        BlockPos bed = home.bed();
        int bedX = bed == null ? player.getBlockX() : bed.getX();
        int bedY = bed == null ? player.getBlockY() : bed.getY();
        int bedZ = bed == null ? player.getBlockZ() : bed.getZ();
        Map<String, List<OldMinerRules.Spot>> samples = scan(level, bedX, bedZ);
        List<String> finished = lead.finishedOres();
        int successes = lead.successes();
        Set<String> attempted = new LinkedHashSet<>();
        int guard = OldMinerRules.ORES.size() + 1;
        while (guard-- > 0) {
            OldMinerRules.Offer offer = OldMinerRules.pickOffer(
                    home.score(), successes, samples.keySet(), finished, attempted, level.getRandom().nextInt());
            if (offer == null) {
                tell(player, "tougher.miner.none", "I know of no vein nearby.");
                return lead.waiting(today);
            }
            List<OldMinerRules.Spot> vein = OldMinerRules.largestVein(samples.get(offer.ore()), bedX, bedY, bedZ);
            if (vein.isEmpty()) {
                attempted.add(offer.ore());
                continue;
            }
            OldMinerRules.Spot anchor = OldMinerRules.anchor(vein, bedX, bedY, bedZ);
            grow(level, vein, offer.ore(), offer.poolSize());
            OldMinerRules.Lead assigned = lead.withProgress(successes, offer.finished())
                    .assigned(offer.ore(), anchor.x(), anchor.y(), anchor.z(), dimension, today);
            tellLead(player, assigned, player.getBlockX(), player.getBlockY(), player.getBlockZ());
            return assigned;
        }
        tell(player, "tougher.miner.none", "I know of no vein nearby.");
        return lead.waiting(today);
    }

    /** Ore in the miner's chunk and the eight chunks around it, grouped by Detect Ore family. */
    static Map<String, List<OldMinerRules.Spot>> scan(ServerLevel level, int bedX, int bedZ) {
        Map<String, List<OldMinerRules.Spot>> byFamily = new HashMap<>();
        int chunkX = bedX >> 4;
        int chunkZ = bedZ >> 4;
        int minY = level.getMinY();
        int maxY = level.getMaxY();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                int cx = chunkX + ox;
                int cz = chunkZ + oz;
                LevelChunk chunk = level.getChunk(cx, cz);
                int x0 = cx << 4;
                int z0 = cz << 4;
                for (int y = minY; y < maxY; y++) {
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            cursor.set(x0 + x, y, z0 + z);
                            BlockState state = chunk.getBlockState(cursor);
                            if (state.isAir()) {
                                continue;
                            }
                            String id = DetectOre.blockId(state);
                            if (!AbilityRules.isDetectableOre(id)) {
                                continue;
                            }
                            String family = AbilityRules.oreFamily(id);
                            byFamily.computeIfAbsent(family, key -> new ArrayList<>())
                                    .add(new OldMinerRules.Spot(cursor.getX(), y, cursor.getZ()));
                        }
                    }
                }
            }
        }
        return byFamily;
    }

    /**
     * Converts stone around the vein into the same ore. The count is the number of ores in the list he drew from.
     */
    static void grow(ServerLevel level, List<OldMinerRules.Spot> vein, String family, int extra) {
        if (level == null || vein == null || vein.isEmpty() || extra <= 0) {
            return;
        }
        Set<Long> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (OldMinerRules.Spot spot : vein) {
            if (spot == null) {
                continue;
            }
            BlockPos pos = new BlockPos(spot.x(), spot.y(), spot.z());
            if (seen.add(pos.asLong())) {
                queue.add(pos);
            }
        }
        int minY = level.getMinY();
        int maxY = level.getMaxY();
        int placed = 0;
        while (!queue.isEmpty() && placed < extra) {
            BlockPos pos = queue.removeFirst();
            for (Direction direction : Direction.values()) {
                if (placed >= extra) {
                    return;
                }
                BlockPos next = pos.relative(direction);
                if (next.getY() < minY || next.getY() >= maxY || !seen.add(next.asLong())) {
                    continue;
                }
                BlockState state = level.getBlockState(next);
                String place = OldMinerRules.oreBlockFor(family, DetectOre.blockId(state));
                if (place.isEmpty()) {
                    continue;
                }
                BlockState ore = oreState(place);
                if (ore == null) {
                    continue;
                }
                level.setBlock(next, ore, Block.UPDATE_ALL);
                placed++;
                queue.add(next.immutable());
            }
        }
    }

    private static BlockState oreState(String id) {
        Identifier parsed = Identifier.tryParse(id);
        if (parsed == null) {
            return null;
        }
        Block block = BuiltInRegistries.BLOCK.getValue(parsed);
        if (block == null || block == Blocks.AIR) {
            return null;
        }
        return block.defaultBlockState();
    }

    private static List<OldMinerRules.Lead> boardOf(ServerPlayer player) {
        List<OldMinerRules.Lead> stored = player.getAttached(EhmAttachments.EHM_OLD_MINER_LEADS);
        return stored == null ? new ArrayList<>() : new ArrayList<>(stored);
    }

    private static void save(ServerPlayer player, List<OldMinerRules.Lead> board, long today) {
        player.setAttached(EhmAttachments.EHM_OLD_MINER_LEADS, new ArrayList<>(board));
        sendHud(player, board, today);
    }

    private static void sendHud(ServerPlayer player, List<OldMinerRules.Lead> board, long today) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        List<String> lines = OldMinerRules.hudLines(
                board,
                level.dimension().identifier().toString(),
                player.getBlockX(),
                player.getBlockY(),
                player.getBlockZ(),
                today);
        String key = String.join("\n", lines);
        String previous = LAST_HUD.put(player.getUUID(), key);
        if (key.equals(previous)) {
            return;
        }
        ServerPlayNetworking.send(player, ClientboundMinerLeadPayload.from(lines));
    }

    private static int indexOf(List<OldMinerRules.Lead> board, String minerId) {
        for (int i = 0; i < board.size(); i++) {
            if (minerId.equals(board.get(i).minerId())) {
                return i;
            }
        }
        return -1;
    }

    private static void put(List<OldMinerRules.Lead> board, int index, OldMinerRules.Lead lead) {
        if (index < 0) {
            board.add(lead);
        } else {
            board.set(index, lead);
        }
    }

    private static void tellLead(ServerPlayer player, OldMinerRules.Lead lead, int px, int py, int pz) {
        String direction = OldMinerRules.direction(lead.x() - px, lead.y() - py, lead.z() - pz);
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.miner.lead",
                "%s: %s %s",
                Component.literal(lead.minerName()),
                Component.literal(OldMinerRules.oreTitle(lead.ore())),
                Component.literal(direction)));
    }

    private static void tellFound(ServerPlayer player, String family) {
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.miner.found", "You found the %s vein.", Component.literal(OldMinerRules.oreTitle(family))));
    }

    private static void tellWait(ServerPlayer player, long today, long nextDay) {
        long left = OldMinerRules.daysUntil(today, nextDay);
        if (left <= 1L) {
            tell(player, "tougher.miner.wait_one", "Come back in 1 Minecraft day.");
            return;
        }
        tell(
                player,
                "tougher.miner.wait",
                "Come back in %s Minecraft days.",
                Component.literal(Long.toString(left)));
    }

    private static void tell(ServerPlayer player, String key, String fallback, Component... args) {
        player.sendSystemMessage(Component.translatableWithFallback(key, fallback, (Object[]) args));
    }
}

package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.ExplorationData;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.LevelData;

/**
 * First biome visits and the first trip 300 blocks from world spawn.
 */
public final class Exploration implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("exploration");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void serverTick(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            tickPlayer(player, level);
        }
    }

    static void tickPlayer(ServerPlayer player, ServerLevel level) {
        if (Achievements.skipPlayer(player)) {
            return;
        }
        long packed = player.blockPosition().asLong();
        long last = player.getAttachedOrElse(EhmAttachments.EHM_EXPLORATION_LAST_POS, Long.MIN_VALUE);
        if (last == packed) {
            return;
        }
        player.setAttached(EhmAttachments.EHM_EXPLORATION_LAST_POS, packed);
        maybeAwardSpawn(player, level);
        maybeAwardBiome(player, level);
    }

    static void maybeAwardSpawn(ServerPlayer player, ServerLevel level) {
        if (Boolean.TRUE.equals(player.getAttachedOrElse(EhmAttachments.EHM_LEFT_SPAWN, Boolean.FALSE))) {
            return;
        }
        LevelData.RespawnData respawn = level.getServer().getRespawnData();
        if (!respawn.dimension().equals(level.dimension())) {
            return;
        }
        BlockPos spawn = respawn.pos();
        double dx = player.getX() - (spawn.getX() + 0.5);
        double dz = player.getZ() - (spawn.getZ() + 0.5);
        if (!ExplorationRules.shouldAwardSpawn(false, dx, dz)) {
            return;
        }
        player.setAttached(EhmAttachments.EHM_LEFT_SPAWN, Boolean.TRUE);
        player.giveExperiencePoints(ExplorationRules.SPAWN_XP);
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.explore_spawn",
                "You have traveled %s blocks from spawn. +%s experience.",
                Component.literal(Integer.toString(ExplorationRules.SPAWN_DISTANCE)),
                Component.literal(Integer.toString(ExplorationRules.SPAWN_XP))));
    }

    static void maybeAwardBiome(ServerPlayer player, ServerLevel level) {
        Holder<Biome> holder = level.getBiome(player.blockPosition());
        String id = biomeId(holder);
        if (id.isEmpty() || !markVisited(player, id)) {
            return;
        }
        boolean worldFirst = ExplorationData.of(level).tryVisit(id);
        Component name = biomeName(holder);
        player.giveExperiencePoints(ExplorationRules.BIOME_VISIT_XP);
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.explore_biome",
                "You discovered %s. +%s experience.",
                name,
                Component.literal(Integer.toString(ExplorationRules.BIOME_VISIT_XP))));
        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS,
                0.6F,
                1.2F);
        if (!worldFirst) {
            return;
        }
        player.giveExperiencePoints(ExplorationRules.WORLD_FIRST_BONUS_XP);
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.explore_biome_first",
                "You are the first to visit %s! +%s extra experience.",
                name,
                Component.literal(Integer.toString(ExplorationRules.WORLD_FIRST_BONUS_XP))));
        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                SoundSource.PLAYERS,
                0.7F,
                1.0F);
    }

    static boolean markVisited(ServerPlayer player, String biomeId) {
        return markBiome(player, EhmAttachments.EHM_VISITED_BIOMES, biomeId);
    }

    /**
     * First habitable home this player has completed in {@code pos}'s biome.
     * Survival players only. XP is house points × 3.
     */
    public static void maybeAwardFirstHome(ServerPlayer player, ServerLevel level, BlockPos pos, int houseScore) {
        if (player == null || Achievements.skipPlayer(player) || pos == null) {
            return;
        }
        int xp = ExplorationRules.homeBiomeXp(houseScore);
        if (xp <= 0) {
            return;
        }
        Holder<Biome> holder = level.getBiome(pos);
        String id = biomeId(holder);
        if (id.isEmpty() || !markBiome(player, EhmAttachments.EHM_HOME_BIOMES, id)) {
            return;
        }
        Component name = biomeName(holder);
        player.giveExperiencePoints(xp);
        player.sendSystemMessage(Component.translatableWithFallback(
                "tougher.message.explore_home_biome",
                "You built a home in %s. +%s experience.",
                name,
                Component.literal(Integer.toString(xp))));
        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS,
                0.6F,
                1.15F);
    }

    static boolean markBiome(ServerPlayer player, AttachmentType<List<String>> attachment, String biomeId) {
        List<String> already = new ArrayList<>(player.getAttachedOrElse(attachment, List.of()));
        if (!ExplorationRules.unseenBiome(already, biomeId)) {
            return false;
        }
        already.add(biomeId);
        player.setAttached(attachment, already);
        return true;
    }

    static String biomeId(Holder<Biome> holder) {
        return holder.unwrapKey().map(key -> key.identifier().toString()).orElse("");
    }

    static Component biomeName(Holder<Biome> holder) {
        return holder.unwrapKey()
                .map(Exploration::biomeName)
                .orElse(Component.literal(ExplorationRules.biomeFallbackName("")));
    }

    static Component biomeName(ResourceKey<Biome> key) {
        Identifier id = key.identifier();
        return Component.translatableWithFallback(
                ExplorationRules.biomeTranslationKey(id.getNamespace(), id.getPath()),
                ExplorationRules.biomeFallbackName(id.getPath()));
    }
}

package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.mixin.PhantomAccess;
import dev.extrahardmode.network.ClientboundCouncilBountyPayload;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.InhabitantData;
import dev.extrahardmode.world.WorldGate;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * Per-player council kill bounties. Each council member can hold one, up to five at a time.
 */
public final class CouncilMissions {
    /** Same offset Phantom.finalizeSpawn uses, so a lifted phantom circles the air and not the ground. */
    private static final int PHANTOM_ANCHOR_ABOVE = 5;
    private static final int PHANTOM_LIFT_MIN = 6;
    private static final int PHANTOM_LIFT_MAX = 24;
    /** Columns tried once a night roll succeeds. A miss spends the roll. */
    private static final int COLUMN_TRIES = 12;
    /** Ground search stays with the player: this far down, and {@link #SCAN_UP} above. */
    private static final int SCAN_DOWN = 24;
    private static final int SCAN_UP = 16;

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
        maybeSpawn(player, today);
    }

    /** Loaded bounty creatures from an earlier overworld day are removed without drops. */
    public static void retireNightSpawns(ServerLevel level, long today) {
        if (level == null) {
            return;
        }
        List<Mob> doomed = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof Mob mob) || !mob.isAlive()) {
                continue;
            }
            long spawned = mob.getAttachedOrElse(EhmAttachments.EHM_BOUNTY_SPAWN_DAY, -1L);
            if (spawned >= 0L && today > spawned) {
                doomed.add(mob);
            }
        }
        for (Mob mob : doomed) {
            mob.discard();
        }
    }

    /**
     * One try per open bounty each second of the overworld night. A hit places a group of that bounty,
     * from one eighth of the kills still owed through one quarter, and at least 1. A later miss ends
     * that group. The other open bounties still roll on their own.
     */
    private static void maybeSpawn(ServerPlayer player, long today) {
        if (!(player.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, Inhabitants.ID)) {
            return;
        }
        if (!player.isAlive() || Achievements.skipPlayer(player)) {
            return;
        }
        if (!CouncilMissionRules.night(level.getOverworldClockTime())) {
            return;
        }
        List<CouncilMissionRules.Mission> open = openMissions(player, today);
        if (open.isEmpty()) {
            return;
        }
        RandomSource random = level.getRandom();
        for (CouncilMissionRules.Mission mission : open) {
            if (!CouncilMissionRules.nightSpawn(random.nextInt(CouncilMissionRules.NIGHT_SPAWN_ONE_IN))) {
                continue;
            }
            int wanted = CouncilMissionRules.bountyGroupSize(mission.remaining(), random.nextInt());
            for (int n = 0; n < wanted; n++) {
                if (!spawnOne(player, level, mission, today, random)) {
                    break;
                }
            }
        }
    }

    private static List<CouncilMissionRules.Mission> openMissions(ServerPlayer player, long today) {
        List<CouncilMissionRules.Mission> open = new ArrayList<>();
        for (CouncilMissionRules.Mission mission : boardOf(player)) {
            if (mission.active() && !CouncilMissionRules.expired(mission, today)) {
                open.add(mission);
            }
        }
        return open;
    }

    private static boolean spawnOne(
            ServerPlayer player,
            ServerLevel level,
            CouncilMissionRules.Mission mission,
            long today,
            RandomSource random) {
        Identifier id = Identifier.tryParse(mission.mobId());
        if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            return false;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
        BlockPos feet = findSpawn(level, player, type, random);
        if (feet == null) {
            return false;
        }
        Entity spawned = type.spawn(level, feet, EntitySpawnReason.EVENT);
        if (!(spawned instanceof Mob mob) || !mob.isAlive() || mob.isInWall()) {
            if (spawned != null) {
                spawned.discard();
            }
            return false;
        }
        mob.setPersistenceRequired();
        mob.setAttached(EhmAttachments.EHM_SPAWN_PROCESSED, Boolean.TRUE);
        mob.setAttached(EhmAttachments.EHM_LOOTLESS, Boolean.FALSE);
        mob.setAttached(EhmAttachments.EHM_UNNATURAL_SPAWN, Boolean.FALSE);
        mob.setAttached(EhmAttachments.EHM_BOUNTY_SPAWN_DAY, today);
        AttributeInstance follow = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (follow != null) {
            follow.setBaseValue(CouncilMissionRules.BOUNTY_SPAWN_NEAR_BLOCKS);
        }
        mob.setTarget(player);
        if (mob instanceof Phantom phantom) {
            ((PhantomAccess) phantom).tougher$setAnchorPoint(mob.blockPosition().above(PHANTOM_ANCHOR_ABOVE));
        }
        if (ConfigManager.global().debug()) {
            ExtraHardModeMod.LOGGER.debug("EHM bounty spawn {} at {}", type, feet);
        }
        return true;
    }

    private static BlockPos findSpawn(ServerLevel level, ServerPlayer player, EntityType<?> type, RandomSource random) {
        double min = CouncilMissionRules.BOUNTY_SPAWN_MIN_BLOCKS;
        double max = CouncilMissionRules.BOUNTY_SPAWN_NEAR_BLOCKS;
        for (int attempt = 0; attempt < COLUMN_TRIES; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = Math.sqrt(random.nextDouble() * (max * max - min * min) + min * min);
            int x = player.getBlockX() + (int) Math.round(Math.cos(angle) * radius);
            int z = player.getBlockZ() + (int) Math.round(Math.sin(angle) * radius);
            BlockPos feet = findFeet(level, player, type, x, z, random);
            if (feet != null) {
                return feet;
            }
        }
        return null;
    }

    /** Closest legal feet to the player's height, down {@link #SCAN_DOWN} and up {@link #SCAN_UP}. */
    private static BlockPos findFeet(
            ServerLevel level, ServerPlayer player, EntityType<?> type, int x, int z, RandomSource random) {
        int origin = player.getBlockY();
        int minY = Math.max(level.getMinY() + 1, origin - SCAN_DOWN);
        int maxY = Math.min(level.getMaxY() - 2, origin + SCAN_UP);
        if (!level.isLoaded(new BlockPos(x, origin, z))) {
            return null;
        }
        for (int delta = 0; delta <= SCAN_DOWN; delta++) {
            int down = origin - delta;
            if (down >= minY && accept(level, player, type, new BlockPos(x, down, z), random)) {
                return new BlockPos(x, down, z);
            }
            if (delta == 0 || delta > SCAN_UP) {
                continue;
            }
            int up = origin + delta;
            if (up <= maxY && accept(level, player, type, new BlockPos(x, up, z), random)) {
                return new BlockPos(x, up, z);
            }
        }
        return null;
    }

    private static boolean accept(
            ServerLevel level, ServerPlayer player, EntityType<?> type, BlockPos feet, RandomSource random) {
        if (!withinRange(player, feet) || !level.getWorldBorder().isWithinBounds(feet)) {
            return false;
        }
        if (flying(type)) {
            return flyingSpot(level, feet, type, random);
        }
        return groundedSpot(level, feet, type, random);
    }

    private static boolean withinRange(ServerPlayer player, BlockPos feet) {
        double dx = feet.getX() + 0.5 - player.getX();
        double dy = feet.getY() - player.getY();
        double dz = feet.getZ() + 0.5 - player.getZ();
        double distance = dx * dx + dy * dy + dz * dz;
        double min = CouncilMissionRules.BOUNTY_SPAWN_MIN_BLOCKS;
        double max = CouncilMissionRules.BOUNTY_SPAWN_NEAR_BLOCKS;
        return distance >= min * min && distance <= max * max;
    }

    /**
     * Phantoms and ghasts hang in an open column 6 to 24 blocks above a solid floor. Air is not a
     * spawn block, so the darkness check is separate from the floor rule.
     */
    private static boolean flyingSpot(ServerLevel level, BlockPos feet, EntityType<?> type, RandomSource random) {
        int top = feet.getY() + bodyBlocks(type) - 1;
        if (top >= level.getMaxY()) {
            return false;
        }
        for (int lift = PHANTOM_LIFT_MIN; lift <= PHANTOM_LIFT_MAX; lift++) {
            BlockPos floor = feet.below(lift);
            if (!level.isLoaded(floor)) {
                return false;
            }
            if (!solidFloor(level, floor)) {
                continue;
            }
            boolean open = true;
            for (int y = floor.getY() + 1; y <= top; y++) {
                if (!bandClear(level, new BlockPos(feet.getX(), y, feet.getZ()), type)) {
                    open = false;
                    break;
                }
            }
            if (open) {
                return Monster.isDarkEnoughToSpawn(level, feet, random);
            }
        }
        return false;
    }

    /** Solid floor, or water when that creature can spawn in it. Lava never qualifies. */
    private static boolean groundedSpot(ServerLevel level, BlockPos feet, EntityType<?> type, RandomSource random) {
        if (!level.isLoaded(feet)) {
            return false;
        }
        BlockPos floor = feet.below();
        if (!level.isLoaded(floor)) {
            return false;
        }
        BlockState floorState = level.getBlockState(floor);
        if (floorState.getFluidState().is(FluidTags.LAVA)) {
            return false;
        }
        boolean fluid = !floorState.getFluidState().isEmpty();
        if (!fluid && !floorState.isFaceSturdy(level, floor, Direction.UP)) {
            return false;
        }
        if (!columnClear(level, feet, type)) {
            return false;
        }
        return Monster.checkMonsterSpawnRules(mobType(type), level, EntitySpawnReason.NATURAL, feet, random);
    }

    @SuppressWarnings("unchecked")
    private static EntityType<? extends Mob> mobType(EntityType<?> type) {
        return (EntityType<? extends Mob>) type;
    }

    private static boolean solidFloor(ServerLevel level, BlockPos floor) {
        BlockState state = level.getBlockState(floor);
        if (state.getFluidState().is(FluidTags.WATER) || state.getFluidState().is(FluidTags.LAVA)) {
            return false;
        }
        return state.isFaceSturdy(level, floor, Direction.UP);
    }

    private static boolean columnClear(ServerLevel level, BlockPos feet, EntityType<?> type) {
        int top = feet.getY() + bodyBlocks(type);
        for (int y = feet.getY(); y < top; y++) {
            if (!bandClear(level, new BlockPos(feet.getX(), y, feet.getZ()), type)) {
                return false;
            }
        }
        return true;
    }

    private static boolean bandClear(ServerLevel level, BlockPos center, EntityType<?> type) {
        int radius = bodyRadius(type);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (!emptySpawn(level, center.offset(dx, 0, dz), type)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean emptySpawn(ServerLevel level, BlockPos pos, EntityType<?> type) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        FluidState fluid = state.getFluidState();
        return NaturalSpawner.isValidEmptySpawnBlock(level, pos, state, fluid, type);
    }

    private static boolean flying(EntityType<?> type) {
        return type == EntityTypes.PHANTOM || type == EntityTypes.GHAST;
    }

    private static int bodyBlocks(EntityType<?> type) {
        if (type == EntityTypes.GHAST) {
            return 4;
        }
        if (type == EntityTypes.ENDERMAN || type == EntityTypes.RAVAGER || type == EntityTypes.WITHER_SKELETON) {
            return 3;
        }
        if (type == EntityTypes.SILVERFISH
                || type == EntityTypes.CAVE_SPIDER
                || type == EntityTypes.SPIDER
                || type == EntityTypes.PHANTOM) {
            return 1;
        }
        return 2;
    }

    private static int bodyRadius(EntityType<?> type) {
        if (type == EntityTypes.GHAST) {
            return 2;
        }
        if (type == EntityTypes.RAVAGER || type == EntityTypes.ELDER_GUARDIAN) {
            return 1;
        }
        return 0;
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
            int wait = CouncilMissionRules.daysUntilNextMission(
                    slot.assignedDay(), today, slot.completedCount());
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

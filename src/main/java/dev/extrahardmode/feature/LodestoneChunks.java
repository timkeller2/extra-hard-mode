package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.world.LodestoneChunkData;
import dev.extrahardmode.world.WorldGate;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Registry;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TicketStorage;

/**
 * A redstone block placed on a lodestone is used up. That chunk and the eight
 * around it keep ticking. A valid residence of 12 points adds one real hour, and
 * each point above 12 adds 5 minutes, at most 24 hours of time left. The time
 * stays on the chunks if the lodestone is broken. It does not run while the
 * server is off, paused, or empty of players.
 */
public final class LodestoneChunks implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("lodestone_chunks");

    /**
     * Loads and simulates, and keeps that dimension ticking while a player is online
     * somewhere. The time left lives in {@link LodestoneChunkData}, so the ticket
     * does not persist. With nobody online the tickets are taken off, which lets an
     * empty server pause, and the saved time stays where it is.
     */
    public static final TicketType TICKET = Registry.register(
            BuiltInRegistries.TICKET_TYPE,
            ResourceKey.create(Registries.TICKET_TYPE, ID),
            new TicketType(
                    0L,
                    TicketType.FLAG_LOADING
                            | TicketType.FLAG_SIMULATION
                            | TicketType.FLAG_KEEP_DIMENSION_ACTIVE));

    /** Wall-clock sample of the last second a player was online. Not saved. */
    private static final Map<ResourceKey<Level>, Long> LAST_RUNNING_AT = new HashMap<>();

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        DispenserBlock.registerBehavior(Items.REDSTONE_BLOCK, new FuelDispense());
        bus.listen(ServerTickEvents.END_LEVEL_TICK, ID, LodestoneChunks::onLevelTick);
    }

    @Override
    public void onWorldLoad(ServerLevel level) {
        LAST_RUNNING_AT.remove(level.dimension());
        refresh(level, playersOnline(level), 0L);
    }

    /**
     * Cancels a redstone block placed on a lodestone and spends it as fuel.
     * At the 24 hour cap the block places as usual.
     */
    public static InteractionResult tryPlace(BlockPlaceContext context, Block placed) {
        if (placed != Blocks.REDSTONE_BLOCK || context == null) {
            return InteractionResult.PASS;
        }
        if (!(context.getLevel() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, ID)) {
            return InteractionResult.PASS;
        }
        BlockPos pos = context.getClickedPos();
        if (pos == null || !level.getBlockState(pos.below()).is(Blocks.LODESTONE) || !context.canPlace()) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        if (player != null && player.isSpectator()) {
            return InteractionResult.PASS;
        }
        ServerPlayer serverPlayer = player instanceof ServerPlayer playing ? playing : null;
        FuelOutcome outcome = fuel(level, pos.below(), serverPlayer);
        if (outcome == FuelOutcome.REFUSED) {
            correctPredictedBlock(serverPlayer, level, pos);
            return InteractionResult.FAIL;
        }
        if (outcome != FuelOutcome.SPENT) {
            return InteractionResult.PASS;
        }
        if (player == null || !player.hasInfiniteMaterials()) {
            context.getItemInHand().shrink(1);
        }
        correctPredictedBlock(serverPlayer, level, pos);
        return InteractionResult.SUCCESS;
    }

    /** Text under the crosshair, or empty when this lodestone's chunks are quiet. */
    public static String lookLabel(ServerLevel level, BlockPos lodestonePos) {
        if (level == null || lodestonePos == null || !WorldGate.isModuleActive(level, ID)) {
            return "";
        }
        if (!level.getBlockState(lodestonePos).is(Blocks.LODESTONE)) {
            return "";
        }
        return LodestoneChunkRules.lookLabel(LodestoneChunkRules.soonestRemaining(remainings(level, lodestonePos)));
    }

    /**
     * Adds this residence's fuel to each of the nine chunks that is under the cap.
     * A lodestone outside a valid residence of 12 points refuses the block.
     */
    public static FuelOutcome fuel(ServerLevel level, BlockPos lodestonePos, ServerPlayer player) {
        ResidenceScan.Result residence = residenceAt(level, lodestonePos);
        boolean found = residence != null;
        boolean eligible = found && residence.gates().eligible();
        int score = found ? residence.gates().score() : 0;
        if (!LodestoneChunkRules.acceptsResidence(eligible, score)) {
            if (player != null) {
                tellResidence(player, found, eligible, score, found ? residence.gates().missing() : java.util.List.of());
            }
            return FuelOutcome.REFUSED;
        }
        long added = LodestoneChunkRules.fuelMillis(score);
        LodestoneChunkData data = LodestoneChunkData.of(level);
        ChunkPos origin = ChunkPos.unpack(ChunkPos.pack(lodestonePos));
        boolean online = playersOnline(level);
        boolean any = false;
        for (int dx = -LodestoneChunkRules.RADIUS; dx <= LodestoneChunkRules.RADIUS; dx++) {
            for (int dz = -LodestoneChunkRules.RADIUS; dz <= LodestoneChunkRules.RADIUS; dz++) {
                long packed = ChunkPos.pack(origin.x() + dx, origin.z() + dz);
                long existing = data.remaining(packed);
                if (!LodestoneChunkRules.extendsTime(existing)) {
                    continue;
                }
                data.setRemaining(packed, LodestoneChunkRules.extendedRemaining(existing, added));
                if (online) {
                    ensureTicket(level, packed);
                }
                any = true;
            }
        }
        if (!any) {
            if (player != null) {
                player.sendOverlayMessage(Component.translatableWithFallback(
                        LodestoneChunkRules.CAP_KEY, LodestoneChunkRules.CAP));
            }
            return FuelOutcome.FULL;
        }
        String label = LodestoneChunkRules.lookLabel(
                LodestoneChunkRules.soonestRemaining(remainings(level, lodestonePos)));
        if (player != null && !label.isEmpty()) {
            player.sendOverlayMessage(Component.literal(label));
        }
        level.playSound(null, lodestonePos, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.BLOCKS, 1.0F, 1.0F);
        return FuelOutcome.SPENT;
    }

    private static ResidenceScan.Result residenceAt(ServerLevel level, BlockPos lodestonePos) {
        BlockPos bed = ResidenceScan.bedOfHouseContaining(level, lodestonePos);
        if (bed == null) {
            return null;
        }
        WorldConfig cfg = ConfigManager.world(level);
        return ResidenceScan.inspect(
                level, bed, Inhabitants.occupiedBeds(level), cfg.inhabitantMinLight(), cfg.inhabitantSpacing());
    }

    private static void tellResidence(
            ServerPlayer player, boolean found, boolean eligible, int score, java.util.List<String> missing) {
        if (!found) {
            player.sendOverlayMessage(
                    Component.translatableWithFallback(LodestoneChunkRules.NO_HOME_KEY, LodestoneChunkRules.NO_HOME));
            return;
        }
        if (score < LodestoneChunkRules.MIN_RESIDENCE_SCORE) {
            player.sendOverlayMessage(Component.translatableWithFallback(
                    LodestoneChunkRules.LOW_SCORE_KEY, LodestoneChunkRules.LOW_SCORE, Integer.toString(score)));
            return;
        }
        String detail = LodestoneChunkRules.residenceRefusal(true, eligible, score, missing);
        if (missing == null || missing.isEmpty()) {
            player.sendOverlayMessage(Component.translatableWithFallback(
                    LodestoneChunkRules.INVALID_KEY, LodestoneChunkRules.INVALID, Integer.toString(score)));
            return;
        }
        player.sendOverlayMessage(Component.literal(detail));
    }

    private static void correctPredictedBlock(ServerPlayer serverPlayer, ServerLevel level, BlockPos pos) {
        if (serverPlayer != null && serverPlayer.connection != null) {
            serverPlayer.connection.send(new ClientboundBlockUpdatePacket(level, pos));
        }
    }

    enum FuelOutcome {
        SPENT,
        FULL,
        REFUSED
    }

    private static void onLevelTick(ServerLevel level) {
        if (level.getGameTime() % 20L != 0L) {
            return;
        }
        boolean running = WorldGate.isModuleActive(level, ID) && playersOnline(level);
        long elapsed = 0L;
        if (running) {
            long now = System.currentTimeMillis();
            Long previous = LAST_RUNNING_AT.put(level.dimension(), now);
            elapsed = LodestoneChunkRules.elapsedRunning(previous == null ? 0L : previous, now);
        } else {
            LAST_RUNNING_AT.remove(level.dimension());
        }
        refresh(level, running, elapsed);
    }

    /**
     * While players are online, counts down and keeps the tickets. While the server is
     * empty, off, or the module is off, takes the tickets away and leaves the time left.
     */
    private static void refresh(ServerLevel level, boolean keepActive, long elapsedMillis) {
        LodestoneChunkData data = LodestoneChunkData.of(level);
        if (data.size() == 0) {
            return;
        }
        TicketStorage tickets = tickets(level);
        for (long packed : data.packedChunks()) {
            long left = LodestoneChunkRules.afterRunning(data.remaining(packed), elapsedMillis);
            if (keepActive && left > 0L) {
                data.setRemaining(packed, left);
                ensureTicket(tickets, packed);
            } else if (left > 0L) {
                data.setRemaining(packed, left);
                removeTicket(tickets, packed);
            } else {
                data.remove(packed);
                removeTicket(tickets, packed);
            }
        }
    }

    private static boolean playersOnline(ServerLevel level) {
        return level.getServer().getPlayerCount() > 0;
    }

    private static long[] remainings(ServerLevel level, BlockPos lodestonePos) {
        LodestoneChunkData data = LodestoneChunkData.of(level);
        ChunkPos origin = ChunkPos.unpack(ChunkPos.pack(lodestonePos));
        long[] remainings = new long[LodestoneChunkRules.coveredChunks()];
        int index = 0;
        for (int dx = -LodestoneChunkRules.RADIUS; dx <= LodestoneChunkRules.RADIUS; dx++) {
            for (int dz = -LodestoneChunkRules.RADIUS; dz <= LodestoneChunkRules.RADIUS; dz++) {
                remainings[index++] = data.remaining(ChunkPos.pack(origin.x() + dx, origin.z() + dz));
            }
        }
        return remainings;
    }

    private static void ensureTicket(ServerLevel level, long packed) {
        ensureTicket(tickets(level), packed);
    }

    private static void ensureTicket(TicketStorage tickets, long packed) {
        if (hasTicket(tickets, packed)) {
            return;
        }
        tickets.addTicket(new Ticket(TICKET, ChunkMap.FORCED_TICKET_LEVEL), ChunkPos.unpack(packed));
    }

    private static void removeTicket(TicketStorage tickets, long packed) {
        if (!hasTicket(tickets, packed)) {
            return;
        }
        tickets.removeTicket(new Ticket(TICKET, ChunkMap.FORCED_TICKET_LEVEL), ChunkPos.unpack(packed));
    }

    private static boolean hasTicket(TicketStorage tickets, long packed) {
        for (Ticket ticket : tickets.getTickets(packed)) {
            if (ticket.getType() == TICKET) {
                return true;
            }
        }
        return false;
    }

    private static TicketStorage tickets(ServerLevel level) {
        return level.getChunkSource().getDataStorage().computeIfAbsent(TicketStorage.TYPE);
    }

    /** Aims at the space above a lodestone and spends the block. Anywhere else, the block drops as usual. */
    private static final class FuelDispense extends OptionalDispenseItemBehavior {
        @Override
        protected ItemStack execute(BlockSource source, ItemStack stack) {
            setSuccess(true);
            ServerLevel level = source.level();
            if (!WorldGate.isModuleActive(level, ID)) {
                return super.execute(source, stack);
            }
            Direction facing = source.state().getValue(DispenserBlock.FACING);
            BlockPos target = source.pos().relative(facing);
            BlockState occupied = level.getBlockState(target);
            if (!level.getBlockState(target.below()).is(Blocks.LODESTONE) || !occupied.canBeReplaced()) {
                return super.execute(source, stack);
            }
            if (fuel(level, target.below(), null) != FuelOutcome.SPENT) {
                setSuccess(false);
                return stack;
            }
            stack.shrink(1);
            return stack;
        }
    }
}

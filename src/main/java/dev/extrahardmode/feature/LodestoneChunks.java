package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.world.LodestoneChunkData;
import dev.extrahardmode.world.WorldGate;
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
 * around it keep ticking until a real-world expiry, at most 24 hours from now.
 * Another block adds one hour to the time still left. The time stays on the
 * chunks if the lodestone is broken.
 */
public final class LodestoneChunks implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("lodestone_chunks");

    /**
     * Loads and simulates, and keeps the dimension awake. The expiry lives in
     * {@link LodestoneChunkData}, so the ticket itself does not persist: a restart
     * puts it back only while the real hour is still left.
     */
    public static final TicketType TICKET = Registry.register(
            BuiltInRegistries.TICKET_TYPE,
            ResourceKey.create(Registries.TICKET_TYPE, ID),
            new TicketType(
                    0L,
                    TicketType.FLAG_LOADING
                            | TicketType.FLAG_SIMULATION
                            | TicketType.FLAG_KEEP_DIMENSION_ACTIVE));

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
        sync(level, true);
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
        if (!fuel(level, pos.below(), serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (player == null || !player.hasInfiniteMaterials()) {
            context.getItemInHand().shrink(1);
        }
        if (serverPlayer != null && serverPlayer.connection != null) {
            serverPlayer.connection.send(new ClientboundBlockUpdatePacket(level, pos));
        }
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
        long now = System.currentTimeMillis();
        long soonest = LodestoneChunkRules.soonestExpiry(now, expiries(level, lodestonePos));
        return LodestoneChunkRules.lookLabel(LodestoneChunkRules.remaining(now, soonest));
    }

    /**
     * Adds one real hour to each of the nine chunks that is under the cap.
     * Returns false when every one of them is already at 24 hours.
     */
    public static boolean fuel(ServerLevel level, BlockPos lodestonePos, ServerPlayer player) {
        long now = System.currentTimeMillis();
        LodestoneChunkData data = LodestoneChunkData.of(level);
        ChunkPos origin = ChunkPos.unpack(ChunkPos.pack(lodestonePos));
        boolean any = false;
        for (int dx = -LodestoneChunkRules.RADIUS; dx <= LodestoneChunkRules.RADIUS; dx++) {
            for (int dz = -LodestoneChunkRules.RADIUS; dz <= LodestoneChunkRules.RADIUS; dz++) {
                long packed = ChunkPos.pack(origin.x() + dx, origin.z() + dz);
                if (!LodestoneChunkRules.extendsTime(now, data.expiry(packed))) {
                    continue;
                }
                data.setExpiry(packed, LodestoneChunkRules.extendedExpiry(now, data.expiry(packed)));
                ensureTicket(level, packed);
                any = true;
            }
        }
        if (!any) {
            if (player != null) {
                player.sendOverlayMessage(Component.translatableWithFallback(
                        LodestoneChunkRules.CAP_KEY, LodestoneChunkRules.CAP));
            }
            return false;
        }
        long soonest = LodestoneChunkRules.soonestExpiry(now, expiries(level, lodestonePos));
        String label = LodestoneChunkRules.lookLabel(LodestoneChunkRules.remaining(now, soonest));
        if (player != null && !label.isEmpty()) {
            player.sendOverlayMessage(Component.literal(label));
        }
        level.playSound(null, lodestonePos, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    private static void onLevelTick(ServerLevel level) {
        if (level.getGameTime() % 20L != 0L) {
            return;
        }
        sync(level, WorldGate.isModuleActive(level, ID));
    }

    /** Puts tickets back for time still left, or takes them off when the hour has passed or the module is off. */
    private static void sync(ServerLevel level, boolean keepActive) {
        LodestoneChunkData data = LodestoneChunkData.of(level);
        if (data.size() == 0) {
            return;
        }
        long now = System.currentTimeMillis();
        TicketStorage tickets = tickets(level);
        for (long packed : data.packedChunks()) {
            if (keepActive && data.expiry(packed) > now) {
                ensureTicket(tickets, packed);
            } else {
                if (!keepActive && data.expiry(packed) > now) {
                    removeTicket(tickets, packed);
                    continue;
                }
                data.remove(packed);
                removeTicket(tickets, packed);
            }
        }
    }

    private static long[] expiries(ServerLevel level, BlockPos lodestonePos) {
        LodestoneChunkData data = LodestoneChunkData.of(level);
        ChunkPos origin = ChunkPos.unpack(ChunkPos.pack(lodestonePos));
        long[] expiries = new long[LodestoneChunkRules.coveredChunks()];
        int index = 0;
        for (int dx = -LodestoneChunkRules.RADIUS; dx <= LodestoneChunkRules.RADIUS; dx++) {
            for (int dz = -LodestoneChunkRules.RADIUS; dz <= LodestoneChunkRules.RADIUS; dz++) {
                expiries[index++] = data.expiry(ChunkPos.pack(origin.x() + dx, origin.z() + dz));
            }
        }
        return expiries;
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
            if (!fuel(level, target.below(), null)) {
                setSuccess(false);
                return stack;
            }
            stack.shrink(1);
            return stack;
        }
    }
}

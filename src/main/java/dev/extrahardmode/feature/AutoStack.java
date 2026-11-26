package dev.extrahardmode.feature;

import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.mixin.CompoundContainerAccess;
import dev.extrahardmode.network.ClientboundAutoStackChestPayload;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.AutoStackChestData;
import dev.extrahardmode.world.WorldGate;
import dev.extrahardmode.network.ClientboundAutoStackFlyPayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.LevelChunk;

/** Moves matching main-inventory stacks into nearby chests the player can open. */
public final class AutoStack {
    private AutoStack() {}

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register(AutoStack::onBreak);
    }

    public static void toggleExcluded(ServerPlayer player, int slot, boolean chest, boolean whole) {
        if (player == null || player.isSpectator() || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (!WorldGate.isActive(level)) {
            if (chest) {
                sendChest(player, false, 0L);
            }
            return;
        }
        if (!chest) {
            if (!AutoStackRules.mainSlot(slot)) {
                return;
            }
            int mask = player.getAttachedOrElse(EhmAttachments.EHM_AUTO_STACK_EXCLUDED, 0);
            int next = whole ? AutoStackRules.toggleMainRest(mask, slot) : AutoStackRules.toggle(mask, slot);
            player.setAttached(EhmAttachments.EHM_AUTO_STACK_EXCLUDED, next);
            return;
        }
        BlockPos pos = openChestPos(player);
        if (pos == null || blocked(player, level, pos) || blocked(player, level, otherHalf(level, pos))) {
            sendChest(player, false, 0L);
            return;
        }
        Container container = ((ChestMenu) player.containerMenu).getContainer();
        int size = container.getContainerSize();
        AutoStackChestData data = AutoStackChestData.of(level);
        long mask = data.mask(pos);
        if (!AutoStackRules.chestSlot(slot, size)) {
            sendChest(player, true, AutoStackRules.clampChest(mask, size));
            return;
        }
        long next = whole
                ? AutoStackRules.toggleChestRest(mask, size, slot)
                : AutoStackRules.toggleChest(mask, slot, size);
        next = AutoStackRules.clampChest(next, size);
        data.set(pos, next);
        broadcastChest(level, pos, next);
    }

    public static void queryChest(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (player.isSpectator() || !WorldGate.isActive(level)) {
            sendChest(player, false, 0L);
            return;
        }
        BlockPos pos = openChestPos(player);
        if (pos == null || blocked(player, level, pos) || blocked(player, level, otherHalf(level, pos))) {
            sendChest(player, false, 0L);
            return;
        }
        int size = ((ChestMenu) player.containerMenu).getContainer().getContainerSize();
        long mask = AutoStackRules.clampChest(AutoStackChestData.of(level).mask(pos), size);
        sendChest(player, true, mask);
    }

    public static void deposit(ServerPlayer player) {
        if (player == null || player.isSpectator() || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (!WorldGate.isActive(level)) {
            return;
        }
        int mask = player.getAttachedOrElse(EhmAttachments.EHM_AUTO_STACK_EXCLUDED, 0);
        Inventory inventory = player.getInventory();
        List<OpenChest> containers = openChests(player, level);
        if (containers.isEmpty()) {
            return;
        }
        List<ItemStack> kinds = new ArrayList<>();
        List<AutoStackRules.Pile> sources = new ArrayList<>(AutoStackRules.MAIN_SLOTS);
        for (int index = AutoStackRules.MAIN_START; index < AutoStackRules.MAIN_END; index++) {
            ItemStack stack = inventory.getItem(index);
            boolean excluded = AutoStackRules.excluded(mask, index);
            if (stack == null || stack.isEmpty() || excluded) {
                sources.add(AutoStackRules.Pile.player("", 0, 0, true));
                continue;
            }
            sources.add(AutoStackRules.Pile.player(
                    kind(kinds, stack), stack.getCount(), stack.getMaxStackSize(), false));
        }
        AutoStackChestData stored = AutoStackChestData.of(level);
        List<List<AutoStackRules.Pile>> chests = new ArrayList<>(containers.size());
        for (OpenChest open : containers) {
            Container container = open.container();
            long chestMask = AutoStackRules.clampChest(stored.mask(open.pos()), container.getContainerSize());
            List<AutoStackRules.Pile> slots = new ArrayList<>(container.getContainerSize());
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                boolean slotExcluded = AutoStackRules.chestExcluded(chestMask, slot);
                ItemStack stack = container.getItem(slot);
                if (stack == null || stack.isEmpty()) {
                    slots.add(AutoStackRules.Pile.chest("", 0, container.getMaxStackSize(), slotExcluded));
                    continue;
                }
                int max = Math.min(stack.getMaxStackSize(), container.getMaxStackSize(stack));
                slots.add(AutoStackRules.Pile.chest(kind(kinds, stack), stack.getCount(), max, slotExcluded));
            }
            chests.add(slots);
        }
        List<AutoStackRules.Move> moves = AutoStackRules.plan(sources, chests);
        if (moves.isEmpty()) {
            return;
        }
        Set<Integer> touched = new HashSet<>();
        Map<Long, FlightAcc> flights = new LinkedHashMap<>();
        int movedAny = 0;
        for (AutoStackRules.Move move : moves) {
            int inventoryIndex = AutoStackRules.MAIN_START + move.source();
            ItemStack sourceStack = inventory.getItem(inventoryIndex);
            if (sourceStack == null || sourceStack.isEmpty()) {
                continue;
            }
            ItemStack shown = sourceStack.copy();
            OpenChest open = containers.get(move.chest());
            int moved = transfer(inventory, inventoryIndex, open.container(), move.slot(), move.count());
            if (moved <= 0) {
                continue;
            }
            movedAny += moved;
            touched.add(move.chest());
            long key = ((long) move.source() << 32) | (move.chest() & 0xffffffffL);
            FlightAcc flight = flights.get(key);
            if (flight == null) {
                shown.setCount(moved);
                flights.put(key, new FlightAcc(shown, open.pos()));
            } else {
                flight.stack.grow(moved);
            }
        }
        if (movedAny <= 0) {
            return;
        }
        for (int index = AutoStackRules.MAIN_START; index < AutoStackRules.MAIN_END; index++) {
            ItemStack stack = inventory.getItem(index);
            if (stack != null && !stack.isEmpty() && stack.getCount() <= 0) {
                inventory.setItem(index, ItemStack.EMPTY);
            }
        }
        inventory.setChanged();
        for (int chest : touched) {
            containers.get(chest).container().setChanged();
        }
        player.containerMenu.broadcastChanges();
        sendFlights(level, player, flights);
    }

    private static void sendFlights(ServerLevel level, ServerPlayer actor, Map<Long, FlightAcc> flights) {
        if (flights.isEmpty()) {
            return;
        }
        List<ClientboundAutoStackFlyPayload.Flight> shown = new ArrayList<>(flights.size());
        for (FlightAcc flight : flights.values()) {
            if (flight.stack.isEmpty() || flight.chest == null) {
                continue;
            }
            shown.add(new ClientboundAutoStackFlyPayload.Flight(flight.stack.copy(), flight.chest));
        }
        if (shown.isEmpty()) {
            return;
        }
        ClientboundAutoStackFlyPayload payload = new ClientboundAutoStackFlyPayload(actor.getId(), shown);
        double reach = 64.0 * 64.0;
        for (ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(actor) <= reach) {
                ServerPlayNetworking.send(viewer, payload);
            }
        }
    }

    private static void onBreak(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (!(level instanceof ServerLevel server) || pos == null || state == null) {
            return;
        }
        if (!(state.getBlock() instanceof ChestBlock) || !state.hasProperty(ChestBlock.TYPE)) {
            return;
        }
        AutoStackChestData data = AutoStackChestData.of(server);
        data.forget(pos);
        if (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            data.forget(ChestBlock.getConnectedBlockPos(pos, state));
        }
    }

    private static void broadcastChest(ServerLevel level, BlockPos canon, long mask) {
        for (ServerPlayer viewer : level.players()) {
            if (canon.equals(openChestPos(viewer))) {
                sendChest(viewer, true, mask);
            }
        }
    }

    private static void sendChest(ServerPlayer player, boolean active, long mask) {
        ServerPlayNetworking.send(player, new ClientboundAutoStackChestPayload(active, mask));
    }

    /** Canonical position of the chest this player has open, or null for a barrel or other menu. */
    private static BlockPos openChestPos(ServerPlayer player) {
        if (player == null || !(player.containerMenu instanceof ChestMenu menu)) {
            return null;
        }
        return canonicalChest(menu.getContainer());
    }

    private static BlockPos canonicalChest(Container container) {
        if (container instanceof ChestBlockEntity chest) {
            return chest.getBlockPos().immutable();
        }
        if (!(container instanceof CompoundContainer compound)) {
            return null;
        }
        CompoundContainerAccess access = (CompoundContainerAccess) compound;
        BlockPos first = chestPos(access.tougher$container1());
        BlockPos second = chestPos(access.tougher$container2());
        if (first == null) {
            return second == null ? null : second.immutable();
        }
        if (second == null) {
            return first.immutable();
        }
        return second.compareTo(first) < 0 ? second.immutable() : first.immutable();
    }

    private static BlockPos chestPos(Container container) {
        if (container instanceof ChestBlockEntity chest) {
            return chest.getBlockPos();
        }
        return null;
    }

    private static int transfer(
            Inventory inventory, int inventoryIndex, Container container, int slot, int amount) {
        if (amount <= 0 || slot < 0 || slot >= container.getContainerSize()) {
            return 0;
        }
        ItemStack source = inventory.getItem(inventoryIndex);
        if (source == null || source.isEmpty()) {
            return 0;
        }
        ItemStack dest = container.getItem(slot);
        int moved = Math.min(amount, source.getCount());
        if (dest == null || dest.isEmpty()) {
            if (!container.canPlaceItem(slot, source)) {
                return 0;
            }
            int limit = Math.min(source.getMaxStackSize(), container.getMaxStackSize(source));
            moved = Math.min(moved, limit);
            if (moved <= 0) {
                return 0;
            }
            ItemStack placed = source.copy();
            placed.setCount(moved);
            source.shrink(moved);
            container.setItem(slot, placed);
            return moved;
        }
        if (!ItemStack.isSameItemSameComponents(dest, source)) {
            return 0;
        }
        int limit = Math.min(dest.getMaxStackSize(), container.getMaxStackSize(dest));
        moved = Math.min(moved, limit - dest.getCount());
        if (moved <= 0) {
            return 0;
        }
        dest.grow(moved);
        source.shrink(moved);
        return moved;
    }

    private static String kind(List<ItemStack> kinds, ItemStack stack) {
        for (int i = 0; i < kinds.size(); i++) {
            if (ItemStack.isSameItemSameComponents(kinds.get(i), stack)) {
                return Integer.toString(i);
            }
        }
        kinds.add(stack.copy());
        return Integer.toString(kinds.size() - 1);
    }

    private static List<OpenChest> openChests(ServerPlayer player, ServerLevel level) {
        BlockPos origin = player.blockPosition();
        List<BlockPos> positions = chestsNear(level, origin);
        List<OpenChest> containers = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        for (BlockPos pos : positions) {
            BlockPos canon = canonical(level, pos);
            if (!seen.add(canon)) {
                continue;
            }
            if (blocked(player, level, pos) || blocked(player, level, otherHalf(level, pos))) {
                continue;
            }
            Container container = openChest(level, canon);
            if (container != null) {
                containers.add(new OpenChest(canon, container));
            }
        }
        return containers;
    }

    private static List<BlockPos> chestsNear(ServerLevel level, BlockPos origin) {
        List<BlockPos> chests = new ArrayList<>();
        int range = AutoStackRules.RANGE;
        int minCx = origin.getX() - range >> 4;
        int maxCx = origin.getX() + range >> 4;
        int minCz = origin.getZ() - range >> 4;
        int maxCz = origin.getZ() + range >> 4;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(cx, cz);
                for (BlockEntity entity : chunk.getBlockEntities().values()) {
                    if (entity == null) {
                        continue;
                    }
                    BlockState state = entity.getBlockState();
                    if (!(state.getBlock() instanceof ChestBlock) || !state.hasProperty(ChestBlock.TYPE)) {
                        continue;
                    }
                    BlockPos pos = entity.getBlockPos();
                    if (!AutoStackRules.inRange(
                            pos.getX() - origin.getX(), pos.getY() - origin.getY(), pos.getZ() - origin.getZ())) {
                        continue;
                    }
                    chests.add(pos.immutable());
                }
            }
        }
        chests.sort(Comparator.naturalOrder());
        return chests;
    }

    private static boolean blocked(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (pos == null || EhmApi.playerBypasses(player)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (!state.is(BlockTags.COPPER_CHESTS)) {
            return false;
        }
        return !CopperChests.allows(level, pos, player.getStringUUID(), player.getPlainTextName());
    }

    private static BlockPos otherHalf(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock) || !state.hasProperty(ChestBlock.TYPE)) {
            return null;
        }
        if (state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return null;
        }
        return ChestBlock.getConnectedBlockPos(pos, state);
    }

    private static BlockPos canonical(ServerLevel level, BlockPos pos) {
        BlockPos other = otherHalf(level, pos);
        if (other == null) {
            return pos.immutable();
        }
        return other.compareTo(pos) < 0 ? other.immutable() : pos.immutable();
    }

    private static Container openChest(ServerLevel level, BlockPos pos) {
        level.getChunkAt(pos);
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock chest) || !state.hasProperty(ChestBlock.TYPE)) {
            return null;
        }
        if (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            level.getChunkAt(ChestBlock.getConnectedBlockPos(pos, state));
        }
        return ChestBlock.getContainer(chest, state, level, pos, true);
    }

    private record OpenChest(BlockPos pos, Container container) {}

    private static final class FlightAcc {
        private final ItemStack stack;
        private final BlockPos chest;

        private FlightAcc(ItemStack stack, BlockPos chest) {
            this.stack = stack;
            this.chest = chest;
        }
    }
}

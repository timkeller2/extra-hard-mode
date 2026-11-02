package dev.extrahardmode.feature;

import dev.extrahardmode.world.PlayerShopData;
import dev.extrahardmode.world.WorldGate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HangingSignBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

/**
 * Traders and wealthy traders offer other players' SELL and BUY chests after
 * their own trades. The chest funds every trade it can still pay.
 */
public final class PlayerShops {
    static final String MARKER = "tougher_player_shop";

    private PlayerShops() {}

    public static void onSignEdited(SignBlockEntity sign, ServerPlayer player) {
        if (sign == null || player == null || player.isSpectator() || !(sign.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!WorldGate.isModuleActive(level, Inhabitants.ID)) {
            return;
        }
        BlockPos signPos = sign.getBlockPos();
        BlockState state = level.getBlockState(signPos);
        String dimension = dimension(level);
        PlayerShopData data = PlayerShopData.of(level);
        String mode = PlayerShopRules.signMode(lines(sign));
        BlockPos attached = attachedChest(level, signPos, state);
        if (mode == null || attached == null) {
            data.forgetSign(dimension, signPos.getX(), signPos.getY(), signPos.getZ());
            return;
        }
        BlockPos chest = canonical(level, attached);
        String key = PlayerShopRules.key(new PlayerShopRules.Shop(
                dimension,
                chest.getX(),
                chest.getY(),
                chest.getZ(),
                signPos.getX(),
                signPos.getY(),
                signPos.getZ(),
                player.getStringUUID(),
                mode));
        data.remember(key);
    }

    public static void onBreak(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (!(level instanceof ServerLevel server) || !WorldGate.isModuleActive(server, Inhabitants.ID) || state == null || pos == null) {
            return;
        }
        String dimension = dimension(server);
        PlayerShopData data = PlayerShopData.of(server);
        if (state.getBlock() instanceof SignBlock || state.getBlock() instanceof HangingSignBlock) {
            data.forgetSign(dimension, pos.getX(), pos.getY(), pos.getZ());
        }
        if (state.getBlock() instanceof ChestBlock && state.hasProperty(ChestBlock.TYPE)) {
            data.forgetChest(dimension, pos.getX(), pos.getY(), pos.getZ());
            if (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                BlockPos other = ChestBlock.getConnectedBlockPos(pos, state);
                data.forgetChest(dimension, other.getX(), other.getY(), other.getZ());
            }
        }
    }

    /** Drops stale player offers and appends every other player's current deals. */
    public static void refresh(Villager villager, ServerPlayer customer) {
        if (villager == null
                || customer == null
                || !(villager.level() instanceof ServerLevel level)
                || !WorldGate.isModuleActive(level, Inhabitants.ID)) {
            return;
        }
        MerchantOffers offers = villager.getOffers();
        if (offers == null) {
            return;
        }
        offers.removeIf(PlayerShops::isShopOffer);
        PlayerShopData data = PlayerShopData.of(level);
        String customerId = customer.getStringUUID();
        for (String raw : data.snapshot()) {
            Prepared prepared = prepare(level, raw, customerId);
            if (prepared == null) {
                continue;
            }
            for (MerchantOffer offer : prepared.offers) {
                offers.add(offer);
            }
        }
    }

    /**
     * Moves the chest for one player-shop trade. Shop offers never become the
     * resident's own sales, so the caller skips trade experience either way.
     */
    public static boolean settle(Villager villager, MerchantOffer traded) {
        if (!isShopOffer(traded)) {
            return false;
        }
        if (villager == null || !(villager.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, Inhabitants.ID)) {
            return true;
        }
        PlayerShopRules.Marker marker = PlayerShopRules.parseMarker(markerText(traded.getResult()));
        if (marker == null) {
            traded.setToOutOfStock();
            Inhabitants.syncOffers(villager);
            return true;
        }
        ServerLevel shopLevel = findLevel(level.getServer(), marker.dimension());
        BlockPos chestPos = new BlockPos(marker.x(), marker.y(), marker.z());
        Container container = shopLevel == null ? null : openChest(shopLevel, chestPos);
        boolean sell = "sell".equals(marker.mode());
        int price = sell ? traded.getItemCostA().count() : traded.getResult().getCount();
        int quantity = sell ? traded.getResult().getCount() : traded.getItemCostA().count();
        ItemStack goods = sell ? cleanResult(traded) : traded.getBaseCostA();
        if (container == null || goods == null || goods.isEmpty() || price <= 0 || quantity <= 0) {
            traded.setToOutOfStock();
            Inhabitants.syncOffers(villager);
            return true;
        }
        if (sell && !traded.getBaseCostA().is(Items.EMERALD)) {
            traded.setToOutOfStock();
            Inhabitants.syncOffers(villager);
            return true;
        }
        ChestRead read = read(container);
        PlayerShopRules.Listing listing = match(read, goods, sell);
        int itemMax = goods.getMaxStackSize();
        boolean funded = listing != null
                && (sell
                        ? PlayerShopRules.canSell(read.slots, listing, quantity, price)
                        : PlayerShopRules.canBuy(read.slots, listing, quantity, price, itemMax));
        List<PlayerShopRules.Slot> next = null;
        if (funded) {
            next = sell
                    ? PlayerShopRules.afterSell(read.slots, listing, quantity, price)
                    : PlayerShopRules.afterBuy(read.slots, listing, quantity, price, itemMax);
        }
        if (next == null) {
            traded.setToOutOfStock();
            Inhabitants.syncOffers(villager);
            return true;
        }
        apply(container, read, next);
        return true;
    }

    public static boolean isShopOffer(MerchantOffer offer) {
        return offer != null && markerText(offer.getResult()) != null;
    }

    public static void stripMarker(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return;
        }
        CompoundTag tag = data.copyTag();
        if (!tag.contains(MARKER)) {
            return;
        }
        tag.remove(MARKER);
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
        }
    }

    private static Prepared prepare(ServerLevel anyLevel, String raw, String customerId) {
        PlayerShopRules.Shop shop = PlayerShopRules.parse(raw);
        if (shop == null) {
            PlayerShopData.of(anyLevel).forget(raw);
            return null;
        }
        if (customerId != null && customerId.equals(shop.owner())) {
            return null;
        }
        ServerLevel level = findLevel(anyLevel.getServer(), shop.dimension());
        if (level == null) {
            return null;
        }
        BlockPos signPos = new BlockPos(shop.signX(), shop.signY(), shop.signZ());
        level.getChunkAt(signPos);
        BlockState signState = level.getBlockState(signPos);
        BlockEntity entity = level.getBlockEntity(signPos);
        if (!(entity instanceof SignBlockEntity sign) || !shop.mode().equals(PlayerShopRules.signMode(lines(sign)))) {
            PlayerShopData.of(level).forget(raw);
            return null;
        }
        BlockPos attached = attachedChest(level, signPos, signState);
        if (attached == null) {
            PlayerShopData.of(level).forget(raw);
            return null;
        }
        BlockPos chest = canonical(level, attached);
        if (chest.getX() != shop.chestX() || chest.getY() != shop.chestY() || chest.getZ() != shop.chestZ()) {
            String moved = PlayerShopRules.key(new PlayerShopRules.Shop(
                    shop.dimension(),
                    chest.getX(),
                    chest.getY(),
                    chest.getZ(),
                    shop.signX(),
                    shop.signY(),
                    shop.signZ(),
                    shop.owner(),
                    shop.mode()));
            PlayerShopData.of(level).remember(moved);
        }
        Container container = openChest(level, chest);
        if (container == null) {
            PlayerShopData.of(level).forget(raw);
            return null;
        }
        boolean sell = "sell".equals(shop.mode());
        ChestRead read = read(container);
        String payload = PlayerShopRules.markerKey(shop.mode(), shop.dimension(), chest.getX(), chest.getY(), chest.getZ());
        if (payload == null) {
            return null;
        }
        List<MerchantOffer> offers = new ArrayList<>();
        for (PlayerShopRules.Listing listing : PlayerShopRules.listings(read.slots, sell)) {
            ItemStack prototype = read.prototype(listing.itemId());
            if (prototype == null || prototype.isEmpty()) {
                continue;
            }
            int uses = sell
                    ? PlayerShopRules.sellUses(read.slots, listing)
                    : PlayerShopRules.buyUses(read.slots, listing, prototype.getMaxStackSize());
            if (uses < 1 || listing.quantity() < 1 || listing.quantity() > prototype.getMaxStackSize()) {
                continue;
            }
            MerchantOffer offer = sell ? sellOffer(prototype, listing, uses, payload) : buyOffer(prototype, listing, uses, payload);
            if (offer != null) {
                offers.add(offer);
            }
        }
        return new Prepared(offers);
    }

    private static MerchantOffer sellOffer(ItemStack prototype, PlayerShopRules.Listing listing, int uses, String payload) {
        ItemStack result = prototype.copy();
        result.setCount(listing.quantity());
        mark(result, payload);
        return new MerchantOffer(
                new ItemCost(Items.EMERALD, listing.price()),
                Optional.empty(),
                result,
                0,
                uses,
                0,
                0.0F);
    }

    private static MerchantOffer buyOffer(ItemStack prototype, PlayerShopRules.Listing listing, int uses, String payload) {
        ItemStack paid = new ItemStack(Items.EMERALD, listing.price());
        mark(paid, payload);
        return new MerchantOffer(costFor(prototype, listing.quantity()), Optional.empty(), paid, 0, uses, 0, 0.0F);
    }

    private static ItemCost costFor(ItemStack prototype, int count) {
        int amount = Math.max(1, count);
        ItemStack plain = new ItemStack(prototype.getItem());
        if (ItemStack.isSameItemSameComponents(prototype, plain)) {
            return new ItemCost(prototype.getItem(), amount);
        }
        return new ItemCost(
                prototype.typeHolder(), amount, DataComponentExactPredicate.allOf(prototype.getComponents()));
    }

    private static PlayerShopRules.Listing match(ChestRead read, ItemStack goods, boolean sell) {
        ItemStack clean = goods.copy();
        stripMarker(clean);
        for (PlayerShopRules.Listing listing : PlayerShopRules.listings(read.slots, sell)) {
            ItemStack prototype = read.prototype(listing.itemId());
            if (prototype != null && ItemStack.isSameItemSameComponents(prototype, clean)) {
                return listing;
            }
        }
        return null;
    }

    private static void apply(Container container, ChestRead read, List<PlayerShopRules.Slot> next) {
        int size = Math.min(container.getContainerSize(), next.size());
        for (int i = 0; i < size; i++) {
            PlayerShopRules.Slot slot = next.get(i);
            ItemStack current = container.getItem(i);
            if (PlayerShopRules.empty(slot)) {
                if (!current.isEmpty()) {
                    container.setItem(i, ItemStack.EMPTY);
                }
                continue;
            }
            if (!current.isEmpty() && slot.itemId().equals(read.keys.get(i))) {
                if (current.getCount() != slot.count()) {
                    ItemStack copy = current.copy();
                    copy.setCount(slot.count());
                    container.setItem(i, copy);
                }
                continue;
            }
            if (!current.isEmpty()) {
                container.setItem(i, ItemStack.EMPTY);
            }
            ItemStack created = created(slot, read);
            if (created != null) {
                container.setItem(i, created);
            }
        }
        container.setChanged();
    }

    private static ItemStack created(PlayerShopRules.Slot slot, ChestRead read) {
        if (PlayerShopRules.EMERALD.equals(slot.itemId())) {
            return new ItemStack(Items.EMERALD, slot.count());
        }
        ItemStack prototype = read.prototype(slot.itemId());
        if (prototype == null) {
            return null;
        }
        ItemStack placed = prototype.copy();
        placed.setCount(slot.count());
        return placed;
    }

    private static ChestRead read(Container container) {
        ChestRead read = new ChestRead();
        for (int i = 0; i < container.getContainerSize(); i++) {
            read.add(container.getItem(i));
        }
        return read;
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

    private static BlockPos canonical(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock) || !state.hasProperty(ChestBlock.TYPE)) {
            return pos.immutable();
        }
        if (state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return pos.immutable();
        }
        BlockPos other = ChestBlock.getConnectedBlockPos(pos, state);
        return other.compareTo(pos) < 0 ? other.immutable() : pos.immutable();
    }

    private static BlockPos attachedChest(ServerLevel level, BlockPos signPos, BlockState state) {
        if (state == null) {
            return null;
        }
        BlockPos support;
        if (state.getBlock() instanceof WallSignBlock && state.hasProperty(WallSignBlock.FACING)) {
            support = signPos.relative(state.getValue(WallSignBlock.FACING).getOpposite());
        } else if (state.getBlock() instanceof WallHangingSignBlock && state.hasProperty(WallHangingSignBlock.FACING)) {
            support = signPos.relative(state.getValue(WallHangingSignBlock.FACING).getOpposite());
        } else if (state.getBlock() instanceof StandingSignBlock) {
            support = signPos.below();
        } else if (state.getBlock() instanceof CeilingHangingSignBlock) {
            support = signPos.above();
        } else {
            return null;
        }
        if (!(level.getBlockState(support).getBlock() instanceof ChestBlock)) {
            return null;
        }
        return support;
    }

    private static List<String> lines(SignBlockEntity sign) {
        List<String> lines = new ArrayList<>();
        for (SignTextSlot side : new SignTextSlot[] {SignTextSlot.FRONT, SignTextSlot.BACK}) {
            SignText text = sign.getText(side);
            if (text == null) {
                continue;
            }
            for (Component line : text.getMessages(false)) {
                lines.add(line == null ? "" : line.getString());
            }
        }
        return lines;
    }

    private static void mark(ItemStack stack, String payload) {
        CompoundTag tag = new CompoundTag();
        CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
        if (existing != null) {
            tag = existing.copyTag();
        }
        tag.putString(MARKER, payload);
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }

    private static String markerText(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        String raw = data.copyTag().getStringOr(MARKER, "");
        return raw.isEmpty() ? null : raw;
    }

    private static ItemStack cleanResult(MerchantOffer offer) {
        ItemStack copy = offer.getResult().copy();
        stripMarker(copy);
        return copy;
    }

    private static ServerLevel findLevel(MinecraftServer server, String dimension) {
        Identifier id = Identifier.tryParse(dimension);
        if (id == null || server == null) {
            return null;
        }
        for (ServerLevel candidate : server.getAllLevels()) {
            if (id.equals(candidate.dimension().identifier())) {
                return candidate;
            }
        }
        return null;
    }

    private static String dimension(ServerLevel level) {
        return level.dimension().identifier().toString();
    }

    private static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return stack.typeHolder().unwrapKey().map(key -> key.identifier().toString()).orElse("");
    }

    private static final class ChestRead {
        final List<PlayerShopRules.Slot> slots = new ArrayList<>();
        final List<String> keys = new ArrayList<>();
        final Map<String, ItemStack> prototypes = new LinkedHashMap<>();
        private final List<ItemStack> componentPrototypes = new ArrayList<>();

        void add(ItemStack stack) {
            int index = slots.size();
            if (stack == null || stack.isEmpty()) {
                slots.add(new PlayerShopRules.Slot(index, "", 0));
                keys.add("");
                return;
            }
            String key = keyOf(stack);
            slots.add(new PlayerShopRules.Slot(index, key, stack.getCount()));
            keys.add(key);
            if (!PlayerShopRules.EMERALD.equals(key)) {
                prototypes.putIfAbsent(key, stack.copyWithCount(1));
            }
        }

        String keyOf(ItemStack stack) {
            if (stack.is(Items.EMERALD)) {
                return PlayerShopRules.EMERALD;
            }
            String id = itemId(stack);
            if (ItemStack.isSameItemSameComponents(stack, new ItemStack(stack.getItem()))) {
                return id;
            }
            for (int i = 0; i < componentPrototypes.size(); i++) {
                if (ItemStack.isSameItemSameComponents(componentPrototypes.get(i), stack)) {
                    return id + "#" + i;
                }
            }
            componentPrototypes.add(stack.copyWithCount(1));
            return id + "#" + (componentPrototypes.size() - 1);
        }

        ItemStack prototype(String key) {
            return prototypes.get(key);
        }
    }

    private record Prepared(List<MerchantOffer> offers) {}
}

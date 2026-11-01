package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.world.LapisPortalData;
import dev.extrahardmode.world.WorldGate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Lapis booths that pair by the largest lapis lazuli stack in the chest.
 * Closing the open door while standing in it spends redstone dust, nether
 * quartz, or both, from that chest.
 */
public final class LapisPortals implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("lapis_portals");

    private static final int SCAN = 3;

    private static final String NO_LAPIS_KEY = "tougher.message.portal_no_lapis";
    private static final String NO_LAPIS = "The chest needs a stack of lapis lazuli.";
    private static final String NONE_KEY = "tougher.message.portal_none";
    private static final String NONE = "No other portal has a stack of %s lapis lazuli.";
    private static final String MANY_KEY = "tougher.message.portal_many";
    private static final String MANY = "More than one other portal has a stack of %s lapis lazuli.";
    private static final String COST_KEY = "tougher.message.portal_cost";
    private static final String COST = "The chest needs %s redstone dust or nether quartz.";
    private static final String PAID_KEY = "tougher.message.portal_paid";
    private static final String PAID = "The portal takes %s.";

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        bus.listen(UseBlockCallback.EVENT, ID, LapisPortals::onUseBlock);
        bus.listen(PlayerBlockBreakEvents.AFTER, ID, LapisPortals::onBreak);
    }

    static InteractionResult onUseBlock(Player player, Level level, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !WorldGate.isModuleActive(server, ID)) {
            return InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.isSpectator()) {
            return InteractionResult.PASS;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = server.getBlockState(pos);
        if (!(state.getBlock() instanceof DoorBlock door)) {
            return InteractionResult.PASS;
        }
        BlockPos lower = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        if (!frameValid(server, lower)) {
            return InteractionResult.PASS;
        }
        remember(server, lower);
        boolean standing = serverPlayer.blockPosition().equals(lower);
        boolean wasOpen = door.isOpen(state);
        Entity heard = door.type().canOpenByHand() ? serverPlayer : null;
        door.setOpen(heard, server, state, pos, !wasOpen);
        if (wasOpen && standing) {
            tryTravel(serverPlayer, server, lower);
        }
        return InteractionResult.SUCCESS;
    }

    public static void onPlaced(BlockPlaceContext context, Block block) {
        if (!(context.getLevel() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, ID)) {
            return;
        }
        if (!(block instanceof DoorBlock) && block != Blocks.LAPIS_BLOCK && !(block instanceof ChestBlock)) {
            return;
        }
        scan(level, context.getClickedPos());
    }

    static void onBreak(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (!(level instanceof ServerLevel server) || !WorldGate.isModuleActive(server, ID)) {
            return;
        }
        Block block = state.getBlock();
        if (!(block instanceof DoorBlock) && block != Blocks.LAPIS_BLOCK && !(block instanceof ChestBlock)) {
            return;
        }
        LapisPortalData data = LapisPortalData.of(server);
        String dimension = dimension(server);
        if (block instanceof DoorBlock) {
            BlockPos lower = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
            data.forgetPlace(dimension, lower.getX(), lower.getY(), lower.getZ());
        }
        for (String raw : data.snapshot()) {
            LapisPortalRules.Place place = LapisPortalRules.parse(raw);
            if (place == null) {
                data.forget(raw);
                continue;
            }
            if (!dimension.equals(place.dimension()) || chebyshev(place, pos) > SCAN) {
                continue;
            }
            if (!frameValid(server, new BlockPos(place.x(), place.y(), place.z()))) {
                data.forget(raw);
            }
        }
    }

    private static void scan(ServerLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -SCAN; dy <= SCAN; dy++) {
            for (int dx = -SCAN; dx <= SCAN; dx++) {
                for (int dz = -SCAN; dz <= SCAN; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    remember(level, cursor);
                }
            }
        }
    }

    private static void remember(ServerLevel level, BlockPos lower) {
        BlockState state = level.getBlockState(lower);
        if (!(state.getBlock() instanceof DoorBlock) || state.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER) {
            return;
        }
        if (!frameValid(level, lower)) {
            return;
        }
        LapisPortalRules.Facing facing = facing(state);
        if (facing == null) {
            return;
        }
        String key = LapisPortalRules.key(new LapisPortalRules.Place(
                dimension(level), lower.getX(), lower.getY(), lower.getZ(), facing));
        LapisPortalData.of(level).remember(key);
    }

    private static void tryTravel(ServerPlayer player, ServerLevel level, BlockPos lower) {
        LapisPortalRules.Facing facing = facing(level.getBlockState(lower));
        Container container = facing == null ? null : chest(level, lower, facing);
        if (container == null) {
            tell(player, NO_LAPIS_KEY, NO_LAPIS);
            return;
        }
        List<LapisPortalRules.Stack> slots = read(container);
        int channel = LapisPortalRules.channel(slots);
        if (channel <= 0) {
            tell(player, NO_LAPIS_KEY, NO_LAPIS);
            return;
        }
        List<Candidate> found = new ArrayList<>();
        found.add(new Candidate(level, lower, channel));
        LapisPortalData data = LapisPortalData.of(level);
        String here = dimension(level);
        for (String raw : data.snapshot()) {
            LapisPortalRules.Place place = LapisPortalRules.parse(raw);
            if (place == null) {
                data.forget(raw);
                continue;
            }
            if (here.equals(place.dimension()) && place.x() == lower.getX() && place.y() == lower.getY() && place.z() == lower.getZ()) {
                continue;
            }
            ServerLevel destLevel = findLevel(level.getServer(), place.dimension());
            if (destLevel == null) {
                continue;
            }
            BlockPos destPos = new BlockPos(place.x(), place.y(), place.z());
            destLevel.getChunk(destPos.getX() >> 4, destPos.getZ() >> 4);
            if (!frameValid(destLevel, destPos)) {
                data.forget(raw);
                continue;
            }
            remember(destLevel, destPos);
            LapisPortalRules.Facing destFacing = facing(destLevel.getBlockState(destPos));
            Container destChest = destFacing == null ? null : chest(destLevel, destPos, destFacing);
            int destChannel = destChest == null ? 0 : LapisPortalRules.channel(read(destChest));
            if (destChannel != channel) {
                continue;
            }
            found.add(new Candidate(destLevel, destPos, destChannel));
        }
        int[] channels = new int[found.size()];
        for (int i = 0; i < found.size(); i++) {
            channels[i] = found.get(i).channel;
        }
        int partner = LapisPortalRules.partner(channels, 0);
        Component count = Component.literal(Integer.toString(channel));
        if (partner == LapisPortalRules.PARTNER_NONE) {
            tell(player, NONE_KEY, NONE, count);
            return;
        }
        if (partner == LapisPortalRules.PARTNER_AMBIGUOUS) {
            tell(player, MANY_KEY, MANY, count);
            return;
        }
        Candidate dest = found.get(partner);
        boolean same = dimension(level).equals(dimension(dest.level));
        double travelled = LapisPortalRules.travelDistance(
                same, LapisPortalRules.distance(lower.getX(), lower.getY(), lower.getZ(), dest.pos.getX(), dest.pos.getY(), dest.pos.getZ()));
        int cost = LapisPortalRules.travelCost(travelled, LapisPortalRules.bothFuels(slots));
        if (cost <= 0) {
            return;
        }
        List<LapisPortalRules.Stack> paid = LapisPortalRules.pay(slots, cost);
        if (paid == null) {
            tell(player, COST_KEY, COST, Component.literal(Integer.toString(cost)));
            return;
        }
        ItemStack[] original = snapshot(container);
        apply(container, paid);
        forceOpen(dest.level, dest.pos);
        Direction outward = dest.level.getBlockState(dest.pos).getValue(DoorBlock.FACING);
        boolean moved = player.teleportTo(
                dest.level,
                dest.pos.getX() + 0.5,
                dest.pos.getY(),
                dest.pos.getZ() + 0.5,
                Set.of(),
                outward.toYRot(),
                player.getXRot(),
                true);
        if (!moved) {
            restore(container, original);
            return;
        }
        level.playSound(null, lower, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 1.0F);
        dest.level.playSound(null, dest.pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 1.0F);
        int spentRedstone = LapisPortalRules.total(slots, LapisPortalRules.REDSTONE)
                - LapisPortalRules.total(paid, LapisPortalRules.REDSTONE);
        int spentQuartz = LapisPortalRules.total(slots, LapisPortalRules.QUARTZ)
                - LapisPortalRules.total(paid, LapisPortalRules.QUARTZ);
        tell(player, PAID_KEY, PAID, Component.literal(LapisPortalRules.spentPhrase(spentRedstone, spentQuartz)));
    }

    private static boolean frameValid(ServerLevel level, BlockPos lower) {
        BlockState state = level.getBlockState(lower);
        if (!(state.getBlock() instanceof DoorBlock) || state.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER) {
            return false;
        }
        BlockState upper = level.getBlockState(lower.above());
        if (upper.getBlock() != state.getBlock() || upper.getValue(DoorBlock.HALF) != DoubleBlockHalf.UPPER) {
            return false;
        }
        LapisPortalRules.Facing facing = facing(state);
        if (facing == null) {
            return false;
        }
        for (LapisPortalRules.Offset offset : LapisPortalRules.lapisOffsets(facing)) {
            if (!level.getBlockState(lower.offset(offset.x(), offset.y(), offset.z())).is(Blocks.LAPIS_BLOCK)) {
                return false;
            }
        }
        LapisPortalRules.Offset chest = LapisPortalRules.chestOffset(facing);
        return level.getBlockState(lower.offset(chest.x(), chest.y(), chest.z())).getBlock() instanceof ChestBlock;
    }

    private static Container chest(ServerLevel level, BlockPos lower, LapisPortalRules.Facing facing) {
        LapisPortalRules.Offset offset = LapisPortalRules.chestOffset(facing);
        BlockPos pos = lower.offset(offset.x(), offset.y(), offset.z());
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock chest)) {
            return null;
        }
        return ChestBlock.getContainer(chest, state, level, pos, true);
    }

    private static List<LapisPortalRules.Stack> read(Container container) {
        List<LapisPortalRules.Stack> slots = new ArrayList<>();
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            slots.add(new LapisPortalRules.Stack(i, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount()));
        }
        return slots;
    }

    private static void apply(Container container, List<LapisPortalRules.Stack> slots) {
        for (LapisPortalRules.Stack slot : slots) {
            if (slot.count() <= 0) {
                container.setItem(slot.index(), ItemStack.EMPTY);
                continue;
            }
            ItemStack stack = container.getItem(slot.index());
            if (!stack.isEmpty() && stack.getCount() != slot.count()) {
                stack.setCount(slot.count());
                container.setItem(slot.index(), stack);
            }
        }
        container.setChanged();
    }

    private static ItemStack[] snapshot(Container container) {
        ItemStack[] copy = new ItemStack[container.getContainerSize()];
        for (int i = 0; i < copy.length; i++) {
            copy[i] = container.getItem(i).copy();
        }
        return copy;
    }

    private static void restore(Container container, ItemStack[] original) {
        for (int i = 0; i < original.length; i++) {
            container.setItem(i, original[i]);
        }
        container.setChanged();
    }

    private static void forceOpen(ServerLevel level, BlockPos lower) {
        BlockState state = level.getBlockState(lower);
        if (state.getBlock() instanceof DoorBlock door && !door.isOpen(state)) {
            door.setOpen(null, level, state, lower, true);
        }
    }

    private static LapisPortalRules.Facing facing(BlockState state) {
        if (!(state.getBlock() instanceof DoorBlock) || !state.hasProperty(DoorBlock.FACING)) {
            return null;
        }
        return LapisPortalRules.Facing.byName(state.getValue(DoorBlock.FACING).getSerializedName());
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

    private static int chebyshev(LapisPortalRules.Place place, BlockPos pos) {
        int dx = Math.abs(place.x() - pos.getX());
        int dy = Math.abs(place.y() - pos.getY());
        int dz = Math.abs(place.z() - pos.getZ());
        return Math.max(dx, Math.max(dy, dz));
    }

    private static void tell(ServerPlayer player, String key, String fallback, Object... args) {
        player.sendSystemMessage(Component.translatableWithFallback(key, fallback, args));
    }

    private record Candidate(ServerLevel level, BlockPos pos, int channel) {}
}

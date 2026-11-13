package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.network.ClientboundCopperChestLookPayload;
import dev.extrahardmode.network.EhmNetworking;
import dev.extrahardmode.mixin.CompoundContainerAccess;
import dev.extrahardmode.world.CopperChestData;
import dev.extrahardmode.world.WorldGate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.CompoundContainer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * A copper chest belongs to the player who placed it. For 90 Minecraft days only that
 * player, or a player named on a sign attached to it, can open it, break it, or change
 * that sign. Opening it starts the 90 days again. Waxed and weathered copper chests
 * keep the claim. A hopper owned by that player can pull from it. Other hoppers, pistons,
 * explosions, and copper golems leave a locked chest alone.
 */
public final class CopperChests implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("copper_chests");

    static final String LOCKED_KEY = "tougher.message.copper_chest_locked";
    static final String LOCKED = "You cannot use this copper chest. Public in %s.";
    static final String JOIN_KEY = "tougher.message.copper_chest_join";
    static final String JOIN = "You cannot join that copper chest. Sneak to place your own.";

    private static final Map<UUID, String> LOOK = new HashMap<>();

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        bus.listen(UseBlockCallback.EVENT, ID, CopperChests::onUseBlock);
        bus.listen(PlayerBlockBreakEvents.BEFORE, ID, CopperChests::onBreakBefore);
        bus.listen(PlayerBlockBreakEvents.AFTER, ID, CopperChests::onBreak);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> LOOK.remove(handler.player.getUUID()));
    }

    @Override
    public void serverTick(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            tickLook(player);
        }
    }

    /** Cancels a copper-chest join the player does not own, and a sign placed on a locked chest. */
    public static InteractionResult denyPlacement(BlockPlaceContext context, Block block) {
        if (context == null || block == null || !(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.PASS;
        }
        if (!WorldGate.isModuleActive(level, ID) || !(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }
        if (player.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (block.defaultBlockState().is(BlockTags.COPPER_CHESTS)) {
            return denyJoin(context, level, player, block);
        }
        if (block.defaultBlockState().is(BlockTags.ALL_SIGNS)) {
            return denySign(context, level, player);
        }
        return InteractionResult.PASS;
    }

    public static void onPlaced(BlockPlaceContext context, Block block) {
        if (context == null
                || block == null
                || !block.defaultBlockState().is(BlockTags.COPPER_CHESTS)
                || !(context.getLevel() instanceof ServerLevel level)
                || !WorldGate.isModuleActive(level, ID)
                || !(context.getPlayer() instanceof ServerPlayer player)
                || player.isSpectator()) {
            return;
        }
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(BlockTags.COPPER_CHESTS)) {
            return;
        }
        long now = now(level);
        long expiry = CopperChestRules.expiresAt(now);
        BlockPos other = connected(level, pos, state);
        if (other != null) {
            CopperChestRules.Claim neighbor = live(level, other);
            if (neighbor != null && player.getStringUUID().equals(neighbor.owner())) {
                expiry = neighbor.expiresAt();
            }
        }
        CopperChestData.of(level)
                .remember(new CopperChestRules.Claim(
                        dimension(level), pos.getX(), pos.getY(), pos.getZ(), player.getStringUUID(), expiry));
    }

    /**
     * Called from the chest's open action. Owners pass through and the wait starts again.
     * Everyone else is refused while the claim lasts.
     */
    public static InteractionResult onOpen(BlockState state, Level level, BlockPos pos, Player player) {
        if (!(level instanceof ServerLevel server)
                || !WorldGate.isModuleActive(server, ID)
                || state == null
                || !state.is(BlockTags.COPPER_CHESTS)
                || !(player instanceof ServerPlayer serverPlayer)
                || serverPlayer.isSpectator()) {
            return InteractionResult.PASS;
        }
        CopperChestRules.Claim claim = live(server, pos);
        if (claim == null || EhmApi.playerBypasses(serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (allowed(serverPlayer, server, pos, claim)) {
            refresh(server, pos, claim);
            return InteractionResult.PASS;
        }
        tellLocked(serverPlayer, claim, now(server));
        return InteractionResult.FAIL;
    }

    /** Refuses editing a sign that is attached to a locked copper chest. */
    static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server)
                || !WorldGate.isModuleActive(server, ID)
                || !(player instanceof ServerPlayer serverPlayer)
                || serverPlayer.isSpectator()
                || EhmApi.playerBypasses(serverPlayer)
                || hit == null) {
            return InteractionResult.PASS;
        }
        ItemStack stack = serverPlayer.getItemInHand(hand);
        if (stack.getItem() instanceof BlockItem) {
            return InteractionResult.PASS;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = server.getBlockState(pos);
        BlockPos chest = attachedChest(server, pos, state);
        if (chest == null) {
            return InteractionResult.PASS;
        }
        CopperChestRules.Claim claim = live(server, chest);
        if (claim == null || allowed(serverPlayer, server, chest, claim)) {
            return InteractionResult.PASS;
        }
        tellLocked(serverPlayer, claim, now(server));
        return InteractionResult.FAIL;
    }

    /** True when this sign edit must be discarded. */
    public static boolean denySignEdit(SignBlockEntity sign, Player player) {
        if (sign == null
                || !(player instanceof ServerPlayer serverPlayer)
                || !(sign.getLevel() instanceof ServerLevel level)
                || !WorldGate.isModuleActive(level, ID)
                || serverPlayer.isSpectator()
                || EhmApi.playerBypasses(serverPlayer)) {
            return false;
        }
        BlockPos chest = attachedChest(level, sign.getBlockPos(), sign.getBlockState());
        if (chest == null) {
            return false;
        }
        CopperChestRules.Claim claim = live(level, chest);
        if (claim == null || allowed(serverPlayer, level, chest, claim)) {
            return false;
        }
        tellLocked(serverPlayer, claim, now(level));
        return true;
    }

    /** A locked copper chest or hopper stops a piston that would push or destroy it. */
    public static boolean blocksPiston(Level level, List<BlockPos> push, List<BlockPos> destroy) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        return anyLocked(server, push) || anyLocked(server, destroy);
    }

    /** Droppers and hoppers cannot push items through a locked copper chest or hopper. */
    public static boolean blocksContainer(Container container) {
        if (container instanceof CompoundContainer compound) {
            CompoundContainerAccess access = (CompoundContainerAccess) compound;
            return blocksContainer(access.tougher$container1()) || blocksContainer(access.tougher$container2());
        }
        if (container instanceof BlockEntity entity) {
            return blocksAutomation(entity.getLevel(), entity.getBlockPos());
        }
        return false;
    }

    public static boolean blocksAutomation(Level level, BlockPos pos) {
        if (OwnedHoppers.lockedAt(level, pos)) {
            return true;
        }
        if (!(level instanceof ServerLevel server)
                || pos == null
                || !WorldGate.isModuleActive(server, ID)
                || !server.getBlockState(pos).is(BlockTags.COPPER_CHESTS)) {
            return false;
        }
        return live(server, pos) != null;
    }

    /**
     * True when this hopper must not pull. A hopper owned by the chest's owner, or by
     * a player named on its sign, may pull. Deposit uses {@link #blocksInsert}.
     */
    public static boolean blocksTake(Hopper hopper, Container container) {
        if (!blocksContainer(container)) {
            return false;
        }
        return !OwnedHoppers.mayPull(hopper, container);
    }

    /** True while this position is still a locked copper chest. */
    public static boolean isLocked(ServerLevel level, BlockPos pos) {
        return live(level, pos) != null;
    }

    /** Placer id of a locked copper chest, or null when that position is public. */
    public static String ownerId(ServerLevel level, BlockPos pos) {
        CopperChestRules.Claim claim = live(level, pos);
        return claim == null ? null : claim.owner();
    }

    /**
     * True when this transfer must not move items. A hopper may push into a locked
     * copper chest whose placer is the same player.
     */
    public static boolean blocksInsert(Container source, Container destination) {
        if (OwnedHoppers.mayDeposit(source, destination)) {
            return false;
        }
        return blocksContainer(source) || blocksContainer(destination);
    }

    /** The placer, or a player named on the chest. A missing claim allows everyone. */
    public static boolean allows(ServerLevel level, BlockPos pos, String playerId, String playerName) {
        CopperChestRules.Claim claim = live(level, pos);
        if (claim == null) {
            return true;
        }
        return CopperChestRules.allowed(claim.owner(), playerId, signLines(level, pos), playerName);
    }

    public static void protectFromExplosion(ServerLevel level, List<BlockPos> positions) {
        if (level == null || positions == null || positions.isEmpty()) {
            return;
        }
        positions.removeIf(pos -> blocksAutomation(level, pos));
    }

    static boolean onBreakBefore(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (!(level instanceof ServerLevel server)
                || !WorldGate.isModuleActive(server, ID)
                || state == null
                || pos == null
                || !(player instanceof ServerPlayer serverPlayer)
                || serverPlayer.isSpectator()
                || EhmApi.playerBypasses(serverPlayer)) {
            return true;
        }
        BlockPos chest = state.is(BlockTags.COPPER_CHESTS) ? pos : attachedChest(server, pos, state);
        if (chest == null) {
            return true;
        }
        CopperChestRules.Claim claim = live(server, chest);
        if (claim == null || allowed(serverPlayer, server, chest, claim)) {
            return true;
        }
        tellLocked(serverPlayer, claim, now(server));
        return false;
    }

    static void onBreak(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (!(level instanceof ServerLevel server)
                || !WorldGate.isModuleActive(server, ID)
                || state == null
                || pos == null
                || !state.is(BlockTags.COPPER_CHESTS)) {
            return;
        }
        CopperChestData.of(server).forget(dimension(server), pos.getX(), pos.getY(), pos.getZ());
    }

    private static InteractionResult denyJoin(BlockPlaceContext context, ServerLevel level, ServerPlayer player, Block block) {
        BlockState future = block.getStateForPlacement(context);
        if (future == null || !future.is(BlockTags.COPPER_CHESTS) || !future.hasProperty(ChestBlock.TYPE)) {
            return InteractionResult.PASS;
        }
        if (future.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return InteractionResult.PASS;
        }
        BlockPos other = ChestBlock.getConnectedBlockPos(context.getClickedPos(), future);
        CopperChestRules.Claim neighbor = live(level, other);
        if (neighbor != null && player.getStringUUID().equals(neighbor.owner())) {
            return InteractionResult.PASS;
        }
        player.sendOverlayMessage(Component.translatableWithFallback(JOIN_KEY, JOIN));
        return InteractionResult.FAIL;
    }

    private static InteractionResult denySign(BlockPlaceContext context, ServerLevel level, ServerPlayer player) {
        if (context.getClickedFace() == null) {
            return InteractionResult.PASS;
        }
        BlockPos support = context.getClickedPos().relative(context.getClickedFace().getOpposite());
        if (!level.getBlockState(support).is(BlockTags.COPPER_CHESTS) || EhmApi.playerBypasses(player)) {
            return InteractionResult.PASS;
        }
        CopperChestRules.Claim claim = live(level, support);
        if (claim == null || allowed(player, level, support, claim)) {
            return InteractionResult.PASS;
        }
        tellLocked(player, claim, now(level));
        return InteractionResult.FAIL;
    }

    private static void refresh(ServerLevel level, BlockPos pos, CopperChestRules.Claim claim) {
        long expiry = CopperChestRules.expiresAt(now(level));
        CopperChestData data = CopperChestData.of(level);
        data.remember(claim.withExpiry(expiry));
        BlockPos other = connected(level, pos, level.getBlockState(pos));
        if (other == null) {
            return;
        }
        CopperChestRules.Claim neighbor = data.find(dimension(level), other.getX(), other.getY(), other.getZ());
        if (neighbor != null && claim.owner().equals(neighbor.owner())) {
            data.remember(neighbor.withExpiry(expiry));
        }
    }

    private static boolean anyLocked(ServerLevel level, List<BlockPos> positions) {
        if (positions == null) {
            return false;
        }
        for (BlockPos pos : positions) {
            if (pos != null && blocksAutomation(level, pos)) {
                return true;
            }
        }
        return false;
    }

    /** The claim while it is locked and the block is still a copper chest. An expired claim is dropped. */
    private static CopperChestRules.Claim live(ServerLevel level, BlockPos pos) {
        if (pos == null) {
            return null;
        }
        String dimension = dimension(level);
        CopperChestData data = CopperChestData.of(level);
        CopperChestRules.Claim claim = data.find(dimension, pos.getX(), pos.getY(), pos.getZ());
        if (claim == null) {
            return null;
        }
        if (!level.getBlockState(pos).is(BlockTags.COPPER_CHESTS) || !CopperChestRules.locked(claim.expiresAt(), now(level))) {
            data.forget(dimension, pos.getX(), pos.getY(), pos.getZ());
            return null;
        }
        return claim;
    }

    private static boolean allowed(ServerPlayer player, ServerLevel level, BlockPos pos, CopperChestRules.Claim claim) {
        return CopperChestRules.allowed(claim.owner(), player.getStringUUID(), signLines(level, pos), player.getPlainTextName());
    }

    private static List<String> signLines(ServerLevel level, BlockPos pos) {
        List<String> lines = new ArrayList<>();
        addSignLines(level, pos, lines);
        BlockPos other = connected(level, pos, level.getBlockState(pos));
        if (other != null) {
            addSignLines(level, other, lines);
        }
        return lines;
    }

    private static void addSignLines(ServerLevel level, BlockPos chest, List<String> lines) {
        for (Direction direction : Direction.values()) {
            BlockPos signPos = chest.relative(direction);
            BlockState state = level.getBlockState(signPos);
            if (!chest.equals(attachedChest(level, signPos, state))) {
                continue;
            }
            if (!(level.getBlockEntity(signPos) instanceof SignBlockEntity sign)) {
                continue;
            }
            for (SignTextSlot side : new SignTextSlot[] {SignTextSlot.FRONT, SignTextSlot.BACK}) {
                SignText text = sign.getText(side);
                if (text == null) {
                    continue;
                }
                for (Component line : text.getMessages(false)) {
                    lines.add(line == null ? "" : line.getString());
                }
            }
        }
    }

    static BlockPos attachedChest(ServerLevel level, BlockPos signPos, BlockState state) {
        if (state == null || signPos == null || !state.is(BlockTags.ALL_SIGNS)) {
            return null;
        }
        BlockPos support;
        Block block = state.getBlock();
        if (block instanceof WallSignBlock && state.hasProperty(WallSignBlock.FACING)) {
            support = signPos.relative(state.getValue(WallSignBlock.FACING).getOpposite());
        } else if (block instanceof WallHangingSignBlock && state.hasProperty(WallHangingSignBlock.FACING)) {
            support = signPos.relative(state.getValue(WallHangingSignBlock.FACING).getOpposite());
        } else if (block instanceof StandingSignBlock) {
            support = signPos.below();
        } else if (block instanceof CeilingHangingSignBlock) {
            support = signPos.above();
        } else {
            return null;
        }
        if (!level.getBlockState(support).is(BlockTags.COPPER_CHESTS)) {
            return null;
        }
        return support;
    }

    private static BlockPos connected(ServerLevel level, BlockPos pos, BlockState state) {
        if (state == null || !(state.getBlock() instanceof ChestBlock) || !state.hasProperty(ChestBlock.TYPE)) {
            return null;
        }
        if (state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return null;
        }
        return ChestBlock.getConnectedBlockPos(pos, state);
    }

    public static void tickLook(ServerPlayer player) {
        String label = lookText(player);
        String last = LOOK.get(player.getUUID());
        if (label.equals(last)) {
            return;
        }
        LOOK.put(player.getUUID(), label);
        EhmNetworking.sendCopperChestLook(
                player, label.isEmpty() ? ClientboundCopperChestLookPayload.HIDDEN : new ClientboundCopperChestLookPayload(true, label));
    }

    private static String lookText(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || Inhabitants.lookAtResident(player) != null) {
            return "";
        }
        HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || blockHit.getType() == HitResult.Type.MISS) {
            return "";
        }
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (state.is(BlockTags.COPPER_CHESTS)) {
            CopperChestRules.Claim claim = live(level, pos);
            if (claim == null || allowed(player, level, pos, claim)) {
                return "";
            }
            return CopperChestRules.lookLabel(CopperChestRules.daysLeft(claim.expiresAt(), now(level)));
        }
        if (OwnedHoppers.isHopper(state)) {
            return OwnedHoppers.lookLabel(player, level, pos);
        }
        return "";
    }

    private static void tellLocked(ServerPlayer player, CopperChestRules.Claim claim, long now) {
        player.sendOverlayMessage(Component.translatableWithFallback(
                LOCKED_KEY, LOCKED, CopperChestRules.daysPhrase(CopperChestRules.daysLeft(claim.expiresAt(), now))));
    }

    private static long now(ServerLevel level) {
        return level.getOverworldClockTime();
    }

    private static String dimension(ServerLevel level) {
        return level.dimension().identifier().toString();
    }
}

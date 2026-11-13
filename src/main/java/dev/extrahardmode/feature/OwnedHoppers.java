package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.mixin.CompoundContainerAccess;
import dev.extrahardmode.world.HopperData;
import dev.extrahardmode.world.WorldGate;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.CompoundContainer;
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
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A hopper belongs to the player who placed it, for the same 90 Minecraft days as a
 * copper chest. That hopper can pull from a copper chest or hopper they own, and it
 * can put items into a copper chest placed by the same player. Other hoppers, pistons, explosions, and copper
 * golems leave it alone.
 */
public final class OwnedHoppers implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("owned_hoppers");

    static final String LOCKED_KEY = "tougher.message.hopper_locked";
    static final String LOCKED = "You cannot use this hopper. Public in %s.";

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        bus.listen(UseBlockCallback.EVENT, ID, OwnedHoppers::onUseBlock);
        bus.listen(PlayerBlockBreakEvents.BEFORE, ID, OwnedHoppers::onBreakBefore);
        bus.listen(PlayerBlockBreakEvents.AFTER, ID, OwnedHoppers::onBreak);
    }

    @Override
    public void serverTick(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            CopperChests.tickLook(player);
        }
    }

    /** Cancels a sign placed on a locked hopper. */
    public static InteractionResult denyPlacement(BlockPlaceContext context, Block block) {
        if (context == null
                || block == null
                || !block.defaultBlockState().is(BlockTags.ALL_SIGNS)
                || !(context.getLevel() instanceof ServerLevel level)
                || !WorldGate.isModuleActive(level, ID)
                || !(context.getPlayer() instanceof ServerPlayer player)
                || player.isSpectator()) {
            return InteractionResult.PASS;
        }
        return denySign(context, level, player);
    }

    public static void onPlaced(BlockPlaceContext context, Block block) {
        if (context == null
                || !(block instanceof HopperBlock)
                || !(context.getLevel() instanceof ServerLevel level)
                || !WorldGate.isModuleActive(level, ID)
                || !(context.getPlayer() instanceof ServerPlayer player)
                || player.isSpectator()) {
            return;
        }
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockState(pos).getBlock() instanceof HopperBlock)) {
            return;
        }
        String name = player.getPlainTextName();
        HopperData.of(level)
                .remember(new HopperRules.Claim(
                        dimension(level),
                        pos.getX(),
                        pos.getY(),
                        pos.getZ(),
                        player.getStringUUID(),
                        name == null ? "" : name,
                        CopperChestRules.expiresAt(now(level))));
    }

    /** Owners pass through and the wait starts again. Everyone else is refused. */
    public static InteractionResult onOpen(BlockState state, Level level, BlockPos pos, Player player) {
        if (!(level instanceof ServerLevel server)
                || !WorldGate.isModuleActive(server, ID)
                || !isHopper(state)
                || !(player instanceof ServerPlayer serverPlayer)
                || serverPlayer.isSpectator()) {
            return InteractionResult.PASS;
        }
        HopperRules.Claim claim = live(server, pos);
        if (claim == null || EhmApi.playerBypasses(serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (allowed(serverPlayer, server, pos, claim)) {
            HopperData.of(server).remember(claim.withExpiry(CopperChestRules.expiresAt(now(server))));
            return InteractionResult.PASS;
        }
        tellLocked(serverPlayer, claim, now(server));
        return InteractionResult.FAIL;
    }

    /** Refuses editing a sign that is attached to a locked hopper. */
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
        BlockPos hopper = attachedHopper(server, pos, server.getBlockState(pos));
        if (hopper == null) {
            return InteractionResult.PASS;
        }
        HopperRules.Claim claim = live(server, hopper);
        if (claim == null || allowed(serverPlayer, server, hopper, claim)) {
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
        BlockPos hopper = attachedHopper(level, sign.getBlockPos(), sign.getBlockState());
        if (hopper == null) {
            return false;
        }
        HopperRules.Claim claim = live(level, hopper);
        if (claim == null || allowed(serverPlayer, level, hopper, claim)) {
            return false;
        }
        tellLocked(serverPlayer, claim, now(level));
        return true;
    }

    /** A locked hopper. Callers that already checked the module can use {@link #live}. */
    public static boolean lockedAt(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel server) || pos == null || !WorldGate.isModuleActive(server, ID)) {
            return false;
        }
        return live(server, pos) != null;
    }

    /**
     * True when this hopper may pull from every locked copper chest or hopper in the
     * container. A hopper with no owner, and a minecart hopper, may not.
     */
    public static boolean mayPull(Hopper hopper, Container container) {
        if (!(hopper instanceof BlockEntity entity) || !(entity.getLevel() instanceof ServerLevel level)) {
            return false;
        }
        HopperRules.Claim claim = live(level, entity.getBlockPos());
        if (claim == null) {
            return false;
        }
        return sourcesAllow(container, claim.owner(), claim.ownerName());
    }

    /**
     * True when {@code source} is a placed hopper and every locked chest in
     * {@code destination} was placed by that same player. A locked hopper in the
     * destination does not accept the push.
     */
    public static boolean mayDeposit(Container source, Container destination) {
        if (!(source instanceof Hopper) || !(source instanceof BlockEntity entity)) {
            return false;
        }
        if (!(entity.getLevel() instanceof ServerLevel level)) {
            return false;
        }
        HopperRules.Claim claim = live(level, entity.getBlockPos());
        if (claim == null) {
            return false;
        }
        return classifyDeposit(destination, claim.owner()) == Deposit.SAME_OWNER;
    }

    /** Label for a non-owner looking at this hopper. Empty for an owner or a public hopper. */
    public static String lookLabel(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (player == null || level == null || !WorldGate.isModuleActive(level, ID)) {
            return "";
        }
        HopperRules.Claim claim = live(level, pos);
        if (claim == null || allowed(player, level, pos, claim)) {
            return "";
        }
        return CopperChestRules.lookLabel(CopperChestRules.daysLeft(claim.expiresAt(), now(level)));
    }

    public static boolean isHopper(BlockState state) {
        return state != null && state.getBlock() instanceof HopperBlock;
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
        BlockPos hopper = isHopper(state) ? pos : attachedHopper(server, pos, state);
        if (hopper == null) {
            return true;
        }
        HopperRules.Claim claim = live(server, hopper);
        if (claim == null || allowed(serverPlayer, server, hopper, claim)) {
            return true;
        }
        tellLocked(serverPlayer, claim, now(server));
        return false;
    }

    static void onBreak(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (!(level instanceof ServerLevel server)
                || !WorldGate.isModuleActive(server, ID)
                || !isHopper(state)
                || pos == null) {
            return;
        }
        HopperData.of(server).forget(dimension(server), pos.getX(), pos.getY(), pos.getZ());
    }

    private enum Deposit {
        NONE,
        SAME_OWNER,
        DENY
    }

    /** Locked chests with this owner pass. Any other lock refuses the whole insert. */
    private static Deposit classifyDeposit(Container container, String ownerId) {
        if (container instanceof CompoundContainer compound) {
            CompoundContainerAccess access = (CompoundContainerAccess) compound;
            Deposit first = classifyDeposit(access.tougher$container1(), ownerId);
            Deposit second = classifyDeposit(access.tougher$container2(), ownerId);
            if (first == Deposit.DENY || second == Deposit.DENY) {
                return Deposit.DENY;
            }
            if (first == Deposit.SAME_OWNER || second == Deposit.SAME_OWNER) {
                return Deposit.SAME_OWNER;
            }
            return Deposit.NONE;
        }
        if (!(container instanceof BlockEntity entity) || !(entity.getLevel() instanceof ServerLevel level)) {
            return Deposit.NONE;
        }
        BlockPos pos = entity.getBlockPos();
        if (CopperChests.isLocked(level, pos)) {
            return HopperRules.sameOwner(ownerId, CopperChests.ownerId(level, pos)) ? Deposit.SAME_OWNER : Deposit.DENY;
        }
        if (live(level, pos) != null) {
            return Deposit.DENY;
        }
        return Deposit.NONE;
    }

    private static boolean sourcesAllow(Container container, String ownerId, String ownerName) {
        if (container instanceof CompoundContainer compound) {
            CompoundContainerAccess access = (CompoundContainerAccess) compound;
            return sourcesAllow(access.tougher$container1(), ownerId, ownerName)
                    && sourcesAllow(access.tougher$container2(), ownerId, ownerName);
        }
        if (!(container instanceof BlockEntity entity) || !(entity.getLevel() instanceof ServerLevel level)) {
            return false;
        }
        BlockPos pos = entity.getBlockPos();
        if (CopperChests.isLocked(level, pos)) {
            return CopperChests.allows(level, pos, ownerId, ownerName);
        }
        if (live(level, pos) != null) {
            return allows(level, pos, ownerId, ownerName);
        }
        return true;
    }

    private static InteractionResult denySign(BlockPlaceContext context, ServerLevel level, ServerPlayer player) {
        if (context.getClickedFace() == null) {
            return InteractionResult.PASS;
        }
        BlockPos support = context.getClickedPos().relative(context.getClickedFace().getOpposite());
        if (!isHopper(level.getBlockState(support)) || EhmApi.playerBypasses(player)) {
            return InteractionResult.PASS;
        }
        HopperRules.Claim claim = live(level, support);
        if (claim == null || allowed(player, level, support, claim)) {
            return InteractionResult.PASS;
        }
        tellLocked(player, claim, now(level));
        return InteractionResult.FAIL;
    }

    private static boolean allowed(ServerPlayer player, ServerLevel level, BlockPos pos, HopperRules.Claim claim) {
        return CopperChestRules.allowed(
                claim.owner(), player.getStringUUID(), signLines(level, pos), player.getPlainTextName());
    }

    private static boolean allows(ServerLevel level, BlockPos pos, String playerId, String playerName) {
        HopperRules.Claim claim = live(level, pos);
        if (claim == null) {
            return true;
        }
        return HopperRules.mayPull(playerId, playerName, claim.owner(), signLines(level, pos));
    }

    /** The claim while it is locked and the block is still a hopper. An expired claim is dropped. */
    private static HopperRules.Claim live(ServerLevel level, BlockPos pos) {
        if (pos == null || !WorldGate.isModuleActive(level, ID)) {
            return null;
        }
        String dimension = dimension(level);
        HopperData data = HopperData.of(level);
        HopperRules.Claim claim = data.find(dimension, pos.getX(), pos.getY(), pos.getZ());
        if (claim == null) {
            return null;
        }
        if (!isHopper(level.getBlockState(pos)) || !CopperChestRules.locked(claim.expiresAt(), now(level))) {
            data.forget(dimension, pos.getX(), pos.getY(), pos.getZ());
            return null;
        }
        return claim;
    }

    private static List<String> signLines(ServerLevel level, BlockPos pos) {
        List<String> lines = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            BlockPos signPos = pos.relative(direction);
            BlockState state = level.getBlockState(signPos);
            if (!pos.equals(attachedHopper(level, signPos, state))) {
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
        return lines;
    }

    static BlockPos attachedHopper(ServerLevel level, BlockPos signPos, BlockState state) {
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
        if (!isHopper(level.getBlockState(support))) {
            return null;
        }
        return support;
    }

    private static void tellLocked(ServerPlayer player, HopperRules.Claim claim, long now) {
        player.sendOverlayMessage(Component.translatableWithFallback(
                LOCKED_KEY,
                LOCKED,
                CopperChestRules.daysPhrase(CopperChestRules.daysLeft(claim.expiresAt(), now))));
    }

    private static long now(ServerLevel level) {
        return level.getOverworldClockTime();
    }

    private static String dimension(ServerLevel level) {
        return level.dimension().identifier().toString();
    }
}

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
 * copper chest. That hopper can pull from and push into a container the same player
 * owns, and into a container nobody owns. Another player's lock stays closed to it.
 * Other hoppers, droppers, pistons, explosions, and copper golems leave it alone.
 */
public final class OwnedHoppers implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("owned_hoppers");
    /** The hopper currently taking an item, so that take is not treated as an outside push. */
    private static final ThreadLocal<Hopper> PULLING = new ThreadLocal<>();

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

    /** Marks a hopper pull so the following insert into that hopper is the pull, not an outside push. */
    public static void beginPull(Hopper hopper) {
        PULLING.set(hopper);
    }

    public static void endPull() {
        PULLING.remove();
    }

    /**
     * True when this hopper must not take from the container. An owned hopper may take
     * from a container with the same owner or with no owner. Any other hopper stops at a lock.
     */
    public static boolean blocksTake(Hopper hopper, Container container) {
        String owner = hopperOwner(hopper);
        if (owner != null) {
            return !transferAllowed(owner, container);
        }
        return CopperChests.blocksContainer(container);
    }

    /**
     * True when this transfer must not move items. A pull into the hopper that started
     * it, and a push from an owned hopper, may use a same-owner container or one nobody
     * owns. A dropper or another player's hopper still cannot push into a lock.
     */
    public static boolean blocksInsert(Container source, Container destination) {
        Hopper pulling = PULLING.get();
        if (pulling != null && destination == pulling) {
            String owner = hopperOwner(pulling);
            if (owner == null) {
                return CopperChests.blocksContainer(source);
            }
            return !transferAllowed(owner, source);
        }
        String sourceOwner = hopperOwner(source instanceof Hopper hopper ? hopper : null);
        if (sourceOwner != null) {
            return !transferAllowed(sourceOwner, destination);
        }
        return CopperChests.blocksContainer(source) || CopperChests.blocksContainer(destination);
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

    /** Blank when nobody owns the container. A double chest must pass on both halves. */
    private static boolean transferAllowed(String hopperOwnerId, Container container) {
        if (container instanceof CompoundContainer compound) {
            CompoundContainerAccess access = (CompoundContainerAccess) compound;
            return transferAllowed(hopperOwnerId, access.tougher$container1())
                    && transferAllowed(hopperOwnerId, access.tougher$container2());
        }
        return HopperRules.mayTransfer(hopperOwnerId, containerOwner(container));
    }

    /** Owner id of a locked copper chest or hopper. Null when this container is public. */
    private static String containerOwner(Container container) {
        if (!(container instanceof BlockEntity entity) || !(entity.getLevel() instanceof ServerLevel level)) {
            return null;
        }
        BlockPos pos = entity.getBlockPos();
        if (CopperChests.isLocked(level, pos)) {
            return CopperChests.ownerId(level, pos);
        }
        HopperRules.Claim claim = live(level, pos);
        return claim == null ? null : claim.owner();
    }

    /** Placer id when this is a locked placed hopper. Null for a public hopper or a minecart. */
    private static String hopperOwner(Hopper hopper) {
        if (!(hopper instanceof BlockEntity entity) || !(entity.getLevel() instanceof ServerLevel level)) {
            return null;
        }
        HopperRules.Claim claim = live(level, entity.getBlockPos());
        return claim == null ? null : claim.owner();
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

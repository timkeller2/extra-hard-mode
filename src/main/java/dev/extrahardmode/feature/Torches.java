package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.module.MessageId;
import dev.extrahardmode.module.MsgService;
import dev.extrahardmode.task.RemoveExposedTorchesTask;
import dev.extrahardmode.task.TorchBurnTask;
import dev.extrahardmode.world.EhmTags;
import dev.extrahardmode.world.TorchLifetimeData;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.world.entity.EquipmentSlot;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.fabric.api.event.player.BlockEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseTorchBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public final class Torches implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("torches");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        bus.listen(UseBlockCallback.EVENT, ID, Torches::onUseBlock);
        bus.listen(BlockEvents.USE_ITEM_ON, ID, Torches::onUseItemOn);
        bus.listen(PlayerBlockBreakEvents.AFTER, ID, Torches::onBreak);
    }

    @Override
    public void serverTick(ServerLevel level) {
        WorldConfig config = ConfigManager.world(level);
        RemoveExposedTorchesTask.run(level, config, RemoveExposedTorchesTask.MAX_TORCHES_PER_TICK);
        TorchBurnTask.run(level, config.torchBurnDays());
        for (ServerPlayer player : level.players()) {
            ejectOffhandLight(player);
        }
    }

    @Override
    public void onWorldUnload(ServerLevel level) {
        RemoveExposedTorchesTask.clear(level);
        TorchBurnTask.clear(level);
    }

    static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        BlockPlaceContext context = new BlockPlaceContext(new UseOnContext(level, player, hand, stack, hit));
        return tryDenyPlacement(context);
    }

    static @Nullable InteractionResult onUseItemOn(
            ItemStack stack,
            BlockState clicked,
            Level level,
            BlockPos clickedPos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        BlockPlaceContext context = new BlockPlaceContext(new UseOnContext(level, player, hand, stack, hit));
        InteractionResult deny = tryDenyPlacement(context);
        return deny.consumesAction() || deny == InteractionResult.FAIL ? deny : null;
    }

    /** Server placement deny for UseBlock, USE_ITEM_ON, and {@code BlockItem.place}. */
    public static InteractionResult tryDenyPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()
                || !(level instanceof ServerLevel serverLevel)
                || !(context.getPlayer() instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (!WorldGate.isModuleActive(serverLevel, ID) || EhmApi.playerBypasses(serverPlayer)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return InteractionResult.PASS;
        }
        DenyReason reason = denyReason(serverLevel, context, blockItem);
        if (reason == DenyReason.NONE) {
            return InteractionResult.PASS;
        }
        WorldConfig config = ConfigManager.world(serverLevel);
        BlockPos pos = context.getClickedPos();
        if (reason == DenyReason.Y) {
            MsgService.noTorchesHere(serverPlayer, serverLevel, pos, config.torchFizz());
        } else {
            MsgService.limitedTorchPlacement(serverPlayer, serverLevel, pos, config.torchFizz());
        }
        return InteractionResult.FAIL;
    }

    public static DenyReason denyReason(ServerLevel level, BlockPlaceContext context, BlockItem blockItem) {
        BlockState placement = blockItem.getBlock().getStateForPlacement(context);
        if (placement == null) {
            return DenyReason.NONE;
        }
        WorldConfig config = ConfigManager.world(level);
        boolean campfire = isCampfire(blockItem.getBlock()) || isCampfire(placement.getBlock());
        if (PlacementRules.denyFlameY(
                config.torchYDeny(),
                config.torchNoPlacementUnderY(),
                context.getClickedPos().getY(),
                placement.is(EhmTags.DEPTH_LIMITED_LIGHTS) || campfire)) {
            return DenyReason.Y;
        }
        if (isTorchLike(blockItem)
                && PlacementRules.denyTorchSoft(
                        config.torchSoftDeny(),
                        level.getBlockState(PlacementRules.againstBlock(context)).is(EhmTags.SOFT_TORCH_SURFACES))) {
            return DenyReason.SOFT;
        }
        return DenyReason.NONE;
    }

    /** Client mixins read only the sync payload, never TOML or live tags. */
    public static DenyReason denyReasonFromPayload(
            boolean torchYDeny,
            int noPlacementUnderY,
            boolean torchSoftDeny,
            Collection<Identifier> depthLimitedLights,
            Collection<Identifier> softSurfaces,
            BlockPlaceContext context,
            BlockItem blockItem) {
        BlockState placement = blockItem.getBlock().getStateForPlacement(context);
        if (placement == null) {
            return DenyReason.NONE;
        }
        Identifier placedId = BuiltInRegistries.BLOCK.getKey(placement.getBlock());
        Identifier itemBlockId = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
        boolean depthLimited = PlacementRules.contains(depthLimitedLights, placedId)
                || PlacementRules.contains(depthLimitedLights, itemBlockId);
        boolean campfire = isCampfire(blockItem.getBlock()) || isCampfire(placement.getBlock());
        if (PlacementRules.denyFlameY(
                torchYDeny, noPlacementUnderY, context.getClickedPos().getY(), depthLimited || campfire)) {
            return DenyReason.Y;
        }
        Identifier clickedId = BuiltInRegistries.BLOCK.getKey(
                context.getLevel().getBlockState(PlacementRules.againstBlock(context)).getBlock());
        boolean torchLike = PlacementRules.torchLikeFromPayload(
                blockItem.getBlock() instanceof BaseTorchBlock, itemBlockId, placedId, depthLimitedLights);
        if (torchLike
                && PlacementRules.denyTorchSoft(
                        torchSoftDeny, PlacementRules.contains(softSurfaces, clickedId))) {
            return DenyReason.SOFT;
        }
        return DenyReason.NONE;
    }

    public static boolean shouldDeny(ServerLevel level, BlockPlaceContext context, BlockItem blockItem) {
        return denyReason(level, context, blockItem) != DenyReason.NONE;
    }

    static boolean isTorchLike(BlockItem blockItem) {
        return isBurnableTorch(blockItem.getBlock());
    }

    public static boolean isBurnableTorch(Block block) {
        return block instanceof BaseTorchBlock || block.defaultBlockState().is(EhmTags.DEPTH_LIMITED_LIGHTS);
    }

    public static boolean isBurnableTorch(BlockState state) {
        return state.getBlock() instanceof BaseTorchBlock || state.is(EhmTags.DEPTH_LIMITED_LIGHTS);
    }

    public static boolean isCopperTorch(Block block) {
        return TorchLifetimeRules.isCopperTorchId(BuiltInRegistries.BLOCK.getKey(block).toString());
    }

    public static boolean isCopperTorch(BlockState state) {
        return isCopperTorch(state.getBlock());
    }

    public static int burnDaysFor(BlockState state, int baseDays) {
        return TorchLifetimeRules.burnDaysFor(baseDays, isCopperTorch(state));
    }

    public static int burningLight(ServerLevel level, BlockPos pos, int vanillaLight) {
        BlockState state = level.getBlockState(pos);
        return TorchLifetimeRules.lightLevel(
                vanillaLight,
                TorchLifetimeData.of(level).placedAt(pos),
                level.getGameTime(),
                burnDaysFor(state, ConfigManager.world(level).torchBurnDays()));
    }

    /** Stamp a newly placed torch so it can burn out. Unstamped torches stay forever. */
    public static void onPlaced(BlockPlaceContext context, Block block) {
        if (!(context.getLevel() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, ID)) {
            return;
        }
        BlockPos pos = context.getClickedPos();
        BlockState placed = level.getBlockState(pos);
        if (!isBurnableTorch(block)
                && !isBurnableTorch(placed)
                && !isCampfire(block)
                && !isCampfire(placed)) {
            return;
        }
        TorchLifetimeData.of(level).record(pos, level.getGameTime());
    }

    static void onBreak(
            Level level,
            Player player,
            BlockPos pos,
            BlockState state,
            net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel) || !WorldGate.isModuleActive(serverLevel, ID)) {
            return;
        }
        if (isBurnableTorch(state) || isCampfire(state)) {
            forgetPlaced(serverLevel, pos);
        }
    }

    public static void forgetPlaced(ServerLevel level, BlockPos pos) {
        TorchLifetimeData.of(level).remove(pos);
    }

    /** Extinguish without dropping the item. */
    public static void burnOut(ServerLevel level, BlockPos pos) {
        if (!isBurnableTorch(level.getBlockState(pos))) {
            return;
        }
        level.removeBlock(pos, false);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 2.0F);
        level.sendParticles(
                ParticleTypes.SMOKE,
                pos.getX() + 0.5,
                pos.getY() + 0.4,
                pos.getZ() + 0.5,
                8,
                0.12,
                0.15,
                0.12,
                0.01);
    }

    public static boolean isCampfire(Block block) {
        return block instanceof CampfireBlock;
    }

    public static boolean isCampfire(BlockState state) {
        return isCampfire(state.getBlock());
    }

    public static boolean isLitCampfire(BlockState state) {
        if (!isCampfire(state)) {
            return false;
        }
        if (!state.hasProperty(CampfireBlock.LIT)) {
            return true;
        }
        return state.getValue(CampfireBlock.LIT);
    }

    /** Extinguish without dropping the campfire. Cooking items still drop. */
    public static void burnOutCampfire(ServerLevel level, BlockPos pos) {
        if (!isCampfire(level.getBlockState(pos))) {
            return;
        }
        level.removeBlock(pos, false);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.2F);
        level.sendParticles(
                ParticleTypes.SMOKE,
                pos.getX() + 0.5,
                pos.getY() + 0.6,
                pos.getZ() + 0.5,
                16,
                0.2,
                0.25,
                0.2,
                0.02);
    }

    /**
     * Consume one log from the nearest chest in range. True when the campfire
     * should keep burning.
     */
    public static boolean tryRefuelCampfire(ServerLevel level, BlockPos campfire) {
        List<BlockPos> chests = chestsInRange(level, campfire, TorchLifetimeRules.CAMPFIRE_REFUEL_RANGE);
        chests.sort(Comparator.comparingLong(pos -> TorchLifetimeRules.distanceSq(
                pos.getX() - campfire.getX(), pos.getY() - campfire.getY(), pos.getZ() - campfire.getZ())));
        for (BlockPos chestPos : chests) {
            if (takeOneLog(level, chestPos)) {
                level.playSound(null, campfire, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.playSound(null, chestPos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.4F, 1.2F);
                return true;
            }
        }
        return false;
    }

    /**
     * Consume one coal or charcoal from the nearest chest in range. True when the
     * torch should keep burning.
     */
    public static boolean tryRefuelTorch(ServerLevel level, BlockPos torch) {
        List<BlockPos> chests = chestsInRange(level, torch, TorchLifetimeRules.TORCH_REFUEL_RANGE);
        chests.sort(Comparator.comparingLong(pos -> TorchLifetimeRules.distanceSq(
                pos.getX() - torch.getX(), pos.getY() - torch.getY(), pos.getZ() - torch.getZ())));
        for (BlockPos chestPos : chests) {
            if (takeOneTorchFuel(level, chestPos)) {
                level.playSound(null, torch, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.4F, 1.4F);
                level.playSound(null, chestPos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.3F, 0.8F);
                return true;
            }
        }
        return false;
    }

    static List<BlockPos> chestsInRange(ServerLevel level, BlockPos origin, int range) {
        List<BlockPos> chests = new ArrayList<>();
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
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof ChestBlockEntity)) {
                        continue;
                    }
                    BlockPos pos = be.getBlockPos();
                    if (!TorchLifetimeRules.chestInRange(
                            pos.getX() - origin.getX(),
                            pos.getY() - origin.getY(),
                            pos.getZ() - origin.getZ(),
                            range)) {
                        continue;
                    }
                    chests.add(pos.immutable());
                }
            }
        }
        return chests;
    }

    static boolean takeOneLog(ServerLevel level, BlockPos pos) {
        return takeOneFromChest(level, pos, stack -> stack.is(ItemTags.LOGS));
    }

    static boolean takeOneTorchFuel(ServerLevel level, BlockPos pos) {
        return takeOneFromChest(level, pos, stack -> stack.is(Items.COAL) || stack.is(Items.CHARCOAL));
    }

    static boolean takeOneFromChest(
            ServerLevel level, BlockPos pos, java.util.function.Predicate<ItemStack> match) {
        Container container = chestContainerAt(level, pos);
        if (container == null) {
            return false;
        }
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty() || !match.test(stack)) {
                continue;
            }
            stack.shrink(1);
            container.setItem(slot, stack);
            container.setChanged();
            return true;
        }
        return false;
    }

    static Container chestContainerAt(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock chest) {
            return ChestBlock.getContainer(chest, state, level, pos, true);
        }
        return null;
    }

    /** Server lighting / keep-lit path. Client mixins use the sync payload, never this. */
    public static boolean denyCampfireAt(ServerLevel level, BlockPos pos) {
        if (!WorldGate.isModuleActive(level, ID)) {
            return false;
        }
        WorldConfig config = ConfigManager.world(level);
        return PlacementRules.denyTorchY(config.torchYDeny(), config.torchNoPlacementUnderY(), pos.getY());
    }

    /**
     * Deny lighting a campfire below the torch Y cutoff. Sends the airflow message when
     * {@code player} is present. Bypass players skip.
     */
    public static boolean tryDenyCampfireLight(ServerLevel level, BlockPos pos, ServerPlayer player) {
        if (player != null && EhmApi.playerBypasses(player)) {
            return false;
        }
        if (!denyCampfireAt(level, pos)) {
            return false;
        }
        if (player != null) {
            MsgService.noTorchesHere(player, level, pos, ConfigManager.world(level).torchFizz());
        }
        return true;
    }

    public static boolean blocksOffhand(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(dev.extrahardmode.tag.EhmTags.NO_OFFHAND_LIGHT);
    }

    public static boolean denyOffhandSwap(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(player.level() instanceof ServerLevel level)
                || !WorldGate.isModuleActive(level, ID)
                || EhmApi.playerBypasses(serverPlayer)
                || serverPlayer.isSpectator()) {
            return false;
        }
        return PlacementRules.denyOffhandLight(true, blocksOffhand(player.getMainHandItem()));
    }

    public static boolean denyOffhandPlace(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(player.level() instanceof ServerLevel level)
                || !WorldGate.isModuleActive(level, ID)
                || EhmApi.playerBypasses(serverPlayer)
                || serverPlayer.isSpectator()) {
            return false;
        }
        return PlacementRules.denyOffhandLight(true, blocksOffhand(stack));
    }

    public static void ejectOffhandLight(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)
                || !WorldGate.isModuleActive(level, ID)
                || EhmApi.playerBypasses(player)
                || player.isSpectator()) {
            return;
        }
        ItemStack offhand = player.getOffhandItem();
        if (!blocksOffhand(offhand)) {
            return;
        }
        player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        player.getInventory().placeItemBackInInventory(offhand);
        MsgService.deny(player, MessageId.NO_OFFHAND_LIGHT);
    }

    public enum DenyReason {
        NONE,
        Y,
        SOFT
    }
}

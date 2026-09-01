package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.module.MsgService;
import dev.extrahardmode.task.RemoveExposedTorchesTask;
import dev.extrahardmode.world.EhmTags;
import dev.extrahardmode.world.WorldGate;
import java.util.Collection;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class Torches implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("torches");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        bus.listen(UseBlockCallback.EVENT, ID, Torches::onUseBlock);
    }

    @Override
    public void serverTick(ServerLevel level) {
        WorldConfig config = ConfigManager.world(level);
        RemoveExposedTorchesTask.run(level, config, RemoveExposedTorchesTask.MAX_TORCHES_PER_TICK);
    }

    @Override
    public void onWorldUnload(ServerLevel level) {
        RemoveExposedTorchesTask.clear(level);
    }

    static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()
                || !(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (!WorldGate.isModuleActive(serverLevel, ID) || EhmApi.playerBypasses(serverPlayer)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return InteractionResult.PASS;
        }
        BlockPlaceContext context = new BlockPlaceContext(new UseOnContext(level, player, hand, stack, hit));
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
        if (placement.is(EhmTags.DEPTH_LIMITED_LIGHTS)
                && PlacementRules.denyTorchY(
                        config.torchYDeny(), config.torchNoPlacementUnderY(), context.getClickedPos().getY())) {
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

    /** Client mixins read only the sync payload, never TOML. */
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
        boolean depthLimited = (placedId != null && depthLimitedLights.contains(placedId))
                || (itemBlockId != null && depthLimitedLights.contains(itemBlockId));
        if (depthLimited
                && PlacementRules.denyTorchY(torchYDeny, noPlacementUnderY, context.getClickedPos().getY())) {
            return DenyReason.Y;
        }
        Identifier clickedId = BuiltInRegistries.BLOCK.getKey(
                context.getLevel().getBlockState(PlacementRules.againstBlock(context)).getBlock());
        boolean soft = clickedId != null && softSurfaces.contains(clickedId);
        if (isTorchLike(blockItem) && PlacementRules.denyTorchSoft(torchSoftDeny, soft)) {
            return DenyReason.SOFT;
        }
        return DenyReason.NONE;
    }

    public static boolean shouldDeny(ServerLevel level, BlockPlaceContext context, BlockItem blockItem) {
        return denyReason(level, context, blockItem) != DenyReason.NONE;
    }

    static boolean isTorchLike(BlockItem blockItem) {
        return blockItem.getBlock() instanceof TorchBlock
                || blockItem.getBlock().defaultBlockState().is(EhmTags.DEPTH_LIMITED_LIGHTS);
    }

    public enum DenyReason {
        NONE,
        Y,
        SOFT
    }
}

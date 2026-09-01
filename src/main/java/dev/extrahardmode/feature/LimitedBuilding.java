package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.module.MsgService;
import dev.extrahardmode.world.WorldGate;
import net.fabricmc.fabric.api.event.player.BlockEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public final class LimitedBuilding implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("limited_building");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        bus.listen(UseBlockCallback.EVENT, ID, LimitedBuilding::onUseBlock);
        bus.listen(BlockEvents.USE_ITEM_ON, ID, LimitedBuilding::onUseItemOn);
    }

    static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()
                || !(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (!WorldGate.isModuleActive(serverLevel, ID)
                || !ConfigManager.world(serverLevel).limitedBuilding()
                || EhmApi.playerBypasses(serverPlayer)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return InteractionResult.PASS;
        }
        BlockPlaceContext context = new BlockPlaceContext(new UseOnContext(level, player, hand, stack, hit));
        if (!shouldDeny(level, player, context, blockItem)) {
            return InteractionResult.PASS;
        }
        MsgService.realisticBuilding(serverPlayer);
        return InteractionResult.FAIL;
    }

    static @Nullable InteractionResult onUseItemOn(
            ItemStack stack,
            BlockState clicked,
            Level level,
            BlockPos clickedPos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (level.isClientSide()
                || !(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return null;
        }
        if (!WorldGate.isModuleActive(serverLevel, ID)
                || !ConfigManager.world(serverLevel).limitedBuilding()
                || EhmApi.playerBypasses(serverPlayer)) {
            return null;
        }
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return null;
        }
        BlockPlaceContext context = new BlockPlaceContext(new UseOnContext(level, player, hand, stack, hit));
        if (!shouldDeny(level, player, context, blockItem)) {
            return null;
        }
        MsgService.realisticBuilding(serverPlayer);
        return InteractionResult.FAIL;
    }

    public static boolean shouldDeny(Level level, Player player, BlockPlaceContext context, BlockItem blockItem) {
        BlockState placement = blockItem.getBlock().getStateForPlacement(context);
        if (placement == null || !placement.blocksMotion()) {
            return false;
        }
        BlockPos place = context.getClickedPos();
        BlockPos underFeet = player.blockPosition().below();
        if (PlacementRules.denyPillar(true, player.onGround(), place, underFeet)) {
            return true;
        }
        return PlacementRules.denySkyBridge(true, level, place, PlacementRules.againstBlock(context), underFeet);
    }
}

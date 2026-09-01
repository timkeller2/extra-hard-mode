package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.api.event.HardenedStoneMineEvent;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.item.EhmComponents;
import dev.extrahardmode.network.EhmNetworking;
import dev.extrahardmode.tag.EhmTags;
import dev.extrahardmode.world.WorldGate;
import java.util.List;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class HardenedStone implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("hardened_stone");

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        bus.listen(PlayerBlockBreakEvents.AFTER, ID, HardenedStone::onBreak);
        bus.listen(UseBlockCallback.EVENT, ID, HardenedStone::onUseBlock);
    }

    public static boolean enabled(Level level) {
        return FeatureBus.guard(level, ID)
                && level instanceof ServerLevel serverLevel
                && ConfigManager.world(serverLevel).hardenedEnable();
    }

    public static boolean shouldZeroDestroySpeed(Player player, BlockState state) {
        return deniesUnlistedTool(player, state);
    }

    public static boolean shouldDenyHarvest(Player player, BlockState state) {
        return deniesUnlistedTool(player, state);
    }

    public static boolean blocksPistonMove(Level level, List<BlockPos> toPush, List<BlockPos> toDestroy) {
        if (!enabled(level)) {
            return false;
        }
        if (!ConfigManager.world((ServerLevel) level).blockPistonMove()) {
            return false;
        }
        return anyHardenedOrOre(level, toPush) || anyHardenedOrOre(level, toDestroy);
    }

    public static void drain(ServerPlayer player, ItemStack tool, BlockState state) {
        applyBudget(player, tool, state);
    }

    private static boolean deniesUnlistedTool(Player player, BlockState state) {
        if (!enabled(player.level())) {
            return false;
        }
        if (!state.is(EhmTags.HARDENED)) {
            return false;
        }
        if (player.hasInfiniteMaterials() || player.isCreative()) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer && EhmApi.playerBypasses(serverPlayer)) {
            return false;
        }
        ItemStack tool = player.getMainHandItem();
        WorldConfig config = ConfigManager.world((ServerLevel) player.level());
        if (isListedMiner(tool, config)) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            HardenedStoneMineEvent event = new HardenedStoneMineEvent(serverPlayer, tool, state, 0);
            HardenedStoneMineEvent.EVENT.invoker().onHardenedStoneMine(event);
            if (event.isCanceled() || event.budget() > 0) {
                return false;
            }
        }
        return true;
    }

    private static void onBreak(Level world, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (!(world instanceof ServerLevel level) || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!WorldGate.isModuleActive(level, ID)) {
            return;
        }
        applyBudget(serverPlayer, serverPlayer.getMainHandItem(), state);
    }

    private static InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
        if (!(world instanceof ServerLevel level) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (!WorldGate.isModuleActive(level, ID)) {
            return InteractionResult.PASS;
        }
        WorldConfig config = ConfigManager.world(level);
        if (!config.hardenedEnable() || !config.blockOreNextToStone()) {
            return InteractionResult.PASS;
        }
        if (serverPlayer.hasInfiniteMaterials() || serverPlayer.isCreative() || EhmApi.playerBypasses(serverPlayer)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return InteractionResult.PASS;
        }
        if (!blockItem.getBlock().defaultBlockState().is(EhmTags.CAVE_IN_ORES)) {
            return InteractionResult.PASS;
        }
        BlockPos placedAt = hit.getBlockPos().relative(hit.getDirection());
        if (touchesHardened(level, placedAt)) {
            EhmNetworking.sendToast(serverPlayer, "no_placing_ore_against_stone");
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }

    private static void applyBudget(ServerPlayer player, ItemStack tool, BlockState state) {
        if (!enabled(player.level())) {
            return;
        }
        if (!state.is(EhmTags.HARDENED) || tool.isEmpty()) {
            return;
        }
        if (player.hasInfiniteMaterials() || player.isCreative() || EhmApi.playerBypasses(player)) {
            return;
        }
        WorldConfig config = ConfigManager.world(player.level());
        int budget = budgetFor(tool, config);
        if (budget <= 0) {
            return;
        }
        HardenedStoneMineEvent event = new HardenedStoneMineEvent(player, tool, state, budget);
        HardenedStoneMineEvent.EVENT.invoker().onHardenedStoneMine(event);
        if (event.isCanceled()) {
            return;
        }
        budget = event.budget();
        if (budget <= 0) {
            return;
        }
        int mined = HardenedBudget.increment(tool.getOrDefault(EhmComponents.HARDENED_MINED, 0));
        tool.set(EhmComponents.HARDENED_MINED, mined);
        if (!HardenedBudget.shouldConsume(mined, budget)) {
            return;
        }
        consumeRemaining(tool, player);
    }

    /** Unbreaking must not extend N — empty the stack even if vanilla damage is skipped. */
    private static void consumeRemaining(ItemStack tool, ServerPlayer player) {
        if (tool.isDamageableItem()) {
            int remaining = Math.max(1, tool.getMaxDamage() - tool.getDamageValue());
            tool.hurtAndBreak(remaining, player.level(), player, item -> {});
        }
        if (!tool.isEmpty()) {
            tool.setCount(0);
        }
    }

    private static boolean isListedMiner(ItemStack tool, WorldConfig config) {
        if (tool.isEmpty()) {
            return false;
        }
        if (tool.is(EhmTags.HARDENED_MINER)) {
            return true;
        }
        return budgetFor(tool, config) > 0;
    }

    private static int budgetFor(ItemStack tool, WorldConfig config) {
        return tool.typeHolder()
                .unwrapKey()
                .map(key -> config.hardenedBudget(key.identifier()))
                .orElse(0);
    }

    private static boolean anyHardenedOrOre(Level level, List<BlockPos> positions) {
        if (positions == null) {
            return false;
        }
        for (BlockPos pos : positions) {
            BlockState state = level.getBlockState(pos);
            if (state.is(EhmTags.HARDENED) || state.is(EhmTags.CAVE_IN_ORES)) {
                return true;
            }
        }
        return false;
    }

    private static boolean touchesHardened(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).is(EhmTags.HARDENED)) {
                return true;
            }
        }
        return false;
    }
}

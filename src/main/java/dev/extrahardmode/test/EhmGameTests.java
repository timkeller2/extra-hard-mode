package dev.extrahardmode.test;

import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.feature.HardenedStone;
import dev.extrahardmode.item.EhmComponents;
import dev.extrahardmode.world.ExtraHardModeBootData;
import dev.extrahardmode.world.WorldGate;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.feature.NetherrackFire;
import dev.extrahardmode.feature.Torches;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import dev.extrahardmode.feature.Water;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;

public class EhmGameTests {
    @GameTest
    public void firstApplyPerDimension(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        MinecraftServer server = overworld.getServer();
        boolean toml = ConfigManager.global().enabledByDefault();
        helper.assertValueEqual(
                toml, overworld.getGameRules().get(WorldGate.ENABLED), "overworld gamerule matches TOML");

        WorldGate.applyIfNeeded(server, overworld);
        ExtraHardModeBootData boot = WorldGate.bootData(server);
        if (boot == null) {
            helper.fail("boot SavedData exists");
            return;
        }
        helper.assertTrue(boot.contains(Level.OVERWORLD.identifier()), "boot contains overworld");
        boolean netherAlreadyApplied = boot.contains(Level.NETHER.identifier());
        if (!netherAlreadyApplied) {
            helper.assertFalse(
                    boot.contains(Level.NETHER.identifier()), "boot contains overworld does not imply nether");
        }

        boolean flipped = !overworld.getGameRules().get(WorldGate.ENABLED);
        overworld.getGameRules().set(WorldGate.ENABLED, flipped, server);

        ServerLevel nether = server.getLevel(Level.NETHER);
        if (nether == null) {
            for (ServerLevel level : server.getAllLevels()) {
                if (level.dimension() == Level.NETHER) {
                    nether = level;
                    break;
                }
            }
        }
        if (nether == null) {
            helper.fail("nether not loaded");
            return;
        }

        WorldGate.applyIfNeeded(server, nether);
        helper.assertValueEqual(
                flipped,
                overworld.getGameRules().get(WorldGate.ENABLED),
                "gamerule unchanged after nether first-apply");
        helper.assertTrue(boot.contains(Level.NETHER.identifier()), "nether stamped independently");
        helper.assertTrue(boot.contains(Level.OVERWORLD.identifier()), "overworld still stamped");
        helper.assertTrue(WorldGate.dimensionEnabled(nether), "nether dim flag defaults true (opt-out)");
        helper.succeed();
    }

    @GameTest
    public void woodenPickCannotHarvestHardened(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        Player player = helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WOODEN_PICKAXE));
        helper.assertTrue(
                player.getDestroySpeed(Blocks.STONE.defaultBlockState()) == 0.0F, "wooden pick destroy speed 0 on stone");
        helper.assertFalse(player.hasCorrectToolForDrops(Blocks.STONE.defaultBlockState()), "wooden pick cannot harvest stone");
        helper.assertTrue(
                player.getDestroySpeed(Blocks.TUFF.defaultBlockState()) == 0.0F, "wooden pick destroy speed 0 on tuff");
        helper.succeed();
    }

    @GameTest
    public void ironPickBreaksAfter128Hardened(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        Player mock = helper.makeMockServerPlayer(GameType.SURVIVAL);
        if (!(mock instanceof ServerPlayer player)) {
            helper.fail("mock server player");
            return;
        }
        ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
        player.setItemInHand(InteractionHand.MAIN_HAND, pick);
        for (int i = 0; i < 127; i++) {
            HardenedStone.drain(player, pick, Blocks.STONE.defaultBlockState());
            helper.assertFalse(pick.isEmpty(), "iron pick survives break " + (i + 1));
        }
        helper.assertValueEqual(127, pick.getOrDefault(EhmComponents.HARDENED_MINED, 0), "127 hardened breaks counted");
        HardenedStone.drain(player, pick, Blocks.STONE.defaultBlockState());
        helper.assertTrue(pick.isEmpty(), "iron pick consumed on 128th hardened break");
    public void torchDenyBelowY0(GameTestHelper helper) {
        ConfigManager.world(level).setEnabled(true);
        BlockPos abs = helper.absolutePos(BlockPos.ZERO);
        BlockPos stone = new BlockPos(abs.getX(), -2, abs.getZ());
        level.setBlock(stone, Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(stone.above(), Blocks.AIR.defaultBlockState(), 3);
        if (!(helper.makeMockServerPlayer(GameType.SURVIVAL) instanceof ServerPlayer player)) {
            helper.fail("expected ServerPlayer");
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(stone).add(0, 0.5, 0), Direction.UP, stone, false);
        ItemStack torch = new ItemStack(Items.TORCH);
        BlockPlaceContext torchContext =
                new BlockPlaceContext(new UseOnContext(level, player, InteractionHand.MAIN_HAND, torch, hit));
                torch.getItem() instanceof BlockItem blockItem
                        && Torches.shouldDeny(level, torchContext, blockItem),
                "torch denied below Y=0");
        ItemStack redstone = new ItemStack(Items.REDSTONE_TORCH);
        BlockPlaceContext redstoneContext =
                new BlockPlaceContext(new UseOnContext(level, player, InteractionHand.MAIN_HAND, redstone, hit));
        helper.assertFalse(
                redstone.getItem() instanceof BlockItem redstoneItem
                        && Torches.shouldDeny(level, redstoneContext, redstoneItem),
                "redstone torch allowed below Y=0");
        BlockPos stoneAtMinus1 = new BlockPos(abs.getX(), -1, abs.getZ());
        level.setBlock(stoneAtMinus1, Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(stoneAtMinus1.above(), Blocks.AIR.defaultBlockState(), 3);
        BlockHitResult hitAtY0 =
                new BlockHitResult(Vec3.atCenterOf(stoneAtMinus1).add(0, 0.5, 0), Direction.UP, stoneAtMinus1, false);
        BlockPlaceContext y0Context =
                new BlockPlaceContext(new UseOnContext(level, player, InteractionHand.MAIN_HAND, torch, hitAtY0));
                torch.getItem() instanceof BlockItem y0Item && Torches.shouldDeny(level, y0Context, y0Item),
                "torch allowed at Y=0");
        WorldConfig config = ConfigManager.world(level);
        boolean previousYDeny = config.torchYDeny();
        config.setTorchYDeny(false);
                torch.getItem() instanceof BlockItem disabledItem
                        && Torches.shouldDeny(level, torchContext, disabledItem),
                "torch Y deny disabled with enable boolean");
        config.setTorchYDeny(previousYDeny);
        helper.succeed();
    }

    @GameTest
    public void ironPickWithUnbreakingBreaksAfter128(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        Player mock = helper.makeMockServerPlayer(GameType.SURVIVAL);
        if (!(mock instanceof ServerPlayer player)) {
            helper.fail("mock server player");
            return;
        }
        ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
        var unbreaking = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.UNBREAKING);
        pick.enchant(unbreaking, 3);
        helper.assertTrue(pick.isEnchanted(), "Unbreaking III applied");
        player.setItemInHand(InteractionHand.MAIN_HAND, pick);
        for (int i = 0; i < 127; i++) {
            HardenedStone.drain(player, pick, Blocks.STONE.defaultBlockState());
            helper.assertFalse(pick.isEmpty(), "Unbreaking iron pick survives break " + (i + 1));
        }
        HardenedStone.drain(player, pick, Blocks.STONE.defaultBlockState());
        helper.assertTrue(pick.isEmpty(), "Unbreaking III does not extend N; 128th break consumes pick");
    public void netherrackFireOnNetherrackBelow(GameTestHelper helper) {
        WorldConfig config = ConfigManager.world(level);
        config.setEnabled(true);
        int previous = config.netherrackFirePercent();
        config.setNetherrackFirePercent(100);

        helper.setBlock(new BlockPos(1, 1, 1), Blocks.NETHERRACK);
        helper.setBlock(new BlockPos(1, 2, 1), Blocks.AIR);
        BlockPos emptyOnNetherrack = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.assertTrue(
                NetherrackFire.tryIgnite(level, null, emptyOnNetherrack), "fire in empty cell above remaining netherrack");
        helper.assertBlockPresent(Blocks.FIRE, new BlockPos(1, 2, 1));
        helper.setBlock(new BlockPos(3, 1, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 2, 1), Blocks.AIR);
        helper.assertFalse(
                NetherrackFire.tryIgnite(level, null, helper.absolutePos(new BlockPos(3, 2, 1))),
                "no fire when block below is not netherrack");
        helper.assertBlockNotPresent(Blocks.FIRE, new BlockPos(3, 2, 1));
        config.setNetherrackFirePercent(previous);
        helper.succeed();
    public void waterFromBucketIsNotSource(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 2, 1);
        helper.setBlock(rel.below(), Blocks.STONE);
        helper.setBlock(rel, Blocks.AIR);
        BlockPos abs = helper.absolutePos(rel);
        BucketItem bucket = (BucketItem) Items.WATER_BUCKET;
        helper.assertTrue(bucket.emptyContents(null, level, abs, null), "emptied water bucket");
        FluidState immediately = level.getFluidState(abs);
        helper.assertTrue(immediately.is(FluidTags.WATER), "bucket placed water");
        helper.assertFalse(immediately.isSource(), "water from bucket is not a source");
        helper.assertValueEqual(
                1, helper.getBlockState(rel).getValue(LiquidBlock.LEVEL), "first write is block LEVEL=1 not 7");
        helper.assertTrue(Water.isMarked(level, abs), "placed water is marked non-source");
        BlockPos slabRel = new BlockPos(2, 2, 1);
        helper.setBlock(slabRel, Blocks.STONE_SLAB.defaultBlockState());
                bucket.emptyContents(null, level, helper.absolutePos(slabRel), null), "emptied onto slab");
        BlockState slab = helper.getBlockState(slabRel);
        helper.assertTrue(slab.getBlock() == Blocks.STONE_SLAB, "slab was not replaced with water");
        helper.assertTrue(slab.getValue(BlockStateProperties.WATERLOGGED), "slab stays waterlogged");
        helper.runAfterDelay(2, () -> {
            FluidState later = level.getFluidState(abs);
            helper.assertFalse(later.isSource(), "still not a source after ticks");
            helper.assertValueEqual(1, helper.getBlockState(rel).getValue(LiquidBlock.LEVEL), "still LEVEL=1");
            helper.succeed();
        });
    }
}

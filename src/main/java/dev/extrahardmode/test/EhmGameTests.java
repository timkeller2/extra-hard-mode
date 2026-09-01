package dev.extrahardmode.test;

import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.feature.NetherrackFire;
import dev.extrahardmode.feature.Torches;
import dev.extrahardmode.world.ExtraHardModeBootData;
import dev.extrahardmode.world.WorldGate;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

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
    public void torchDenyBelowY0(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        ConfigManager.world(level).setEnabled(true);

        BlockPos abs = helper.absolutePos(BlockPos.ZERO);
        BlockPos stone = new BlockPos(abs.getX(), -2, abs.getZ());
        level.setBlock(stone, Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(stone.above(), Blocks.AIR.defaultBlockState(), 3);

        if (!(helper.makeMockServerPlayer(GameType.SURVIVAL) instanceof ServerPlayer player)) {
            helper.fail("expected ServerPlayer");
            return;
        }
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(stone).add(0, 0.5, 0), Direction.UP, stone, false);

        ItemStack torch = new ItemStack(Items.TORCH);
        BlockPlaceContext torchContext =
                new BlockPlaceContext(new UseOnContext(level, player, InteractionHand.MAIN_HAND, torch, hit));
        helper.assertTrue(
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
        helper.assertFalse(
                torch.getItem() instanceof BlockItem y0Item && Torches.shouldDeny(level, y0Context, y0Item),
                "torch allowed at Y=0");

        WorldConfig config = ConfigManager.world(level);
        boolean previousYDeny = config.torchYDeny();
        config.setTorchYDeny(false);
        helper.assertFalse(
                torch.getItem() instanceof BlockItem disabledItem
                        && Torches.shouldDeny(level, torchContext, disabledItem),
                "torch Y deny disabled with enable boolean");
        config.setTorchYDeny(previousYDeny);
        helper.succeed();
    }

    @GameTest
    public void netherrackFireOnNetherrackBelow(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
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
    }
}

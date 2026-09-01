package dev.extrahardmode.test;

import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.feature.Water;
import dev.extrahardmode.world.ExtraHardModeBootData;
import dev.extrahardmode.world.WorldGate;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
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
    public void waterFromBucketIsNotSource(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        ConfigManager.world(level).setEnabled(true);
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
        helper.assertTrue(
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

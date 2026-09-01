package dev.extrahardmode.test;

import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.feature.CaveIns;
import dev.extrahardmode.feature.FallingBlocks;
import dev.extrahardmode.feature.HardenedStone;
import dev.extrahardmode.feature.RealisticChopping;
import dev.extrahardmode.item.EhmComponents;
import dev.extrahardmode.module.PhysicsQueue;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.tag.EhmTags;
import dev.extrahardmode.world.ExtraHardModeBootData;
import dev.extrahardmode.world.PhysicsSkip;
import dev.extrahardmode.world.WorldGate;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.phys.AABB;

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
        helper.succeed();
    }

    @GameTest
    public void coalOreCaveIn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        BlockPos ore = new BlockPos(2, 3, 2);
        helper.setBlock(ore, Blocks.COAL_ORE);
        for (Direction direction : Direction.values()) {
            helper.setBlock(ore.relative(direction), Blocks.STONE);
        }
        BlockPos abs = helper.absolutePos(ore);
        level.setBlock(abs, Blocks.AIR.defaultBlockState(), 3);
        CaveIns.onOreBroken(level, abs, Blocks.COAL_ORE.defaultBlockState());
        PhysicsQueue.tick(level);
        helper.succeedWhen(() -> {
            helper.assertTrue(caveInProducedCobble(helper, ore), "coal-ore cave-in softened stone to cobble");
        });
    }

    @GameTest
    public void deepDarkSkipsCaveIn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        helper.setBiome(Biomes.DEEP_DARK);
        BlockPos ore = new BlockPos(2, 3, 2);
        helper.setBlock(ore, Blocks.COAL_ORE);
        for (Direction direction : Direction.values()) {
            helper.setBlock(ore.relative(direction), Blocks.STONE);
        }
        BlockPos abs = helper.absolutePos(ore);
        level.setBlock(abs, Blocks.AIR.defaultBlockState(), 3);
        CaveIns.onOreBroken(level, abs, Blocks.COAL_ORE.defaultBlockState());
        PhysicsQueue.tick(level);
        helper.runAfterDelay(5, () -> {
            for (Direction direction : Direction.values()) {
                helper.assertBlockPresent(Blocks.STONE, ore.relative(direction));
            }
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 200, padding = 8)
    public void copperBlobNoTickTimeout(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        int droppedBefore = PhysicsQueue.of(level).dropped();
        for (int x = 1; x <= 4; x++) {
            for (int z = 1; z <= 4; z++) {
                BlockPos rel = new BlockPos(x, 2, z);
                helper.setBlock(rel, Blocks.COPPER_ORE);
                helper.setBlock(rel.above(), Blocks.STONE);
                helper.setBlock(rel.below(), Blocks.STONE);
            }
        }
        for (int x = 1; x <= 4; x++) {
            for (int z = 1; z <= 4; z++) {
                BlockPos rel = new BlockPos(x, 2, z);
                BlockPos abs = helper.absolutePos(rel);
                level.setBlock(abs, Blocks.AIR.defaultBlockState(), 3);
                CaveIns.onOreBroken(level, abs, Blocks.COPPER_ORE.defaultBlockState());
            }
        }
        helper.succeedWhen(() -> {
            helper.assertTrue(PhysicsQueue.of(level).queueDepth() == 0, "copper blob physics queue drained");
            helper.assertTrue(
                    PhysicsQueue.of(level).dropped() == droppedBefore,
                    "16-block copper blob must not overflow physics queue");
        });
    }

    private static boolean caveInProducedCobble(GameTestHelper helper, BlockPos ore) {
        for (Direction direction : Direction.values()) {
            if (helper.getBlockState(ore.relative(direction)).is(Blocks.COBBLESTONE)) {
                return true;
            }
        }
        ServerLevel level = helper.getLevel();
        AABB box = new AABB(helper.absolutePos(ore)).inflate(3.0);
        for (FallingBlockEntity falling : level.getEntities(EntityTypes.FALLING_BLOCK, box, entity -> true)) {
            if (falling.getBlockState().is(Blocks.COBBLESTONE)) {
                return true;
            }
        }
        return false;
    }

    @GameTest
    public void convertDoesNotOverwriteUnrelatedBlock(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        BlockPos rel = new BlockPos(2, 2, 2);
        helper.setBlock(rel, Blocks.STONE);
        BlockPos abs = helper.absolutePos(rel);
        PhysicsQueue.of(level)
                .enqueueConvert(
                        level,
                        abs,
                        Blocks.STONE.defaultBlockState(),
                        Blocks.COBBLESTONE.defaultBlockState(),
                        true);
        helper.setBlock(rel, Blocks.CHEST);
        PhysicsQueue.tick(level);
        helper.assertBlockPresent(Blocks.CHEST, rel);
        helper.succeed();
    }

    @GameTest
    public void fallingDamageGatedToEhmTags(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        BlockPos rel = new BlockPos(2, 4, 2);
        helper.setBlock(rel, Blocks.STONE);
        BlockPos abs = helper.absolutePos(rel);

        FallingBlockEntity cobble = FallingBlockEntity.fall(level, abs, Blocks.COBBLESTONE.defaultBlockState());
        cobble.setAttached(EhmAttachments.EHM_OURS, Boolean.TRUE);
        helper.assertTrue(FallingBlocks.appliesFallDamage(cobble), "EHM cobble is gated in");

        helper.setBlock(rel, Blocks.STONE);
        FallingBlockEntity sand = FallingBlockEntity.fall(level, abs, Blocks.SAND.defaultBlockState());
        helper.assertFalse(FallingBlocks.appliesFallDamage(sand), "sand/gravel stay vanilla (no extra)");

        helper.setBlock(rel, Blocks.STONE);
        FallingBlockEntity anvil = FallingBlockEntity.fall(level, abs, Blocks.ANVIL.defaultBlockState());
        helper.assertFalse(FallingBlocks.appliesFallDamage(anvil), "anvil stays vanilla");
        helper.assertTrue(FallingBlocks.isVanillaFallDamage(anvil.getBlockState()), "anvil is vanilla fall-damage");
        helper.succeed();
    }

    @GameTest
    public void fallingCobbleDealsDamageAfterRealFall(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        Player mock = helper.makeMockServerPlayer(GameType.SURVIVAL);
        if (!(mock instanceof ServerPlayer player)) {
            helper.fail("mock server player");
            return;
        }
        BlockPos rel = new BlockPos(2, 3, 2);
        helper.setBlock(rel.below(), Blocks.STONE);
        BlockPos abs = helper.absolutePos(rel);
        player.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        FallingBlockEntity cobble = FallingBlockEntity.fall(level, abs.above(4), Blocks.COBBLESTONE.defaultBlockState());
        cobble.setAttached(EhmAttachments.EHM_OURS, Boolean.TRUE);
        cobble.setHurtsEntities(2.0F, 2);
        cobble.snapTo(player.getX(), player.getY(), player.getZ());
        float before = player.getHealth();
        cobble.causeFallDamage(4.0, 1.0F, cobble.damageSources().fallingBlock(cobble));
        helper.assertTrue(player.getHealth() <= before - 2.0F + 0.001F, "real cobble fall deals 2");
        helper.succeed();
    }

    @GameTest
    public void physicsProtectedStructuresTagContainsAncientCity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var tag = structures.get(EhmTags.PHYSICS_PROTECTED_STRUCTURES);
        helper.assertTrue(tag.isPresent(), "#physics_protected_structures loaded");
        boolean ancient = tag.get().stream().anyMatch(holder -> holder.is(BuiltinStructures.ANCIENT_CITY));
        boolean trials = tag.get().stream().anyMatch(holder -> holder.is(BuiltinStructures.TRIAL_CHAMBERS));
        helper.assertTrue(ancient, "tag includes minecraft:ancient_city");
        helper.assertTrue(trials, "tag includes minecraft:trial_chambers");
        BlockPos abs = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.assertFalse(
                PhysicsSkip.never(level, abs) && !level.getBiome(abs).is(EhmTags.NO_PHYSICS),
                "empty test platform is not a protected structure piece");
        helper.succeed();
    }

    @GameTest(maxTicks = 40, padding = 16)
    public void oakTreeFalls(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        Player mock = helper.makeMockServerPlayer(GameType.SURVIVAL);
        BlockPos base = new BlockPos(2, 1, 2);
        for (int y = 1; y <= 5; y++) {
            helper.setBlock(base.above(y - 1), Blocks.OAK_LOG);
        }
        helper.setBlock(new BlockPos(2, 5, 1), Blocks.OAK_LEAVES);
        helper.setBlock(new BlockPos(2, 5, 3), Blocks.OAK_LEAVES);
        helper.setBlock(new BlockPos(1, 5, 2), Blocks.OAK_LEAVES);
        helper.setBlock(new BlockPos(3, 5, 2), Blocks.OAK_LEAVES);
        BlockPos[] remaining = remainingColumn(base, 5);
        breakAndFell(helper, mock, base, Blocks.OAK_LOG.defaultBlockState());
        assertNoPlacedLogs(helper, Blocks.OAK_LOG, remaining);
        helper.runAfterDelay(2, () -> {
            assertNoPlacedLogs(helper, Blocks.OAK_LOG, base.above(4));
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 40, padding = 16)
    public void jungleTwoByTwoFalls(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        Player mock = helper.makeMockServerPlayer(GameType.SURVIVAL);
        BlockPos[] columns = {
            new BlockPos(2, 1, 2), new BlockPos(3, 1, 2), new BlockPos(2, 1, 3), new BlockPos(3, 1, 3)
        };
        for (BlockPos column : columns) {
            for (int y = 0; y < 6; y++) {
                helper.setBlock(column.above(y), Blocks.JUNGLE_LOG);
            }
        }
        helper.setBlock(new BlockPos(2, 6, 1), Blocks.JUNGLE_LEAVES);
        helper.setBlock(new BlockPos(3, 6, 1), Blocks.JUNGLE_LEAVES);
        helper.setBlock(new BlockPos(1, 6, 2), Blocks.JUNGLE_LEAVES);
        helper.setBlock(new BlockPos(1, 6, 3), Blocks.JUNGLE_LEAVES);
        BlockPos[] remaining = remainingTwoByTwo(columns[0], columns);
        BlockPos[] crown = {columns[0].above(5), columns[1].above(5), columns[2].above(5), columns[3].above(5)};
        breakAndFell(helper, mock, columns[0], Blocks.JUNGLE_LOG.defaultBlockState());
        assertNoPlacedLogs(helper, Blocks.JUNGLE_LOG, remaining);
        helper.runAfterDelay(2, () -> {
            assertNoPlacedLogs(helper, Blocks.JUNGLE_LOG, crown);
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 40, padding = 16)
    public void acaciaBendFalls(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        Player mock = helper.makeMockServerPlayer(GameType.SURVIVAL);
        BlockPos base = new BlockPos(2, 1, 2);
        helper.setBlock(base, Blocks.ACACIA_LOG);
        helper.setBlock(base.above(), Blocks.ACACIA_LOG);
        helper.setBlock(new BlockPos(3, 2, 2), Blocks.ACACIA_LOG);
        helper.setBlock(new BlockPos(4, 2, 2), Blocks.ACACIA_LOG);
        helper.setBlock(new BlockPos(4, 2, 1), Blocks.ACACIA_LEAVES);
        helper.setBlock(new BlockPos(4, 2, 3), Blocks.ACACIA_LEAVES);
        helper.setBlock(new BlockPos(4, 3, 2), Blocks.ACACIA_LEAVES);
        helper.setBlock(new BlockPos(5, 2, 2), Blocks.ACACIA_LEAVES);
        BlockPos[] remaining = {base.above(), new BlockPos(3, 2, 2), new BlockPos(4, 2, 2)};
        breakAndFell(helper, mock, base, Blocks.ACACIA_LOG.defaultBlockState());
        assertNoPlacedLogs(helper, Blocks.ACACIA_LOG, remaining);
        helper.runAfterDelay(2, () -> {
            assertNoPlacedLogs(helper, Blocks.ACACIA_LOG, remaining);
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 40, padding = 16)
    public void logPillarWithNearbyLeafDoesNotFell(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        Player mock = helper.makeMockServerPlayer(GameType.SURVIVAL);
        BlockPos base = new BlockPos(2, 1, 2);
        for (int y = 1; y <= 8; y++) {
            helper.setBlock(base.above(y - 1), Blocks.OAK_LOG);
        }
        helper.setBlock(new BlockPos(4, 4, 2), Blocks.OAK_LEAVES);
        breakAndFell(helper, mock, base, Blocks.OAK_LOG.defaultBlockState());
        helper.runAfterDelay(8, () -> {
            for (int y = 2; y <= 8; y++) {
                helper.assertBlockPresent(Blocks.OAK_LOG, base.above(y - 1));
            }
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 40, padding = 16)
    public void threeAdjacentLeavesDoNotFell(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        Player mock = helper.makeMockServerPlayer(GameType.SURVIVAL);
        BlockPos base = new BlockPos(2, 1, 2);
        for (int y = 1; y <= 5; y++) {
            helper.setBlock(base.above(y - 1), Blocks.OAK_LOG);
        }
        helper.setBlock(new BlockPos(2, 5, 1), Blocks.OAK_LEAVES);
        helper.setBlock(new BlockPos(2, 5, 3), Blocks.OAK_LEAVES);
        helper.setBlock(new BlockPos(1, 5, 2), Blocks.OAK_LEAVES);
        breakAndFell(helper, mock, base, Blocks.OAK_LOG.defaultBlockState());
        helper.runAfterDelay(8, () -> {
            for (int y = 2; y <= 5; y++) {
                helper.assertBlockPresent(Blocks.OAK_LOG, base.above(y - 1));
            }
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 40, padding = 16)
    public void worldGateOffDoesNotFell(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, false, level.getServer());
        Player mock = helper.makeMockServerPlayer(GameType.SURVIVAL);
        BlockPos base = new BlockPos(2, 1, 2);
        for (int y = 1; y <= 5; y++) {
            helper.setBlock(base.above(y - 1), Blocks.OAK_LOG);
        }
        helper.setBlock(new BlockPos(2, 5, 1), Blocks.OAK_LEAVES);
        helper.setBlock(new BlockPos(2, 5, 3), Blocks.OAK_LEAVES);
        helper.setBlock(new BlockPos(1, 5, 2), Blocks.OAK_LEAVES);
        helper.setBlock(new BlockPos(3, 5, 2), Blocks.OAK_LEAVES);
        breakAndFell(helper, mock, base, Blocks.OAK_LOG.defaultBlockState());
        helper.runAfterDelay(8, () -> {
            for (int y = 2; y <= 5; y++) {
                helper.assertBlockPresent(Blocks.OAK_LOG, base.above(y - 1));
            }
            helper.succeed();
        });
    }

    @GameTest
    public void fallingLogDealsGatedDamage(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        BlockPos rel = new BlockPos(2, 4, 2);
        helper.setBlock(rel, Blocks.STONE);
        BlockPos abs = helper.absolutePos(rel);
        FallingBlockEntity log = FallingBlockEntity.fall(level, abs, Blocks.OAK_LOG.defaultBlockState());
        log.setAttached(EhmAttachments.EHM_OURS, Boolean.TRUE);
        helper.assertTrue(FallingBlocks.appliesFallDamage(log), "EHM oak log is gated in");
        helper.succeed();
    }

    @GameTest(maxTicks = 40, padding = 8)
    public void netherStemDoesNotFell(GameTestHelper helper) {
        helper.assertFalse(
                Blocks.CRIMSON_STEM.defaultBlockState().is(EhmTags.FELLABLE_LOGS), "crimson stem is not fellable");
        helper.assertFalse(
                Blocks.WARPED_STEM.defaultBlockState().is(EhmTags.FELLABLE_LOGS), "warped stem is not fellable");
        helper.assertFalse(
                Blocks.CRIMSON_HYPHAE.defaultBlockState().is(EhmTags.FELLABLE_LOGS), "crimson hyphae is not fellable");
        helper.assertFalse(
                Blocks.WARPED_HYPHAE.defaultBlockState().is(EhmTags.FELLABLE_LOGS), "warped hyphae is not fellable");
        helper.assertTrue(Blocks.OAK_LOG.defaultBlockState().is(EhmTags.FELLABLE_LOGS), "oak log is fellable");
        helper.assertTrue(Blocks.JUNGLE_LOG.defaultBlockState().is(EhmTags.FELLABLE_LOGS), "jungle log is fellable");

        ServerLevel level = helper.getLevel();
        level.getGameRules().set(WorldGate.ENABLED, true, level.getServer());
        Player mock = helper.makeMockServerPlayer(GameType.SURVIVAL);
        BlockPos base = new BlockPos(2, 1, 2);
        for (int y = 1; y <= 5; y++) {
            helper.setBlock(base.above(y - 1), Blocks.CRIMSON_STEM);
        }
        helper.setBlock(new BlockPos(2, 5, 1), Blocks.JUNGLE_LEAVES);
        helper.setBlock(new BlockPos(2, 5, 3), Blocks.JUNGLE_LEAVES);
        helper.setBlock(new BlockPos(1, 5, 2), Blocks.JUNGLE_LEAVES);
        helper.setBlock(new BlockPos(3, 5, 2), Blocks.JUNGLE_LEAVES);
        breakAndFell(helper, mock, base, Blocks.CRIMSON_STEM.defaultBlockState());
        helper.runAfterDelay(8, () -> {
            for (int y = 2; y <= 5; y++) {
                helper.assertBlockPresent(Blocks.CRIMSON_STEM, base.above(y - 1));
            }
            helper.succeed();
        });
    }

    private static void breakAndFell(GameTestHelper helper, Player player, BlockPos rel, BlockState broken) {
        ServerLevel level = helper.getLevel();
        BlockPos abs = helper.absolutePos(rel);
        helper.setBlock(rel, Blocks.AIR);
        RealisticChopping.tryFell(level, player, abs, broken);
        PhysicsQueue.tick(level);
    }

    private static BlockPos[] remainingColumn(BlockPos base, int height) {
        BlockPos[] column = new BlockPos[height - 1];
        for (int i = 1; i < height; i++) {
            column[i - 1] = base.above(i);
        }
        return column;
    }

    private static BlockPos[] remainingTwoByTwo(BlockPos broken, BlockPos[] bases) {
        int height = 6;
        BlockPos[] remaining = new BlockPos[bases.length * height - 1];
        int i = 0;
        for (BlockPos column : bases) {
            for (int y = 0; y < height; y++) {
                BlockPos cell = column.above(y);
                if (cell.equals(broken)) {
                    continue;
                }
                remaining[i++] = cell;
            }
        }
        return remaining;
    }

    private static void assertNoPlacedLogs(GameTestHelper helper, Block log, BlockPos... cells) {
        ServerLevel level = helper.getLevel();
        for (BlockPos rel : cells) {
            helper.assertFalse(helper.getBlockState(rel).is(log), "placed log remaining at " + rel);
            BlockPos abs = helper.absolutePos(rel);
            boolean fallingHere = false;
            AABB box = new AABB(abs).inflate(0.1);
            for (FallingBlockEntity falling : level.getEntities(EntityTypes.FALLING_BLOCK, box, entity -> true)) {
                if (falling.getBlockState().is(log)) {
                    fallingHere = true;
                    break;
                }
            }
            helper.assertTrue(
                    helper.getBlockState(rel).isAir() || fallingHere,
                    "converted log must be air or a falling entity at " + rel);
        }
    }
}

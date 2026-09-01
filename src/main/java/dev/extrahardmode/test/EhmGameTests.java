package dev.extrahardmode.test;

import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.feature.CaveIns;
import dev.extrahardmode.feature.HardenedStone;
import dev.extrahardmode.item.EhmComponents;
import dev.extrahardmode.module.PhysicsQueue;
import dev.extrahardmode.world.ExtraHardModeBootData;
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
import net.minecraft.world.level.block.Blocks;
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
}

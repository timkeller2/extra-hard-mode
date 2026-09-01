package dev.extrahardmode.test;

import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.feature.monster.Silverfish;
import dev.extrahardmode.feature.monster.Skeletons;
import dev.extrahardmode.module.SpawnReplaceService;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.ExtraHardModeBootData;
import dev.extrahardmode.world.WorldGate;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;

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
    public void spawnProcessedSurvivesChunkReload(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = new BlockPos(1, 2, 1);
        Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, pos, EntitySpawnReason.COMMAND);
        zombie.setPersistenceRequired();
        zombie.setAttached(EhmAttachments.EHM_SPAWN_PROCESSED, true);
        UUID id = zombie.getUUID();
        ChunkPos chunkPos = ChunkPos.containing(zombie.blockPosition());
        level.getChunkSource().save(true);

        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        if (!zombie.save(output)) {
            helper.fail("processed zombie did not save");
            return;
        }
        CompoundTag tag = output.buildResult();
        zombie.discard();

        ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag);
        Entity loaded = EntityType.loadEntityRecursive(input, level, EntitySpawnReason.LOAD, entity -> {
            if (!level.addFreshEntity(entity)) {
                return null;
            }
            return entity;
        });
        if (!(loaded instanceof Zombie reloaded)) {
            helper.fail("reloaded entity was not a zombie");
            return;
        }
        helper.assertValueEqual(id, reloaded.getUUID(), "uuid");
        helper.assertTrue(
                Boolean.TRUE.equals(reloaded.getAttached(EhmAttachments.EHM_SPAWN_PROCESSED)),
                "spawn_processed persisted across chunk reload");
        helper.assertTrue(
                level.getChunkSource().getChunkNow(chunkPos.x(), chunkPos.z()) != null, "chunk reloaded");
        SpawnReplaceService.replaceIfNeeded(reloaded, level, EntitySpawnReason.NATURAL);
        helper.assertTrue(reloaded.getType() == EntityTypes.ZOMBIE && !reloaded.isRemoved(), "not replaced again");
        helper.assertEntityNotPresent(EntityTypes.WITCH);
        helper.succeed();
    }

    @GameTest
    public void spawnReplaceSkippedWhenWorldGateInactive(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        boolean previous = level.getGameRules().get(WorldGate.ENABLED);
        level.getGameRules().set(WorldGate.ENABLED, false, server);
        try {
            Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1), EntitySpawnReason.NATURAL);
            SpawnReplaceService.replaceIfNeeded(zombie, level, EntitySpawnReason.NATURAL);
            helper.assertFalse(
                    Boolean.TRUE.equals(
                            zombie.getAttachedOrElse(EhmAttachments.EHM_SPAWN_PROCESSED, Boolean.FALSE)),
                    "spawn_processed not stamped when inactive");
            helper.assertTrue(zombie.getType() == EntityTypes.ZOMBIE, "zombie not replaced when inactive");
            helper.succeed();
        } finally {
            level.getGameRules().set(WorldGate.ENABLED, previous, server);
        }
    }

    @GameTest
    public void skeletonSpecialShooterFilter(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.assertTrue(
                Skeletons.isSpecialShooter(helper.spawn(EntityTypes.SKELETON, pos, EntitySpawnReason.COMMAND)),
                "skeleton is special shooter");
        helper.assertTrue(
                Skeletons.isSpecialShooter(helper.spawn(EntityTypes.BOGGED, pos, EntitySpawnReason.COMMAND)),
                "bogged shares skeleton table");
        helper.assertFalse(
                Skeletons.isSpecialShooter(helper.spawn(EntityTypes.STRAY, pos, EntitySpawnReason.COMMAND)),
                "stray excluded");
        helper.assertFalse(
                Skeletons.isSpecialShooter(helper.spawn(EntityTypes.WITHER_SKELETON, pos, EntitySpawnReason.COMMAND)),
                "wither skeleton excluded");
        helper.assertFalse(
                Skeletons.isSpecialShooter(helper.spawn(EntityTypes.PARCHED, pos, EntitySpawnReason.COMMAND)),
                "parched excluded");
        helper.succeed();
    }

    @GameTest
    public void silverfishDropsCobble(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        boolean previous = level.getGameRules().get(WorldGate.ENABLED);
        level.getGameRules().set(WorldGate.ENABLED, true, server);
        try {
            BlockPos pos = new BlockPos(1, 2, 1);
            var fish = helper.spawn(EntityTypes.SILVERFISH, pos, EntitySpawnReason.COMMAND);
            helper.kill(fish);
            helper.succeedWhen(() -> helper.assertItemEntityPresent(net.minecraft.world.item.Items.COBBLESTONE, pos, 3.0));
        } finally {
            level.getGameRules().set(WorldGate.ENABLED, previous, server);
        }
    }

    @GameTest
    public void silverfishDoesNotEnterStoneWhenActive(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        boolean previous = level.getGameRules().get(WorldGate.ENABLED);
        level.getGameRules().set(WorldGate.ENABLED, true, server);
        try {
            helper.assertTrue(Silverfish.cantEnterBlocks(level), "cantEnterBlocks when WorldGate on");
            var fish = helper.spawn(EntityTypes.SILVERFISH, new BlockPos(2, 2, 2), EntitySpawnReason.COMMAND);
            BlockPos hostPos = hostBeside(fish, Direction.NORTH);
            level.setBlock(hostPos, Blocks.STONE.defaultBlockState(), 3);
            invokeMergeStart(fish, Direction.NORTH);
            helper.assertTrue(level.getBlockState(hostPos).is(Blocks.STONE), "host not infested when active");
            helper.succeed();
        } catch (ReflectiveOperationException e) {
            helper.fail("merge goal reflect: " + e.getMessage());
        } finally {
            level.getGameRules().set(WorldGate.ENABLED, previous, server);
        }
    }

    @GameTest
    public void silverfishEntersStoneWhenWorldGateOff(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        boolean previous = level.getGameRules().get(WorldGate.ENABLED);
        level.getGameRules().set(WorldGate.ENABLED, false, server);
        try {
            helper.assertFalse(Silverfish.cantEnterBlocks(level), "cantEnterBlocks off when WorldGate off");
            var fish = helper.spawn(EntityTypes.SILVERFISH, new BlockPos(2, 2, 2), EntitySpawnReason.COMMAND);
            BlockPos hostPos = hostBeside(fish, Direction.NORTH);
            level.setBlock(hostPos, Blocks.STONE.defaultBlockState(), 3);
            invokeMergeStart(fish, Direction.NORTH);
            helper.assertTrue(
                    level.getBlockState(hostPos).is(Blocks.INFESTED_STONE), "vanilla infest when WorldGate off");
            helper.succeed();
        } catch (ReflectiveOperationException e) {
            helper.fail("merge goal reflect: " + e.getMessage());
        } finally {
            level.getGameRules().set(WorldGate.ENABLED, previous, server);
        }
    }

    private static BlockPos hostBeside(net.minecraft.world.entity.monster.Silverfish fish, Direction direction) {
        return BlockPos.containing(fish.getX(), fish.getY() + 0.5, fish.getZ()).relative(direction);
    }

    private static void invokeMergeStart(net.minecraft.world.entity.monster.Silverfish fish, Direction direction)
            throws ReflectiveOperationException {
        Class<?> goalClass = Class.forName("net.minecraft.world.entity.monster.Silverfish$SilverfishMergeWithStoneGoal");
        var ctor = goalClass.getDeclaredConstructor(net.minecraft.world.entity.monster.Silverfish.class);
        ctor.setAccessible(true);
        Object goal = ctor.newInstance(fish);
        var doMerge = goalClass.getDeclaredField("doMerge");
        doMerge.setAccessible(true);
        doMerge.setBoolean(goal, true);
        var selected = goalClass.getDeclaredField("selectedDirection");
        selected.setAccessible(true);
        selected.set(goal, direction);
        goalClass.getMethod("start").invoke(goal);
    }
}

package dev.extrahardmode.test;

import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.module.SpawnReplaceService;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.ExtraHardModeBootData;
import dev.extrahardmode.world.WorldGate;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
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
}

package dev.extrahardmode.test;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.world.ExtraHardModeBootData;
import dev.extrahardmode.world.WorldGate;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Fabric GameTests. DESIGN PR 13 acceptance contract is documented in README.md.
 */
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
    public void gameruleOffIsNoOp(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        boolean previousGamerule = level.getGameRules().get(WorldGate.ENABLED);
        WorldConfig config = ConfigManager.world(level);
        boolean previousDim = config.enabled();
        try {
            config.setEnabled(true);
            level.getGameRules().set(WorldGate.ENABLED, true, server);
            helper.assertTrue(WorldGate.isActive(level), "gamerule on + dim on → WorldGate active");
            helper.assertTrue(
                    WorldGate.isModuleActive(level, ExtraHardModeMod.id("hardened_stone")),
                    "gamerule on → modules active");

            level.getGameRules().set(WorldGate.ENABLED, false, server);
            helper.assertFalse(WorldGate.isActive(level), "gamerule off → WorldGate inactive");
            helper.assertFalse(
                    WorldGate.isModuleActive(level, ExtraHardModeMod.id("hardened_stone")),
                    "gamerule off → modules inactive");
            helper.assertFalse(
                    WorldGate.isModuleActive(level, ExtraHardModeMod.id("cave_ins")),
                    "gamerule off → cave-ins inactive");
            helper.succeed();
        } finally {
            level.getGameRules().set(WorldGate.ENABLED, previousGamerule, server);
            config.setEnabled(previousDim);
        }
    }
}

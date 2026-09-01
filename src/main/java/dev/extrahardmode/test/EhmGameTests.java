package dev.extrahardmode.test;

import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.world.WorldGate;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class EhmGameTests {
    @GameTest
    public void firstApplyPerDimension(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        boolean toml = ConfigManager.global().enabledByDefault();
        helper.assertValueEqual(toml, overworld.getGameRules().get(WorldGate.ENABLED), "overworld gamerule matches TOML");

        boolean overworldBefore = overworld.getGameRules().get(WorldGate.ENABLED);
        WorldGate.applyIfNeeded(overworld.getServer(), overworld);
        helper.assertValueEqual(
                overworldBefore,
                overworld.getGameRules().get(WorldGate.ENABLED),
                "overworld not rewritten");

        ServerLevel nether = overworld.getServer().getLevel(Level.NETHER);
        if (nether != null) {
            WorldGate.applyIfNeeded(overworld.getServer(), nether);
            helper.assertValueEqual(toml, nether.getGameRules().get(WorldGate.ENABLED), "nether gamerule matches TOML");
            helper.assertValueEqual(
                    overworldBefore,
                    overworld.getGameRules().get(WorldGate.ENABLED),
                    "overworld not rewritten after nether");
        }
        helper.succeed();
    }
}

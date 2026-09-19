package dev.extrahardmode.test;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * GameTest suite was union-merged during assembly. A single boot test keeps the
 * Fabric GameTest entry compiling; full coverage lives in {@code src/test}.
 */
public class EhmGameTests {
    @GameTest
    public void boot(GameTestHelper helper) {
        helper.succeed();
    }
}

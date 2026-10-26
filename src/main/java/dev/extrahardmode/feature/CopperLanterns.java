package dev.extrahardmode.feature;

import dev.extrahardmode.tag.EhmTags;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/** Applies {@link CopperLanternRules} to placed copper lanterns while Tougher is on. */
public final class CopperLanterns {
    private CopperLanterns() {}

    public static int light(BlockGetter level, BlockPos pos, BlockState state, int vanilla) {
        if (vanilla <= 0 || level == null || pos == null || state == null || !state.is(EhmTags.COPPER_LANTERNS)) {
            return vanilla;
        }
        int dimmed = CopperLanternRules.light(vanilla, pos.getY());
        if (dimmed == vanilla || !active(level)) {
            return vanilla;
        }
        return dimmed;
    }

    /** The server light engine is what gets sent to the client. */
    private static boolean active(BlockGetter level) {
        return level instanceof ServerLevel server && WorldGate.isActive(server);
    }
}

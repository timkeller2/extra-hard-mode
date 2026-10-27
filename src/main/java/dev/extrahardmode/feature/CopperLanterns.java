package dev.extrahardmode.feature;

import dev.extrahardmode.tag.EhmTags;
import dev.extrahardmode.world.WorldGate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/** Applies {@link CopperLanternRules} to placed copper lanterns while Tougher is on. */
public final class CopperLanterns {
    /** Client light engine has no {@link ServerLevel}. The sync payload is its gate. */
    private static volatile boolean clientActive;

    private CopperLanterns() {}

    public static void setClientActive(boolean active) {
        clientActive = active;
    }

    public static int light(BlockGetter level, BlockPos pos, BlockState state, int vanilla) {
        if (vanilla <= 0 || level == null || pos == null || state == null || !state.is(EhmTags.COPPER_LANTERNS)) {
            return vanilla;
        }
        int dimmed = CopperLanternRules.light(vanilla, pos.getY());
        if (dimmed == vanilla || !engineActive(level)) {
            return vanilla;
        }
        return dimmed;
    }

    /**
     * The server engine follows {@link WorldGate}. The client engine follows the
     * synced world flag, so a placed lantern dims in that same tick.
     */
    static boolean engineActive(boolean serverLevel, boolean serverGate, boolean clientGate) {
        return serverLevel ? serverGate : clientGate;
    }

    private static boolean engineActive(BlockGetter level) {
        if (level instanceof ServerLevel server) {
            return engineActive(true, WorldGate.isActive(server), clientActive);
        }
        return engineActive(false, false, clientActive);
    }
}

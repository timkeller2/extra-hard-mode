package dev.extrahardmode.world;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

/** Every mixin inject's first statement must be a WorldGate check. */
public final class WorldGate {
    private WorldGate() {}

    public static boolean isActive(ServerLevel level) {
        return false;
    }

    public static boolean isModuleActive(ServerLevel level, Identifier moduleId) {
        return false;
    }
}

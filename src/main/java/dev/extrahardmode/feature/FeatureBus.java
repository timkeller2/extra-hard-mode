package dev.extrahardmode.feature;

import dev.extrahardmode.world.WorldGate;
import java.util.Objects;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Modules register Fabric callbacks here. {@link #listen} cannot extract a
 * {@link ServerLevel} from an arbitrary callback type; handlers must call
 * {@link #guard} before applying gameplay.
 */
public final class FeatureBus {
    public <T> void listen(Event<T> event, Identifier moduleId, T handler) {
        Objects.requireNonNull(moduleId, "moduleId");
        event.register(handler);
    }

    public boolean guard(ServerLevel level, Identifier moduleId) {
        return WorldGate.isModuleActive(level, moduleId);
    }

    /** Mixin injects call this first. No-op when the level is not a logical server. */
    public static boolean guard(Level level, Identifier moduleId) {
        return level instanceof ServerLevel serverLevel && WorldGate.isModuleActive(serverLevel, moduleId);
    }
}

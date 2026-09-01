package dev.extrahardmode.feature;

import dev.extrahardmode.world.WorldGate;
import java.util.Objects;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

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
}

package dev.extrahardmode.feature;

import dev.extrahardmode.world.WorldGate;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

/**
 * Modules register Fabric callbacks here. Typed wrappers extract a {@link ServerLevel}
 * and skip the handler when {@link WorldGate} is inactive.
 */
public final class FeatureBus {
    public <T> void listen(Event<T> event, Identifier moduleId, T handler) {
        event.register(handler);
    }

    public boolean guard(ServerLevel level, Identifier moduleId) {
        return WorldGate.isModuleActive(level, moduleId);
    }
}

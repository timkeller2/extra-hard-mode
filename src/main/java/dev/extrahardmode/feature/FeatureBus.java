package dev.extrahardmode.feature;

import dev.extrahardmode.world.WorldGate;
import java.util.Objects;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;

/**
 * Modules register Fabric callbacks here. {@link #listen(Event, Identifier, Object)}
 * cannot extract a {@link ServerLevel} from an arbitrary callback type; handlers
 * must call {@link #guard} before applying gameplay. Player-entity use is wrapped.
 */
public final class FeatureBus {
    public <T> void listen(Event<T> event, Identifier moduleId, T handler) {
        Objects.requireNonNull(moduleId, "moduleId");
        event.register(handler);
    }

    public void listen(Event<UseEntityCallback> event, Identifier moduleId, UseEntityCallback handler) {
        Objects.requireNonNull(moduleId, "moduleId");
        event.register((player, level, hand, entity, hit) -> {
            if (level instanceof ServerLevel serverLevel && !guard(serverLevel, moduleId)) {
                return InteractionResult.PASS;
            }
            return handler.interact(player, level, hand, entity, hit);
        });
    }

    public boolean guard(ServerLevel level, Identifier moduleId) {
        return WorldGate.isModuleActive(level, moduleId);
    }
}

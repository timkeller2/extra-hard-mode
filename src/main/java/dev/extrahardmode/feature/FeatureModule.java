package dev.extrahardmode.feature;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

public interface FeatureModule {
    Identifier id();

    default void bootstrap(FeatureBus bus) {}

    default void onWorldLoad(ServerLevel level) {}

    default void onWorldUnload(ServerLevel level) {}

    default void serverTick(ServerLevel level) {}
}

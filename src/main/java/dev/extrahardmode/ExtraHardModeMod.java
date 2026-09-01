package dev.extrahardmode;

import dev.extrahardmode.command.EhmCommands;
import dev.extrahardmode.command.EhmPermissions;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.feature.FeatureRegistry;
import dev.extrahardmode.feature.HardenedStone;
import dev.extrahardmode.feature.LimitedBuilding;
import dev.extrahardmode.feature.NetherrackFire;
import dev.extrahardmode.feature.Torches;
import dev.extrahardmode.item.EhmComponents;
import dev.extrahardmode.feature.Players;
import dev.extrahardmode.network.EhmNetworking;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.WorldGate;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtraHardModeMod implements ModInitializer {
    public static final String MOD_ID = "extrahardmode";
    public static final Logger LOGGER = LoggerFactory.getLogger("ExtraHardMode");
    public static final FeatureRegistry FEATURES = new FeatureRegistry();
    public static final Identifier ARMOR_SLOWDOWN = id("armor_slowdown");

    @Override
    public void onInitialize() {
        ConfigManager.load();
        EhmComponents.register();
        EhmAttachments.register();
        EhmPermissions.register();
        WorldGate.register();
        EhmNetworking.register();
        EhmCommands.register();
        FEATURES.register(new HardenedStone());
        FEATURES.register(new Torches());
        FEATURES.register(new NetherrackFire());
        FEATURES.register(new LimitedBuilding());
        FEATURES.register(Players.INSTANCE);
        ServerLevelEvents.LOAD.register((server, level) -> {
            WorldGate.onLevelLoad(server, level);
            FEATURES.onWorldLoad(level);
        });
        ServerLevelEvents.UNLOAD.register((server, level) -> FEATURES.onWorldUnload(level));
        ServerTickEvents.END_LEVEL_TICK.register(FEATURES::serverTick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ConfigManager.clearWorldCache());
        LOGGER.info("EHM loaded, {} modules", FEATURES.count());
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}

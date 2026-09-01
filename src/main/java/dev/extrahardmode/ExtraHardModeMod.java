package dev.extrahardmode;

import dev.extrahardmode.command.EhmCommands;
import dev.extrahardmode.command.EhmPermissions;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.feature.AnimalCrowdControl;
import dev.extrahardmode.feature.AntiFarming;
import dev.extrahardmode.feature.CaveIns;
import dev.extrahardmode.feature.FallingBlocks;
import dev.extrahardmode.feature.FeatureRegistry;
import dev.extrahardmode.feature.HardenedStone;
import dev.extrahardmode.feature.LimitedBuilding;
import dev.extrahardmode.feature.NetherrackFire;
import dev.extrahardmode.feature.Torches;
import dev.extrahardmode.item.EhmComponents;
import dev.extrahardmode.feature.Players;
import dev.extrahardmode.feature.Water;
import dev.extrahardmode.feature.monster.Silverfish;
import dev.extrahardmode.feature.monster.Skeletons;
import dev.extrahardmode.feature.monster.Endermen;
import dev.extrahardmode.feature.monster.Horses;
import dev.extrahardmode.feature.monster.Witches;
import dev.extrahardmode.feature.monster.BiomeReplacements;
import dev.extrahardmode.module.SpawnReplaceService;
import dev.extrahardmode.module.PhysicsQueue;
import dev.extrahardmode.network.EhmNetworking;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.WorldGate;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.item.FallingBlockEntity;
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
        SpawnReplaceService.init();
        BiomeReplacements.register(FEATURES);
        EhmPermissions.register();
        WorldGate.register();
        EhmNetworking.register();
        EhmCommands.register();
        FEATURES.register(new HardenedStone());
        FEATURES.register(new Torches());
        FEATURES.register(new NetherrackFire());
        FEATURES.register(new LimitedBuilding());
        FEATURES.register(Players.INSTANCE);
        FEATURES.register(new AntiFarming());
        FEATURES.register(new Water());
        FEATURES.register(new AnimalCrowdControl());
        FEATURES.register(new CaveIns());
        FEATURES.register(new FallingBlocks());
        FEATURES.register(new Skeletons());
        FEATURES.register(new Silverfish());
        FEATURES.register(new Endermen());
        FEATURES.register(new Witches());
        FEATURES.register(new Horses());
        ServerLevelEvents.LOAD.register((server, level) -> {
            WorldGate.onLevelLoad(server, level);
            FEATURES.onWorldLoad(level);
        });
        ServerLevelEvents.UNLOAD.register((server, level) -> {
            FEATURES.onWorldUnload(level);
            PhysicsQueue.discard(level);
        });
        ServerTickEvents.END_LEVEL_TICK.register(level -> {
            FEATURES.serverTick(level);
            PhysicsQueue.tick(level);
        });
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof FallingBlockEntity falling) {
                PhysicsQueue.of(level).trackLive(falling);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ConfigManager.clearWorldCache());
        LOGGER.info("EHM loaded, {} modules", FEATURES.count());
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}

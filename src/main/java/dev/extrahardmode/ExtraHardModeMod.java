package dev.extrahardmode;

import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.feature.FeatureRegistry;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtraHardModeMod implements ModInitializer {
    public static final String MOD_ID = "extrahardmode";
    public static final Logger LOGGER = LoggerFactory.getLogger("ExtraHardMode");
    public static final FeatureRegistry FEATURES = new FeatureRegistry();

    @Override
    public void onInitialize() {
        ConfigManager.load();
        LOGGER.info("EHM loaded, {} modules", FEATURES.count());
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}

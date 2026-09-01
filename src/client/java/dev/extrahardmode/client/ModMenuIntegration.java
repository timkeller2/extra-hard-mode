package dev.extrahardmode.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

/** Mod Menu button only opens a screen when Cloth Config is loaded. */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (!FabricLoader.getInstance().isModLoaded("cloth-config")
                && !FabricLoader.getInstance().isModLoaded("cloth-config2")) {
            return parent -> null;
        }
        return ClothConfigScreen::create;
    }
}

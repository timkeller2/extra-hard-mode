package dev.extrahardmode.feature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.level.ServerLevel;

public final class FeatureRegistry implements Iterable<FeatureModule> {
    private final List<FeatureModule> modules = new ArrayList<>();
    private final FeatureBus bus = new FeatureBus();

    public void register(FeatureModule module) {
        modules.add(Objects.requireNonNull(module, "module"));
        module.bootstrap(bus);
    }

    public int count() {
        return modules.size();
    }

    public List<FeatureModule> modules() {
        return Collections.unmodifiableList(modules);
    }

    @Override
    public Iterator<FeatureModule> iterator() {
        return modules().iterator();
    }

    public void onWorldLoad(ServerLevel level) {
        for (FeatureModule module : modules) {
            if (bus.guard(level, module.id())) {
                module.onWorldLoad(level);
            }
        }
    }

    public void onWorldUnload(ServerLevel level) {
        for (FeatureModule module : modules) {
            module.onWorldUnload(level);
        }
    }

    public void serverTick(ServerLevel level) {
        for (FeatureModule module : modules) {
            if (bus.guard(level, module.id())) {
                module.serverTick(level);
            }
        }
    }
}

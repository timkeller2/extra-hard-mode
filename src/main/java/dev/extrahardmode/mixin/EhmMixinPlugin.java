package dev.extrahardmode.mixin;

import dev.extrahardmode.module.SpawnReplaceService;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class EhmMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (!mixinClassName.endsWith("NaturalSpawnerMixin")) {
            return;
        }
        if (hasEhmSpawnHook(targetClass)) {
            SpawnReplaceService.markMixinApplied();
        } else {
            SpawnReplaceService.warnIfMixinMissing();
        }
    }

    private static boolean hasEhmSpawnHook(ClassNode classNode) {
        for (MethodNode method : classNode.methods) {
            if (method.name.contains("ehm$")) {
                return true;
            }
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode call
                        && (call.name.contains("ehm$") || call.owner.endsWith("NaturalSpawnerMixin"))) {
                    return true;
                }
            }
        }
        return false;
    }
}

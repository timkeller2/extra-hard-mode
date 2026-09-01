package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Datapack {@code data/minecraft/recipe/tnt.json} is always count 3 (overwrite vanilla).
 * Crafting mixins consult WorldGate / this module / {@code explosions.tnt.perRecipe}
 * so gamerule-off and {@code more_tnt=false} fall back to 1.
 */
public final class MoreTnt implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("more_tnt");

    private static final ThreadLocal<ServerLevel> CRAFT_LEVEL = new ThreadLocal<>();

    @Override
    public Identifier id() {
        return ID;
    }

    public static void captureCraftLevel(ServerLevel level) {
        CRAFT_LEVEL.set(level);
    }

    public static void clearCraftLevel() {
        CRAFT_LEVEL.remove();
    }

    public static ItemStack adjustCaptured(ItemStack stack) {
        ServerLevel level = CRAFT_LEVEL.get();
        return level == null ? stack : adjustResult(level, stack);
    }

    public static ItemStack adjustResult(ServerLevel level, ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.is(Items.TNT)) {
            return stack;
        }
        int count = resultCount(FeatureBus.guard((Level) level, ID), ConfigManager.world(level).explosions().tntPerRecipe());
        if (stack.getCount() == count) {
            return stack;
        }
        return stack.copyWithCount(count);
    }

    /** Minecraft-free so gamerule-off → 1 is unit-tested. */
    public static int resultCount(boolean moduleActive, int perRecipe) {
        return moduleActive ? Math.max(1, perRecipe) : 1;
    }
}

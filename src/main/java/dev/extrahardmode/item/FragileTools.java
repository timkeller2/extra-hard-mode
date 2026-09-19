package dev.extrahardmode.item;

import java.util.Set;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.fabricmc.fabric.api.item.v1.FabricComponentMapBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Wood, copper, and stone tools last one third as long as vanilla. */
public final class FragileTools {
    public static final int DURABILITY_DIVISOR = 3;

    private static Set<Item> tools = Set.of();

    private FragileTools() {}

    public static void register() {
        tools = Set.of(
                Items.WOODEN_SWORD,
                Items.WOODEN_SHOVEL,
                Items.WOODEN_PICKAXE,
                Items.WOODEN_AXE,
                Items.WOODEN_HOE,
                Items.WOODEN_SPEAR,
                Items.STONE_SWORD,
                Items.STONE_SHOVEL,
                Items.STONE_PICKAXE,
                Items.STONE_AXE,
                Items.STONE_HOE,
                Items.STONE_SPEAR,
                Items.COPPER_SWORD,
                Items.COPPER_SHOVEL,
                Items.COPPER_PICKAXE,
                Items.COPPER_AXE,
                Items.COPPER_HOE,
                Items.COPPER_SPEAR);
        DefaultItemComponentEvents.MODIFY.register(
                context -> context.modify(FragileTools::isFragile, (builder, registries, item) -> {
                    Integer max = ((FabricComponentMapBuilder) builder).get(DataComponents.MAX_DAMAGE);
                    if (max == null || max <= 0) {
                        return;
                    }
                    builder.set(DataComponents.MAX_DAMAGE, scaledDurability(max));
                }));
    }

    public static int scaledDurability(int vanilla) {
        return Math.max(1, vanilla / DURABILITY_DIVISOR);
    }

    public static boolean isFragile(Item item) {
        return tools.contains(item);
    }

    /** Existing stacks keep a proportional wear after max durability is cut to 1/3. */
    public static void rescaleInventory(Player player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            rescaleStack(inventory.getItem(slot));
        }
    }

    static void rescaleStack(ItemStack stack) {
        if (stack.isEmpty() || !isFragile(stack.getItem()) || !stack.isDamageableItem()) {
            return;
        }
        int max = stack.getMaxDamage();
        int damage = stack.getDamageValue();
        if (max <= 0 || damage <= max) {
            return;
        }
        stack.setDamageValue(scaledDamage(damage, max));
    }

    static int scaledDamage(int damage, int newMax) {
        int vanillaMax = newMax * DURABILITY_DIVISOR;
        return Math.max(0, Math.min(newMax, (int) Math.round(damage * (newMax / (double) vanillaMax))));
    }
}

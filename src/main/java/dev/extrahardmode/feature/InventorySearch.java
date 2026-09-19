package dev.extrahardmode.feature;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.BundleContents;

/** Inventory scans that also look inside bundles (including nested bundles). */
public final class InventorySearch {
    private InventorySearch() {}

    public static int count(Player player, Item item) {
        return count(player, stack -> stack.is(item));
    }

    public static int count(Player player, Predicate<ItemStack> match) {
        return count(player.getInventory(), match);
    }

    public static int count(Inventory inventory, Predicate<ItemStack> match) {
        int total = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            total += countIn(inventory.getItem(slot), match);
        }
        return total;
    }

    public static boolean has(Player player, Item item) {
        return count(player, item) > 0;
    }

    public static boolean has(Player player, Predicate<ItemStack> match) {
        return count(player, match) > 0;
    }

    public static boolean consumeOne(Player player, Item item) {
        return consume(player, stack -> stack.is(item), 1);
    }

    /** Hotbar and backpack only. Does not search bundles, armor, or offhand. */
    public static boolean hasOneMain(Player player, Item item) {
        if (player == null || item == null) {
            return false;
        }
        Inventory inventory = player.getInventory();
        int n = Math.min(AbilityRules.MAIN_INVENTORY_SLOTS, inventory.getContainerSize());
        for (int slot = 0; slot < n; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack != null && !stack.isEmpty() && stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    /** Hotbar and backpack only. Does not search bundles, armor, or offhand. */
    public static boolean consumeOneMain(Player player, Item item) {
        if (player == null || item == null) {
            return false;
        }
        Inventory inventory = player.getInventory();
        int n = Math.min(AbilityRules.MAIN_INVENTORY_SLOTS, inventory.getContainerSize());
        for (int slot = 0; slot < n; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.isEmpty() || !stack.is(item)) {
                continue;
            }
            stack.shrink(1);
            if (stack.isEmpty()) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
            return true;
        }
        return false;
    }

    public static boolean consumeOne(Player player, Predicate<ItemStack> match) {
        return consume(player, match, 1);
    }

    public static boolean consume(Player player, Item item, int amount) {
        return consume(player, stack -> stack.is(item), amount);
    }

    public static boolean consume(Player player, Predicate<ItemStack> match, int amount) {
        if (amount <= 0) {
            return true;
        }
        Inventory inventory = player.getInventory();
        if (count(inventory, match) < amount) {
            return false;
        }
        int left = amount;
        for (int slot = 0; slot < inventory.getContainerSize() && left > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            left = consumeFrom(stack, match, left);
            if (stack.isEmpty()) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }
        return left == 0;
    }

    static int countIn(ItemStack stack, Predicate<ItemStack> match) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        int total = match.test(stack) ? stack.getCount() : 0;
        BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) {
            return total;
        }
        for (ItemStackTemplate template : contents.items()) {
            total += countIn(template.create(), match);
        }
        return total;
    }

    static int consumeFrom(ItemStack stack, Predicate<ItemStack> match, int left) {
        if (left <= 0 || stack == null || stack.isEmpty()) {
            return left;
        }
        if (match.test(stack)) {
            int take = Math.min(left, stack.getCount());
            stack.shrink(take);
            return left - take;
        }
        BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) {
            return left;
        }
        List<ItemStackTemplate> items = new ArrayList<>(contents.items());
        boolean changed = false;
        for (int i = 0; i < items.size() && left > 0; i++) {
            ItemStack inner = items.get(i).create();
            int before = left;
            left = consumeFrom(inner, match, left);
            if (left == before) {
                continue;
            }
            changed = true;
            if (inner.isEmpty()) {
                items.remove(i);
                i--;
            } else {
                items.set(i, ItemStackTemplate.fromStack(inner));
            }
        }
        if (changed) {
            stack.set(DataComponents.BUNDLE_CONTENTS, items.isEmpty() ? BundleContents.EMPTY : new BundleContents(items));
        }
        return left;
    }
}

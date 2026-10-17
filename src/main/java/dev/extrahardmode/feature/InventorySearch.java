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

    /** Any inventory slot, but not items stored inside a bundle. */
    public static boolean hasIgnoringBundles(Player player, Item item) {
        if (player == null || item == null) {
            return false;
        }
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack != null && !stack.isEmpty() && stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    /** Shrinks one matching slot stack. Does not open bundles. */
    public static boolean consumeOneIgnoringBundles(Player player, Item item) {
        if (player == null || item == null) {
            return false;
        }
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
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

    /**
     * Count of the largest matching stack in the hotbar and backpack.
     * Does not open bundles, and does not look at armor or the offhand. Ties keep the earlier slot.
     */
    public static int largestMainStack(Player player, Item item) {
        int slot = largestMainSlot(player, item);
        if (slot < 0) {
            return 0;
        }
        ItemStack stack = player.getInventory().getItem(slot);
        return stack == null || stack.isEmpty() ? 0 : stack.getCount();
    }

    /** Slot of {@link #largestMainStack}, or -1 when the player has no such stack. */
    public static int largestMainSlot(Player player, Item item) {
        if (player == null || item == null) {
            return -1;
        }
        Inventory inventory = player.getInventory();
        int n = Math.min(AbilityRules.MAIN_INVENTORY_SLOTS, inventory.getContainerSize());
        int bestSlot = -1;
        int bestCount = 0;
        for (int slot = 0; slot < n; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.isEmpty() || !stack.is(item) || stack.getCount() <= bestCount) {
                continue;
            }
            bestCount = stack.getCount();
            bestSlot = slot;
        }
        return bestSlot;
    }

    /**
     * Shrinks the largest hotbar or backpack stack by {@code amount}.
     * Does not open bundles. An amount of 0 succeeds without changing the inventory.
     */
    public static boolean consumeLargestMainStack(Player player, Item item, int amount) {
        if (amount <= 0) {
            return true;
        }
        int slot = largestMainSlot(player, item);
        if (slot < 0) {
            return false;
        }
        Inventory inventory = player.getInventory();
        ItemStack stack = inventory.getItem(slot);
        if (stack.getCount() < amount) {
            return false;
        }
        stack.shrink(amount);
        if (stack.isEmpty()) {
            inventory.setItem(slot, ItemStack.EMPTY);
        }
        return true;
    }

    /** Total in the hotbar and backpack. Does not open bundles, armor, or the offhand. */
    public static int countMain(Player player, Item item) {
        if (player == null || item == null) {
            return 0;
        }
        Inventory inventory = player.getInventory();
        int n = Math.min(AbilityRules.MAIN_INVENTORY_SLOTS, inventory.getContainerSize());
        int total = 0;
        for (int slot = 0; slot < n; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack != null && !stack.isEmpty() && stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /**
     * Removes {@code amount} across hotbar and backpack stacks, earliest slot first.
     * Does not open bundles. A short total changes nothing.
     */
    public static boolean consumeMain(Player player, Item item, int amount) {
        if (amount <= 0) {
            return true;
        }
        if (countMain(player, item) < amount) {
            return false;
        }
        Inventory inventory = player.getInventory();
        int n = Math.min(AbilityRules.MAIN_INVENTORY_SLOTS, inventory.getContainerSize());
        int left = amount;
        for (int slot = 0; slot < n && left > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.isEmpty() || !stack.is(item)) {
                continue;
            }
            int take = Math.min(left, stack.getCount());
            stack.shrink(take);
            left -= take;
            if (stack.isEmpty()) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }
        return left == 0;
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

package dev.extrahardmode.feature;

import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.task.WeightCheckTask;
import dev.extrahardmode.world.NetherPortalTollData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PortalProcessor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;

/**
 * A nether portal spends the traveler's weight in redstone blocks, quartz blocks, and
 * amethyst blocks from that player's inventory and offhand. The 15 second wait is stored
 * on the player. Leaving the overworld also rolls the shared backlash counter.
 * Vanilla portal cooldown is reset to half a second while someone stands in the exit, so it
 * cannot hold this wait.
 */
public final class NetherPortals {
    static final String COST_KEY = "tougher.message.nether_portal_cost";
    static final String COST = "You need %s redstone blocks, quartz blocks, or amethyst blocks.";
    static final String WAIT_KEY = "tougher.message.nether_portal_wait";
    static final String WAIT = "You must wait to use a nether portal. %s";
    static final String PAID_KEY = "tougher.message.nether_portal_paid";
    static final String PAID = "The portal takes %s.";
    static final String SHUT_KEY = "tougher.message.nether_portal_shut";
    static final String SHUT = "Someone with you cannot take this portal yet.";

    /** Same-tick guard so a mount and its rider do not roll the backlash twice. */
    private static final Map<UUID, Long> BACKLASH_TICK = new ConcurrentHashMap<>();

    private NetherPortals() {}

    /**
     * Called at the start of a portal teleport. True means the teleport must not happen.
     * Fuel is spent only when every paying traveler can go.
     */
    public static boolean blockTeleport(Entity entity, ServerLevel from, TeleportTransition transition) {
        if (entity == null || from == null || transition == null || !EhmApi.isActive(from)) {
            return false;
        }
        if (!netherPortal(entity, from) || !willTeleport(entity, from, transition)) {
            return false;
        }
        List<ServerPlayer> travelers = travelers(entity);
        if (travelers.isEmpty()) {
            return false;
        }
        List<ServerPlayer> paying = new ArrayList<>();
        for (ServerPlayer player : travelers) {
            if (player.isAlive() && !player.isSpectator() && !EhmApi.playerBypasses(player)) {
                paying.add(player);
            }
        }
        long now = from.getServer().overworld().getGameTime();
        if (paying.isEmpty()) {
            backlash(from, entity, travelers, now);
            return false;
        }
        List<Bill> bills = new ArrayList<>();
        List<ServerPlayer> denied = new ArrayList<>();
        boolean shortFuel = false;
        boolean cooling = false;
        for (ServerPlayer player : paying) {
            int left = NetherPortalRules.secondsLeft(
                    player.getAttachedOrElse(EhmAttachments.EHM_NETHER_PORTAL_READY, 0L), now);
            if (left > 0) {
                denied.add(player);
                cooling = true;
                if (!silenced(player, now)) {
                    tell(player, WAIT_KEY, WAIT, Component.literal(NetherPortalRules.waitPhrase(left)));
                    markTold(player, now);
                }
                continue;
            }
            List<LapisPortalRules.Stack> slots = read(player);
            int cost = NetherPortalRules.travelCost(
                    WeightCheckTask.inventoryWeight(player, ConfigManager.world(from).player()),
                    LapisPortalRules.fuelKinds(slots, NetherPortalRules.FUELS));
            List<LapisPortalRules.Stack> paid = LapisPortalRules.pay(slots, cost, NetherPortalRules.FUELS);
            if (paid == null) {
                denied.add(player);
                shortFuel = true;
                if (!silenced(player, now)) {
                    tell(player, COST_KEY, COST, Component.literal(Integer.toString(cost)));
                    markTold(player, now);
                }
                continue;
            }
            bills.add(new Bill(player, slots, paid));
        }
        if (cooling || shortFuel) {
            if (!denied.isEmpty()) {
                for (ServerPlayer player : travelers) {
                    if (denied.contains(player) || silenced(player, now)) {
                        continue;
                    }
                    tell(player, SHUT_KEY, SHUT);
                    markTold(player, now);
                }
            }
            return true;
        }
        for (Bill bill : bills) {
            apply(bill.player, bill.paid);
            bill.player.setAttached(EhmAttachments.EHM_NETHER_PORTAL_READY, NetherPortalRules.readyAt(now));
            String spent = NetherPortalRules.spentPhrase(bill.spentRedstone(), bill.spentQuartz(), bill.spentAmethyst());
            if (!spent.isEmpty()) {
                tell(bill.player, PAID_KEY, PAID, Component.literal(spent));
            }
        }
        backlash(from, entity, travelers, now);
        return false;
    }

    /**
     * Overworld departures only. One d20 per trip. The counter, after a raise when the
     * roll beats it, is damage. Listed animals within 64 blocks lose health first.
     * Whatever is left hits each traveler on this trip.
     */
    private static void backlash(ServerLevel from, Entity entity, List<ServerPlayer> travelers, long now) {
        if (from == null || from.dimension() != Level.OVERWORLD) {
            return;
        }
        List<ServerPlayer> riding = livingTravelers(travelers);
        if (riding.isEmpty() || !backlashFresh(riding, now)) {
            return;
        }
        for (ServerPlayer player : riding) {
            BACKLASH_TICK.put(player.getUUID(), now);
        }
        int roll = from.getRandom().nextInt(NetherPortalRules.DIE_SIDES) + 1;
        float left = strikeAnimals(from, entity, NetherPortalTollData.of(from).applyRoll(roll));
        if (left <= 0.0F) {
            return;
        }
        var source = from.damageSources().magic();
        for (ServerPlayer player : riding) {
            if (player.isAlive()) {
                player.hurtServer(from, source, left);
            }
        }
    }

    private static float strikeAnimals(ServerLevel level, Entity origin, float damage) {
        if (damage <= 0.0F || origin == null) {
            return Math.max(0.0F, damage);
        }
        double range = NetherPortalRules.BACKLASH_RANGE;
        AABB box = origin.getBoundingBox().inflate(range);
        List<LivingEntity> found = level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                living -> backlashAnimal(living) && living.distanceToSqr(origin) <= range * range);
        found.sort(Comparator.comparingDouble(living -> living.distanceToSqr(origin)));
        float left = damage;
        var source = level.damageSources().magic();
        for (LivingEntity creature : found) {
            if (left <= 0.0F) {
                break;
            }
            if (!creature.isAlive()) {
                continue;
            }
            float health = creature.getHealth();
            if (health <= 0.0F) {
                continue;
            }
            creature.hurtServer(level, source, Math.min(left, health));
            float nowHealth = creature.isAlive() ? creature.getHealth() : 0.0F;
            float lost = health - Math.max(0.0F, nowHealth);
            if (lost <= 0.0F) {
                continue;
            }
            left -= Math.min(left, lost);
        }
        return Math.max(0.0F, left);
    }

    /** Livestock and the other listed animals. Players, villagers, traders, golems, fish, allays, and bats are skipped. */
    private static boolean backlashAnimal(LivingEntity entity) {
        if (!(entity instanceof Mob mob) || !mob.isAlive()) {
            return false;
        }
        return NetherPortalRules.backlashAnimal(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString());
    }

    private static List<ServerPlayer> livingTravelers(List<ServerPlayer> travelers) {
        List<ServerPlayer> living = new ArrayList<>();
        if (travelers == null) {
            return living;
        }
        for (ServerPlayer player : travelers) {
            if (player.isAlive() && !player.isSpectator()) {
                living.add(player);
            }
        }
        return living;
    }

    private static boolean backlashFresh(List<ServerPlayer> travelers, long now) {
        for (ServerPlayer player : travelers) {
            if (BACKLASH_TICK.getOrDefault(player.getUUID(), Long.MIN_VALUE) != now) {
                return true;
            }
        }
        return false;
    }

    private static boolean netherPortal(Entity entity, ServerLevel level) {
        PortalProcessor process = entity.portalProcess;
        if (process == null) {
            return false;
        }
        BlockPos entry = process.getEntryPosition();
        return entry != null && level.getBlockState(entry).getBlock() instanceof NetherPortalBlock;
    }

    private static boolean willTeleport(Entity entity, ServerLevel from, TeleportTransition transition) {
        ServerLevel destination = transition.newLevel();
        if (destination == null || !from.isAllowedToEnterPortal(destination)) {
            return false;
        }
        return destination.dimension().equals(from.dimension()) || entity.canTeleport(from, destination);
    }

    private static List<ServerPlayer> travelers(Entity entity) {
        List<ServerPlayer> found = new ArrayList<>();
        collect(entity, found);
        return found;
    }

    private static void collect(Entity entity, List<ServerPlayer> found) {
        if (entity instanceof ServerPlayer player && !found.contains(player)) {
            found.add(player);
        }
        for (Entity passenger : entity.getPassengers()) {
            collect(passenger, found);
        }
    }

    /** Hotbar, backpack, and offhand. Armor and the ender chest stay put. */
    private static List<LapisPortalRules.Stack> read(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        List<LapisPortalRules.Stack> slots = new ArrayList<>();
        int size = inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            if (!carriedSlot(i)) {
                continue;
            }
            ItemStack stack = inventory.getItem(i);
            if (stack == null || stack.isEmpty() || stack.getCount() <= 0) {
                continue;
            }
            slots.add(new LapisPortalRules.Stack(
                    i, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount()));
        }
        return slots;
    }

    private static boolean carriedSlot(int index) {
        return (index >= 0 && index < Inventory.INVENTORY_SIZE) || index == Inventory.SLOT_OFFHAND;
    }

    private static void apply(ServerPlayer player, List<LapisPortalRules.Stack> slots) {
        Inventory inventory = player.getInventory();
        for (LapisPortalRules.Stack slot : slots) {
            if (!carriedSlot(slot.index())) {
                continue;
            }
            if (slot.count() <= 0) {
                inventory.setItem(slot.index(), ItemStack.EMPTY);
                continue;
            }
            ItemStack stack = inventory.getItem(slot.index());
            if (!stack.isEmpty() && stack.getCount() != slot.count()) {
                stack.setCount(slot.count());
                inventory.setItem(slot.index(), stack);
            }
        }
        inventory.setChanged();
        player.containerMenu.broadcastChanges();
    }

    private static boolean silenced(ServerPlayer player, long now) {
        return NetherPortalRules.repeatTooSoon(
                player.getAttachedOrElse(EhmAttachments.EHM_NETHER_PORTAL_TOLD, 0L), now);
    }

    private static void markTold(ServerPlayer player, long now) {
        player.setAttached(EhmAttachments.EHM_NETHER_PORTAL_TOLD, now);
    }

    private static void tell(ServerPlayer player, String key, String fallback, Object... args) {
        player.sendSystemMessage(Component.translatableWithFallback(key, fallback, args));
    }

    private static final class Bill {
        private final ServerPlayer player;
        private final List<LapisPortalRules.Stack> before;
        private final List<LapisPortalRules.Stack> paid;

        private Bill(ServerPlayer player, List<LapisPortalRules.Stack> before, List<LapisPortalRules.Stack> paid) {
            this.player = player;
            this.before = before;
            this.paid = paid;
        }

        private int spentRedstone() {
            return spent(NetherPortalRules.REDSTONE_BLOCK);
        }

        private int spentQuartz() {
            return spent(NetherPortalRules.QUARTZ_BLOCK);
        }

        private int spentAmethyst() {
            return spent(NetherPortalRules.AMETHYST_BLOCK);
        }

        private int spent(String itemId) {
            return LapisPortalRules.total(before, itemId) - LapisPortalRules.total(paid, itemId);
        }
    }
}

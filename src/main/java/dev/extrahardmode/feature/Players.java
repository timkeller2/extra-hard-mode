package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.api.event.PlayerExtinguishFireEvent;
import dev.extrahardmode.api.event.PlayerInventoryLossEvent;
import dev.extrahardmode.command.EhmPermissions;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.PlayerSettings;
import dev.extrahardmode.config.PotionEffectHolder;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.player.DeathForfeit;
import dev.extrahardmode.task.ArmorWeightTask;
import dev.extrahardmode.task.SetPlayerHealthAndFoodTask;
import dev.extrahardmode.task.WeightCheckTask;
import dev.extrahardmode.world.EhmTags;
import dev.extrahardmode.world.WorldGate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Player-triggered Extra Hard Mode rules: death forfeit, weak respawn, environmental
 * injuries, punching fire, inventory-weight drowning, and armor slowdown.
 */
public final class Players implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("players");
    public static final Players INSTANCE = new Players();

    private static final List<PendingRespawn> PENDING_RESPAWNS = new ArrayList<>();

    private Players() {}

    private record PendingRespawn(UUID playerId, float health, int food, long readyTick) {}

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        bus.listen(ServerLivingEntityEvents.ALLOW_DAMAGE, ID, Players::onAllowDamage);
        bus.listen(ServerPlayerEvents.AFTER_RESPAWN, ID, Players::onAfterRespawn);
        bus.listen(ServerPlayerEvents.LEAVE, ID, ArmorWeightTask::clear);
        ServerTickEvents.END_LEVEL_TICK.register(level -> {
            if (level.getGameTime() % 20 != 0) {
                return;
            }
            if (!WorldGate.isModuleActive(level, ID)) {
                ArmorWeightTask.clearAll(level);
            }
        });
    }

    @Override
    public void serverTick(ServerLevel level) {
        flushRespawns(level);
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        WorldConfig world = ConfigManager.world(level);
        WeightCheckTask.run(level, world);
        ArmorWeightTask.run(level, world);
    }

    private static void flushRespawns(ServerLevel level) {
        if (PENDING_RESPAWNS.isEmpty()) {
            return;
        }
        long time = level.getGameTime();
        Iterator<PendingRespawn> iterator = PENDING_RESPAWNS.iterator();
        while (iterator.hasNext()) {
            PendingRespawn pending = iterator.next();
            if (time < pending.readyTick()) {
                continue;
            }
            iterator.remove();
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(pending.playerId());
            if (player == null || player.level() != level) {
                continue;
            }
            new SetPlayerHealthAndFoodTask(player, pending.health(), pending.food()).run();
        }
    }

    static boolean onAllowDamage(net.minecraft.world.entity.LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof ServerPlayer player)) {
            return true;
        }
        ServerLevel level = player.level();
        if (!WorldGate.isModuleActive(level, ID) || EhmApi.playerBypasses(player)) {
            return true;
        }
        PlayerSettings settings = ConfigManager.world(level).player();
        if (!settings.environmentEnable()) {
            return true;
        }
        PotionEffectHolder effect = effectFor(source, settings, amount);
        if (effect != null) {
            effect.apply(player);
        }
        return true;
    }

    public static float scaleIncomingDamage(ServerPlayer player, DamageSource source, float amount) {
        PlayerSettings settings = ConfigManager.world(player.level()).player();
        if (!settings.environmentEnable() || EhmApi.playerBypasses(player)) {
            return amount;
        }
        double multiplier = multiplierFor(source, settings, player);
        if (multiplier == 1.0) {
            return amount;
        }
        return (float) (amount * multiplier);
    }

    private static double multiplierFor(DamageSource source, PlayerSettings settings, ServerPlayer player) {
        if (source.is(DamageTypeTags.IS_FALL)) {
            return settings.fallMultiplier();
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            return settings.explosionMultiplier();
        }
        if (source.is(DamageTypes.IN_WALL)) {
            if (player.getVehicle() instanceof AbstractHorse) {
                return settings.suffocationMultiplier() / 2.0;
            }
            return settings.suffocationMultiplier();
        }
        if (source.is(DamageTypes.LAVA)) {
            return settings.lavaMultiplier();
        }
        if (source.is(DamageTypes.ON_FIRE)) {
            return settings.burnMultiplier();
        }
        if (source.is(DamageTypes.STARVE)) {
            return settings.starvationMultiplier();
        }
        if (source.is(DamageTypes.DROWN)) {
            return settings.drownMultiplier();
        }
        return 1.0;
    }

    private static PotionEffectHolder effectFor(DamageSource source, PlayerSettings settings, float amount) {
        if (source.is(DamageTypeTags.IS_FALL)) {
            return settings.fallEffect();
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            return amount > 2.0f ? settings.explosionEffect() : PotionEffectHolder.NONE;
        }
        if (source.is(DamageTypes.IN_WALL)) {
            return settings.suffocationEffect();
        }
        if (source.is(DamageTypes.LAVA)) {
            return settings.lavaEffect();
        }
        if (source.is(DamageTypes.ON_FIRE)) {
            return settings.burnEffect();
        }
        if (source.is(DamageTypes.STARVE)) {
            return settings.starvationEffect();
        }
        if (source.is(DamageTypes.DROWN)) {
            return settings.drownEffect();
        }
        return null;
    }

    static void onAfterRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
        if (alive) {
            return;
        }
        ServerLevel level = newPlayer.level();
        if (!WorldGate.isModuleActive(level, ID) || EhmApi.playerBypasses(newPlayer)) {
            return;
        }
        PlayerSettings settings = ConfigManager.world(level).player();
        if (!settings.respawnHealthEnable()) {
            return;
        }
        float health = newPlayer.getMaxHealth() * settings.respawnHealthPercent() / 100.0f;
        PENDING_RESPAWNS.add(new PendingRespawn(
                newPlayer.getUUID(), health, settings.respawnFood(), level.getGameTime() + 1));
    }

    public static void onDeath(ServerPlayer player) {
        ServerLevel level = player.level();
        if (!WorldGate.isModuleActive(level, ID)) {
            return;
        }
        if (inventoryBypasses(player)) {
            return;
        }
        PlayerSettings settings = ConfigManager.world(level).player();
        if (!settings.forfeitEnable()) {
            return;
        }
        List<ItemStack> drops = new ArrayList<>();
        collectNonEmpty(player, drops);
        List<ItemStack> eligible = new ArrayList<>();
        for (ItemStack stack : drops) {
            if (!stack.is(EhmTags.DEATH_ITEM_BLACKLIST)) {
                eligible.add(stack);
            }
        }
        int removeCount = DeathForfeit.stacksToRemove(eligible.size(), settings.forfeitPercent());
        if (removeCount <= 0) {
            return;
        }
        List<ItemStack> toRemove = new ArrayList<>(removeCount);
        List<ItemStack> pool = new ArrayList<>(eligible);
        for (int i = 0; i < removeCount && !pool.isEmpty(); i++) {
            toRemove.add(pool.remove(player.getRandom().nextInt(pool.size())));
        }
        PlayerInventoryLossEvent event = new PlayerInventoryLossEvent(player, drops, toRemove);
        PlayerInventoryLossEvent.EVENT.invoker().onInventoryLoss(event);
        if (event.isCancelled()) {
            return;
        }
        for (ItemStack stack : event.stacksToRemove()) {
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(EhmTags.DEATH_VALUABLE_TOOLS) && stack.isDamageableItem()) {
                damageTool(stack, settings);
            } else {
                stack.setCount(0);
            }
        }
    }

    public static void igniteFromFire(ServerPlayer player) {
        ServerLevel level = player.level();
        if (!WorldGate.isModuleActive(level, ID) || EhmApi.playerBypasses(player)) {
            return;
        }
        PlayerSettings settings = ConfigManager.world(level).player();
        if (!settings.extinguishIgnites()) {
            return;
        }
        PlayerExtinguishFireEvent event = new PlayerExtinguishFireEvent(player, settings.extinguishBurnTicks());
        PlayerExtinguishFireEvent.EVENT.invoker().onExtinguishFire(event);
        if (event.isCancelled()) {
            return;
        }
        player.igniteForTicks(event.burnTicks());
    }

    private static boolean inventoryBypasses(ServerPlayer player) {
        if (EhmApi.playerBypasses(player)) {
            return true;
        }
        WorldConfig config = ConfigManager.world(player.level());
        return config.checkPermission() && player.checkPermission(EhmPermissions.BYPASS_INVENTORY, false);
    }

    private static void collectNonEmpty(ServerPlayer player, List<ItemStack> out) {
        Inventory inventory = player.getInventory();
        for (ItemStack stack : inventory.getNonEquipmentItems()) {
            if (!stack.isEmpty()) {
                out.add(stack);
            }
        }
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty()) {
            out.add(offhand);
        }
        for (net.minecraft.world.entity.EquipmentSlot slot : List.of(
                net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET)) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                out.add(stack);
            }
        }
    }

    private static void damageTool(ItemStack stack, PlayerSettings settings) {
        int max = stack.getMaxDamage();
        if (max <= 0) {
            stack.setCount(0);
            return;
        }
        int extra = max * settings.toolDamagePercent() / 100;
        int damage = stack.getDamageValue() + extra;
        if (damage >= max) {
            if (settings.keepHeavilyDamagedTools()) {
                stack.setDamageValue(max - 1);
            } else {
                stack.setCount(0);
            }
            return;
        }
        stack.setDamageValue(damage);
    }
}

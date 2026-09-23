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
import dev.extrahardmode.item.FragileTools;
import dev.extrahardmode.player.DeathForfeit;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.task.ArmorWeightTask;
import dev.extrahardmode.task.SetPlayerHealthAndFoodTask;
import dev.extrahardmode.task.WeightCheckTask;
import dev.extrahardmode.world.EhmTags;
import dev.extrahardmode.world.WorldGate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Player-triggered Tougher rules: death forfeit, weak respawn, bed-camp
 * clear, environmental injuries, punching fire, inventory-weight drowning,
 * armor slowdown, leaky shields, and boats that smash on long falls.
 */
public final class Players implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("players");
    public static final Players INSTANCE = new Players();

    private static final List<PendingRespawn> PENDING_RESPAWNS = new ArrayList<>();
    private static final Map<UUID, PendingBedClear> PENDING_BED_CLEARS = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> UNSCALED_DAMAGE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private Players() {}

    private record PendingRespawn(UUID playerId, float health, int food, int readyTick) {}

    private record PendingBedClear(ResourceKey<Level> dimension, BlockPos bed) {}

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        bus.listen(ServerLivingEntityEvents.ALLOW_DAMAGE, ID, Players::onAllowDamage);
        bus.listen(ServerPlayerEvents.AFTER_RESPAWN, ID, Players::onAfterRespawn);
        bus.listen(ServerPlayerEvents.LEAVE, ID, player -> {
            ArmorWeightTask.clear(player);
            PENDING_BED_CLEARS.remove(player.getUUID());
        });
        bus.listen(UseBlockCallback.EVENT, ID, Players::onUseBlock);
        bus.listen(PlayerBlockBreakEvents.BEFORE, ID, Players::onBreakBurning);
        ServerTickEvents.END_SERVER_TICK.register(Players::flushRespawns);
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
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        WorldConfig world = ConfigManager.world(level);
        WeightCheckTask.run(level, world);
        ArmorWeightTask.run(level, world);
        for (ServerPlayer player : level.players()) {
            FragileTools.rescaleInventory(player);
        }
    }

    /** Server tick count, not per-dimension gameTime — nether/end respawns must not be eaten by overworld. */
    private static void flushRespawns(MinecraftServer server) {
        if (PENDING_RESPAWNS.isEmpty()) {
            return;
        }
        int tick = server.getTickCount();
        Iterator<PendingRespawn> iterator = PENDING_RESPAWNS.iterator();
        while (iterator.hasNext()) {
            PendingRespawn pending = iterator.next();
            if (tick < pending.readyTick()) {
                continue;
            }
            iterator.remove();
            ServerPlayer player = server.getPlayerList().getPlayer(pending.playerId());
            if (player == null) {
                continue;
            }
            ServerLevel level = player.level();
            if (!WorldGate.isModuleActive(level, ID) || EhmApi.playerBypasses(player)) {
                continue;
            }
            new SetPlayerHealthAndFoodTask(player, pending.health(), pending.food()).run();
        }
    }

    static boolean onAllowDamage(net.minecraft.world.entity.LivingEntity entity, DamageSource source, float amount) {
        if (isUnscaledDamage()) {
            return true;
        }
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

    /** Vanilla blocked amount after Tougher. Config percent of that amount is soaked. */
    public static float scaleBlockedDamage(LivingEntity entity, ServerLevel level, float blocked) {
        if (blocked <= 0.0F || !WorldGate.isModuleActive(level, ID)) {
            return blocked;
        }
        if (entity instanceof ServerPlayer player && EhmApi.playerBypasses(player)) {
            return blocked;
        }
        return ShieldRules.absorbed(blocked, ConfigManager.world(level).player().shieldAbsorbPercent());
    }

    /** Vanilla shield item-damage after Tougher, times the configured durability multiplier. */
    public static int scaleShieldDurability(LivingEntity entity, Level level, int amount) {
        if (amount <= 0 || !(level instanceof ServerLevel server) || !WorldGate.isModuleActive(server, ID)) {
            return amount;
        }
        if (entity instanceof ServerPlayer player && EhmApi.playerBypasses(player)) {
            return amount;
        }
        return ShieldRules.durabilityHit(amount, ConfigManager.world(server).player().shieldDurabilityMultiplier());
    }

    /**
     * Occupied boats that land after more than 3 blocks shatter with no drop, and
     * the riders take the fall. Returns true when the boat was removed.
     */
    public static boolean smashBoatIfLongFall(AbstractBoat boat, boolean onGround) {
        if (!(boat.level() instanceof ServerLevel level) || !WorldGate.isModuleActive(level, ID)) {
            return false;
        }
        List<ServerPlayer> riders = new ArrayList<>();
        for (Entity passenger : boat.getPassengers()) {
            if (passenger instanceof ServerPlayer player && !EhmApi.playerBypasses(player)) {
                riders.add(player);
            }
        }
        if (!BoatRules.shouldBreak(boat.fallDistance, onGround, !riders.isEmpty())) {
            return false;
        }
        double fall = boat.fallDistance;
        boat.ejectPassengers();
        boat.discard();
        DamageSource source = level.damageSources().fall();
        for (ServerPlayer player : riders) {
            player.fallDistance = fall;
            player.causeFallDamage(fall, 1.0F, source);
        }
        return true;
    }

    public static float scaleIncomingDamage(ServerPlayer player, DamageSource source, float amount) {
        if (isUnscaledDamage()) {
            return amount;
        }
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
            PENDING_BED_CLEARS.remove(newPlayer.getUUID());
            return;
        }
        clearPendingBedHostiles(newPlayer);
        PlayerSettings settings = ConfigManager.world(level).player();
        if (!settings.respawnHealthEnable()) {
            return;
        }
        float health = newPlayer.getMaxHealth() * settings.respawnHealthPercent() / 100.0f;
        int food = settings.respawnFood();
        new SetPlayerHealthAndFoodTask(newPlayer, health, food).run();
        MinecraftServer server = level.getServer();
        PENDING_RESPAWNS.add(new PendingRespawn(newPlayer.getUUID(), health, food, server.getTickCount() + 1));
    }

    public static void hurtUnscaled(ServerPlayer player, DamageSource source, float amount) {
        UNSCALED_DAMAGE.set(Boolean.TRUE);
        try {
            player.hurtServer(player.level(), source, amount);
        } finally {
            UNSCALED_DAMAGE.set(Boolean.FALSE);
        }
    }

    private static boolean isUnscaledDamage() {
        return Boolean.TRUE.equals(UNSCALED_DAMAGE.get());
    }

    static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        if (!(level instanceof ServerLevel serverLevel) || !WorldGate.isModuleActive(serverLevel, ID)) {
            return InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || stack.is(Items.WATER_BUCKET) || !(stack.getItem() instanceof BlockItem)) {
            return InteractionResult.PASS;
        }
        BlockPos pos = hit.getBlockPos();
        boolean fire = level.getBlockState(pos).getBlock() instanceof BaseFireBlock
                || level.getBlockState(pos.relative(hit.getDirection())).getBlock() instanceof BaseFireBlock;
        if (fire) {
            igniteFromFire(serverPlayer);
        }
        return InteractionResult.PASS;
    }

    public static void onDeath(ServerPlayer player) {
        ServerLevel level = player.level();
        if (!WorldGate.isModuleActive(level, ID)) {
            return;
        }
        if (!EhmApi.playerBypasses(player) && !player.isSpectator()) {
            noteDeathNearBed(player);
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

    /** Breaking fire with a tool, or breaking a block that has fire against it, ignites like punching fire. */
    static boolean onBreakBurning(
            Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.isSpectator()) {
            return true;
        }
        boolean brokenIsFire = state.getBlock() instanceof BaseFireBlock;
        boolean adjacentFire = false;
        if (!brokenIsFire) {
            for (Direction direction : Direction.values()) {
                if (level.getBlockState(pos.relative(direction)).getBlock() instanceof BaseFireBlock) {
                    adjacentFire = true;
                    break;
                }
            }
        }
        if (FireBreakRules.igniteOnBreak(brokenIsFire, adjacentFire, player.getMainHandItem().isEmpty())) {
            igniteFromFire(serverPlayer);
        }
        return true;
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

    static void noteDeathNearBed(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos bed = findBed(level, player);
        if (bed == null) {
            PENDING_BED_CLEARS.remove(player.getUUID());
            return;
        }
        BlockPos death = player.blockPosition();
        if (!BedDeathRules.withinRangeOfBed(
                death.getX(), death.getY(), death.getZ(), bed.getX(), bed.getY(), bed.getZ())) {
            PENDING_BED_CLEARS.remove(player.getUUID());
            return;
        }
        PENDING_BED_CLEARS.put(player.getUUID(), new PendingBedClear(level.dimension(), bed.immutable()));
        clearHostilesAroundBed(level, bed);
    }

    static void clearPendingBedHostiles(ServerPlayer player) {
        PendingBedClear pending = PENDING_BED_CLEARS.remove(player.getUUID());
        if (pending == null) {
            return;
        }
        ServerLevel level = player.level();
        if (!pending.dimension().equals(level.dimension())) {
            return;
        }
        clearHostilesAroundBed(level, pending.bed());
    }

    static BlockPos findBed(ServerLevel level, ServerPlayer player) {
        ServerPlayer.RespawnConfig config = player.getRespawnConfig();
        if (config == null) {
            return null;
        }
        LevelData.RespawnData data = config.respawnData();
        if (data == null || !data.dimension().equals(level.dimension())) {
            return null;
        }
        BlockPos origin = data.pos();
        if (level.getBlockState(origin).is(BlockTags.BEDS)) {
            return origin;
        }
        BlockPos min = origin.offset(-1, -1, -1);
        BlockPos max = origin.offset(1, 1, 1);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (level.getBlockState(pos).is(BlockTags.BEDS)) {
                return pos.immutable();
            }
        }
        return null;
    }

    static void clearHostilesAroundBed(ServerLevel level, BlockPos bed) {
        AABB box = new AABB(bed).inflate(BedDeathRules.RANGE + 1);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box, Players::isClearableHostile)) {
            BlockPos feet = mob.blockPosition();
            if (!BedDeathRules.withinRangeOfBed(
                    feet.getX(), feet.getY(), feet.getZ(), bed.getX(), bed.getY(), bed.getZ())) {
                continue;
            }
            mob.discard();
        }
    }

    static boolean isClearableHostile(Mob mob) {
        if (!mob.isAlive() || !(mob instanceof Enemy)) {
            return false;
        }
        if (mob instanceof WitherBoss || mob instanceof EnderDragon) {
            return false;
        }
        return !Boolean.TRUE.equals(mob.getAttachedOrElse(EhmAttachments.EHM_BIOME_BOSS, Boolean.FALSE));
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

package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.api.ExplosionType;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.DragonConfig;
import dev.extrahardmode.mixin.EnderDragonFightAccess;
import dev.extrahardmode.network.EhmNetworking;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.task.CreateExplosionTask;
import dev.extrahardmode.task.DragonAttackPatternTask;
import dev.extrahardmode.task.DragonAttackTask;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.item.ArmorStandItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.EndCrystalItem;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.HangingEntityItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.end.DragonRespawnStage;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class Dragon implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("dragon");

    private static final Map<Identifier, ArrayDeque<DragonAttackTask>> ATTACKS = new ConcurrentHashMap<>();
    private static final Map<Identifier, ArrayDeque<DragonAttackPatternTask>> PATTERNS = new ConcurrentHashMap<>();
    private static final Map<Identifier, ArrayDeque<CreateExplosionTask>> BLASTS = new ConcurrentHashMap<>();
    private static final Map<UUID, String> FIGHTERS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> HEALED = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> LOOTED = new ConcurrentHashMap<>();

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        bus.listen(ServerEntityEvents.ENTITY_LOAD, ID, Dragon::onEntityLoad);
        bus.listen(ServerLivingEntityEvents.AFTER_DEATH, ID, Dragon::onDeath);
        bus.listen(UseBlockCallback.EVENT, ID, (player, level, hand, hit) -> denyUse(player, level, hand));
        bus.listen(UseItemCallback.EVENT, ID, Dragon::denyUse);
        bus.listen(
                ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL,
                ID,
                (player, origin, destination) -> {
                    if (!enabled(origin)) {
                        return;
                    }
                    onPlayerChangeLevel(player, origin);
                });
        bus.listen(ServerPlayConnectionEvents.DISCONNECT, ID, (handler, server) -> {
            ServerPlayer player = handler.player;
            if (!(player.level() instanceof ServerLevel level)) {
                return;
            }
            if (!enabled(level)) {
                return;
            }
            onPlayerChangeLevel(player, level);
        });
    }

    @Override
    public void onWorldUnload(ServerLevel level) {
        Identifier id = level.dimension().identifier();
        ATTACKS.remove(id);
        PATTERNS.remove(id);
        BLASTS.remove(id);
    }

    @Override
    public void serverTick(ServerLevel level) {
        Identifier id = level.dimension().identifier();
        ArrayDeque<CreateExplosionTask> blasts = BLASTS.get(id);
        if (blasts != null && !blasts.isEmpty()) {
            int snapshot = blasts.size();
            for (int i = 0; i < snapshot && !blasts.isEmpty(); i++) {
                CreateExplosionTask task = blasts.pollFirst();
                if (task == null) {
                    break;
                }
                if (!task.tick()) {
                    blasts.addLast(task);
                }
            }
        }
        ArrayDeque<DragonAttackTask> attacks = ATTACKS.get(id);
        if (attacks != null && !attacks.isEmpty()) {
            int snapshot = attacks.size();
            for (int i = 0; i < snapshot && !attacks.isEmpty(); i++) {
                DragonAttackTask task = attacks.pollFirst();
                if (task == null) {
                    break;
                }
                if (!task.tick(level)) {
                    attacks.addLast(task);
                }
            }
        }
        ArrayDeque<DragonAttackPatternTask> patterns = PATTERNS.get(id);
        if (patterns != null && !patterns.isEmpty()) {
            DragonAttackPatternTask.Scheduler scheduler = new DragonAttackPatternTask.Scheduler() {
                @Override
                public void scheduleAttack(DragonAttackTask task) {
                    enqueueAttack(level, task);
                }

                @Override
                public void onFighterDefeated(ServerLevel world, EnderDragon dragon, UUID playerId) {
                    defeatFighter(world, dragon, playerId, true);
                }
            };
            int snapshot = patterns.size();
            for (int i = 0; i < snapshot && !patterns.isEmpty(); i++) {
                DragonAttackPatternTask task = patterns.pollFirst();
                if (task == null) {
                    break;
                }
                if (!task.tick(level, scheduler)) {
                    patterns.addLast(task);
                }
            }
        }
    }

    public static boolean enabled(Level level) {
        return FeatureBus.guard(level, ID);
    }

    public static InteractionResult denyUse(Player player, Level level, InteractionHand hand) {
        if (!enabled(level) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!shouldDenyEndUse(serverLevel, player, stack)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            EhmNetworking.sendToast(serverPlayer, "limited_end_building");
        }
        return InteractionResult.FAIL;
    }

    public static InteractionResult denyPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        if (!enabled(level) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        if (!shouldDenyEndUse(serverLevel, context.getPlayer(), context.getItemInHand())) {
            return InteractionResult.PASS;
        }
        if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
            EhmNetworking.sendToast(serverPlayer, "limited_end_building");
        }
        return InteractionResult.FAIL;
    }

    public static boolean allowPlaceItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        String id = stack.typeHolder()
                .unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse("");
        return DragonRules.allowEndPlace(id);
    }

    public static boolean isPlacementItem(Item item) {
        return item instanceof BlockItem
                || item instanceof BucketItem
                || item instanceof EndCrystalItem
                || item instanceof FlintAndSteelItem
                || item instanceof FireChargeItem
                || item instanceof SpawnEggItem
                || item instanceof ArmorStandItem
                || item instanceof HangingEntityItem
                || item instanceof BoatItem
                || item instanceof MinecartItem;
    }

    static boolean shouldDenyEndUse(ServerLevel level, Player player, ItemStack stack) {
        if (level.dimension() != Level.END) {
            return false;
        }
        boolean bypass = false;
        if (player != null) {
            bypass = player.hasInfiniteMaterials()
                    || player.isCreative()
                    || (player instanceof ServerPlayer serverPlayer && EhmApi.playerBypasses(serverPlayer));
        }
        boolean empty = stack == null || stack.isEmpty();
        return DragonRules.denyEndUse(
                ConfigManager.world(level).dragon().noBuilding(),
                true,
                bypass,
                empty,
                allowPlaceItem(stack),
                !empty && isPlacementItem(stack.getItem()));
    }

    public static void applyHealth(EnderDragon dragon, ServerLevel level) {
        if (!enabled(level)) {
            return;
        }
        int configured = ConfigManager.world(level).dragon().health();
        if (configured <= 0) {
            return;
        }
        AttributeInstance attribute = dragon.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) {
            return;
        }
        double previous = attribute.getBaseValue();
        float health = dragon.getHealth();
        boolean fill = DragonRules.shouldFillHealth(previous, health, configured);
        if (previous != configured) {
            attribute.setBaseValue(configured);
        }
        if (fill) {
            dragon.setHealth((float) configured);
        } else {
            dragon.setHealth(Math.min(dragon.getHealth(), (float) configured));
        }
    }

    public static void onDamaged(EnderDragon dragon, DamageSource source) {
        if (!(dragon.level() instanceof ServerLevel level) || !enabled(level)) {
            return;
        }
        DragonConfig config = ConfigManager.world(level).dragon();
        if (!config.harderBattle()) {
            return;
        }
        ServerPlayer damager = playerFrom(source);
        if (damager == null || damager.level() != level) {
            return;
        }
        UUID id = damager.getUUID();
        boolean first = FIGHTERS.put(id, damager.getScoreboardName()) == null;
        if (first) {
            HEALED.remove(id);
            enqueuePattern(level, dragon.getUUID(), id, 1);
            if (config.announcements()) {
                announce(level, "extrahardmode.chat.dragon_challenge", damager.getScoreboardName());
                ExtraHardModeMod.LOGGER.info("EHM dragon: {} is challenging the dragon", damager.getScoreboardName());
            }
        }
        RandomSource random = level.getRandom();
        for (int i = 0; i < 5; i++) {
            enqueueAttack(level, new DragonAttackTask(dragon.getUUID(), id, 20 * random.nextInt(15)));
        }
        aggroEndermen(level, damager);
    }

    public static void onFireballHit(DragonFireball fireball, HitResult hit) {
        if (!(fireball.level() instanceof ServerLevel level) || !enabled(level)) {
            return;
        }
        DragonConfig config = ConfigManager.world(level).dragon();
        if (!config.harderBattle()) {
            return;
        }
        Entity owner = fireball.getOwner();
        if (!(owner instanceof EnderDragon dragon)) {
            return;
        }
        Vec3 origin = hit.getLocation();
        Entity source = dragon.isRemoved() ? fireball : dragon;
        BLASTS.computeIfAbsent(level.dimension().identifier(), id -> new ArrayDeque<>())
                .addLast(new CreateExplosionTask(level, origin, ExplosionType.DRAGON_FIREBALL, source, 1));
        spawnShrapnel(level, origin);
        spawnMinions(level, origin, config.alternativeMinions());
    }

    public static void onDragonKilled(EnderDragon dragon) {
        if (!(dragon.level() instanceof ServerLevel level) || !enabled(level)) {
            return;
        }
        if (LOOTED.put(dragon.getUUID(), Boolean.TRUE) != null) {
            return;
        }
        DragonConfig config = ConfigManager.world(level).dragon();
        BlockPos podium = podium(level);
        if (config.dropEgg()) {
            drop(level, Vec3.atCenterOf(podium.above(2)), new ItemStack(Items.DRAGON_EGG));
        }
        if (config.dropVillagerEggs()) {
            drop(level, Vec3.atCenterOf(podium.offset(2, 2, 0)), new ItemStack(Items.VILLAGER_SPAWN_EGG, 2));
            for (String name : FIGHTERS.values()) {
                ServerPlayer player = level.getServer().getPlayerList().getPlayerByName(name);
                if (player != null) {
                    EhmNetworking.sendToast(player, "dragon_fountain_tip");
                }
            }
        }
        if (config.announcements()) {
            String names = String.join(", ", FIGHTERS.values());
            announce(level, "extrahardmode.chat.dragon_killed", names);
            ExtraHardModeMod.LOGGER.info("EHM dragon defeated by {}", names);
        }
        FIGHTERS.clear();
        HEALED.clear();
        Identifier id = level.dimension().identifier();
        ATTACKS.remove(id);
        PATTERNS.remove(id);
        BLASTS.remove(id);
    }

    public static boolean blocksTarget(LivingEntity target, Level level) {
        return enabled(level) && target instanceof EnderDragon;
    }

    public static boolean skipLoot(Entity entity) {
        return entity instanceof Mob
                && Boolean.TRUE.equals(entity.getAttachedOrElse(EhmAttachments.EHM_OURS, Boolean.FALSE));
    }

    private static void onEntityLoad(Entity entity, ServerLevel level) {
        if (!enabled(level)) {
            return;
        }
        if (entity instanceof EnderDragon dragon) {
            applyHealth(dragon, level);
        }
    }

    private static void onDeath(LivingEntity entity, DamageSource source) {
        if (!(entity.level() instanceof ServerLevel level) || !enabled(level)) {
            return;
        }
        if (entity instanceof ServerPlayer player) {
            if (FIGHTERS.containsKey(player.getUUID())) {
                EnderDragon dragon = null;
                for (EnderDragon candidate : level.getDragons()) {
                    if (candidate.isAlive()) {
                        dragon = candidate;
                        break;
                    }
                }
                if (ConfigManager.world(level).dragon().announcements()) {
                    announce(level, "extrahardmode.chat.dragon_player_killed", player.getScoreboardName());
                    ExtraHardModeMod.LOGGER.info(
                            "EHM dragon: {} was killed fighting the dragon", player.getScoreboardName());
                }
                defeatFighter(level, dragon, player.getUUID(), true);
            }
        }
    }

    private static void onPlayerChangeLevel(ServerPlayer player, ServerLevel origin) {
        if (!enabled(origin)) {
            return;
        }
        if (origin.dimension() != Level.END) {
            return;
        }
        FIGHTERS.remove(player.getUUID());
        if (noOtherPlayers(origin, player)) {
            onEndEmpty(origin);
        }
    }

    private static boolean noOtherPlayers(ServerLevel level, ServerPlayer leaving) {
        for (ServerPlayer player : level.players()) {
            if (player != leaving) {
                return false;
            }
        }
        return true;
    }

    private static void onEndEmpty(ServerLevel level) {
        if (!enabled(level)) {
            return;
        }
        discardMinions(level);
        EnderDragonFight fight = level.getDragonFight();
        boolean dragonAlive = false;
        for (EnderDragon dragon : level.getDragons()) {
            if (dragon.isAlive()) {
                dragonAlive = true;
                applyHealth(dragon, level);
                dragon.setHealth(dragon.getMaxHealth());
            }
        }
        DragonConfig config = ConfigManager.world(level).dragon();
        boolean ritual = fight != null && ((EnderDragonFightAccess) fight).extrahardmode$respawnStage() != null;
        boolean killed = fight != null && ((EnderDragonFightAccess) fight).extrahardmode$dragonKilled();
        if (DragonRules.shouldAutoRespawn(config.autoRespawn(), killed, ritual, dragonAlive) && fight != null) {
            DragonRespawnStage stage = ((EnderDragonFightAccess) fight).extrahardmode$respawnStage();
            if (stage == null) {
                EnderDragon spawned = ((EnderDragonFightAccess) fight).extrahardmode$createNewDragon();
                if (spawned != null) {
                    applyHealth(spawned, level);
                }
            }
        }
        FIGHTERS.clear();
        HEALED.clear();
        Identifier id = level.dimension().identifier();
        ATTACKS.remove(id);
        PATTERNS.remove(id);
        BLASTS.remove(id);
    }

    private static void spawnMinions(ServerLevel level, Vec3 origin, boolean alternative) {
        int random = alternative ? level.getRandom().nextInt(150) : level.getRandom().nextInt(100);
        DragonRules.MinionRoll roll = DragonRules.roll(random, alternative);
        BlockPos pos = BlockPos.containing(origin);
        switch (roll) {
            case BLAZE -> {
                spawnMinion(level, pos, EntityTypes.BLAZE);
                igniteGround(level, pos);
            }
            case ZOMBIE_VILLAGERS -> {
                spawnMinion(level, pos, EntityTypes.ZOMBIE_VILLAGER);
                spawnMinion(level, pos, EntityTypes.ZOMBIE_VILLAGER);
            }
            case SKELETONS -> {
                spawnMinion(level, pos, EntityTypes.SKELETON);
                spawnMinion(level, pos, EntityTypes.SKELETON);
            }
            case ENDERMAN -> spawnMinion(level, pos, EntityTypes.ENDERMAN);
            case NONE -> {}
        }
    }

    private static void spawnMinion(ServerLevel level, BlockPos pos, net.minecraft.world.entity.EntityType<? extends Mob> type) {
        Mob mob = type.spawn(level, pos, EntitySpawnReason.MOB_SUMMONED);
        if (mob == null) {
            return;
        }
        mob.setAttached(EhmAttachments.EHM_OURS, Boolean.TRUE);
        mob.skipDropExperience();
        targetFighter(level, mob);
    }

    private static void igniteGround(ServerLevel level, BlockPos origin) {
        BlockState fire = Blocks.FIRE.defaultBlockState();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 2; y >= -2; y--) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (!level.getBlockState(pos).isAir()) {
                        continue;
                    }
                    BlockState below = level.getBlockState(pos.below());
                    if (below.isAir() || below.is(Blocks.FIRE)) {
                        continue;
                    }
                    if (fire.canSurvive(level, pos)) {
                        level.setBlock(pos, fire, Block.UPDATE_ALL);
                    }
                }
            }
        }
    }

    private static void spawnShrapnel(ServerLevel level, Vec3 origin) {
        BlockPos air = BlockPos.containing(origin.x, origin.y + 1.0, origin.z);
        if (!level.getBlockState(air).isAir() && !level.getBlockState(air).is(Blocks.FIRE)) {
            air = air.above();
        }
        if (!level.getBlockState(air).isAir() && !level.getBlockState(air).is(Blocks.FIRE)) {
            return;
        }
        for (int i = 0; i < 10; i++) {
            launchFalling(level, air, Blocks.FIRE.defaultBlockState());
        }
        for (int i = 0; i < 4; i++) {
            launchFalling(level, air, Blocks.NETHERRACK.defaultBlockState());
        }
    }

    private static void launchFalling(ServerLevel level, BlockPos pos, BlockState state) {
        FallingBlockEntity entity = FallingBlockEntity.fall(level, pos, state);
        entity.setAttached(EhmAttachments.EHM_OURS, Boolean.TRUE);
        entity.dropItem = false;
        RandomSource random = level.getRandom();
        double x = random.nextDouble();
        double y = random.nextDouble();
        double z = random.nextDouble();
        if (random.nextBoolean()) {
            x = -x;
        }
        if (random.nextBoolean()) {
            z = -z;
        }
        entity.setDeltaMovement(x, y, z);
    }

    private static void aggroEndermen(ServerLevel level, ServerPlayer player) {
        AABB box = player.getBoundingBox().inflate(16.0);
        for (EnderMan enderman : level.getEntities(EntityTypes.ENDERMAN, box, Entity::isAlive)) {
            enderman.setTarget(player);
        }
    }

    private static void targetFighter(ServerLevel level, Mob mob) {
        ServerPlayer nearest = null;
        double best = 64.0 * 64.0;
        for (UUID id : FIGHTERS.keySet()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
            if (player == null || player.level() != level) {
                continue;
            }
            double dist = player.distanceToSqr(mob);
            if (dist < best) {
                best = dist;
                nearest = player;
            }
        }
        if (nearest != null) {
            mob.setTarget(nearest);
        }
    }

    private static void discardMinions(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Blaze
                    || entity instanceof Zombie
                    || entity instanceof ZombieVillager
                    || entity instanceof Skeleton) {
                if (Boolean.TRUE.equals(entity.getAttachedOrElse(EhmAttachments.EHM_OURS, Boolean.FALSE))) {
                    entity.discard();
                }
            }
        }
    }

    private static void defeatFighter(ServerLevel level, EnderDragon dragon, UUID playerId, boolean heal) {
        FIGHTERS.remove(playerId);
        if (!heal || dragon == null || !dragon.isAlive()) {
            return;
        }
        if (HEALED.put(playerId, Boolean.TRUE) != null) {
            return;
        }
        int percent = ConfigManager.world(level).dragon().healOnPlayerKillPercent();
        float amount = DragonRules.healAmount(dragon.getMaxHealth(), percent);
        if (amount > 0.0F) {
            dragon.heal(amount);
        }
    }

    private static void enqueueAttack(ServerLevel level, DragonAttackTask task) {
        ATTACKS.computeIfAbsent(level.dimension().identifier(), id -> new ArrayDeque<>()).addLast(task);
    }

    private static void enqueuePattern(ServerLevel level, UUID dragonId, UUID playerId, int delay) {
        PATTERNS.computeIfAbsent(level.dimension().identifier(), id -> new ArrayDeque<>())
                .addLast(new DragonAttackPatternTask(dragonId, playerId, delay));
    }

    private static ServerPlayer playerFrom(DamageSource source) {
        Entity entity = source.getEntity();
        if (entity instanceof ServerPlayer player) {
            return player;
        }
        if (source.getDirectEntity() instanceof Projectile projectile
                && projectile.getOwner() instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }

    private static BlockPos podium(ServerLevel level) {
        EnderDragonFight fight = level.getDragonFight();
        if (fight != null) {
            BlockPos exit = ((EnderDragonFightAccess) fight).extrahardmode$exitPortalLocation();
            if (exit != null) {
                return exit;
            }
        }
        return new BlockPos(0, 64, 0);
    }

    private static void drop(ServerLevel level, Vec3 pos, ItemStack stack) {
        ItemEntity item = new ItemEntity(level, pos.x, pos.y, pos.z, stack);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }

    private static void announce(ServerLevel level, String key, String arg) {
        Component message = Component.translatable(key, arg);
        level.getServer().getPlayerList().broadcastSystemMessage(message, false);
    }
}

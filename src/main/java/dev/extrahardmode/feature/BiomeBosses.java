package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.feature.monster.PigMen;
import dev.extrahardmode.item.EhmItems;
import dev.extrahardmode.module.SpawnReplaceService;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.tag.EhmTags;
import dev.extrahardmode.world.BiomeBossData;
import dev.extrahardmode.world.WorldGate;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Rare persistent biome-family bosses. Each family appears once per game.
 * Biome is sampled at the player (caves included); Deep Dark and the End are skipped.
 */
public final class BiomeBosses implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("biome_bosses");
    private static final int SPAWN_TRIES = 32;
    private static final Identifier HEALTH_MOD = ExtraHardModeMod.id("biome_boss_health");
    private static final Identifier SCALE_MOD = ExtraHardModeMod.id("biome_boss_scale");
    private static final Identifier ATTACK_MOD = ExtraHardModeMod.id("biome_boss_attack");
    private static final Identifier KNOCKBACK_MOD = ExtraHardModeMod.id("biome_boss_knockback");
    private static final Identifier DISTANCE_MOD = ExtraHardModeMod.id("biome_boss_distance");
    private static final Identifier DEFEAT_MOD = ExtraHardModeMod.id("biome_boss_defeats");
    private static final Identifier BROOD_SPEED_MOD = ExtraHardModeMod.id("biome_boss_brood_speed");
    private static long creditsAtTick = -1L;
    /** Loaded bosses only. An unloaded boss drops its bar and gets a new one when the chunk loads. */
    private static final Map<UUID, TrackedBar> BARS = new HashMap<>();
    /** Last open position of a loaded boss, and when another unstuck search is allowed. */
    private static final Map<UUID, RescueState> RESCUE = new HashMap<>();
    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final Item[][] ARMOR_TIERS = {
        {
            Items.LEATHER_HELMET,
            Items.LEATHER_CHESTPLATE,
            Items.LEATHER_LEGGINGS,
            Items.LEATHER_BOOTS
        },
        {
            Items.CHAINMAIL_HELMET,
            Items.CHAINMAIL_CHESTPLATE,
            Items.CHAINMAIL_LEGGINGS,
            Items.CHAINMAIL_BOOTS
        },
        {Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS},
        {Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS},
        {Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS}
    };

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        bus.listen(ServerLivingEntityEvents.AFTER_DEATH, ID, BiomeBosses::onDeath);
        bus.listen(ServerLivingEntityEvents.AFTER_DAMAGE, ID, BiomeBosses::onAttacked);
        ServerTickEvents.END_LEVEL_TICK.register(BiomeBosses::tickBossBars);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            dismissAllBars();
            RESCUE.clear();
        });
    }

    @Override
    public void onWorldUnload(ServerLevel level) {
        Identifier dimension = level.dimension().identifier();
        dismissDimension(dimension);
        RESCUE.entrySet().removeIf(entry -> dimension.equals(entry.getValue().dimension()));
    }

    @Override
    public void serverTick(ServerLevel level) {
        tickCredits(level);
        if (WorldGate.isModuleActive(level, ID)) {
            tickBroods(level);
            BossMining.ensureGoals(level);
        }
        if (level.getDifficulty() == Difficulty.PEACEFUL || level.dimension() == Level.END) {
            return;
        }
        WorldConfig config = ConfigManager.world(level);
        int interval = Math.max(20, config.bossCheckIntervalTicks());
        if (level.getGameTime() % interval != 0) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            trySpawnNear(level, player, config);
        }
    }

    static void onDeath(LivingEntity entity, DamageSource source) {
        if (!(entity.level() instanceof ServerLevel level) || !isBoss(entity)) {
            return;
        }
        dismissBar(entity.getUUID());
        if (entity instanceof Mob mob) {
            BossMining.stop(mob);
        }
        BiomeBossData.of(level).onDeath(entity.getUUID());
        if (!WorldGate.isModuleActive(level, ID)) {
            return;
        }
        WorldConfig config = ConfigManager.world(level);
        double treasure = treasureMultiplier(entity, config);
        dropSpecialLoot(level, entity, config, treasure);
        ExperienceOrb.award(
                level, entity.position(), BiomeBossesRules.scaleXp(BiomeBossesRules.DEFAULT_BONUS_XP, treasure));
        int defeated = BiomeBossData.campaign(level).recordDefeat();
        onBossDefeated(level, entity, defeated);
    }

    public static boolean isBoss(Entity entity) {
        return entity != null
                && Boolean.TRUE.equals(entity.getAttachedOrElse(EhmAttachments.EHM_BIOME_BOSS, Boolean.FALSE));
    }

    public static boolean anySpawned(MinecraftServer server) {
        if (server == null) {
            return false;
        }
        for (ServerLevel level : server.getAllLevels()) {
            BiomeBossData data = BiomeBossData.of(level);
            for (BossFamily family : BossFamily.values()) {
                if (data.hasLiving(level, family)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static List<Entity> loadedBosses(MinecraftServer server) {
        List<Entity> found = new ArrayList<>();
        if (server == null) {
            return found;
        }
        Set<UUID> seen = new HashSet<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (BiomeBossData.FamilyState state : BiomeBossData.of(level).families().values()) {
                Optional<UUID> living = state.living();
                if (living.isEmpty() || !seen.add(living.get())) {
                    continue;
                }
                Entity entity = level.getEntityInAnyDimension(living.get());
                if (entity != null && entity.isAlive() && isBoss(entity)) {
                    found.add(entity);
                }
            }
        }
        return found;
    }

    /** Same-dimension bosses win; otherwise the nearest loaded boss in any dimension. */
    public static Entity nearestBoss(Entity origin) {
        if (!(origin != null && origin.level() instanceof ServerLevel level) || level.getServer() == null) {
            return null;
        }
        Entity best = null;
        boolean bestSame = false;
        double bestDist = Double.POSITIVE_INFINITY;
        for (Entity boss : loadedBosses(level.getServer())) {
            boolean same = boss.level() == origin.level();
            double dist = origin.distanceToSqr(boss);
            if (best == null || (same && !bestSame) || (same == bestSame && dist < bestDist)) {
                best = boss;
                bestSame = same;
                bestDist = dist;
            }
        }
        return best;
    }

    public static BossFamily familyOf(Entity entity) {
        if (entity == null) {
            return null;
        }
        return BossFamily.byId(entity.getAttached(EhmAttachments.EHM_BOSS_FAMILY));
    }

    public static boolean isBroodBoss(Entity entity) {
        if (!isBoss(entity)) {
            return false;
        }
        BossFamily family = familyOf(entity);
        return family != null && family.brood();
    }

    static void trySpawnNear(ServerLevel level, ServerPlayer player, WorldConfig config) {
        if (player.isSpectator() || player.isCreative()) {
            return;
        }
        BlockPos origin = spawnOrigin(level);
        if (!BiomeBossesRules.farEnough(
                player.getX() - origin.getX(), player.getZ() - origin.getZ(), config.bossMinDistanceFromSpawn())) {
            return;
        }
        BossFamily family = familyAt(level, player.blockPosition());
        if (family == null) {
            return;
        }
        BiomeBossData data = BiomeBossData.of(level);
        if (data.hasLiving(level, family)) {
            return;
        }
        if (familySpent(level, family)) {
            return;
        }
        BlockPos pos = findSpawnPos(level, player, family);
        if (pos == null) {
            return;
        }
        if (!BiomeBossesRules.spawnRoll(level.getRandom().nextInt(100), config.bossSpawnChancePercent())) {
            return;
        }
        long now = System.currentTimeMillis();
        Entity spawned = family.entityType().spawn(level, pos, EntitySpawnReason.EVENT);
        if (!(spawned instanceof Mob mob)) {
            if (spawned != null) {
                spawned.discard();
            }
            return;
        }
        double distance = BiomeBossesRules.horizontalDistance(
                mob.getX() - origin.getX(), mob.getZ() - origin.getZ());
        stampBoss(mob, family, config, distance);
        data.markSpawned(family, mob.getUUID(), now);
        BiomeBossData.campaign(level).markAppeared(family);
        announce(level, mob, family);
        thunderAll(level.getServer());
    }

    /** True once this family has spawned in any dimension of this game. */
    static boolean familySpent(ServerLevel level, BossFamily family) {
        long lastSpawn = 0L;
        for (ServerLevel world : level.getServer().getAllLevels()) {
            lastSpawn = Math.max(lastSpawn, BiomeBossData.of(world).state(family).lastSpawnEpochMs());
        }
        return BiomeBossesRules.familyAlreadyUsed(lastSpawn, BiomeBossData.campaign(level).hasAppeared(family));
    }

    static BossFamily familyAt(ServerLevel level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        if (biome.is(EhmTags.NO_BIOME_BOSSES)) {
            return null;
        }
        for (BossFamily family : BossFamily.values()) {
            if (biome.is(family.biomes())) {
                return family;
            }
        }
        return null;
    }

    static BlockPos spawnOrigin(ServerLevel level) {
        LevelData.RespawnData respawn = level.getServer().getRespawnData();
        if (respawn.dimension().equals(level.dimension())) {
            return respawn.pos();
        }
        return BlockPos.ZERO;
    }

    static BlockPos findSpawnPos(ServerLevel level, ServerPlayer player, BossFamily family) {
        RandomSource random = level.getRandom();
        BlockPos playerPos = player.blockPosition();
        int clearance = BiomeBossesRules.SPAWN_CLEARANCE_BLOCKS;
        for (int attempt = 0; attempt < SPAWN_TRIES; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            int dist = BiomeBossesRules.spawnSearchDistance(
                    random.nextInt(), clearance, BiomeBossesRules.SPAWN_SEARCH_EXTRA);
            int x = playerPos.getX() + (int) Math.round(Math.cos(angle) * dist);
            int z = playerPos.getZ() + (int) Math.round(Math.sin(angle) * dist);
            int y = playerPos.getY() + random.nextInt(17) - 8;
            BlockPos cursor = new BlockPos(x, y, z);
            for (int drop = 0; drop < 24; drop++) {
                if (!level.isLoaded(cursor)) {
                    break;
                }
                if (suitable(level, cursor, family.aquatic())
                        && familyAt(level, cursor) == family
                        && clearOfPlayersAndChests(level, cursor, clearance)) {
                    return cursor;
                }
                cursor = cursor.below();
            }
        }
        return null;
    }

    /**
     * 3D distance. Chests are read from loaded chunks only, so a chest in an unloaded chunk is not seen.
     * The candidate itself is loaded, and it sits just past 80 blocks from a player, so nearby bases usually are too.
     */
    static boolean clearOfPlayersAndChests(ServerLevel level, BlockPos pos, int clearance) {
        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;
        for (ServerPlayer player : level.players()) {
            if (BiomeBossesRules.tooClose(player.getX() - x, player.getY() - y, player.getZ() - z, clearance)) {
                return false;
            }
        }
        return !chestWithin(level, pos, x, y, z, clearance);
    }

    static boolean chestWithin(ServerLevel level, BlockPos pos, double x, double y, double z, int clearance) {
        int minCx = (pos.getX() - clearance) >> 4;
        int maxCx = (pos.getX() + clearance) >> 4;
        int minCz = (pos.getZ() - clearance) >> 4;
        int maxCz = (pos.getZ() + clearance) >> 4;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(cx, cz);
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!isChest(blockEntity)) {
                        continue;
                    }
                    BlockPos chest = blockEntity.getBlockPos();
                    if (BiomeBossesRules.tooClose(
                            chest.getX() + 0.5 - x, chest.getY() + 0.5 - y, chest.getZ() + 0.5 - z, clearance)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    static boolean isChest(BlockEntity blockEntity) {
        return blockEntity instanceof ChestBlockEntity || blockEntity instanceof EnderChestBlockEntity;
    }

    static boolean suitable(ServerLevel level, BlockPos pos, boolean aquatic) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState below = level.getBlockState(pos.below());
        FluidState fluid = feet.getFluidState();
        if (aquatic) {
            return fluid.is(FluidTags.WATER)
                    && (head.isAir() || head.getFluidState().is(FluidTags.WATER));
        }
        return below.isSolid() && feet.isAir() && head.isAir() && fluid.isEmpty();
    }

    static void stampBoss(Mob mob, BossFamily family, WorldConfig config, double distanceFromSpawn) {
        mob.setAttached(EhmAttachments.EHM_BIOME_BOSS, Boolean.TRUE);
        mob.setAttached(EhmAttachments.EHM_BOSS_FAMILY, family.id());
        int steps = BiomeBossesRules.distanceSteps(
                distanceFromSpawn, config.bossMinDistanceFromSpawn(), config.bossDistanceStepBlocks());
        double difficultyPercent =
                BiomeBossesRules.difficultyPercent(steps, config.bossDifficultyPercentPerStep());
        mob.setAttached(EhmAttachments.EHM_BOSS_DIFFICULTY_PERCENT, difficultyPercent);
        int defeated = 0;
        if (mob.level() instanceof ServerLevel server) {
            defeated = BiomeBossData.campaign(server).defeated();
        }
        mob.setAttached(EhmAttachments.EHM_BOSS_DEFEAT_COUNT, defeated);
        mob.setPersistenceRequired();
        mob.setCustomName(Component.translatableWithFallback(family.translationKey(), family.fallbackName()));
        mob.setCustomNameVisible(true);
        if (mob instanceof Zombie zombie) {
            zombie.setBaby(false);
        }
        if (mob instanceof MagmaCube cube) {
            cube.setSize(3, true);
        }
        if (mob instanceof Hoglin hoglin) {
            hoglin.setImmuneToZombification(true);
        }
        RandomSource random = mob.getRandom();
        int healthMultiplier = BiomeBossesRules.randomHealthMultiplier(
                random.nextInt(), config.bossHealthMultiplierMin(), config.bossHealthMultiplierMax());
        applyModifier(
                mob.getAttribute(Attributes.MAX_HEALTH),
                HEALTH_MOD,
                BiomeBossesRules.extraMultiplierAmount(healthMultiplier),
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        equipRandomArmor(mob, BiomeBossesRules.armorTier(random.nextInt()));
        applyModifier(
                mob.getAttribute(Attributes.SCALE),
                SCALE_MOD,
                Math.max(0.0, config.bossScale() - 1.0),
                AttributeModifier.Operation.ADD_VALUE);
        applyModifier(
                mob.getAttribute(Attributes.ATTACK_DAMAGE),
                ATTACK_MOD,
                BiomeBossesRules.extraMultiplierAmount(config.bossAttackMultiplier()),
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        applyModifier(
                mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE),
                KNOCKBACK_MOD,
                0.6,
                AttributeModifier.Operation.ADD_VALUE);
        double distanceBonus = BiomeBossesRules.difficultyBonusAmount(difficultyPercent);
        applyModifier(
                mob.getAttribute(Attributes.MAX_HEALTH),
                DISTANCE_MOD,
                distanceBonus,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        applyModifier(
                mob.getAttribute(Attributes.ATTACK_DAMAGE),
                DISTANCE_MOD,
                distanceBonus,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        applyModifier(
                mob.getAttribute(Attributes.MOVEMENT_SPEED),
                DISTANCE_MOD,
                distanceBonus,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        applyModifier(
                mob.getAttribute(Attributes.FLYING_SPEED),
                DISTANCE_MOD,
                distanceBonus,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        double defeatBonus =
                BiomeBossesRules.extraMultiplierAmount(BiomeBossesRules.defeatCompound(defeated));
        applyModifier(
                mob.getAttribute(Attributes.MAX_HEALTH),
                DEFEAT_MOD,
                defeatBonus,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        applyModifier(
                mob.getAttribute(Attributes.ATTACK_DAMAGE),
                DEFEAT_MOD,
                defeatBonus,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        applyModifier(
                mob.getAttribute(Attributes.MOVEMENT_SPEED),
                DEFEAT_MOD,
                defeatBonus,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        applyModifier(
                mob.getAttribute(Attributes.FLYING_SPEED),
                DEFEAT_MOD,
                defeatBonus,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        mob.setHealth(mob.getMaxHealth());
        if (family.brood()) {
            tuneBrood(mob);
        }
    }

    static void onAttacked(
            LivingEntity entity, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked) {
        if (!(entity instanceof Mob mob) || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (!WorldGate.isModuleActive(level, ID) || !isBroodBoss(mob)) {
            return;
        }
        if (!(source.getEntity() instanceof LivingEntity attacker) || !attacker.isAlive() || attacker == mob) {
            return;
        }
        if (attacker instanceof ServerPlayer player && (player.isCreative() || player.isSpectator())) {
            return;
        }
        mob.setTarget(attacker);
        tuneBrood(mob);
    }

    static void tickBroods(ServerLevel level) {
        BiomeBossData data = BiomeBossData.of(level);
        long now = level.getGameTime();
        for (BossFamily family : BossFamily.values()) {
            if (!family.brood()) {
                continue;
            }
            Optional<UUID> living = data.state(family).living();
            if (living.isEmpty()) {
                continue;
            }
            if (!(level.getEntity(living.get()) instanceof Mob mob) || !mob.isAlive() || !isBroodBoss(mob)) {
                continue;
            }
            tuneBrood(mob);
            trySpit(level, mob, now);
        }
    }

    static void tuneBrood(Mob mob) {
        if (Boolean.TRUE.equals(mob.getAttachedOrElse(EhmAttachments.EHM_BROOD_TUNED, Boolean.FALSE))) {
            return;
        }
        AttributeInstance follow = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (follow != null) {
            follow.setBaseValue(BiomeBossesRules.BROOD_FOLLOW_RANGE);
        }
        AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && !speed.hasModifier(BROOD_SPEED_MOD)) {
            speed.addOrReplacePermanentModifier(new AttributeModifier(
                    BROOD_SPEED_MOD,
                    BiomeBossesRules.BROOD_SPEED_BONUS,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        if (mob.level() instanceof ServerLevel server && mob.getAttached(EhmAttachments.EHM_BROOD_SPIT_TICK) == null) {
            mob.setAttached(EhmAttachments.EHM_BROOD_SPIT_TICK, server.getGameTime());
        }
        mob.setAttached(EhmAttachments.EHM_BROOD_TUNED, Boolean.TRUE);
    }

    static void trySpit(ServerLevel level, Mob mob, long now) {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive() || target == mob) {
            return;
        }
        if (target instanceof ServerPlayer player && (player.isCreative() || player.isSpectator())) {
            return;
        }
        double range = BiomeBossesRules.BROOD_SPIT_RANGE;
        if (mob.distanceToSqr(target) > range * range || !mob.hasLineOfSight(target)) {
            return;
        }
        long last = mob.getAttachedOrElse(EhmAttachments.EHM_BROOD_SPIT_TICK, 0L);
        if (!BiomeBossesRules.spitReady(now, last, BiomeBossesRules.BROOD_SPIT_INTERVAL_TICKS)) {
            if (last <= 0L) {
                mob.setAttached(EhmAttachments.EHM_BROOD_SPIT_TICK, now);
            }
            return;
        }
        mob.setAttached(EhmAttachments.EHM_BROOD_SPIT_TICK, now);
        launchSpit(level, mob, target);
    }

    static void launchSpit(ServerLevel level, Mob mob, LivingEntity target) {
        LlamaSpit spit = new LlamaSpit(EntityTypes.LLAMA_SPIT, level);
        spit.setOwner(mob);
        float yawRad = mob.yBodyRot * ((float) Math.PI / 180.0F);
        double forward = (mob.getBbWidth() + 1.0F) * 0.5D;
        spit.setPos(
                mob.getX() - forward * Mth.sin(yawRad),
                mob.getEyeY() - 0.1D,
                mob.getZ() + forward * Mth.cos(yawRad));
        double dx = target.getX() - mob.getX();
        double dy = target.getY(0.3333333333333333D) - spit.getY();
        double dz = target.getZ() - mob.getZ();
        double lift = Math.sqrt(dx * dx + dz * dz) * 0.2D;
        Projectile.spawnProjectileUsingShoot(
                spit,
                level,
                ItemStack.EMPTY,
                dx,
                dy + lift,
                dz,
                BiomeBossesRules.BROOD_SPIT_VELOCITY,
                BiomeBossesRules.BROOD_SPIT_INACCURACY);
        level.playSound(
                null,
                mob.getX(),
                mob.getY(),
                mob.getZ(),
                SoundEvents.LLAMA_SPIT,
                mob.getSoundSource(),
                1.0F,
                0.6F + level.getRandom().nextFloat() * 0.2F);
    }

    public static void onBroodSpitHit(ServerLevel level, LlamaSpit spit, Entity target) {
        Entity owner = spit.getOwner();
        if (!(target instanceof LivingEntity living) || !living.isAlive() || target == owner) {
            return;
        }
        if (living instanceof ServerPlayer player && (player.isCreative() || player.isSpectator())) {
            return;
        }
        if (owner instanceof LivingEntity attacker) {
            float damage = BiomeBossesRules.spitDamage(attacker.getAttributeValue(Attributes.ATTACK_DAMAGE));
            if (damage > 0.0F) {
                DamageSource source = level.damageSources().spit(spit, attacker);
                if (living.hurtServer(level, source, damage)) {
                    EnchantmentHelper.doPostAttackEffects(level, living, source);
                }
            }
        }
        living.addEffect(
                new MobEffectInstance(MobEffects.BLINDNESS, BiomeBossesRules.BROOD_SPIT_BLIND_TICKS, 0), owner);
    }

    static void equipRandomArmor(Mob mob, int tier) {
        int index = Math.floorMod(tier, ARMOR_TIERS.length);
        Item[] pieces = ARMOR_TIERS[index];
        for (int slot = 0; slot < ARMOR_SLOTS.length; slot++) {
            mob.setItemSlot(ARMOR_SLOTS[slot], new ItemStack(pieces[slot]));
            mob.setDropChance(ARMOR_SLOTS[slot], 0.0F);
        }
    }

    private static void applyModifier(
            AttributeInstance instance, Identifier id, double amount, AttributeModifier.Operation operation) {
        if (instance == null || amount == 0.0) {
            return;
        }
        instance.addPermanentModifier(new AttributeModifier(id, amount, operation));
    }

    static void announce(ServerLevel level, Mob mob, BossFamily family) {
        Component message = Component.translatableWithFallback(
                "tougher.chat.biome_boss",
                "A %s prowls nearby!",
                Component.translatableWithFallback(family.translationKey(), family.fallbackName()));
        for (ServerPlayer player : level.players()) {
            if (BiomeBossesRules.withinBarRange(player.distanceToSqr(mob))) {
                player.sendSystemMessage(message);
            }
        }
    }

    /**
     * Shows one purple bar per loaded boss to players within {@link BiomeBossesRules#BAR_RANGE_BLOCKS}.
     * Runs even when the module is off so a bar does not stay up after the feature is disabled.
     */
    static void tickBossBars(ServerLevel level) {
        Identifier dimension = level.dimension().identifier();
        if (!WorldGate.isModuleActive(level, ID)) {
            dismissDimension(dimension);
            RESCUE.entrySet().removeIf(entry -> dimension.equals(entry.getValue().dimension()));
            return;
        }
        Set<UUID> present = new HashSet<>();
        for (BiomeBossData.FamilyState state : BiomeBossData.of(level).families().values()) {
            if (state.living().isEmpty()) {
                continue;
            }
            UUID id = state.living().get();
            Entity entity = level.getEntity(id);
            if (!(entity instanceof LivingEntity boss) || !boss.isAlive() || !isBoss(boss)) {
                continue;
            }
            present.add(id);
            rescueIfStuck(level, boss);
            syncBar(level, dimension, boss);
        }
        List<UUID> stale = new ArrayList<>();
        for (Map.Entry<UUID, TrackedBar> entry : BARS.entrySet()) {
            if (dimension.equals(entry.getValue().dimension()) && !present.contains(entry.getKey())) {
                stale.add(entry.getKey());
            }
        }
        for (UUID id : stale) {
            dismissBar(id);
        }
        RESCUE.entrySet().removeIf(
                entry -> dimension.equals(entry.getValue().dimension()) && !present.contains(entry.getKey()));
    }

    private static void syncBar(ServerLevel level, Identifier dimension, LivingEntity boss) {
        UUID id = boss.getUUID();
        TrackedBar tracked = BARS.get(id);
        if (tracked == null || !dimension.equals(tracked.dimension())) {
            if (tracked != null) {
                tracked.event().removeAllPlayers();
            }
            ServerBossEvent event = new ServerBossEvent(
                    id, barName(boss), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
            event.setDarkenScreen(false);
            event.setPlayBossMusic(false);
            event.setCreateWorldFog(false);
            tracked = new TrackedBar(dimension, event);
            BARS.put(id, tracked);
        }
        ServerBossEvent event = tracked.event();
        event.setName(barName(boss));
        event.setProgress(BiomeBossesRules.barProgress(boss.getHealth(), boss.getMaxHealth()));
        Set<ServerPlayer> near = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            if (BiomeBossesRules.withinBarRange(player.distanceToSqr(boss))) {
                near.add(player);
                event.addPlayer(player);
            }
        }
        for (ServerPlayer player : List.copyOf(event.getPlayers())) {
            if (!near.contains(player)) {
                event.removePlayer(player);
            }
        }
    }

    private static Component barName(LivingEntity boss) {
        Component custom = boss.getCustomName();
        if (custom != null) {
            return custom;
        }
        BossFamily family = familyOf(boss);
        if (family != null) {
            return Component.translatableWithFallback(family.translationKey(), family.fallbackName());
        }
        return boss.getName();
    }

    private static void dismissBar(UUID id) {
        TrackedBar tracked = BARS.remove(id);
        if (tracked != null) {
            tracked.event().removeAllPlayers();
        }
    }

    private static void dismissDimension(Identifier dimension) {
        List<UUID> ids = new ArrayList<>();
        for (Map.Entry<UUID, TrackedBar> entry : BARS.entrySet()) {
            if (dimension.equals(entry.getValue().dimension())) {
                ids.add(entry.getKey());
            }
        }
        for (UUID id : ids) {
            dismissBar(id);
        }
    }

    private static void dismissAllBars() {
        for (TrackedBar tracked : BARS.values()) {
            tracked.event().removeAllPlayers();
        }
        BARS.clear();
    }

    private record TrackedBar(Identifier dimension, ServerBossEvent event) {}

    /** {@code retryAt} is {@link Long#MIN_VALUE} when another search may run immediately. */
    private record RescueState(Identifier dimension, Vec3 lastFree, long retryAt) {}

    static void thunderAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            playTo(player, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.0F, 0.8F);
        }
    }

    static double treasureMultiplier(LivingEntity entity, WorldConfig config) {
        Double stored = entity.getAttached(EhmAttachments.EHM_BOSS_DIFFICULTY_PERCENT);
        double difficultyPercent = stored == null ? 0.0 : stored;
        Integer defeats = entity.getAttachedOrElse(EhmAttachments.EHM_BOSS_DEFEAT_COUNT, 0);
        int defeated = defeats == null ? 0 : Math.max(0, defeats);
        return BiomeBossesRules.treasureWithDefeats(
                difficultyPercent, config.bossTreasurePercentScale(), defeated);
    }

    static void dropSpecialLoot(ServerLevel level, LivingEntity entity, WorldConfig config, double treasure) {
        RandomSource random = level.getRandom();
        drop(level, entity, Items.DIAMOND, BiomeBossesRules.scaleCount(1 + random.nextInt(3), treasure));
        drop(level, entity, Items.GOLD_INGOT, BiomeBossesRules.scaleCount(2 + random.nextInt(5), treasure));
        drop(level, entity, Items.IRON_INGOT, BiomeBossesRules.scaleCount(4 + random.nextInt(9), treasure));
        int appleCount = BiomeBossesRules.scaleCount(1, treasure);
        if (BiomeBossesRules.spawnRoll(
                random.nextInt(100), BiomeBossesRules.scalePercent(25, treasure))) {
            drop(level, entity, Items.ENCHANTED_GOLDEN_APPLE, appleCount);
        } else {
            drop(level, entity, Items.GOLDEN_APPLE, appleCount);
        }
        if (BiomeBossesRules.spawnRoll(
                random.nextInt(100), BiomeBossesRules.scalePercent(50, treasure))) {
            int potions = BiomeBossesRules.scaleCount(1, treasure);
            for (int i = 0; i < potions; i++) {
                entity.spawnAtLocation(
                        level, PotionContents.createItemStack(Items.SPLASH_POTION, Potions.STRONG_STRENGTH));
            }
        }
        int swords = BiomeBossesRules.scaleCount(1, treasure);
        for (int i = 0; i < swords; i++) {
            ItemStack gear = new ItemStack(random.nextBoolean() ? Items.DIAMOND_SWORD : Items.IRON_SWORD);
            entity.spawnAtLocation(
                    level, EnchantmentHelper.enchantItem(random, gear, 20, level.registryAccess(), Optional.empty()));
        }
        if (BiomeBossesRules.spawnRoll(
                random.nextInt(100),
                BiomeBossesRules.scalePercent(config.bossHeavyArmorDropPercent(), treasure))) {
            int pieces = BiomeBossesRules.scaleCount(1, treasure);
            for (int i = 0; i < pieces; i++) {
                entity.spawnAtLocation(level, new ItemStack(EhmItems.randomPiece(random)));
            }
        }
        dropFamilyExtras(level, entity, familyOf(entity), random, treasure);
    }

    static void dropFamilyExtras(
            ServerLevel level, LivingEntity entity, BossFamily family, RandomSource random, double treasure) {
        if (family == null) {
            return;
        }
        switch (family) {
            case FOREST, LUSH_CAVES, JUNGLE -> {
                drop(level, entity, Items.SPIDER_EYE, BiomeBossesRules.scaleCount(2 + random.nextInt(3), treasure));
                drop(level, entity, Items.STRING, BiomeBossesRules.scaleCount(4 + random.nextInt(5), treasure));
            }
            case DESERT -> drop(
                    level, entity, Items.GOLD_INGOT, BiomeBossesRules.scaleCount(2 + random.nextInt(4), treasure));
            case SWAMP -> drop(
                    level, entity, Items.GLOWSTONE_DUST, BiomeBossesRules.scaleCount(4 + random.nextInt(5), treasure));
            case OCEAN, DRIPSTONE, DROWNED -> {
                drop(
                        level,
                        entity,
                        Items.PRISMARINE_SHARD,
                        BiomeBossesRules.scaleCount(3 + random.nextInt(5), treasure));
                drop(level, entity, Items.NAUTILUS_SHELL, BiomeBossesRules.scaleCount(1, treasure));
            }
            case COLD -> drop(level, entity, Items.BONE, BiomeBossesRules.scaleCount(4 + random.nextInt(5), treasure));
            case PLAINS, SAVANNA -> drop(
                    level, entity, Items.EMERALD, BiomeBossesRules.scaleCount(2 + random.nextInt(4), treasure));
            case NETHER_WASTES -> drop(
                    level, entity, Items.BLAZE_ROD, BiomeBossesRules.scaleCount(2 + random.nextInt(4), treasure));
            case CRIMSON -> drop(
                    level, entity, Items.COOKED_PORKCHOP, BiomeBossesRules.scaleCount(3 + random.nextInt(4), treasure));
            case SOUL_SAND -> drop(
                    level, entity, Items.COAL, BiomeBossesRules.scaleCount(4 + random.nextInt(5), treasure));
            case SULFUR_CAVES -> drop(
                    level, entity, Items.MAGMA_CREAM, BiomeBossesRules.scaleCount(2 + random.nextInt(4), treasure));
            case DARK_FOREST, MUSHROOM -> drop(
                    level, entity, Items.IRON_INGOT, BiomeBossesRules.scaleCount(3 + random.nextInt(5), treasure));
        }
    }

    private static void drop(ServerLevel level, LivingEntity entity, Item item, int count) {
        if (count > 0) {
            entity.spawnAtLocation(level, new ItemStack(item, count));
        }
    }

    static void onBossDefeated(ServerLevel level, LivingEntity entity, int defeated) {
        Component name = entity.hasCustomName()
                ? entity.getCustomName()
                : Component.translatableWithFallback("tougher.boss.unknown", "biome boss");
        Component count = Component.literal(Integer.toString(defeated));
        Component defeat = Component.translatableWithFallback(
                "tougher.chat.biome_boss_defeat",
                "%s has been defeated! Bosses vanquished: %s",
                name,
                count);
        level.getServer().getPlayerList().broadcastSystemMessage(defeat, false);
        localSpectacle(level, entity.getX(), entity.getY(), entity.getZ());
        if (BiomeBossesRules.isThirdMilestone(defeated)) {
            Component third = Component.translatableWithFallback(
                    "tougher.chat.third_boss",
                    "The third boss is vanquished!  Your fame is growing!");
            level.getServer().getPlayerList().broadcastSystemMessage(third, false);
            worldSpectacle(
                    level.getServer(),
                    Component.translatableWithFallback(
                            "tougher.title.third_boss", "The third boss is vanquished!"),
                    Component.translatableWithFallback(
                            "tougher.subtitle.third_boss", "Your fame is growing!"),
                    BiomeBossesRules.TITLE_STAY_TICKS,
                    false);
        }
        if (BiomeBossesRules.isCreditsMilestone(defeated)) {
            Component victory = Component.translatableWithFallback(
                    "tougher.chat.ehm_defeated", "Tougher Defeated!");
            level.getServer().getPlayerList().broadcastSystemMessage(victory, false);
            worldSpectacle(
                    level.getServer(),
                    Component.translatableWithFallback(
                            "tougher.title.ehm_defeated", "Tougher Defeated!"),
                    Component.translatableWithFallback(
                            "tougher.subtitle.ehm_defeated", "You may continue if you like."),
                    BiomeBossesRules.SEVENTH_TITLE_STAY_TICKS,
                    true);
            creditsAtTick = level.getServer().overworld().getGameTime() + BiomeBossesRules.CREDITS_DELAY_TICKS;
        }
    }

    static void tickCredits(ServerLevel level) {
        if (creditsAtTick < 0L || level.getServer().overworld() != level) {
            return;
        }
        if (level.getGameTime() < creditsAtTick) {
            return;
        }
        creditsAtTick = -1L;
        showCredits(level.getServer());
    }

    static void showCredits(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.WIN_GAME, 0.0F));
        }
    }

    static void localSpectacle(ServerLevel level, double x, double y, double z) {
        for (int i = 0; i < 3; i++) {
            strikeLightning(level, x, y, z);
        }
        spawnFinale(level, x, y, z);
        spawnFireworks(level, x, y + 1.0, z, 8);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y + 1.0, z, 2, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y + 1.0, z, 80, 1.5, 2.0, 1.5, 0.35);
        level.sendParticles(ParticleTypes.END_ROD, x, y + 1.5, z, 40, 1.0, 1.5, 1.0, 0.08);
        level.playSound(null, x, y, z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 10000.0F, 0.8F);
        level.playSound(null, x, y, z, SoundEvents.ENDER_DRAGON_DEATH, SoundSource.HOSTILE, 4.0F, 1.0F);
        level.playSound(null, x, y, z, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.UI, 1.0F, 1.0F);
    }

    static void worldSpectacle(
            MinecraftServer server, Component title, Component subtitle, int stayTicks, boolean seventh) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!(player.level() instanceof ServerLevel playerLevel)) {
                continue;
            }
            strikeLightning(playerLevel, player.getX(), player.getY(), player.getZ());
            spawnFireworks(playerLevel, player.getX(), player.getY() + 1.0, player.getZ(), seventh ? 5 : 3);
            playerLevel.sendParticles(
                    ParticleTypes.TOTEM_OF_UNDYING,
                    player.getX(),
                    player.getY() + 1.0,
                    player.getZ(),
                    seventh ? 60 : 30,
                    1.0,
                    1.5,
                    1.0,
                    0.25);
            showTitle(player, title, subtitle, stayTicks);
            playTo(player, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.UI, 1.0F, 1.0F);
            playTo(
                    player,
                    seventh ? SoundEvents.ENDER_DRAGON_DEATH : SoundEvents.WITHER_DEATH,
                    SoundSource.HOSTILE,
                    1.0F,
                    1.0F);
            playTo(player, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.0F, 0.8F);
        }
    }

    static void showTitle(ServerPlayer player, Component title, Component subtitle, int stayTicks) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(
                BiomeBossesRules.TITLE_FADE_IN_TICKS, stayTicks, BiomeBossesRules.TITLE_FADE_OUT_TICKS));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
    }

    static void playTo(ServerPlayer player, SoundEvent sound, SoundSource source, float volume, float pitch) {
        player.connection.send(new ClientboundSoundPacket(
                Holder.direct(sound),
                source,
                player.getX(),
                player.getEyeY(),
                player.getZ(),
                volume,
                pitch,
                player.getRandom().nextLong()));
    }

    /**
     * Visual bolt only. Marked processed before it loads so {@link PigMen} does not also summon a piglin wave.
     * The death roster is {@link #spawnFinale}.
     */
    static void strikeLightning(ServerLevel level, double x, double y, double z) {
        LightningBolt bolt = new LightningBolt(EntityTypes.LIGHTNING_BOLT, level);
        bolt.setPos(x, y, z);
        bolt.setVisualOnly(true);
        bolt.setAttached(EhmAttachments.EHM_SPAWN_PROCESSED, Boolean.TRUE);
        level.addFreshEntity(bolt);
    }

    /**
     * Overworld death summons. Same gates as lightning pigmen: monsters allowed, and not a no-replacement
     * biome or structure. Nether and End bosses keep the lightning show only.
     */
    static void spawnFinale(ServerLevel level, double x, double y, double z) {
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }
        if (!level.getGameRules().get(GameRules.SPAWN_MONSTERS)) {
            return;
        }
        BlockPos at = BlockPos.containing(x, y, z);
        if (level.getBiome(at).is(SpawnReplaceService.NO_SPAWN_REPLACEMENTS)) {
            return;
        }
        if (level.structureManager()
                .getStructureWithPieceAt(at, SpawnReplaceService.NO_SPAWN_REPLACEMENT_STRUCTURES)
                .isValid()) {
            return;
        }
        List<BiomeBossesRules.FinaleKind> roster = BiomeBossesRules.finaleRoster();
        ServerPlayer target = nearestFighter(level, x, y, z);
        for (int i = 0; i < roster.size(); i++) {
            BiomeBossesRules.FinaleKind kind = roster.get(i);
            switch (kind) {
                case PIGLIN -> placeFinale(level, EntityTypes.ZOMBIFIED_PIGLIN, kind, x, y, z, i, roster.size(), target);
                case VEX -> placeFinale(level, EntityTypes.VEX, kind, x, y, z, i, roster.size(), target);
                case WITCH -> placeFinale(level, EntityTypes.WITCH, kind, x, y, z, i, roster.size(), target);
                case SKELETON -> placeFinale(level, EntityTypes.SKELETON, kind, x, y, z, i, roster.size(), target);
                case CREEPER -> placeFinale(level, EntityTypes.CREEPER, kind, x, y, z, i, roster.size(), target);
                case ZOMBIE -> placeFinale(level, EntityTypes.ZOMBIE, kind, x, y, z, i, roster.size(), target);
                case PHANTOM -> placeFinale(level, EntityTypes.PHANTOM, kind, x, y, z, i, roster.size(), target);
            }
        }
    }

    private static ServerPlayer nearestFighter(ServerLevel level, double x, double y, double z) {
        double best = BiomeBossesRules.FINALE_AGGRO_BLOCKS * BiomeBossesRules.FINALE_AGGRO_BLOCKS;
        ServerPlayer nearest = null;
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
                continue;
            }
            double distance = player.distanceToSqr(x, y, z);
            if (distance <= best) {
                best = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    private static <T extends Mob> void placeFinale(
            ServerLevel level,
            EntityType<T> type,
            BiomeBossesRules.FinaleKind kind,
            double x,
            double y,
            double z,
            int index,
            int count,
            ServerPlayer target) {
        T mob = type.create(level, EntitySpawnReason.EVENT);
        if (mob == null) {
            return;
        }
        BiomeBossesRules.PlaneOffset ring = BiomeBossesRules.finaleRingOffset(index, count);
        Vec3 preferred = new Vec3(x + ring.x(), y, z + ring.z());
        mob.snapTo(preferred.x, preferred.y, preferred.z);
        boolean wantsFloor = BiomeBossesRules.finaleWantsFloor(kind);
        Vec3 spot = nearestClear(level, mob, preferred, BiomeBossesRules.FINALE_SEARCH_RADIUS, wantsFloor);
        if (spot == null && wantsFloor) {
            spot = nearestClear(level, mob, preferred, BiomeBossesRules.FINALE_SEARCH_RADIUS, false);
        }
        if (spot == null) {
            return;
        }
        mob.snapTo(spot.x, spot.y, spot.z);
        mob.finalizeSpawn(
                level, level.getCurrentDifficultyAt(BlockPos.containing(spot)), EntitySpawnReason.EVENT, null);
        mob.setAttached(EhmAttachments.EHM_SPAWN_PROCESSED, Boolean.TRUE);
        if (kind == BiomeBossesRules.FinaleKind.PHANTOM) {
            mob.setAttached(EhmAttachments.EHM_NO_SUN_BURN, Boolean.TRUE);
        }
        if (mob instanceof ZombifiedPiglin piglin) {
            PigMen.anger(piglin);
            if (target != null) {
                piglin.setPersistentAngerTarget(EntityReference.of(target));
            }
        }
        if (target != null) {
            mob.setTarget(target);
        }
        level.addFreshEntity(mob);
    }

    /** A suffocating boss moves to the nearest open spot and the fight continues. */
    private static void rescueIfStuck(ServerLevel level, LivingEntity boss) {
        Identifier dimension = level.dimension().identifier();
        if (!boss.isInWall()) {
            RESCUE.put(boss.getUUID(), new RescueState(dimension, boss.position(), Long.MIN_VALUE));
            return;
        }
        RescueState state = RESCUE.get(boss.getUUID());
        long now = level.getGameTime();
        if (state != null && dimension.equals(state.dimension()) && now < state.retryAt()) {
            return;
        }
        Vec3 dest = nearestEscape(level, boss, state, dimension);
        Vec3 remembered = state == null || !dimension.equals(state.dimension()) ? null : state.lastFree();
        if (dest == null) {
            RESCUE.put(
                    boss.getUUID(),
                    new RescueState(dimension, remembered, now + BiomeBossesRules.UNSTUCK_RETRY_TICKS));
            return;
        }
        float yaw = boss.getYRot();
        float pitch = boss.getXRot();
        double fromX = boss.getX();
        double fromY = boss.getY();
        double fromZ = boss.getZ();
        if (!boss.teleportTo(level, dest.x, dest.y, dest.z, Set.of(), yaw, pitch, true)) {
            RESCUE.put(
                    boss.getUUID(),
                    new RescueState(dimension, remembered, now + BiomeBossesRules.UNSTUCK_RETRY_TICKS));
            return;
        }
        boss.resetFallDistance();
        boss.setDeltaMovement(Vec3.ZERO);
        RESCUE.put(boss.getUUID(), new RescueState(dimension, dest, Long.MIN_VALUE));
        level.playSound(null, fromX, fromY, fromZ, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.0F);
        level.playSound(null, dest.x, dest.y, dest.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.0F);
        level.sendParticles(
                ParticleTypes.PORTAL,
                dest.x,
                dest.y + boss.getBbHeight() * 0.5,
                dest.z,
                24,
                0.4,
                0.5,
                0.4,
                0.15);
    }

    private static Vec3 nearestEscape(ServerLevel level, LivingEntity boss, RescueState state, Identifier dimension) {
        if (state != null && dimension.equals(state.dimension()) && state.lastFree() != null) {
            Vec3 remembered = state.lastFree();
            if (nearEnough(boss, remembered) && clearOfWall(level, boss, remembered, false)) {
                return remembered;
            }
        }
        Vec3 origin = boss.position();
        Vec3 floored = searchShells(level, boss, origin, BiomeBossesRules.UNSTUCK_RADIUS, true);
        if (floored != null) {
            return floored;
        }
        return searchShells(level, boss, origin, BiomeBossesRules.UNSTUCK_RADIUS, false);
    }

    private static Vec3 nearestClear(ServerLevel level, Entity entity, Vec3 origin, int radius, boolean needFloor) {
        if (clearOfWall(level, entity, origin, needFloor)) {
            return origin;
        }
        return searchShells(level, entity, origin, radius, needFloor);
    }

    private static Vec3 searchShells(ServerLevel level, Entity entity, Vec3 origin, int radius, boolean needFloor) {
        for (int ring = 1; ring <= radius; ring++) {
            for (int dy = -ring; dy <= ring; dy++) {
                for (int dx = -ring; dx <= ring; dx++) {
                    for (int dz = -ring; dz <= ring; dz++) {
                        if (BiomeBossesRules.chebyshev(dx, dy, dz) != ring) {
                            continue;
                        }
                        Vec3 candidate = origin.add(dx, dy, dz);
                        if (clearOfWall(level, entity, candidate, needFloor)) {
                            return candidate;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean nearEnough(Entity entity, Vec3 feet) {
        BlockPos from = entity.blockPosition();
        BlockPos to = BlockPos.containing(feet);
        return BiomeBossesRules.withinUnstuck(to.getX() - from.getX(), to.getY() - from.getY(), to.getZ() - from.getZ());
    }

    /**
     * A spot the body can occupy without suffocating. The eye box matches {@link Entity#isInWall()}.
     * A sturdy floor is preferred for the boss and for ground summons. Flyers and a buried boss with no
     * floor nearby accept a clear spot without one.
     */
    private static boolean clearOfWall(ServerLevel level, Entity entity, Vec3 feet, boolean needFloor) {
        if (feet == null || !Double.isFinite(feet.x) || !Double.isFinite(feet.y) || !Double.isFinite(feet.z)) {
            return false;
        }
        double height = entity.getBbHeight();
        if (feet.y < level.getMinY() || feet.y + height >= level.getMaxY()) {
            return false;
        }
        BlockPos feetPos = BlockPos.containing(feet);
        BlockPos headPos = BlockPos.containing(feet.x, feet.y + Math.max(0.0, height - 0.01), feet.z);
        if (!level.isLoaded(feetPos) || !level.isLoaded(headPos)) {
            return false;
        }
        if (level.getFluidState(feetPos).is(FluidTags.LAVA)) {
            return false;
        }
        if (needFloor) {
            BlockPos below = feetPos.below();
            if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                return false;
            }
        }
        double dx = feet.x - entity.getX();
        double dy = feet.y - entity.getY();
        double dz = feet.z - entity.getZ();
        AABB body = entity.getBoundingBox().move(dx, dy, dz);
        if (!level.noBlockCollision(entity, body) || !level.noBorderCollision(entity, body)) {
            return false;
        }
        return !eyeWouldSuffocate(level, entity, dx, dy, dz);
    }

    /** Same thin eye box and suffocating-shape test as {@link Entity#isInWall()}. */
    private static boolean eyeWouldSuffocate(ServerLevel level, Entity entity, double dx, double dy, double dz) {
        float width = entity.getDimensions(entity.getPose()).width() * 0.8F;
        Vec3 eye = entity.getEyePosition().add(dx, dy, dz);
        AABB box = AABB.ofSize(eye, width, 1.0E-6D, width);
        return BlockPos.betweenClosedStream(box).anyMatch(pos -> {
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || !state.isSuffocating(level, pos)) {
                return false;
            }
            VoxelShape shape = state.getCollisionShape(level, pos).move(pos);
            return Shapes.joinIsNotEmpty(shape, Shapes.create(box), BooleanOp.AND);
        });
    }

    static void spawnFireworks(ServerLevel level, double x, double y, double z, int count) {
        RandomSource random = level.getRandom();
        DyeColor[] colors = DyeColor.values();
        for (int i = 0; i < count; i++) {
            DyeColor color = colors[random.nextInt(colors.length)];
            IntArrayList fireworkColors = new IntArrayList();
            fireworkColors.add(color.getFireworkColor());
            FireworkExplosion burst = new FireworkExplosion(
                    i % 2 == 0 ? FireworkExplosion.Shape.LARGE_BALL : FireworkExplosion.Shape.STAR,
                    fireworkColors,
                    new IntArrayList(),
                    true,
                    true);
            ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
            rocket.set(DataComponents.FIREWORKS, new Fireworks(1, List.of(burst)));
            double ox = (random.nextDouble() - 0.5) * 3.0;
            double oz = (random.nextDouble() - 0.5) * 3.0;
            level.addFreshEntity(new FireworkRocketEntity(level, x + ox, y, z + oz, rocket));
        }
    }
}

package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.network.ClientboundWiseLessonPayload;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.tag.EhmTags;
import dev.extrahardmode.world.InhabitantData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Hunts offered by wise teachers. Each player keeps his own list, one lesson per teacher.
 * An unfinished hunt ends after {@link WiseLessonRules#MISSION_DAYS} days.
 */
public final class WiseLessons {
    private static final Map<UUID, String> LAST_HUD = new HashMap<>();
    private static final Identifier MARK_HEALTH = ExtraHardModeMod.id("wise_lesson_mark_health");
    private static final Identifier LEAD_HEALTH = ExtraHardModeMod.id("wise_lesson_lead_health");
    private static final Identifier LEAD_SCALE = ExtraHardModeMod.id("wise_lesson_lead_scale");
    private static final Identifier LEAD_KNOCKBACK = ExtraHardModeMod.id("wise_lesson_lead_knockback");
    private static final Identifier FINAL_HEALTH = ExtraHardModeMod.id("wise_lesson_final_health");
    private static final Identifier FINAL_DAMAGE = ExtraHardModeMod.id("wise_lesson_final_damage");
    /** Long enough that the five-tick refresh does not let the outline blink off. */
    private static final int OUTLINE_TICKS = 40;
    private static final int EMERALD_STACK = 64;
    /** Soft red dust on the body. Scale is the puff size. */
    private static final DustParticleOptions MARK_MIST = new DustParticleOptions(0xC41A1A, 1.6F);
    /** Darker dust a little farther out, so the cloud reads as mist rather than sparks. */
    private static final DustParticleOptions MARK_HAZE = new DustParticleOptions(0x7A1010, 2.2F);
    private static final ColorParticleOption MARK_VEIL =
            ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.62F, 0.05F, 0.05F);

    private WiseLessons() {}

    public static boolean isLessonMob(LivingEntity entity) {
        return entity != null
                && Boolean.TRUE.equals(entity.getAttachedOrElse(EhmAttachments.EHM_WISE_LESSON_MOB, Boolean.FALSE));
    }

    /** The first creature of its hunt. */
    public static boolean isLead(LivingEntity entity) {
        return isLessonMob(entity)
                && Boolean.TRUE.equals(entity.getAttachedOrElse(EhmAttachments.EHM_WISE_LESSON_LEAD, Boolean.FALSE));
    }

    /** The last creature of its hunt. */
    public static boolean isFinal(LivingEntity entity) {
        return isLessonMob(entity)
                && Boolean.TRUE.equals(entity.getAttachedOrElse(EhmAttachments.EHM_WISE_LESSON_FINAL, Boolean.FALSE));
    }

    public static void onTalk(ServerPlayer player, Villager villager, ServerLevel level, InhabitantData.Home home) {
        if (player == null || villager == null || level == null || home == null) {
            return;
        }
        long today = AntiFarming.overworldDay(level);
        String teacherId = villager.getStringUUID();
        List<WiseLessonRules.Lesson> board = boardOf(player);
        WiseLessonRules.Lesson existing = find(board, teacherId);
        WiseLessonRules.Lesson ended = WiseLessonRules.endIfExpired(existing, today);
        if (ended != existing && !WiseLessonRules.offerDue(ended.assignedDay(), today)) {
            List<WiseLessonRules.Lesson> quiet = new ArrayList<>();
            for (WiseLessonRules.Lesson prior : board) {
                quiet.add(teacherId.equals(prior.teacherId()) ? ended : prior);
            }
            player.setAttached(EhmAttachments.EHM_WISE_LESSONS, quiet);
            publish(player, quiet);
            mentionBossBiomes(player, level, teacherId, ended.assignedDay());
            return;
        }
        if (existing != null && !WiseLessonRules.offerDue(existing.assignedDay(), today)) {
            if (existing.active()) {
                player.sendSystemMessage(Component.literal(lineFor(player, existing)));
            }
            mentionBossBiomes(player, level, teacherId, existing.assignedDay());
            return;
        }
        String ability = villager.getAttached(EhmAttachments.EHM_WISE_ABILITY);
        boolean taught = ManaAbilities.taughtByWise(player, ability);
        boolean mana = InhabitantRules.alreadyTookManaLesson(
                player.getAttachedOrElse(EhmAttachments.EHM_WISE_MANA, List.of()), teacherId);
        int given = Math.max(0, player.getAttachedOrElse(EhmAttachments.EHM_WISE_LESSON_COUNT, 0));
        RandomSource random = level.getRandom();
        WiseLessonRules.Mob mob = WiseLessonRules.pickMob(given, random.nextInt());
        double stock = Inhabitants.shopScore(villager, home.score());
        int size = WiseLessonRules.packSize(stock, WiseLessonRules.serviceUses(taught, mana));
        WiseLessonRules.Lesson lesson = new WiseLessonRules.Lesson(
                teacherId,
                InhabitantRules.nameplate(home.name(), home.specialty()),
                level.dimension().identifier().toString(),
                villager.getX(),
                villager.getZ(),
                mob.id(),
                mob.plural(),
                size,
                WiseLessonRules.distance(random.nextInt()),
                WiseLessonRules.direction(random.nextInt()),
                today,
                0,
                0,
                -1,
                true,
                List.of());
        List<WiseLessonRules.Lesson> next = new ArrayList<>();
        for (WiseLessonRules.Lesson prior : board) {
            if (!teacherId.equals(prior.teacherId())) {
                next.add(prior);
            }
        }
        next.add(lesson);
        player.setAttached(EhmAttachments.EHM_WISE_LESSON_COUNT, given + 1);
        player.setAttached(EhmAttachments.EHM_WISE_LESSONS, next);
        player.sendSystemMessage(Component.literal(lineFor(player, lesson)));
        level.playSound(null, villager.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);
        publish(player, next);
    }

    public static void tick(ServerPlayer player, ServerLevel level, long today) {
        if (player == null || level == null) {
            return;
        }
        List<WiseLessonRules.Lesson> board = boardOf(player);
        if (board.isEmpty()) {
            publish(player, board);
            return;
        }
        boolean present = player.isAlive() && !player.isSpectator();
        boolean gate = present && WiseLessonRules.spawnGate(onLand(player), night(level), belowGround(level, player));
        String dimension = level.dimension().identifier().toString();
        RandomSource random = level.getRandom();
        List<WiseLessonRules.Lesson> next = new ArrayList<>();
        boolean changed = false;
        for (WiseLessonRules.Lesson lesson : board) {
            WiseLessonRules.Lesson updated = WiseLessonRules.endIfExpired(
                    refresh(lesson, level.getServer(), today), today);
            if (updated.active() && present) {
                boolean inRange = WiseLessonRules.atRange(
                        updated.dimension(),
                        dimension,
                        updated.originX(),
                        updated.originZ(),
                        player.getX(),
                        player.getZ(),
                        updated.distance());
                WiseLessonRules.Second step = WiseLessonRules.afterSecond(updated, inRange, gate, random.nextInt());
                updated = step.lesson();
                if (step.attemptSpawn()) {
                    int batch = WiseLessonRules.spawnsThisSecond(updated.spawned(), updated.size());
                    for (int n = 0; n < batch; n++) {
                        boolean lead = WiseLessonRules.leadSpawn(updated.spawned());
                        boolean last = WiseLessonRules.finalSpawn(updated.spawned(), updated.size());
                        String mark = trySpawn(player, level, updated, today, lead, last);
                        updated = WiseLessonRules.afterAttempt(updated, mark != null, mark, random.nextInt());
                        if (mark == null) {
                            break;
                        }
                    }
                }
            }
            if (!updated.equals(lesson)) {
                changed = true;
            }
            next.add(updated);
        }
        if (changed) {
            player.setAttached(EhmAttachments.EHM_WISE_LESSONS, next);
        }
        publish(player, next);
    }

    /** Red mist for every player in this level who can currently see his marks. */
    public static void mist(ServerLevel level) {
        if (level == null) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            refreshMist(player);
            outlineHostiles(player);
        }
    }

    /**
     * White outline on hostile mobs within 8 blocks times the cast's Sense Evil level.
     * The glowing effect is drawn through solid blocks. It is refreshed while the cast lasts.
     */
    public static void outlineHostiles(ServerPlayer player) {
        if (player == null || !player.isAlive() || !(player.level() instanceof ServerLevel level) || level.getServer() == null) {
            return;
        }
        long until = player.getAttachedOrElse(EhmAttachments.EHM_MARK_OUTLINE_UNTIL, -1L);
        if (!WiseLessonRules.senseOutlineActive(until, level.getServer().overworld().getGameTime())) {
            return;
        }
        double power = player.getAttachedOrElse(EhmAttachments.EHM_SENSE_OUTLINE_POWER, 0.0);
        double range = AbilityRules.senseHostileRange(power);
        if (range <= 0.0) {
            return;
        }
        AABB box = player.getBoundingBox().inflate(range);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, box, WiseLessons::senseHostile)) {
            if (AbilityRules.senseHostileInRange(player.distanceToSqr(living), power)) {
                living.addEffect(new MobEffectInstance(MobEffects.GLOWING, OUTLINE_TICKS, 0, false, false));
            }
        }
    }

    /** Hostile mobs, not players. */
    private static boolean senseHostile(LivingEntity living) {
        return living instanceof Enemy && !(living instanceof Player) && living.isAlive() && !living.isRemoved();
    }

    /** Puts the current mist on this player's loaded marks. A Sense Evil cast uses it at once. */
    public static void refreshMist(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level) || level.getServer() == null) {
            return;
        }
        for (WiseLessonRules.Lesson lesson : boardOf(player)) {
            if (lesson.mobs() == null) {
                continue;
            }
            for (String raw : lesson.mobs()) {
                WiseLessonRules.Mark mark = WiseLessonRules.parseMark(raw);
                if (mark == null) {
                    continue;
                }
                UUID id;
                try {
                    id = UUID.fromString(mark.uuid());
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                LivingEntity living = find(level.getServer(), id);
                if (living != null && living.isAlive() && !living.isRemoved()) {
                    showMist(living);
                }
            }
        }
    }

    /** Loaded lesson creatures past their third day are removed without drops. */
    public static void despawnExpired(ServerLevel level, long today) {
        if (level == null) {
            return;
        }
        List<LivingEntity> doomed = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof LivingEntity living) || !isLessonMob(living)) {
                continue;
            }
            long expire = living.getAttachedOrElse(EhmAttachments.EHM_WISE_EXPIRE_DAY, Long.MAX_VALUE);
            if (today >= expire) {
                doomed.add(living);
            }
        }
        for (LivingEntity living : doomed) {
            forget(living);
            living.discard();
        }
    }

    public static void onMobDeath(LivingEntity entity) {
        if (!isLessonMob(entity)) {
            return;
        }
        forget(entity);
    }

    public static void sendHud(ServerPlayer player) {
        if (player == null) {
            return;
        }
        LAST_HUD.remove(player.getUUID());
        publish(player, boardOf(player));
    }

    public static void clearHud(ServerPlayer player) {
        if (player != null) {
            LAST_HUD.remove(player.getUUID());
        }
    }

    private static void forget(LivingEntity entity) {
        if (entity == null || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        UUID owner = entity.getAttached(EhmAttachments.EHM_WISE_LESSON_OWNER);
        if (owner == null) {
            return;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
        if (player == null) {
            return;
        }
        removeMark(player, entity.getStringUUID());
    }

    private static void removeMark(ServerPlayer player, String uuid) {
        List<WiseLessonRules.Lesson> next = new ArrayList<>();
        boolean changed = false;
        for (WiseLessonRules.Lesson lesson : boardOf(player)) {
            WiseLessonRules.Lesson updated = lesson.withoutMob(uuid);
            if (updated != lesson) {
                changed = true;
            }
            next.add(updated);
        }
        if (!changed) {
            return;
        }
        player.setAttached(EhmAttachments.EHM_WISE_LESSONS, next);
        publish(player, next);
    }

    private static WiseLessonRules.Lesson refresh(WiseLessonRules.Lesson lesson, MinecraftServer server, long today) {
        if (lesson.mobs() == null || lesson.mobs().isEmpty()) {
            if (lesson.active() && lesson.spawned() >= lesson.size()) {
                return lesson.withActive(false);
            }
            return lesson;
        }
        List<String> kept = new ArrayList<>();
        boolean changed = false;
        for (String raw : lesson.mobs()) {
            WiseLessonRules.Mark mark = WiseLessonRules.parseMark(raw);
            if (mark == null) {
                changed = true;
                continue;
            }
            UUID id;
            try {
                id = UUID.fromString(mark.uuid());
            } catch (IllegalArgumentException ex) {
                changed = true;
                continue;
            }
            LivingEntity living = find(server, id);
            if (living == null) {
                if (today >= mark.expireDay()) {
                    changed = true;
                    continue;
                }
                kept.add(raw);
                continue;
            }
            if (!living.isAlive() || living.isRemoved()) {
                changed = true;
                continue;
            }
            long expire = living.getAttachedOrElse(EhmAttachments.EHM_WISE_EXPIRE_DAY, mark.expireDay());
            if (today >= expire) {
                living.discard();
                changed = true;
                continue;
            }
            kept.add(raw);
        }
        boolean active = lesson.active();
        if (active && lesson.spawned() >= lesson.size() && kept.isEmpty()) {
            active = false;
            changed = true;
        }
        if (!changed) {
            return lesson;
        }
        return lesson.withSpawn(lesson.spawned(), lesson.spawnWait(), kept, active);
    }

    /**
     * The first creature drops one diamond. The first and the last drop emeralds from 10% through
     * 20% of that creature's own health.
     */
    public static void dropBonusTreasure(LivingEntity mob, ServerLevel level) {
        if (mob == null || level == null) {
            return;
        }
        boolean lead = isLead(mob);
        boolean finale = isFinal(mob);
        if (WiseLessonRules.dropsDiamond(lead)) {
            mob.spawnAtLocation(level, new ItemStack(Items.DIAMOND));
        }
        if (!WiseLessonRules.dropsEmeralds(lead, finale)) {
            return;
        }
        int emeralds = WiseLessonRules.leadEmeralds(
                WiseLessonRules.wholeHealth(mob.getMaxHealth()), level.getRandom().nextInt());
        int left = emeralds;
        while (left > 0) {
            int pile = Math.min(EMERALD_STACK, left);
            mob.spawnAtLocation(level, new ItemStack(Items.EMERALD, pile));
            left -= pile;
        }
    }

    /** Ordinary marks gain 50% to 100% health and spawn full. */
    private static void bolster(Mob mob, int roll) {
        addBonus(mob.getAttribute(Attributes.MAX_HEALTH), MARK_HEALTH, WiseLessonRules.markHealthBonus(roll));
        mob.setHealth(mob.getMaxHealth());
    }

    /** The last creature has twice the first creature's health and hits 50% harder. */
    private static void promoteFinal(Mob mob) {
        mob.setAttached(EhmAttachments.EHM_WISE_LESSON_FINAL, Boolean.TRUE);
        addBonus(mob.getAttribute(Attributes.MAX_HEALTH), FINAL_HEALTH, WiseLessonRules.finalHealthBonus());
        addBonus(mob.getAttribute(Attributes.ATTACK_DAMAGE), FINAL_DAMAGE, WiseLessonRules.finalDamageBonus());
        mob.setHealth(mob.getMaxHealth());
    }

    private static void promoteLead(Mob mob) {
        mob.setAttached(EhmAttachments.EHM_WISE_LESSON_LEAD, Boolean.TRUE);
        addBonus(mob.getAttribute(Attributes.MAX_HEALTH), LEAD_HEALTH, WiseLessonRules.leadHealthBonus());
        addBonus(mob.getAttribute(Attributes.SCALE), LEAD_SCALE, WiseLessonRules.leadScaleBonus());
        addFlat(
                mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE),
                LEAD_KNOCKBACK,
                WiseLessonRules.leadKnockbackResistance());
        mob.setHealth(mob.getMaxHealth());
    }

    /**
     * Red mist on and around a hunt creature. Only the owner's line of sight shows it, unless a golden
     * carrot is still keeping them saturated or Sense Evil is revealing marks.
     */
    private static void showMist(LivingEntity living) {
        if (living == null || !isLessonMob(living)) {
            return;
        }
        if (!(living.level() instanceof ServerLevel level) || level.getServer() == null) {
            clearOutline(living);
            return;
        }
        UUID ownerId = living.getAttached(EhmAttachments.EHM_WISE_LESSON_OWNER);
        ServerPlayer owner = ownerId == null ? null : level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null || !owner.isAlive() || owner.level() != level) {
            clearOutline(living);
            return;
        }
        boolean carrot = LocatorBarRules.armed(
                Boolean.TRUE.equals(owner.getAttached(EhmAttachments.EHM_LOCATOR_CARROT)),
                false,
                owner.getFoodData().getSaturationLevel());
        long until = owner.getAttachedOrElse(EhmAttachments.EHM_MARK_OUTLINE_UNTIL, -1L);
        boolean sense = WiseLessonRules.senseOutlineActive(until, level.getServer().overworld().getGameTime());
        boolean sight = owner.hasLineOfSight(living);
        if (WiseLessonRules.showMist(sight, carrot, sense)) {
            spawnMist(level, owner, living);
        }
        if (WiseLessonRules.showOutline(carrot, sense)) {
            living.addEffect(new MobEffectInstance(MobEffects.GLOWING, OUTLINE_TICKS, 0, false, false));
        } else {
            clearOutline(living);
        }
    }

    /** The glowing effect is the white outline drawn through blocks. */
    private static void clearOutline(LivingEntity living) {
        if (living.hasEffect(MobEffects.GLOWING)) {
            living.removeEffect(MobEffects.GLOWING);
        }
    }

    /** Dust on the body, a wider darker cloud, and drifting red spores. Sent only to the owner. */
    private static void spawnMist(ServerLevel level, ServerPlayer owner, LivingEntity living) {
        double x = living.getX();
        double y = living.getY() + living.getBbHeight() * 0.55;
        double z = living.getZ();
        double body = Math.max(0.3, living.getBbWidth() * 0.45);
        double around = Math.max(0.55, living.getBbWidth() * 0.85);
        double lift = Math.max(0.4, living.getBbHeight() * 0.5);
        puff(level, owner, MARK_MIST, x, y, z, 8, body, lift * 0.7, 0.004);
        puff(level, owner, MARK_HAZE, x, y, z, 10, around, lift, 0.008);
        puff(level, owner, ParticleTypes.CRIMSON_SPORE, x, y, z, 5, around * 0.8, lift * 0.6, 0.01);
        puff(level, owner, MARK_VEIL, x, y, z, 3, body * 0.8, lift * 0.5, 0.0);
    }

    private static void puff(
            ServerLevel level,
            ServerPlayer owner,
            ParticleOptions particle,
            double x,
            double y,
            double z,
            int count,
            double spreadX,
            double spreadY,
            double speed) {
        level.sendParticles(owner, particle, true, false, x, y, z, count, spreadX, spreadY, spreadX, speed);
    }

    private static void addBonus(AttributeInstance attribute, Identifier id, double amount) {
        if (attribute == null || attribute.hasModifier(id) || amount == 0.0) {
            return;
        }
        attribute.addPermanentModifier(
                new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    private static void addFlat(AttributeInstance attribute, Identifier id, double amount) {
        if (attribute == null || attribute.hasModifier(id) || amount == 0.0) {
            return;
        }
        attribute.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
    }

    private static String trySpawn(
            ServerPlayer player,
            ServerLevel level,
            WiseLessonRules.Lesson lesson,
            long today,
            boolean lead,
            boolean last) {
        RandomSource random = level.getRandom();
        int radius = WiseLessonRules.spawnRadius(random.nextInt());
        double angle = random.nextFloat() * Math.PI * 2.0;
        double dx = Math.cos(angle) * radius;
        double dz = Math.sin(angle) * radius;
        if (dx == 0.0 && dz == 0.0) {
            dx = radius;
        }
        int x = player.getBlockX() + (int) Math.round(dx);
        int z = player.getBlockZ() + (int) Math.round(dz);
        BlockPos feet = findFeet(level, x, z, player.getBlockY());
        if (feet == null) {
            return null;
        }
        Identifier id = Identifier.tryParse(lesson.mobId());
        if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            return null;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
        Entity spawned = type.spawn(level, feet, EntitySpawnReason.COMMAND);
        if (!(spawned instanceof Mob mob)) {
            if (spawned != null) {
                spawned.discard();
            }
            return null;
        }
        mob.setPersistenceRequired();
        mob.setTarget(player);
        mob.setAttached(EhmAttachments.EHM_LOOTLESS, Boolean.FALSE);
        mob.setAttached(EhmAttachments.EHM_UNNATURAL_SPAWN, Boolean.FALSE);
        mob.setAttached(EhmAttachments.EHM_WISE_LESSON_MOB, Boolean.TRUE);
        mob.setAttached(EhmAttachments.EHM_WISE_LESSON_OWNER, player.getUUID());
        if (lead) {
            promoteLead(mob);
        } else if (last) {
            promoteFinal(mob);
        } else {
            bolster(mob, random.nextInt());
        }
        showMist(mob);
        long expire = WiseLessonRules.expireDay(today);
        mob.setAttached(EhmAttachments.EHM_WISE_EXPIRE_DAY, expire);
        return WiseLessonRules.mark(mob.getStringUUID(), expire);
    }

    private static BlockPos findFeet(ServerLevel level, int x, int z, int playerY) {
        for (int y = playerY + 4; y >= playerY - 12; y--) {
            BlockPos feet = new BlockPos(x, y, z);
            if (!level.isLoaded(feet)) {
                return null;
            }
            if (suitable(level, feet)) {
                return feet;
            }
        }
        return null;
    }

    private static boolean suitable(ServerLevel level, BlockPos feet) {
        BlockState below = level.getBlockState(feet.below());
        BlockState body = level.getBlockState(feet);
        BlockState head = level.getBlockState(feet.above());
        if (!below.isSolid() || !body.isAir() || !head.isAir()) {
            return false;
        }
        if (!body.getFluidState().isEmpty() || !head.getFluidState().isEmpty()) {
            return false;
        }
        return !below.getFluidState().is(FluidTags.WATER) && !below.getFluidState().is(FluidTags.LAVA);
    }

    private static boolean onLand(ServerPlayer player) {
        return !player.isInWater() && !player.isUnderWater() && !player.isEyeInFluid(FluidTags.WATER);
    }

    private static boolean night(ServerLevel level) {
        if (!level.dimensionType().hasSkyLight()) {
            return true;
        }
        return WiseLessonRules.nightTime(level.getOverworldClockTime());
    }

    private static boolean belowGround(ServerLevel level, ServerPlayer player) {
        return !level.canSeeSky(player.blockPosition());
    }

    private static LivingEntity find(MinecraftServer server, UUID id) {
        if (server == null || id == null) {
            return null;
        }
        for (ServerLevel world : server.getAllLevels()) {
            if (world.getEntity(id) instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    private static List<WiseLessonRules.Lesson> boardOf(ServerPlayer player) {
        List<WiseLessonRules.Lesson> stored = player.getAttached(EhmAttachments.EHM_WISE_LESSONS);
        if (stored == null || stored.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(stored);
    }

    private static void mentionBossBiomes(ServerPlayer player, ServerLevel level, String teacherId, long assignedDay) {
        List<String> eligible = eligibleBossBiomes(player, level);
        List<WiseLessonRules.BossHint> stored = hintsOf(player);
        WiseLessonRules.BossHint prior = findHint(stored, teacherId);
        int roll = 0;
        if (WiseLessonRules.choosesBossHint(prior, teacherId, assignedDay, eligible.size())) {
            roll = level.getRandom().nextInt();
        }
        WiseLessonRules.BossHint hint = WiseLessonRules.nextBossHint(prior, teacherId, assignedDay, eligible, roll);
        if (!hint.equals(prior)) {
            player.setAttached(EhmAttachments.EHM_WISE_BOSS_HINTS, replaceHint(stored, hint));
        }
        if (hint.biomes().isEmpty()) {
            return;
        }
        player.sendSystemMessage(bossBiomeLine(hint.biomes()));
    }

    /** Visited biomes whose boss family has not appeared yet, in the order they were explored. */
    private static List<String> eligibleBossBiomes(ServerPlayer player, ServerLevel level) {
        List<String> visited = player.getAttachedOrElse(EhmAttachments.EHM_VISITED_BIOMES, List.of());
        if (visited.isEmpty()) {
            return List.of();
        }
        Registry<Biome> registry = level.registryAccess().lookupOrThrow(Registries.BIOME);
        boolean[] spent = new boolean[BossFamily.values().length];
        boolean[] known = new boolean[BossFamily.values().length];
        List<String> eligible = new ArrayList<>();
        for (String raw : visited) {
            Identifier id = Identifier.tryParse(raw);
            if (id == null) {
                continue;
            }
            Optional<Holder.Reference<Biome>> found = registry.get(ResourceKey.create(Registries.BIOME, id));
            if (found.isEmpty()) {
                continue;
            }
            Holder<Biome> holder = found.get();
            if (holder.is(EhmTags.NO_BIOME_BOSSES)) {
                continue;
            }
            BossFamily family = null;
            for (BossFamily candidate : BossFamily.values()) {
                if (holder.is(candidate.biomes())) {
                    family = candidate;
                    break;
                }
            }
            if (family == null) {
                continue;
            }
            int index = family.ordinal();
            if (!known[index]) {
                spent[index] = BiomeBosses.familySpent(level, family);
                known[index] = true;
            }
            if (!spent[index]) {
                eligible.add(raw);
            }
        }
        return eligible;
    }

    private static Component bossBiomeLine(List<String> biomeIds) {
        List<Component> names = new ArrayList<>();
        for (String raw : biomeIds) {
            Identifier id = Identifier.tryParse(raw);
            if (id == null) {
                names.add(Component.literal(ExplorationRules.biomeFallbackName(raw)));
                continue;
            }
            names.add(Exploration.biomeName(ResourceKey.create(Registries.BIOME, id)));
        }
        return Component.translatableWithFallback(
                "tougher.wise.boss_biomes",
                "A great beast may still wait in %s.",
                joinBiomeNames(names));
    }

    /** Same conjunctions as {@link WiseLessonRules#joinNames}. */
    private static Component joinBiomeNames(List<Component> names) {
        if (names.size() == 1) {
            return names.get(0);
        }
        MutableComponent line = Component.empty();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                String separator = names.size() == 2 ? " and " : (i == names.size() - 1 ? ", and " : ", ");
                line.append(Component.literal(separator));
            }
            line.append(names.get(i));
        }
        return line;
    }

    private static List<WiseLessonRules.BossHint> hintsOf(ServerPlayer player) {
        List<WiseLessonRules.BossHint> stored = player.getAttached(EhmAttachments.EHM_WISE_BOSS_HINTS);
        if (stored == null || stored.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(stored);
    }

    private static WiseLessonRules.BossHint findHint(List<WiseLessonRules.BossHint> stored, String teacherId) {
        for (WiseLessonRules.BossHint hint : stored) {
            if (teacherId.equals(hint.teacherId())) {
                return hint;
            }
        }
        return null;
    }

    private static List<WiseLessonRules.BossHint> replaceHint(
            List<WiseLessonRules.BossHint> stored, WiseLessonRules.BossHint hint) {
        List<WiseLessonRules.BossHint> next = new ArrayList<>();
        boolean replaced = false;
        for (WiseLessonRules.BossHint prior : stored) {
            if (hint.teacherId().equals(prior.teacherId())) {
                if (!replaced) {
                    next.add(hint);
                    replaced = true;
                }
                continue;
            }
            next.add(prior);
        }
        if (!replaced) {
            next.add(hint);
        }
        return next;
    }

    private static WiseLessonRules.Lesson find(List<WiseLessonRules.Lesson> board, String teacherId) {
        for (WiseLessonRules.Lesson lesson : board) {
            if (teacherId.equals(lesson.teacherId())) {
                return lesson;
            }
        }
        return null;
    }

    private static String lineFor(ServerPlayer player, WiseLessonRules.Lesson lesson) {
        String dimension = player.level().dimension().identifier().toString();
        boolean near = WiseLessonRules.atRange(
                lesson.dimension(),
                dimension,
                lesson.originX(),
                lesson.originZ(),
                player.getX(),
                player.getZ(),
                lesson.distance());
        return WiseLessonRules.statusLine(
                lesson.teacherName(), WiseLessonRules.marksLeft(lesson), lesson.plural(), lesson.direction(), near);
    }

    private static void publish(ServerPlayer player, List<WiseLessonRules.Lesson> lessons) {
        String dimension = player.level().dimension().identifier().toString();
        List<String> lines = WiseLessonRules.hudLines(lessons, dimension, player.getX(), player.getZ());
        String text = String.join("\n", lines);
        if (text.equals(LAST_HUD.get(player.getUUID()))) {
            return;
        }
        LAST_HUD.put(player.getUUID(), text);
        ServerPlayNetworking.send(player, ClientboundWiseLessonPayload.from(lines));
    }
}

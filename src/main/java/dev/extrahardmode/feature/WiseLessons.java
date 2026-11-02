package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.network.ClientboundWiseLessonPayload;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.InhabitantData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.state.BlockState;

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
    private static final int EMERALD_STACK = 64;
    /** Outline lasts this long and is put back while the mark is loaded. */
    private static final int MARK_GLOW_TICKS = 20 * 60 * 10;

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
            return;
        }
        if (existing != null && !WiseLessonRules.offerDue(existing.assignedDay(), today)) {
            if (existing.active()) {
                player.sendSystemMessage(Component.literal(lineFor(player, existing)));
            }
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
                        String mark = trySpawn(player, level, updated, today, lead);
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
                continue;
            }
            showMark(living);
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

    /** One diamond, and emeralds from 10% through 20% of this creature's health. */
    public static void dropLeadTreasure(LivingEntity mob, ServerLevel level) {
        if (mob == null || level == null || !isLead(mob)) {
            return;
        }
        mob.spawnAtLocation(level, new ItemStack(Items.DIAMOND));
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

    /** White outline so a hunt creature is not mistaken for an ordinary one. */
    private static void showMark(LivingEntity living) {
        if (living == null) {
            return;
        }
        MobEffectInstance current = living.getEffect(MobEffects.GLOWING);
        if (current != null && current.getDuration() > 200) {
            return;
        }
        living.addEffect(new MobEffectInstance(MobEffects.GLOWING, MARK_GLOW_TICKS, 0, false, false, false));
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
            ServerPlayer player, ServerLevel level, WiseLessonRules.Lesson lesson, long today, boolean lead) {
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
        } else {
            bolster(mob, random.nextInt());
        }
        showMark(mob);
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

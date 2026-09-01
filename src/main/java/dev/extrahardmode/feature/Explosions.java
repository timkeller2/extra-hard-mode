package dev.extrahardmode.feature;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.api.ExplosionType;
import dev.extrahardmode.api.event.EhmExplosionEvent;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.ExplosionConfig;
import dev.extrahardmode.config.ExplosionSettings;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.module.PhysicsQueue;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.tag.EhmTags;
import dev.extrahardmode.task.CreateExplosionTask;
import dev.extrahardmode.world.PhysicsSkip;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class Explosions implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("explosions");

    private static final ThreadLocal<ExplosionType> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> REPLACING = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> ALLOW_WORLD_DAMAGE = new ThreadLocal<>();
    private static final Map<Identifier, ArrayDeque<CreateExplosionTask>> DELAYED = new ConcurrentHashMap<>();
    private static final Map<Identifier, List<OreBreak>> PENDING_ORES = new ConcurrentHashMap<>();

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void onWorldUnload(ServerLevel level) {
        Identifier id = level.dimension().identifier();
        DELAYED.remove(id);
        PENDING_ORES.remove(id);
    }

    @Override
    public void serverTick(ServerLevel level) {
        ArrayDeque<CreateExplosionTask> queue = DELAYED.get(level.dimension().identifier());
        if (queue == null || queue.isEmpty()) {
            return;
        }
        int snapshot = queue.size();
        for (int i = 0; i < snapshot && !queue.isEmpty(); i++) {
            CreateExplosionTask task = queue.pollFirst();
            if (task == null) {
                break;
            }
            if (!task.tick()) {
                queue.addLast(task);
            }
        }
    }

    public static ExplosionType classify(Entity source) {
        ExplosionType current = CURRENT.get();
        if (current != null) {
            return current;
        }
        if (source == null) {
            return null;
        }
        if (source instanceof PrimedTnt || source instanceof MinecartTNT || source.is(EntityTypes.TNT)) {
            return ExplosionType.TNT;
        }
        if (source instanceof Creeper creeper) {
            return creeper.isPowered() ? ExplosionType.CREEPER_CHARGED : ExplosionType.CREEPER;
        }
        if (source instanceof LargeFireball fireball && fireball.getOwner() instanceof Ghast) {
            return ExplosionType.GHAST_FIREBALL;
        }
        return null;
    }

    /**
     * Rewrite a vanilla explosion to RootNode power/fire/world-damage, then schedule TNT craters.
     * Recurses through {@link ServerLevel#explode} so the client packet matches the new power.
     */
    public static void interceptLevelExplode(
            ServerLevel level, Entity source, double x, double y, double z, CallbackInfo ci) {
        if (Boolean.TRUE.equals(REPLACING.get()) || CURRENT.get() != null) {
            return;
        }
        ExplosionType type = classify(source);
        ExplosionConfig config = ConfigManager.world(level).explosions();
        if (type == null || !config.custom(type)) {
            return;
        }
        if (type == ExplosionType.TNT && !level.getGameRules().get(GameRules.TNT_EXPLODES)) {
            return;
        }
        ci.cancel();
        ExplosionSettings.Applied applied = config.applied(type, y);
        boolean worldDamage = applied.worldDamage();
        if (type.isMob() && !level.getGameRules().get(GameRules.MOB_GRIEFING)) {
            worldDamage = false;
        }
        boolean fire = ExplosionSettings.allowFire(false, worldDamage, applied.fire());
        ALLOW_WORLD_DAMAGE.remove();
        REPLACING.set(Boolean.TRUE);
        try {
            level.explode(source, x, y, z, applied.power(), fire, interactionFor(type, worldDamage));
        } finally {
            REPLACING.remove();
        }
        boolean allowBlocks = Boolean.TRUE.equals(ALLOW_WORLD_DAMAGE.get());
        ALLOW_WORLD_DAMAGE.remove();
        if (ExplosionSettings.shouldScheduleCraters(
                type == ExplosionType.TNT && config.tntMultiple(), allowBlocks && worldDamage)) {
            scheduleCraters(level, new Vec3(x, y, z), liveSource(source));
        }
    }

    /** Later modules (creepers, blazes, witches) and TNT crater tasks call this. */
    public static void create(ServerLevel level, Vec3 origin, ExplosionType type, Entity source) {
        createFromModule(level, ID, origin, type, source);
    }

    /**
     * Custom blast gated on {@code moduleId} (e.g. creepers charged-on-damage), not
     * {@link #ID}. Passes {@code source} even if already removed so the event can
     * name the creeper.
     */
    public static void createFromModule(
            ServerLevel level, Identifier moduleId, Vec3 origin, ExplosionType type, Entity source) {
        if (!FeatureBus.guard((Level) level, moduleId)) {
            return;
        }
        explode(level, origin, type, source);
    }

    private static void explode(ServerLevel level, Vec3 origin, ExplosionType type, Entity source) {
        if (type == ExplosionType.TNT && !level.getGameRules().get(GameRules.TNT_EXPLODES)) {
            return;
        }
        ExplosionConfig config = ConfigManager.world(level).explosions();
        ExplosionSettings.Applied applied = config.applied(type, origin.y);
        boolean worldDamage = applied.worldDamage();
        if (type.isMob() && !level.getGameRules().get(GameRules.MOB_GRIEFING)) {
            worldDamage = false;
        }
        boolean fire = ExplosionSettings.allowFire(false, worldDamage, applied.fire());
        CURRENT.set(type);
        try {
            level.explode(
                    source,
                    origin.x,
                    origin.y,
                    origin.z,
                    applied.power(),
                    fire,
                    interactionFor(type, worldDamage));
        } finally {
            CURRENT.remove();
        }
    }

    public static void schedule(ServerLevel level, Vec3 origin, ExplosionType type, Entity source, int delayTicks) {
        if (delayTicks <= 0) {
            create(level, origin, type, source);
            return;
        }
        DELAYED.computeIfAbsent(level.dimension().identifier(), id -> new ArrayDeque<>())
                .addLast(new CreateExplosionTask(level, origin, type, liveSource(source), delayTicks));
    }

    /**
     * Fabric 0.158.0+26.2 has no explosion callback; fire ours at {@code explode} HEAD.
     * Cancel / {@code worldDamage=false} → KEEP (entity damage still runs).
     */
    public static void applyEvent(ServerLevel level, Explosion explosion, Entity source, boolean fire) {
        ExplosionType type = classify(source);
        ExplosionConfig config = ConfigManager.world(level).explosions();
        if (type == null && !config.otherModExplosions()) {
            return;
        }
        if (type == null) {
            type = ExplosionType.EFFECT;
        }
        boolean worldDamage = explosion.getBlockInteraction() != Explosion.BlockInteraction.KEEP;
        if (type.isMob() && !level.getGameRules().get(GameRules.MOB_GRIEFING)) {
            worldDamage = false;
        }
        EhmExplosionEvent event = new EhmExplosionEvent(
                level, explosion.center(), type, explosion.radius(), fire, worldDamage, source);
        EhmExplosionEvent.EVENT.invoker().onEhmExplosion(event);
        boolean allowBlocks = !event.isCanceled() && event.worldDamage();
        ALLOW_WORLD_DAMAGE.set(allowBlocks);
        if (!allowBlocks) {
            ((BlockInteractionMutator) explosion).extrahardmode$setBlockInteraction(Explosion.BlockInteraction.KEEP);
        }
        ((RadiusMutator) explosion).extrahardmode$setRadius(event.power());
        ((FireMutator) explosion)
                .extrahardmode$setFire(ExplosionSettings.allowFire(event.isCanceled(), event.worldDamage(), event.fire()));
    }

    public static void beforeBlocks(ServerLevel level, Explosion explosion, List<BlockPos> positions) {
        if (explosion.getBlockInteraction() == Explosion.BlockInteraction.KEEP) {
            return;
        }
        ExplosionType type = classify(explosion.getDirectSourceEntity());
        ExplosionConfig config = ConfigManager.world(level).explosions();
        if (type == null && !config.otherModExplosions()) {
            return;
        }
        WorldConfig world = ConfigManager.world(level);
        List<OreBreak> ores = pendingOres(level);
        ores.clear();
        Iterator<BlockPos> iterator = positions.iterator();
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            if (PhysicsSkip.never(level, pos) || PhysicsSkip.notReady(level, pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.is(EhmTags.CAVE_IN_ORES)) {
                ores.add(new OreBreak(pos.immutable(), state));
            }
            if (config.turnStoneToCobble()) {
                BlockState cobble = CaveIns.soften(world, state);
                if (cobble != null) {
                    PhysicsQueue.of(level).enqueueConvert(level, pos, state, cobble, world.caveInsApplyPhysics());
                    iterator.remove();
                    continue;
                }
            }
            if (config.flyingBlocks() && state.isSolid() && !state.liquid()) {
                if (level.getRandom().nextInt(100) < config.flyingPercent()) {
                    PhysicsQueue.of(level)
                            .enqueueFlying(
                                    level,
                                    pos,
                                    state,
                                    explosion.center(),
                                    config.upVelocity(),
                                    config.spreadVelocity());
                    iterator.remove();
                }
            }
        }
    }

    public static void afterBlocks(ServerLevel level) {
        List<OreBreak> ores = pendingOres(level);
        if (ores.isEmpty()) {
            return;
        }
        for (OreBreak ore : ores) {
            CaveIns.onOreBroken(level, ore.pos, ore.state);
        }
        ores.clear();
    }

    public static boolean discardFlyingIfFar(FallingBlockEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return false;
        }
        Vec3 origin = entity.getAttached(EhmAttachments.EHM_FLY_ORIGIN);
        if (origin == null) {
            return false;
        }
        if (!entity.onGround()) {
            return false;
        }
        double radius = ConfigManager.world(level).explosions().autoremoveRadius();
        if (entity.position().distanceTo(origin) <= radius) {
            return false;
        }
        entity.disableDrop();
        PhysicsQueue.of(level).markLanded(entity);
        entity.discard();
        return true;
    }

    private static void scheduleCraters(ServerLevel level, Vec3 origin, Entity source) {
        BlockPos pos = BlockPos.containing(origin);
        int random1 = Math.floorMod((int) level.getOverworldClockTime() + pos.getZ(), 8);
        int random2 = Math.floorMod((int) level.getOverworldClockTime() + pos.getX(), 8);
        Vec3[] nearby = {
            origin.add(random1, 1.0, random2),
            origin.add(-random2, 0.0, random1 / 2.0),
            origin.add(-random1 / 2.0, -1.0, -random2)
        };
        ArrayDeque<CreateExplosionTask> queue =
                DELAYED.computeIfAbsent(level.dimension().identifier(), id -> new ArrayDeque<>());
        Entity live = liveSource(source);
        for (int i = 0; i < nearby.length; i++) {
            queue.addLast(new CreateExplosionTask(level, nearby[i], ExplosionType.TNT, live, 3 * (i + 1)));
        }
    }

    private static Entity liveSource(Entity source) {
        return source == null || source.isRemoved() ? null : source;
    }

    private static Level.ExplosionInteraction interactionFor(ExplosionType type, boolean worldDamage) {
        if (!worldDamage || type == ExplosionType.EFFECT) {
            return Level.ExplosionInteraction.NONE;
        }
        return type == ExplosionType.TNT ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.MOB;
    }

    private static List<OreBreak> pendingOres(ServerLevel level) {
        return PENDING_ORES.computeIfAbsent(level.dimension().identifier(), id -> new ArrayList<>());
    }

    private record OreBreak(BlockPos pos, BlockState state) {}

    public interface RadiusMutator {
        void extrahardmode$setRadius(float radius);
    }

    public interface FireMutator {
        void extrahardmode$setFire(boolean fire);
    }

    public interface BlockInteractionMutator {
        void extrahardmode$setBlockInteraction(Explosion.BlockInteraction interaction);
    }
}

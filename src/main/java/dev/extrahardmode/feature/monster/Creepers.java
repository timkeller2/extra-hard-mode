package dev.extrahardmode.feature.monster;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.api.EhmApi;
import dev.extrahardmode.api.ExplosionType;
import dev.extrahardmode.api.event.CreeperDropTntEvent;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.MonsterConfig;
import dev.extrahardmode.feature.Explosions;
import dev.extrahardmode.feature.FeatureBus;
import dev.extrahardmode.feature.FeatureModule;
import dev.extrahardmode.module.EntityHelper;
import dev.extrahardmode.module.SpawnReplaceService;
import dev.extrahardmode.task.CoolCreeperExplosion;
import dev.extrahardmode.world.WorldGate;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

/**
 * Charged NATURAL spawn %, charged explode on damage, primed TNT on death,
 * burning creeper fireworks. Custom blasts go through {@link Explosions}.
 */
public final class Creepers implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("creepers");

    private static final Map<Identifier, ArrayDeque<CoolCreeperExplosion>> BURNING = new ConcurrentHashMap<>();

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        SpawnReplaceService.register(EntityTypes.CREEPER, Creepers::rollCharged);
        bus.listen(ServerLivingEntityEvents.ALLOW_DAMAGE, ID, Creepers::onAllowDamage);
        bus.listen(ServerLivingEntityEvents.AFTER_DAMAGE, ID, Creepers::onAfterDamage);
        bus.listen(ServerLivingEntityEvents.AFTER_DEATH, ID, Creepers::onDeath);
    }

    @Override
    public void onWorldUnload(ServerLevel level) {
        BURNING.remove(level.dimension().identifier());
    }

    @Override
    public void serverTick(ServerLevel level) {
        ArrayDeque<CoolCreeperExplosion> queue = BURNING.get(level.dimension().identifier());
        if (queue == null || queue.isEmpty()) {
            return;
        }
        int snapshot = queue.size();
        for (int i = 0; i < snapshot && !queue.isEmpty(); i++) {
            CoolCreeperExplosion task = queue.pollFirst();
            if (task == null) {
                break;
            }
            if (!task.tick()) {
                queue.addLast(task);
            }
        }
    }

    public static boolean enabled(Level level) {
        return FeatureBus.guard(level, ID);
    }

    public static net.minecraft.world.entity.EntityType<?> rollCharged(Mob original, ServerLevel level) {
        if (!WorldGate.isModuleActive(level, ID)) {
            return null;
        }
        if (!(original instanceof Creeper creeper)) {
            return null;
        }
        int percent = ConfigManager.world(level).monsters().chargedPercent();
        if (EntityHelper.percent(level.getRandom(), percent)) {
            ((PoweredMutator) creeper).extrahardmode$setPowered(true);
        }
        return null;
    }

    private static boolean onAllowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof Creeper creeper) || !(entity.level() instanceof ServerLevel level)) {
            return true;
        }
        if (!enabled(level) || creeper.isRemoved() || !creeper.isPowered()) {
            return true;
        }
        MonsterConfig config = ConfigManager.world(level).monsters();
        if (!config.chargedExplodeOnDamage()) {
            return true;
        }
        Player player = playerFrom(source);
        if (player instanceof ServerPlayer serverPlayer && EhmApi.playerBypasses(serverPlayer)) {
            return true;
        }
        if (creeper.getTarget() == null && player == null) {
            return true;
        }
        EntityHelper.markLootless(creeper);
        var origin = creeper.position();
        creeper.discard();
        Explosions.schedule(level, origin, ExplosionType.CREEPER_CHARGED, null, 0);
        return false;
    }

    private static void onAfterDamage(
            LivingEntity entity, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked) {
        if (!(entity instanceof Creeper creeper) || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (!enabled(level) || creeper.isRemoved()) {
            return;
        }
        MonsterConfig config = ConfigManager.world(level).monsters();
        if (!config.fireExplosion()) {
            return;
        }
        if (!source.is(DamageTypeTags.IS_FIRE)) {
            return;
        }
        if (creeper.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            return;
        }
        if (EntityHelper.ignored(creeper)) {
            return;
        }
        EntityHelper.markIgnored(creeper);
        BURNING.computeIfAbsent(level.dimension().identifier(), id -> new ArrayDeque<>())
                .addLast(new CoolCreeperExplosion(level, creeper, config));
    }

    private static void onDeath(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof Creeper creeper) || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (!enabled(level)) {
            return;
        }
        MonsterConfig config = ConfigManager.world(level).monsters();
        if (config.dropTntPercent() <= 0) {
            return;
        }
        if (entity.getY() > config.dropTntMaxY()) {
            return;
        }
        if (!EntityHelper.percent(level.getRandom(), config.dropTntPercent())) {
            return;
        }
        ServerPlayer killer = entity.getKillCredit() instanceof ServerPlayer player ? player : null;
        CreeperDropTntEvent event = new CreeperDropTntEvent(killer, creeper, creeper.position());
        CreeperDropTntEvent.EVENT.invoker().onCreeperDropTnt(event);
        if (event.isCanceled()) {
            return;
        }
        PrimedTnt tnt = EntityTypes.TNT.create(level, EntitySpawnReason.EVENT);
        if (tnt == null) {
            return;
        }
        tnt.snapTo(event.location().x, event.location().y, event.location().z);
        level.addFreshEntity(tnt);
        if (config.creeperTntWarning()) {
            level.playSound(null, creeper.blockPosition(), SoundEvents.GHAST_WARN, SoundSource.HOSTILE, 1.0F, 1.0F);
        }
    }

    public interface PoweredMutator {
        void extrahardmode$setPowered(boolean powered);
    }

    private static Player playerFrom(DamageSource source) {
        Entity entity = source.getEntity();
        if (entity instanceof Player player) {
            return player;
        }
        if (entity instanceof Projectile projectile && projectile.getOwner() instanceof Player player) {
            return player;
        }
        return null;
    }
}

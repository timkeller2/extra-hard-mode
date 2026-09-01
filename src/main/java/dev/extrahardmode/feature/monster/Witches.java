package dev.extrahardmode.feature.monster;

import dev.extrahardmode.ExtraHardModeMod;
import dev.extrahardmode.config.ConfigManager;
import dev.extrahardmode.config.WorldConfig;
import dev.extrahardmode.feature.FeatureBus;
import dev.extrahardmode.feature.FeatureModule;
import dev.extrahardmode.module.SpawnReplaceService;
import dev.extrahardmode.player.EhmAttachments;
import dev.extrahardmode.world.WorldGate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Extra witch splash attacks matching original {@code Witches.java}: 30/30/30/10
 * baby-or-explode / teleport / visual explosion + 3 armor-ignoring damage /
 * vanilla effects on players only. Bonus 5% grass-zombie → witch via
 * {@link SpawnReplaceService}.
 */
public final class Witches implements FeatureModule {
    public static final Identifier ID = ExtraHardModeMod.id("witches");
    public static final float EXPLOSION_DAMAGE = 3.0F;

    public enum SplashAttack {
        BABY_OR_EXPLODE,
        TELEPORT,
        EXPLODE,
        VANILLA_POISON
    }

    @Override
    public Identifier id() {
        return ID;
    }

    @Override
    public void bootstrap(FeatureBus bus) {
        SpawnReplaceService.register(EntityTypes.ZOMBIE, Witches::rollWitchReplace);
    }

    public static SplashAttack attackFor(int roll) {
        if (roll < 30) {
            return SplashAttack.BABY_OR_EXPLODE;
        }
        if (roll < 60) {
            return SplashAttack.TELEPORT;
        }
        if (roll < 90) {
            return SplashAttack.EXPLODE;
        }
        return SplashAttack.VANILLA_POISON;
    }

    /**
     * @return true when vanilla splash application should be cancelled
     */
    public static boolean onSplash(ThrownSplashPotion potion, ServerLevel level, ItemStack stack, HitResult hit) {
        if (!WorldGate.isModuleActive(level, ID)) {
            return false;
        }
        WorldConfig config = ConfigManager.world(level);
        if (!config.witchesAdditionalAttacks()) {
            return false;
        }
        if (!(potion.getOwner() instanceof Witch witch) || witch.isRemoved()) {
            return false;
        }
        SplashAttack attack = attackFor(witch.getRandom().nextInt(100));
        List<LivingEntity> splash = splashTargets(potion, level, hit);
        return switch (attack) {
            case BABY_OR_EXPLODE -> {
                if (!spawnBabyZombie(level, witch, hit.getLocation())) {
                    explode(potion, witch, level, hit.getLocation(), splash);
                }
                yield true;
            }
            case TELEPORT -> {
                Vec3 to = hit.getLocation();
                witch.teleportTo(level, to.x, to.y, to.z, Set.of(), witch.getYRot(), witch.getXRot(), true);
                yield true;
            }
            case EXPLODE -> {
                explode(potion, witch, level, hit.getLocation(), splash);
                yield true;
            }
            case VANILLA_POISON -> {
                applyVanillaToPlayers(potion, level, stack, splash);
                yield true;
            }
        };
    }

    static EntityType<?> rollWitchReplace(Mob original, ServerLevel level) {
        if (!WorldGate.isModuleActive(level, ID)) {
            return null;
        }
        if (level.dimension() != Level.OVERWORLD) {
            return null;
        }
        int percent = ConfigManager.world(level).witchesBonusSpawnPercent();
        if (percent <= 0) {
            return null;
        }
        if (!level.getBlockState(original.blockPosition().below()).is(Blocks.GRASS_BLOCK)) {
            return null;
        }
        if (original.getRandom().nextInt(100) >= percent) {
            return null;
        }
        return EntityTypes.WITCH;
    }

    static boolean spawnBabyZombie(ServerLevel level, Witch witch, Vec3 at) {
        if (babyZombieInChunk(level, BlockPos.containing(at))) {
            return false;
        }
        Entity spawned = EntityTypes.ZOMBIE_VILLAGER.spawn(level, BlockPos.containing(at), EntitySpawnReason.EVENT);
        if (!(spawned instanceof ZombieVillager zombie)) {
            return false;
        }
        zombie.setBaby(true);
        zombie.setAttached(EhmAttachments.EHM_SPAWN_PROCESSED, true);
        zombie.setAttached(EhmAttachments.EHM_LOOTLESS, true);
        zombie.skipDropExperience();
        LivingEntity target = witch.getTarget();
        if (target != null && target.isAlive()) {
            zombie.setTarget(target);
        }
        return true;
    }

    static boolean babyZombieInChunk(ServerLevel level, BlockPos pos) {
        ChunkPos chunk = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
        AABB box = new AABB(
                chunk.getMinBlockX(),
                level.getMinY(),
                chunk.getMinBlockZ(),
                chunk.getMaxBlockX() + 1,
                level.getMaxY() + 1,
                chunk.getMaxBlockZ() + 1);
        return !level.getEntitiesOfClass(ZombieVillager.class, box, ZombieVillager::isBaby).isEmpty();
    }

    static void explode(ThrownSplashPotion potion, Witch witch, ServerLevel level, Vec3 at, List<LivingEntity> splash) {
        level.playSound(
                witch, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
        DamageSource magic = level.damageSources().indirectMagic(potion, witch);
        for (LivingEntity target : splash) {
            if (target instanceof ServerPlayer player && player.isAlive()) {
                player.hurtServer(level, magic, EXPLOSION_DAMAGE);
            }
        }
    }

    static void applyVanillaToPlayers(
            ThrownSplashPotion potion, ServerLevel level, ItemStack stack, List<LivingEntity> splash) {
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        Entity source = potion.getEffectSource();
        for (LivingEntity target : splash) {
            if (!(target instanceof Player) || !target.isAffectedByPotions()) {
                continue;
            }
            for (MobEffectInstance instance : contents.getAllEffects()) {
                MobEffect effect = instance.getEffect().value();
                if (effect.isInstantaneous()) {
                    effect.applyInstantaneousEffect(
                            level, potion, potion.getOwner(), target, instance.getAmplifier(), 1.0);
                } else {
                    target.addEffect(new MobEffectInstance(instance), source);
                }
            }
        }
    }

    static List<LivingEntity> splashTargets(ThrownSplashPotion potion, ServerLevel level, HitResult hit) {
        AABB potionBox = potion.getBoundingBox().move(hit.getLocation().subtract(potion.position()));
        AABB inflated = potionBox.inflate(4.0, 2.0, 4.0);
        float margin = ProjectileUtil.computeMargin(potion);
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, inflated)) {
            if (!entity.isAffectedByPotions()) {
                continue;
            }
            if (potionBox.inflate(margin).distanceToSqr(entity.getBoundingBox()) < 16.0) {
                out.add(entity);
            }
        }
        return out;
    }
}

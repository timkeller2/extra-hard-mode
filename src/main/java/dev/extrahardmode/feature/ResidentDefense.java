package dev.extrahardmode.feature;

import dev.extrahardmode.player.EhmAttachments;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.AABB;

/** Health, regen, bow, and sword for residents. */
public final class ResidentDefense {
    private ResidentDefense() {}

    public static void tick(ServerLevel level, Villager villager, int housePoints) {
        applyHealth(villager, housePoints);
        regen(level, villager);
        equipArrows(villager);
        LivingEntity melee = meleeTarget(level, villager);
        if (melee != null && villager.distanceTo(melee) <= ResidentCombatRules.CHASE_RANGE) {
            hold(villager, Items.IRON_SWORD);
            villager.getLookControl().setLookAt(melee, 30.0F, 30.0F);
            if (villager.distanceTo(melee) <= ResidentCombatRules.MELEE_RANGE) {
                swing(level, villager, melee);
            } else if (villager.getNavigation() != null) {
                villager.getNavigation().moveTo(melee, 0.6);
            }
            return;
        }
        hold(villager, Items.BOW);
        Mob shooterTarget = shootTarget(level, villager);
        if (shooterTarget != null) {
            villager.getLookControl().setLookAt(shooterTarget, 30.0F, 30.0F);
            shoot(level, villager, shooterTarget);
        }
    }

    public static void noteHit(Villager villager, LivingEntity attacker) {
        villager.setAttached(EhmAttachments.EHM_RESIDENT_HEALTH_BEFORE, villager.getHealth());
        if (attacker instanceof Mob && !(attacker instanceof Player) && attacker.isAlive()) {
            villager.setAttached(EhmAttachments.EHM_RESIDENT_MELEE, villager.level().getGameTime());
            villager.setAttached(EhmAttachments.EHM_RESIDENT_ATTACKER, attacker.getUUID());
        }
    }

    public static float healthBeforeHit(Villager villager) {
        Float stored = villager.getAttached(EhmAttachments.EHM_RESIDENT_HEALTH_BEFORE);
        return stored == null ? villager.getHealth() : stored;
    }

    static void applyHealth(Villager villager, int housePoints) {
        AttributeInstance max = villager.getAttribute(Attributes.MAX_HEALTH);
        if (max == null) {
            return;
        }
        float desired = ResidentCombatRules.maxHealth(housePoints);
        float oldMax = (float) max.getBaseValue();
        if (Math.abs(oldMax - desired) < 0.05F) {
            return;
        }
        boolean full = villager.getHealth() >= oldMax - 0.1F;
        max.setBaseValue(desired);
        if (full) {
            villager.setHealth(desired);
        }
    }

    static void regen(ServerLevel level, Villager villager) {
        long now = level.getGameTime();
        long last = villager.getAttachedOrElse(EhmAttachments.EHM_RESIDENT_REGEN, -1L);
        if (last < 0L) {
            villager.setAttached(EhmAttachments.EHM_RESIDENT_REGEN, now);
            return;
        }
        if (now - last < ResidentCombatRules.REGEN_TICKS) {
            return;
        }
        villager.setAttached(EhmAttachments.EHM_RESIDENT_REGEN, now);
        if (villager.getHealth() < villager.getMaxHealth()) {
            villager.heal(1.0F);
        }
    }

    static LivingEntity meleeTarget(ServerLevel level, Villager villager) {
        long marked = villager.getAttachedOrElse(EhmAttachments.EHM_RESIDENT_MELEE, -1L);
        if (marked < 0L || level.getGameTime() - marked > ResidentCombatRules.MELEE_WINDOW) {
            return null;
        }
        UUID id = villager.getAttached(EhmAttachments.EHM_RESIDENT_ATTACKER);
        if (id == null || !(level.getEntity(id) instanceof LivingEntity living) || !living.isAlive()) {
            return null;
        }
        if (living instanceof Player) {
            return null;
        }
        return living;
    }

    static Mob shootTarget(ServerLevel level, Villager villager) {
        AABB box = villager.getBoundingBox().inflate(ResidentCombatRules.SHOOT_RANGE);
        List<Mob> mobs = level.getEntitiesOfClass(
                Mob.class,
                box,
                mob -> mob != villager
                        && mob.isAlive()
                        && mob.getTarget() == villager
                        && villager.distanceTo(mob) <= ResidentCombatRules.SHOOT_RANGE);
        Mob closest = null;
        double best = Double.MAX_VALUE;
        for (Mob mob : mobs) {
            double dist = villager.distanceToSqr(mob);
            if (dist < best) {
                best = dist;
                closest = mob;
            }
        }
        return closest;
    }

    static void shoot(ServerLevel level, Villager villager, LivingEntity target) {
        long now = level.getGameTime();
        long last = villager.getAttachedOrElse(EhmAttachments.EHM_RESIDENT_SHOOT, -1L);
        if (last >= 0L && now - last < ResidentCombatRules.SHOOT_COOLDOWN) {
            return;
        }
        villager.setAttached(EhmAttachments.EHM_RESIDENT_SHOOT, now);
        Arrow arrow = new Arrow(level, villager, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
        double dx = target.getX() - villager.getX();
        double dy = target.getEyeY() - villager.getEyeY();
        double dz = target.getZ() - villager.getZ();
        arrow.setPos(villager.getX(), villager.getEyeY() - 0.1, villager.getZ());
        arrow.shoot(dx, dy, dz, 1.6F, 8.0F);
        arrow.setBaseDamage(2.0 + ResidentCombatRules.BOW_BONUS);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        level.addFreshEntity(arrow);
        level.playSound(null, villager.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    static void swing(ServerLevel level, Villager villager, LivingEntity target) {
        long now = level.getGameTime();
        long last = villager.getAttachedOrElse(EhmAttachments.EHM_RESIDENT_SWING, -1L);
        if (last >= 0L && now - last < ResidentCombatRules.MELEE_COOLDOWN) {
            return;
        }
        villager.setAttached(EhmAttachments.EHM_RESIDENT_SWING, now);
        villager.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT);
        target.hurtServer(level, level.damageSources().mobAttack(villager), ResidentCombatRules.SWORD_DAMAGE);
    }

    static void hold(Villager villager, net.minecraft.world.item.Item item) {
        if (!villager.getMainHandItem().is(item)) {
            villager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item));
            villager.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        }
        equipArrows(villager);
    }

    static void equipArrows(Villager villager) {
        if (!villager.getOffhandItem().is(Items.ARROW)) {
            villager.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.ARROW, 64));
            villager.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        }
    }
}

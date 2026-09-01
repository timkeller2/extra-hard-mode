package dev.extrahardmode.module;

import dev.extrahardmode.player.EhmAttachments;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Shared EHM entity flags. Minecraft-free percent helper lives here for JUnit. */
public final class EntityHelper {
    private EntityHelper() {}

    public static boolean percent(RandomSource random, int percent) {
        return percent(random.nextInt(100), percent);
    }

    /** {@code roll} is 0–99 from {@code random.nextInt(100)}. */
    public static boolean percent(int roll, int percent) {
        if (percent <= 0) {
            return false;
        }
        if (percent >= 100) {
            return true;
        }
        return roll < percent;
    }

    public static boolean ignored(Entity entity) {
        return Boolean.TRUE.equals(entity.getAttachedOrElse(EhmAttachments.EHM_IGNORE, Boolean.FALSE));
    }

    public static void markIgnored(Entity entity) {
        entity.setAttached(EhmAttachments.EHM_IGNORE, Boolean.TRUE);
    }

    public static boolean lootless(Entity entity) {
        return Boolean.TRUE.equals(entity.getAttachedOrElse(EhmAttachments.EHM_LOOTLESS, Boolean.FALSE));
    }

    public static void markLootless(LivingEntity entity) {
        entity.setAttached(EhmAttachments.EHM_LOOTLESS, Boolean.TRUE);
        if (entity instanceof Mob mob) {
            for (var slot : net.minecraft.world.entity.EquipmentSlot.values()) {
                mob.setDropChance(slot, 0.0F);
            }
        }
    }

    public static int reanimateCount(Entity entity) {
        Integer value = entity.getAttached(EhmAttachments.EHM_REANIMATE_COUNT);
        return value == null ? 0 : value;
    }

    public static void setReanimateCount(Entity entity, int count) {
        entity.setAttached(EhmAttachments.EHM_REANIMATE_COUNT, count);
    }
}
